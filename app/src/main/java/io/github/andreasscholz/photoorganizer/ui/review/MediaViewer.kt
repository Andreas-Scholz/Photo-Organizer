package io.github.andreasscholz.photoorganizer.ui.review

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.data.media.contentUri
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.model.MediaType
import androidx.media3.common.MediaItem as PlayerMediaItem

/** Shows a photo, or a video that starts playing once its play button is tapped. */
@Composable
fun MediaViewer(item: MediaItem, modifier: Modifier = Modifier) {
    var isPlaying by remember(item.id) { mutableStateOf(false) }

    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        if (isPlaying) {
            VideoPlayer(uri = item.contentUri, modifier = Modifier.fillMaxSize())
        } else {
            AsyncImage(
                model = item.contentUri,
                contentDescription = item.displayName,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            if (item.type == MediaType.VIDEO) {
                FilledTonalIconButton(
                    onClick = { isPlaying = true },
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_play_arrow),
                        contentDescription = stringResource(R.string.review_play_video),
                        modifier = Modifier.size(40.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoPlayer(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) {
        onDispose { player.release() }
    }
    AndroidView(
        factory = { LayoutInflater.from(it).inflate(R.layout.video_player, null) as PlayerView },
        update = { it.player = player },
        modifier = modifier,
    )
}
