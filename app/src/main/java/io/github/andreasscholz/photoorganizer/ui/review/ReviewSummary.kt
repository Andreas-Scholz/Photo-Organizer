package io.github.andreasscholz.photoorganizer.ui.review

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.domain.review.ChangePlan

/** Lists what the review will change and lets the user apply it. */
@Composable
fun ReviewSummary(
    plan: ChangePlan,
    state: ReviewUiState,
    onApply: () -> Unit,
    onUndo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (plan.toTrash.isNotEmpty()) {
                SummaryLine(
                    icon = R.drawable.ic_delete,
                    text = pluralStringResource(R.plurals.summary_trash, plan.toTrash.size, plan.toTrash.size),
                )
            }
            if (plan.toFavorite.isNotEmpty()) {
                SummaryLine(
                    icon = R.drawable.ic_favorite,
                    text = pluralStringResource(
                        R.plurals.summary_favorite,
                        plan.toFavorite.size,
                        plan.toFavorite.size,
                    ),
                )
            }
            if (plan.toUnfavorite.isNotEmpty()) {
                SummaryLine(
                    icon = R.drawable.ic_favorite_border,
                    text = pluralStringResource(
                        R.plurals.summary_unfavorite,
                        plan.toUnfavorite.size,
                        plan.toUnfavorite.size,
                    ),
                )
            }
            plan.toMove.forEach { (album, items) ->
                SummaryLine(
                    icon = R.drawable.ic_folder,
                    text = pluralStringResource(R.plurals.summary_move, items.size, items.size, album.name),
                )
            }
            if (plan.keptCount > 0) {
                SummaryLine(
                    icon = R.drawable.ic_check,
                    text = pluralStringResource(R.plurals.summary_keep, plan.keptCount, plan.keptCount),
                )
            }

            Text(
                text = stringResource(
                    if (plan.isEmpty) R.string.summary_no_changes else R.string.summary_confirm_hint,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            val notice = when (val notice = state.notice) {
                null -> null
                ReviewNotice.ConfirmationDeclined -> stringResource(R.string.summary_declined)
                is ReviewNotice.MoveFailed ->
                    pluralStringResource(R.plurals.summary_move_failed, notice.count, notice.count)
            }
            if (notice != null) {
                Text(
                    text = notice,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }

        Button(
            onClick = onApply,
            enabled = !state.isApplying,
            modifier = Modifier.fillMaxWidth(),
        ) {
            if (state.isApplying) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
                Text(
                    stringResource(
                        when {
                            plan.isEmpty -> R.string.summary_done
                            state.hasAppliedChanges -> R.string.summary_continue
                            else -> R.string.summary_apply
                        },
                    ),
                )
            }
        }
        if (state.canUndo) {
            TextButton(onClick = onUndo, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(stringResource(R.string.review_undo))
            }
        }
    }
}

@Composable
private fun SummaryLine(@DrawableRes icon: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.size(16.dp))
        Text(text = text, style = MaterialTheme.typography.bodyLarge)
    }
}
