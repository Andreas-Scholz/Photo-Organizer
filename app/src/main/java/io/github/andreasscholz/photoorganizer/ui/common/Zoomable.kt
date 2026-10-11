package io.github.andreasscholz.photoorganizer.ui.common

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

/**
 * Zoom level and pan position of a [ZoomableBox].
 *
 * Positions are measured from the centre of the box, around which the content is scaled.
 */
@Stable
class ZoomState(private val maxScale: Float = 5f) {

    var scale by mutableFloatStateOf(1f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    val isZoomed: Boolean get() = scale > 1f

    /** Applies one step of a pinch gesture, keeping the content under [centroid] in place. */
    fun transform(centroid: Offset, pan: Offset, zoom: Float, boxSize: Size) {
        zoomTo((scale * zoom).coerceIn(1f, maxScale), centroid, boxSize, pan)
    }

    /** Zooms in on [position], or back out if already zoomed. */
    fun toggle(position: Offset, boxSize: Size) {
        if (isZoomed) reset() else zoomTo(DOUBLE_TAP_SCALE, position, boxSize)
    }

    fun reset() {
        scale = 1f
        offset = Offset.Zero
    }

    /**
     * Call when the fingers are lifted. Pinching back out hardly ever ends at exactly the
     * normal size; without snapping back the content would stay zoomed in imperceptibly.
     */
    fun settle() {
        if (scale < SNAP_BACK_SCALE) reset()
    }

    private fun zoomTo(newScale: Float, anchor: Offset, boxSize: Size, pan: Offset = Offset.Zero) {
        if (newScale <= 1f) return reset()

        val newOffset = anchor - (anchor - offset) * (newScale / scale) + pan
        // The content may be panned until its edge reaches the edge of the box, not beyond.
        val maxX = boxSize.width * (newScale - 1f) / 2f
        val maxY = boxSize.height * (newScale - 1f) / 2f
        scale = newScale
        offset = Offset(newOffset.x.coerceIn(-maxX, maxX), newOffset.y.coerceIn(-maxY, maxY))
    }

    private companion object {
        const val DOUBLE_TAP_SCALE = 2.5f
        const val SNAP_BACK_SCALE = 1.15f
    }
}

/**
 * Lets [content] be zoomed by pinching or double tapping, and panned while zoomed in.
 *
 * Single-finger drags are left alone at the normal zoom level, so that a surrounding
 * swipe gesture keeps working until the user zooms in.
 */
@Composable
fun ZoomableBox(
    state: ZoomState,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .pointerInput(state) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val isPinch = event.changes.count { it.pressed } > 1
                        val centroid = event.calculateCentroid(useCurrent = false)
                        if ((isPinch || state.isZoomed) && centroid.isSpecified) {
                            state.transform(
                                centroid = centroid - size.center(),
                                pan = event.calculatePan(),
                                zoom = event.calculateZoom(),
                                boxSize = size.toSize(),
                            )
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                    state.settle()
                }
            }
            .pointerInput(state) {
                detectTapGestures(
                    onDoubleTap = { position -> state.toggle(position - size.center(), size.toSize()) },
                )
            },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = state.scale
                    scaleY = state.scale
                    translationX = state.offset.x
                    translationY = state.offset.y
                },
        ) {
            content()
        }
    }
}

private fun IntSize.center() = Offset(width / 2f, height / 2f)
