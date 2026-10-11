package io.github.andreasscholz.photoorganizer.ui.review

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.review.Disposition
import io.github.andreasscholz.photoorganizer.ui.common.MediaThumbnail

/**
 * The photos taken together with [currentItem], for comparing them before deciding.
 * Tapping one shows it; [onKeepOnlyCurrent] settles the whole series at once.
 */
@Composable
fun SeriesStrip(
    series: List<MediaItem>,
    currentItem: MediaItem,
    dispositionOf: (MediaItem) -> Disposition?,
    onItemClick: (MediaItem) -> Unit,
    onKeepOnlyCurrent: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    // Keeps the current photo in view when browsing through a series longer than the screen.
    LaunchedEffect(currentItem.id) {
        val index = series.indexOfFirst { it.id == currentItem.id }
        if (index >= 0) listState.animateScrollToItem(index)
    }

    Column(modifier = modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(series, key = { _, item -> item.id }) { index, item ->
                SeriesThumbnail(
                    item = item,
                    isCurrent = item.id == currentItem.id,
                    disposition = dispositionOf(item),
                    label = stringResource(R.string.review_series_photo, index + 1, series.size),
                    onClick = { onItemClick(item) },
                )
            }
        }
        OutlinedButton(onClick = onKeepOnlyCurrent) {
            Text(stringResource(R.string.review_keep_only_this))
        }
    }
}

@Composable
private fun SeriesThumbnail(
    item: MediaItem,
    isCurrent: Boolean,
    disposition: Disposition?,
    label: String,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(shape)
            .then(
                if (isCurrent) Modifier.border(3.dp, MaterialTheme.colorScheme.primary, shape) else Modifier,
            )
            .clickable(onClickLabel = label, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        MediaThumbnail(item = item, modifier = Modifier.fillMaxSize())
        if (disposition != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f)),
            )
            Icon(
                painter = painterResource(
                    when (disposition) {
                        Disposition.Keep -> R.drawable.ic_check
                        Disposition.Delete -> R.drawable.ic_delete
                        is Disposition.MoveTo -> R.drawable.ic_folder
                    },
                ),
                contentDescription = null,
                tint = Color.White,
            )
        }
    }
}
