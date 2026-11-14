package io.github.vitalyostanin.geoshare.control

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Binder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.managed.ManagedConfiguration
import io.github.vitalyostanin.geoshare.permission.devicePermissions
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.service.SharingService
import io.github.vitalyostanin.geoshare.service.SharingSetting
import io.github.vitalyostanin.geoshare.service.refusalToStart
import io.github.vitalyostanin.geoshare.service.startConditions
import java.security.MessageDigest
import io.github.vitalyostanin.geoshare.control.SharingControlProtocol as Protocol

/**
 * Lets the application of the device policy controller (a device owner or MDM app) start, stop
 * and ask after sharing.
 *
 * Exported, because a service nobody else can bind is of no use to it; what keeps everybody else
 * out is the check in [callerIsTrusted], which does not look at a package name the caller gives
 * but at the uid the system says the message came from, and at the certificates the packages of
 * that uid are signed with. What is trusted comes only from the managed configuration; the
 * application carries no list of its own, so a device without a controller refuses everybody.
 *
 * A [Messenger] rather than an AIDL interface: the other application then needs no copy of an
 * interface file, only the numbers and keys of [SharingControlProtocol], and every call arrives
 * one at a time on the main thread. The reply goes to the caller's own [Message.replyTo].
 */
class SharingControlService : Service() {
    private val messenger by lazy {
        Messenger(
            Handler(Looper.getMainLooper()) { message ->
                handle(message)
                true
            },
        )
    }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    private fun handle(message: Message) {
        // The uid the system stamped on the message as it crossed the process boundary.
        // Binder.getCallingUid() is only the fallback: a Messenger hands the message to a
        // Handler, and outside the binder transaction itself that call answers with this
        // application's own uid, which is on nobody's list, so it fails closed.
        val uid = message.sendingUid.takeIf { it > 0 } ?: Binder.getCallingUid()

        val reply = if (!callerIsTrusted(uid)) {
            Protocol.Reply.Refused()
        } else {
            when (Protocol.requestOf(message.what)) {
                Protocol.Request.Start -> start()
                Protocol.Request.Stop -> stop()
                Protocol.Request.Status -> Protocol.Reply.State(status())
                null -> Protocol.Reply.Error("unknown_request")
            }
        }

        val answer = Message.obtain(null, Protocol.WHAT_REPLY, Protocol.resultOf(reply), 0)
        answer.data = bundleOf(Protocol.fields(reply))
        try {
            message.replyTo?.send(answer)
        } catch (e: android.os.RemoteException) {
            AppLog.w(TAG, "the answer to uid $uid could not be sent", e)
        }
    }

    /**
     * Turns sharing on and starts the service if the device allows it. The switch is set first
     * so that a start the platform or a missing permission holds back is still carried out when
     * the member fixes it. The start counts as one from the background, since the controller's
     * application is not a screen of ours, and the platform judges it so.
     */
    private fun start(): Protocol.Reply {
        SharingSetting(this).enabled = true
        val refusal = refusalToStart(startConditions(this, fromBackground = true))
            ?: return try {
                SharingService.start(this)
                Protocol.Reply.Ok
            } catch (e: IllegalStateException) {
                // ForegroundServiceStartNotAllowedException is one; the class does not exist on
                // the older versions this still runs on.
                AppLog.w(TAG, "the platform refused the start", e)
                Protocol.Reply.Error(Protocol.ERROR_START_REFUSED_BY_PLATFORM)
            }
        return Protocol.Reply.Refused(refusal)
    }

    /**
     * Stops sharing. Allowed whatever the managed configuration says about locking the member's
     * switch: the lock is there to keep the member from turning sharing off, not the controller.
     */
    private fun stop(): Protocol.Reply {
        SharingSetting(this).enabled = false
        stopService(Intent(this, SharingService::class.java))
        return Protocol.Reply.Ok
    }

    private fun status(): Protocol.Status {
        val permissions = devicePermissions(this)
        return Protocol.Status(
            sharingWanted = SharingSetting(this).enabled,
            groupConnected = Connections.connected,
            // As in startConditions: one group with a session is reason enough.
            signedIn = Connections.all.value.any { it.auth.currentUser != null },
            locationGranted = permissions.locationGranted,
            backgroundLocationGranted = permissions.backgroundLocationGranted,
            notificationsGranted = permissions.notificationsGranted,
            serviceRunning = SharingService.running,
        )
    }

    private fun callerIsTrusted(uid: Int): Boolean {
        val list = ManagedConfiguration.current.trustedControllers
        if (list.isEmpty()) return false

        val names = packageManager.getPackagesForUid(uid)?.toList().orEmpty()
        val trusted = Protocol.callerTrusted(
            list,
            names.associateWith { certificateDigests(this, it) },
        )
        if (!trusted) {
            // Written with the package named, so a controller whose digest is wrong in the
            // configuration can be seen to be the one asking.
            AppLog.w(TAG, "control refused for uid $uid, packages ${names.ifEmpty { "none" }}")
        }
        return trusted
    }

    private companion object {
        const val TAG = "SharingControl"

        fun bundleOf(fields: Map<String, Any>): Bundle = Bundle().apply {
            for ((key, value) in fields) {
                when (value) {
                    is Boolean -> putBoolean(key, value)
                    is String -> putString(key, value)
                }
            }
        }

        /**
         * The SHA-256 digests (hex) of the certificates [packageName] is signed with. From
         * Android 9 that is the signers of the package, or, for a single signer, its whole
         * history, so that a controller that rotated its key is still known by the old
         * certificate in the configuration. Before that the signatures are all there is. A
         * package the system will not describe has no digests.
         */
        @Suppress("DEPRECATION")
        fun certificateDigests(context: Context, packageName: String): List<String> {
            val manager = context.packageManager
            val signatures = try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    val info: PackageInfo = manager.getPackageInfo(
                        packageName,
                        PackageManager.GET_SIGNING_CERTIFICATES,
                    )
                    val signing = info.signingInfo ?: return emptyList()
                    if (signing.hasMultipleSigners()) {
                        signing.apkContentsSigners
                    } else {
                        signing.signingCertificateHistory
                    }
                } else {
                    manager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES).signatures
                }
            } catch (e: PackageManager.NameNotFoundException) {
                return emptyList()
            } ?: return emptyList()

            return signatures.map { signature ->
                MessageDigest.getInstance("SHA-256").digest(signature.toByteArray())
                    .joinToString("") { "%02x".format(it) }
            }
        }
    }
}
