package io.github.vitalyostanin.geoshare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.google.firebase.firestore.FirebaseFirestoreException
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.download.DownloadSetting
import io.github.vitalyostanin.geoshare.map.SettingsActions
import io.github.vitalyostanin.geoshare.map.logFailure
import io.github.vitalyostanin.geoshare.map.publishSharing
import io.github.vitalyostanin.geoshare.map.runCatchingFailure
import io.github.vitalyostanin.geoshare.map.withShare
import io.github.vitalyostanin.geoshare.map.withinAnswerWait
import io.github.vitalyostanin.geoshare.map.writePhoneName
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.membership.ownProjectGroup
import io.github.vitalyostanin.geoshare.project.Connections
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.project.RefusedJoin
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import io.github.vitalyostanin.geoshare.service.askReset
import io.github.vitalyostanin.geoshare.service.callSiren
import io.github.vitalyostanin.geoshare.service.deviceIdOf
import io.github.vitalyostanin.geoshare.service.endSearch
import io.github.vitalyostanin.geoshare.service.forgetAidingData
import io.github.vitalyostanin.geoshare.service.foundGroup
import io.github.vitalyostanin.geoshare.service.invitationLive
import io.github.vitalyostanin.geoshare.service.joinGroup
import io.github.vitalyostanin.geoshare.service.makeInvitation
import io.github.vitalyostanin.geoshare.service.openSearch
import io.github.vitalyostanin.geoshare.service.phoneReset
import io.github.vitalyostanin.geoshare.service.phoneSiren
import io.github.vitalyostanin.geoshare.service.readProjectMark
import io.github.vitalyostanin.geoshare.service.refusedOutright
import io.github.vitalyostanin.geoshare.service.removeFromGroup
import io.github.vitalyostanin.geoshare.service.renameGroup
import io.github.vitalyostanin.geoshare.service.renewSearch
import io.github.vitalyostanin.geoshare.service.setRunning
import io.github.vitalyostanin.geoshare.service.stopSiren
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * What the group's writes say back to the screen, where nothing in the
 * database says it.
 */
internal class GroupAnswers {
    /**
     * The invitation just made. Kept on the screen rather than in the
     * database: the code is written where nobody may read it back, this device
     * included, so the only copy to show is the one just made.
     */
    var invitation by mutableStateOf<MadeInvitation?>(null)

    /**
     * Whether the last write to the group was refused: the person who pressed
     * the button is owed an answer.
     */
    var groupTrouble by mutableStateOf(false)

    /**
     * Whether the rules refused the founding of the group, which they allow
     * only to the project owner (ADR-0038) and only while no group exists:
     * the screen says so in place of the general line. Cleared by every
     * other write.
     */
    var foundRefused by mutableStateOf(false)

    /**
     * The invitation this device came with, when the rules turned it down:
     * said on the map in place of an empty group, not as [groupTrouble].
     */
    var refusedJoin by mutableStateOf<RefusedJoin?>(null)

    companion object {
        /**
         * Keeps the invitation across a recreated screen (a change of theme or
         * of orientation). Nothing in the database can give it back, so the
         * copy held here is the only one; the refusal and the trouble are
         * answers to a write and are left to be asked for again.
         */
        val Saver =
            listSaver<GroupAnswers, String>(
                save = { answers ->
                    answers.invitation?.let {
                        listOf(it.groupId, it.code, it.expiresAt.toString(), it.email)
                    } ?: emptyList()
                },
                restore = { saved ->
                    GroupAnswers().also { answers ->
                        if (saved.size == 4) {
                            answers.invitation =
                                MadeInvitation(
                                    groupId = saved[0],
                                    code = saved[1],
                                    expiresAt = Instant.parse(saved[2]),
                                    email = saved[3],
                                )
                        }
                    }
                },
            )
    }
}

/**
 * What the settings and the panels over the map can ask for, wired to this
 * device, this account and the group on the screen.
 *
 * [sharedWith] and [roster] are asked when an action runs rather than when it
 * is built, so that an action reads what the screen holds at that moment.
 */
