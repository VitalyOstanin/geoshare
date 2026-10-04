package io.github.vitalyostanin.geoshare.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.vitalyostanin.geoshare.R

/**
 * What a member who has published no name is called in a line of text:
 * plainly a person, rather than a string of letters that reads as a fault.
 */
@Composable
fun unnamedMember(memberId: String): String =
    stringResource(R.string.member_unnamed, idTail(memberId))
