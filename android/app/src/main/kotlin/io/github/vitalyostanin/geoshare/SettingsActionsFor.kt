package io.github.vitalyostanin.geoshare

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import io.github.vitalyostanin.geoshare.auth.AuthState
import io.github.vitalyostanin.geoshare.download.DownloadSetting
import io.github.vitalyostanin.geoshare.log.AppLog
import io.github.vitalyostanin.geoshare.map.SettingsActions
import io.github.vitalyostanin.geoshare.map.publishSharing
import io.github.vitalyostanin.geoshare.map.withShare
import io.github.vitalyostanin.geoshare.map.withinAnswerWait
import io.github.vitalyostanin.geoshare.map.writePhoneName
import io.github.vitalyostanin.geoshare.membership.Roster
import io.github.vitalyostanin.geoshare.project.GroupConnection
import io.github.vitalyostanin.geoshare.service.LanguageChoice
import io.github.vitalyostanin.geoshare.service.MadeInvitation
import io.github.vitalyostanin.geoshare.service.ThemeChoice
import io.github.vitalyostanin.geoshare.service.askReset
import io.github.vitalyostanin.geoshare.service.callSiren
import io.github.vitalyostanin.geoshare.service.endSearch
import io.github.vitalyostanin.geoshare.service.forgetAidingData
import io.github.vitalyostanin.geoshare.service.foundGroup
import io.github.vitalyostanin.geoshare.service.invitationStanding
import io.github.vitalyostanin.geoshare.service.joinGroup
import io.github.vitalyostanin.geoshare.service.makeInvitation
import io.github.vitalyostanin.geoshare.service.openSearch
import io.github.vitalyostanin.geoshare.service.phoneReset
import io.github.vitalyostanin.geoshare.service.phoneSiren
import io.github.vitalyostanin.geoshare.service.removeFromGroup
import io.github.vitalyostanin.geoshare.service.renameGroup
import io.github.vitalyostanin.geoshare.service.renewSearch
import io.github.vitalyostanin.geoshare.service.setRunning
import io.github.vitalyostanin.geoshare.service.stopSiren
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.time.Instant

/**
 * What the group's writes say back to the screen, where nothing in the
 * database says it.
 */
internal class GroupAnswers {
    /**
     * The invitation just made. Kept on the screen rather than in the
     * database: the code is written where nobody may read it back, this phone
     * included, so the only copy to show is the one just made.
     */
    var invitation by mutableStateOf<MadeInvitation?>(null)

    /**
     * Whether the last write to the group was refused: the person who pressed
     * the button is owed an answer.
     */
    var groupTrouble by mutableStateOf(false)
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
    // request has to outlive this screen, since the phone may not have a
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
    // Written into the grant, and left to be read back: the phones of a
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
    // Read back through the phones of this member, like every other phone
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
    // question the screen cannot ask: a phone outside a group cannot read
    // the roster, so it cannot tell an empty household from one it is not
    // in.
    onFoundGroup = { name ->
        activity.lifecycleScope.launch {
            answers.groupTrouble =
                !activity.withGroup("found a group") {
                    foundGroup(
                        activity,
                        current.memberId,
                        name,
                        connection = connection,
                    )
                }
        }
    },
    onMakeInvitation = { email ->
        activity.lifecycleScope.launch {
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
    invitationStanding = { code -> invitationStanding(code, connection) },
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
        try {
            withinAnswerWait {
                stopSiren(phoneId, current.memberId, connection = connection)
            }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (refusal: Exception) {
            AppLog.w(
                MainActivity.TAG,
                "a siren could not be stopped",
                refusal,
            )
        }
    },
    // From every group, each under the account it knows the member by: the
    // device is the same one in all of them.
    onForgetPhone = { phoneId -> forgetDeviceEverywhere(phoneId.substringAfter('.')) },
    onEndSearch = { phoneId ->
        try {
            withinAnswerWait { endSearch(phoneId, connection = connection) }
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (refusal: Exception) {
            AppLog.w(
                MainActivity.TAG,
                "a search could not be ended",
                refusal,
            )
        }
    },
)