internal fun settingsActions(
    activity: MainActivity,
    connection: GroupConnection,
    current: AuthState.SignedIn,
    answers: GroupAnswers,
    sharedWith: () -> Map<String, Instant>,
    roster: () -> Roster?,
    downloadSetting: DownloadSetting,
    onSharingOn: (Boolean) -> Unit,
    onJumpsFiltered: (Boolean) -> Unit,
    onSourcesLeftOut: (Set<String>) -> Unit,
    onTheme: (ThemeChoice) -> Unit,
    onStartupSet: (Boolean) -> Unit,
): SettingsActions = SettingsActions(
    onSharingChange = { wanted ->
        onSharingOn(wanted)
        activity.applySharing(wanted)
    },
    // Read by the service out of the same store, which is where it looks on
    // every fix that arrives.
    onJumpsFilteredChange = { wanted ->
        onJumpsFiltered(wanted)
        activity.jumps.enabled = wanted
    },
    // Left in the same store for the service to pick up and spend: the
    // request has to outlive this screen, since the device may not have a
    // fix to answer it with until minutes from now. The service reads the
    // same store on every answer it gets, so the choice takes effect at the
    // next fix rather than at the next restart.
    onSourceUseChange = { source, counted ->
        activity.sources.use(source, counted)
        onSourcesLeftOut(activity.sources.leftOut)
    },
    onDownloadNetworkChange = { network, allowed ->
        downloadSetting.allow(network, allowed)
    },
    onForgetPlace = { activity.jumps.askToForget() },
    // Done here and not in the service: the command is answered by the
    // receiver on the spot, and what it answered is what the row shows.
    onForgetAiding = {
        forgetAidingData(activity)
    },
    onThemeChange = { wanted ->
        onTheme(wanted)
        activity.appearance.choice = wanted
    },
    // Handed to the platform and not kept here: it stores the answer, tells
    // the system settings about it and builds the screens again in the new
    // language.
    onLanguageChange = { wanted -> activity.speak(wanted) },
    // Written into the grant, and left to be read back: the devices of a
    // member share the same permissions, so the answer has to come from
    // where all of them look.
    onShare = { readerId, span ->
        activity.lifecycleScope.launch {
            publishSharing(
                memberId = current.memberId,
                sharedWith =
                withShare(
                    sharedWith = sharedWith(),
                    readerId = readerId,
                    span = span,
                    now = Instant.now(),
                ),
                connection = connection,
            )
        }
    },
    // Read back through the devices of this member, like every other device
    // reads it.
    onPhoneNameChange = { name ->
        activity.lifecycleScope.launch {
            writePhoneName(
                current.memberId,
                activity.device.id,
                name,
                connection = connection,
            )
        }
    },
    onOpenSettings = activity::openApplicationSettings,
    onAllowBattery = activity::askBatteryExemption,
    // Read back in onStart, as the battery row is.
    onAllowSirenThroughSilence = activity::askSirenThroughSilence,
    onOpenStartup = activity::openStartupScreen,
    onStartupSetChange = { set ->
        onStartupSet(set)
        activity.backgroundWork.startupSet = set
    },
    // Refused when a group already exists, which is the answer to the
    // question the screen cannot ask: a device outside a group cannot read
    // the roster, so it cannot tell an empty household from one it is not
    // in.
    onFoundGroup = { name ->
        activity.lifecycleScope.launch {
            // Only the rules' own refusal gets the owner's line (ADR-0038);
            // anything else is the general one.
            var denied = false

            answers.groupTrouble =
                !activity.withGroup("found a group") {
                    try {
                        foundGroup(
                            activity,
                            current.memberId,
                            name,
                            connection = connection,
                        )
                    } catch (refusal: FirebaseFirestoreException) {
                        denied = refusedOutright(refusal)

                        throw refusal
                    }
                }
            answers.foundRefused = denied
        }
    },
    // Read when a screen without a group is shown, once: see readProjectMark.
    // A mark naming this account's own group moves the device into it, and
    // the screen follows the move rather than calling the project taken.
    onReadProjectMark = {
        readProjectMark(connection).also { mark ->
            ownProjectGroup(mark, current.memberId, connection.group)?.let { own ->
                Connections.moveTo(activity, connection.projectId, own)
            }
        }
    },
    onMakeInvitation = { email ->
        activity.lifecycleScope.launch {
            answers.foundRefused = false
            answers.groupTrouble =
                !activity.withGroup("made an invitation") {
                    answers.invitation =
                        makeInvitation(
                            current.memberId,
                            email,
                            connection = connection,
                        )
                }
        }
    },
    onJoinGroup = { invited ->
        activity.lifecycleScope.launch {
            answers.foundRefused = false
            answers.groupTrouble =
                !activity.withGroup("joined a group") {
                    joinGroup(
                        activity,
                        current.memberId,
                        invited,
                        connection = connection,
                    )
                }
        }
    },
    // A link typed or pasted here is the same thing a messenger hands over
    // as an intent, so it takes the same path: ConnectFlow reads it, and
    // the dialog above the map shows what it asks for.
    onPasteLink = activity.connectFlow::pasteLink,
    onRemoveMember = { memberId ->
        activity.lifecycleScope.launch {
            answers.foundRefused = false
            answers.groupTrouble =
                !activity.withGroup("removed a member") {
                    roster()?.let {
                        removeFromGroup(it, memberId, connection = connection)
                    }
                }
        }
    },
    onSetRunning = { memberId, runs ->
        activity.lifecycleScope.launch {
            answers.foundRefused = false
            answers.groupTrouble =
                !activity.withGroup("changed who runs the group") {
                    setRunning(memberId, runs, connection = connection)
                }
        }
    },
    // Called from the dialog's own coroutine, as onSearch and onCallSiren
    // are: it has to stay open on a refusal and say so.
    onRenameGroup = { name ->
        activity.writeOutcome("the group could not be renamed") {
            renameGroup(name, connection = connection)
        }
    },
    onSignOut = { everywhere ->
        activity.lifecycleScope.launch {
            if (everywhere) {
                activity.signOutEverywhere()
            } else {
                activity.signOutHere(connection)
            }
        }
    },
    // A suspend function called from the dialog's own coroutine rather than
    // wrapped in lifecycleScope.launch here: the dialog has to stay open on
    // a refusal and say so, which nothing fire-and-forget could tell it.
    onSearch = { phoneId, phoneName, renew, hours ->
        activity.writeOutcome("a search could not be written") {
            if (renew) {
                renewSearch(
                    phoneId,
                    phoneName,
                    current.memberId,
                    hours,
                    connection = connection,
                )
            } else {
                openSearch(
                    phoneId,
                    phoneName,
                    current.memberId,
                    hours,
                    connection = connection,
                )
            }
        }
    },
    sirenOf = { phoneId -> phoneSiren(phoneId, connection) },
    invitationLive = { code -> invitationLive(code, connection) },
    onCallSiren = { phoneId ->
        activity.writeOutcome("a siren could not be called") {
            callSiren(phoneId, current.memberId, connection = connection)
        }
    },
    resetOf = { phoneId -> phoneReset(phoneId, connection) },
    onAskReset = { phoneId ->
        activity.writeOutcome("a place reset could not be asked") {
            askReset(phoneId, current.memberId, connection = connection)
        }
    },
    onStopSiren = { phoneId ->
        runCatchingFailure {
            withinAnswerWait {
                stopSiren(phoneId, current.memberId, connection = connection)
            }
        }.onFailure { refusal ->
            logFailure(MainActivity.TAG, "a siren could not be stopped", refusal)
        }
    },
    // From every group, each under the account it knows the member by: the
    // device is the same one in all of them.
    onForgetPhone = { phoneId -> forgetDeviceEverywhere(deviceIdOf(phoneId)) },
    onEndSearch = { phoneId ->
        runCatchingFailure {
            withinAnswerWait { endSearch(phoneId, connection = connection) }
        }.onFailure { refusal ->
            logFailure(MainActivity.TAG, "a search could not be ended", refusal)
        }
    },
)
