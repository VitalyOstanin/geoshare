package io.github.vitalyostanin.geoshare.collection

import androidx.annotation.StringRes
import io.github.vitalyostanin.geoshare.R

/** One line of the help: a reason, what the phone saw and what that means. */
data class ReasonExplained(
    /** The word the phone writes into the document, as [leftOutReasonLabel] reads it. */
    val reason: String,
    @param:StringRes val name: Int,
    @param:StringRes val sign: Int,
    @param:StringRes val means: Int,
)

/**
 * Every reason a position can be rejected for, in the order the help reads
 * them: by how much the rule had to look at before it could speak.
 *
 * Not the order of [JumpReason], which is the order the rules happened to be
 * written in. A member opening this has just read one reason on a card, and
 * what places it among the others is what it rests on — the answer alone, then
 * the answer against the one before it, then the phone's own sensors, then the
 * other radios, then the shape of a whole track, then the sky itself. The
 * switched-off source comes last because it is not a fault at all.
 *
 * The satellites' time sits with the sky rather than after it: what it rests
 * on is the same epochs the sky is read from, and a member who has just read
 * about a flat sky is reading about the same signal.
 */
fun reasonsExplained(): List<ReasonExplained> = listOf(
    ReasonExplained(
        JumpReason.Width.name,
        R.string.left_out_width,
        R.string.reason_help_sign_width,
        R.string.reason_help_means_width,
    ),
    ReasonExplained(
        JumpReason.Speed.name,
        R.string.left_out_speed,
        R.string.reason_help_sign_speed,
        R.string.reason_help_means_speed,
    ),
    ReasonExplained(
        JumpReason.Contradiction.name,
        R.string.left_out_contradiction,
        R.string.reason_help_sign_contradiction,
        R.string.reason_help_means_contradiction,
    ),
    ReasonExplained(
        JumpReason.FalsePlace.name,
        R.string.left_out_false_place,
        R.string.reason_help_sign_false_place,
        R.string.reason_help_means_false_place,
    ),
    ReasonExplained(
        JumpReason.Unmoved.name,
        R.string.left_out_unmoved,
        R.string.reason_help_sign_unmoved,
        R.string.reason_help_means_unmoved,
    ),
    ReasonExplained(
        JumpReason.Drifted.name,
        R.string.left_out_drifted,
        R.string.reason_help_sign_drifted,
        R.string.reason_help_means_drifted,
    ),
    ReasonExplained(
        JumpReason.Hurried.name,
        R.string.left_out_hurried,
        R.string.reason_help_sign_hurried,
        R.string.reason_help_means_hurried,
    ),
    ReasonExplained(
        JumpReason.Contradicted.name,
        R.string.left_out_contradicted,
        R.string.reason_help_sign_contradicted,
        R.string.reason_help_means_contradicted,
    ),
    ReasonExplained(
        JumpReason.Circling.name,
        R.string.left_out_circling,
        R.string.reason_help_sign_circling,
        R.string.reason_help_means_circling,
    ),
    ReasonExplained(
        JumpReason.Repeating.name,
        R.string.left_out_repeating,
        R.string.reason_help_sign_repeating,
        R.string.reason_help_means_repeating,
    ),
    ReasonExplained(
        JumpReason.OneBand.name,
        R.string.left_out_one_band,
        R.string.reason_help_sign_one_band,
        R.string.reason_help_means_one_band,
    ),
    ReasonExplained(
        JumpReason.OneTransmitter.name,
        R.string.left_out_one_transmitter,
        R.string.reason_help_sign_one_transmitter,
        R.string.reason_help_means_one_transmitter,
    ),
    ReasonExplained(
        JumpReason.TimeApart.name,
        R.string.left_out_time_apart,
        R.string.reason_help_sign_time_apart,
        R.string.reason_help_means_time_apart,
    ),
    ReasonExplained(
        JumpReason.OffOrbit.name,
        R.string.left_out_off_orbit,
        R.string.reason_help_sign_off_orbit,
        R.string.reason_help_means_off_orbit,
    ),
    ReasonExplained(
        JumpReason.Settling.name,
        R.string.left_out_settling,
        R.string.reason_help_sign_settling,
        R.string.reason_help_means_settling,
    ),
    ReasonExplained(
        LEFT_OUT_BY_SOURCE,
        R.string.left_out_by_source,
        R.string.reason_help_sign_by_source,
        R.string.reason_help_means_by_source,
    ),
)
