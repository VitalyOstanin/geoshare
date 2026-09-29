package io.github.vitalyostanin.geoshare.service

import android.app.NotificationManager
import android.content.Context
import android.content.res.Resources
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import androidx.core.content.getSystemService
import io.github.vitalyostanin.geoshare.R
import io.github.vitalyostanin.geoshare.log.AppLog
import java.io.IOException

private const val TAG = "geoshare.siren"

/**
 * Sounds the device the way an alarm clock would: the alarm stream at full
 * volume, "Do Not Disturb" lifted just enough to let alarms through, and the
 * app's own SOS siren looping until [stop] is called, falling back to the
 * device's alarm sound if the siren cannot be loaded. How long it sounds is
 * the service's call, not this class's; it only starts and stops.
 */
class SirenPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var previousAlarmVolume: Int? = null
    private var previousInterruptionFilter: Int? = null

    /** Starts the alarm sound. Does nothing if it is already sounding. */
    fun start() {
        if (mediaPlayer != null) {
            return
        }

        // Whatever fails below undoes what was already changed: a siren that
        // cannot sound must not leave the alarm at full volume either.
        try {
            raiseAlarmVolume()
            raiseInterruptionFilter()
            // Our own siren is preferred; a device's stock alarm sound is
            // used only when it cannot be loaded, so this still sounds like
            // an alarm rather than staying silent.
            mediaPlayer =
                ownSirenPlayer() ?: MediaPlayer().preparedWith {
                    setDataSource(context, alarmSoundUri())
                }
        } catch (refusal: IOException) {
            AppLog.w(TAG, "could not load the alarm sound", refusal)
            stop()
        } catch (refusal: IllegalStateException) {
            AppLog.w(TAG, "the siren player was in the wrong state", refusal)
            stop()
        } catch (refusal: SecurityException) {
            AppLog.w(TAG, "not allowed to play the siren sound", refusal)
            stop()
        }
    }

    // Loads the app's own SOS siren. Returns null, with the failed player
    // released, if the resource cannot be read or played, so the caller
    // falls back to the device's alarm sound instead of staying silent.
    private fun ownSirenPlayer(): MediaPlayer? {
        val player = MediaPlayer()

        fun refused(refusal: Exception): MediaPlayer? {
            AppLog.w(TAG, "could not play the siren sound, falling back to the alarm", refusal)
            player.release()

            return null
        }

        return try {
            player.preparedWith {
                context.resources.openRawResourceFd(R.raw.siren_sos).use {
                    setDataSource(it.fileDescriptor, it.startOffset, it.length)
                }
            }
        } catch (refusal: IOException) {
            refused(refusal)
        } catch (refusal: Resources.NotFoundException) {
            refused(refusal)
        } catch (refusal: IllegalStateException) {
            refused(refusal)
        }
    }

    // Common setup for both the siren and its alarm-sound fallback: alarm
    // audio attributes, looping, and starting playback once prepared.
    private fun MediaPlayer.preparedWith(setSource: MediaPlayer.() -> Unit): MediaPlayer = apply {
        setAudioAttributes(
            AudioAttributes
                .Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        setSource()
        isLooping = true
        prepare()
        start()
    }

    /** Stops the alarm sound and undoes both the volume and "Do Not Disturb" changes. */
    fun stop() {
        releasePlayer()
        restoreAlarmVolume()
        restoreInterruptionFilter()
    }

    private fun releasePlayer() {
        mediaPlayer?.let {
            try {
                it.stop()
            } catch (refusal: IllegalStateException) {
                AppLog.w(TAG, "the siren player was not playing", refusal)
            }
            it.release()
        }
        mediaPlayer = null
    }

    private fun raiseAlarmVolume() {
        val audioManager = context.getSystemService<AudioManager>() ?: return

        previousAlarmVolume = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        audioManager.setStreamVolume(
            AudioManager.STREAM_ALARM,
            audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
            0,
        )
    }

    private fun restoreAlarmVolume() {
        val volume = previousAlarmVolume ?: return

        context
            .getSystemService<AudioManager>()
            ?.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0)
        previousAlarmVolume = null
    }

    // "Do Not Disturb" set to total silence swallows the alarm stream along
    // with everything else, so it is loosened to "alarms only" for the
    // siren's sake. Any looser filter than "none" already lets alarms
    // through and is left untouched.
    private fun raiseInterruptionFilter() {
        val notificationManager = context.getSystemService<NotificationManager>() ?: return

        val silence = NotificationManager.INTERRUPTION_FILTER_NONE

        if (
            notificationManager.isNotificationPolicyAccessGranted &&
            notificationManager.currentInterruptionFilter == silence
        ) {
            previousInterruptionFilter = silence
            notificationManager.setInterruptionFilter(
                NotificationManager.INTERRUPTION_FILTER_ALARMS,
            )
        }
    }

    private fun restoreInterruptionFilter() {
        val filter = previousInterruptionFilter ?: return
        val notificationManager = context.getSystemService<NotificationManager>()

        if (notificationManager?.isNotificationPolicyAccessGranted == true) {
            notificationManager.setInterruptionFilter(filter)
        }
        previousInterruptionFilter = null
    }

    private fun alarmSoundUri(): Uri =
        RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI
}

/** Whether the app is allowed to change "Do Not Disturb", needed to sound the siren loudly. */
fun dndAccessGranted(context: Context): Boolean =
    context.getSystemService<NotificationManager>()?.isNotificationPolicyAccessGranted ?: false
