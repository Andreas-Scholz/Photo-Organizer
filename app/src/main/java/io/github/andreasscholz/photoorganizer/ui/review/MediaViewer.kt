package io.github.andreasscholz.photoorganizer.ui.review

import android.net.Uri
import android.view.LayoutInflater
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import io.github.andreasscholz.photoorganizer.R
import io.github.andreasscholz.photoorganizer.data.media.contentUri
import io.github.andreasscholz.photoorganizer.domain.model.MediaItem
import io.github.andreasscholz.photoorganizer.domain.model.MediaType
import io.github.andreasscholz.photoorganizer.ui.common.ZoomState
import io.github.andreasscholz.photoorganizer.ui.common.ZoomableBox
import androidx.media3.common.MediaItem as PlayerMediaItem

/** Longest edge, in pixels, at which a photo is loaded once the user zooms in. */
private const val DETAIL_SIZE = 4096

/** The outer thirds of the viewer browse to the neighboring items when tapped. */
private enum class TapZone {
    PREVIOUS, CENTER, NEXT;

    companion object {
        fun at(x: Float, width: Int): TapZone = when {
            x < width / 3f -> PREVIOUS
            x > width * 2 / 3f -> NEXT
            else -> CENTER
        }
    }
}

/**
 * Shows a zoomable photo, or a video that starts playing once its play button is tapped.
 * Tapping the left or right third calls [onPrevious] or [onNext].
 */
@Composable
fun MediaViewer(
    item: MediaItem,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val onZoneTapped: (TapZone) -> Unit = { zone ->
        when (zone) {
            TapZone.PREVIOUS -> onPrevious()
            TapZone.NEXT -> onNext()
            TapZone.CENTER -> Unit
        }
    }
    Box(modifier = modifier.background(Color.Black), contentAlignment = Alignment.Center) {
        when (item.type) {
            MediaType.IMAGE -> ZoomablePhoto(item, onZoneTapped)
            MediaType.VIDEO -> Video(item, onZoneTapped)
        }
    }
}

/** Reports which [TapZone] was tapped, without getting in the way of drags. */
private fun Modifier.tapZones(onTap: (TapZone) -> Unit): Modifier = composed {
    val currentOnTap by rememberUpdatedState(onTap)
    pointerInput(Unit) {
        detectTapGestures(onTap = { position -> currentOnTap(TapZone.at(position.x, size.width)) })
    }
}

@Composable
private fun ZoomablePhoto(item: MediaItem, onZoneTapped: (TapZone) -> Unit) {
    val zoomState = remember(item.id) { ZoomState() }

    // Photos are first decoded at screen size, which is fast but blurry when magnified.
    // A sharper version is loaded on top once it is needed, and kept from then on.
    var showDetail by remember(item.id) { mutableStateOf(false) }
    LaunchedEffect(zoomState.isZoomed) {
        if (zoomState.isZoomed) showDetail = true
    }

    ZoomableBox(
        state = zoomState,
        modifier = Modifier.fillMaxSize(),
        onTap = { position, boxSize -> onZoneTapped(TapZone.at(position.x, boxSize.width)) },
    ) {
        AsyncImage(
            model = item.contentUri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (showDetail) {
            val context = LocalContext.current
            AsyncImage(
                model = remember(item.id) {
                    ImageRequest.Builder(context)
                        .data(item.contentUri)
                        .size(DETAIL_SIZE, DETAIL_SIZE)
                        .build()
                },
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun Video(item: MediaItem, onZoneTapped: (TapZone) -> Unit) {
    var isStarted by remember(item.id) { mutableStateOf(false) }

    if (isStarted) {
        VideoPlayer(uri = item.contentUri, onZoneTapped = onZoneTapped, modifier = Modifier.fillMaxSize())
    } else {
        AsyncImage(
            model = item.contentUri,
            contentDescription = item.displayName,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .tapZones(onZoneTapped),
        )
        FilledTonalIconButton(
            onClick = { isStarted = true },
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

/** Plays [uri] right away; tapping the middle of the picture pauses and resumes it. */
@Composable
private fun VideoPlayer(uri: Uri, onZoneTapped: (TapZone) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val player = remember(uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(PlayerMediaItem.fromUri(uri))
            prepare()
            playWhenReady = true
        }
    }
    var isPlaying by remember(player) { mutableStateOf(true) }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                isPlaying = playWhenReady && player.playbackState != Player.STATE_ENDED
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isPlaying = player.playWhenReady && playbackState != Player.STATE_ENDED
            }
        }
        player.addListener(listener)
        onDispose { player.release() }
    }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            factory = { LayoutInflater.from(it).inflate(R.layout.video_player, null) as PlayerView },
            update = { it.player = player },
            modifier = Modifier.fillMaxSize(),
        )
        // The player view would swallow all touches, including the swipes of the review card.
        // This layer receives them instead and only reacts to taps.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .tapZones { zone ->
                    when {
                        zone != TapZone.CENTER -> onZoneTapped(zone)
                        player.playbackState == Player.STATE_ENDED -> {
                            player.seekTo(0)
                            player.play()
                        }
                        player.playWhenReady -> player.pause()
                        else -> player.play()
                    }
                },
        )
        if (!isPlaying) {
            Icon(
                painter = painterResource(R.drawable.ic_play_arrow),
                contentDescription = stringResource(R.string.review_play_video),
                tint = Color.White,
                modifier = Modifier
                    .size(72.dp)
                    .background(Color.Black.copy(alpha = 0.5f), CircleShape),
            )
        }
    }
}
