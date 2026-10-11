package io.github.andreasscholz.photoorganizer.ui.review

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.input.pointer.util.addPointerInputChange
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

/** Share of the card width that has to be dragged for a swipe to count. */
private const val SWIPE_THRESHOLD = 0.3f

/** A drag released faster than this counts as a swipe regardless of its length. */
private val FLING_VELOCITY = 600.dp
private const val MAX_ROTATION_DEGREES = 8f

/**
 * A card that can be flung off to either side.
 *
 * [content] receives the drag progress: 0 at rest, -1 once a release would trigger
 * [onSwipedLeft], +1 for [onSwipedRight]. Read it in a draw or layer block to avoid recomposing.
 */
@Composable
fun SwipeableCard(
    onSwipedLeft: () -> Unit,
    onSwipedRight: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (progress: () -> Float) -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnSwipedLeft by rememberUpdatedState(onSwipedLeft)
    val currentOnSwipedRight by rememberUpdatedState(onSwipedRight)

    var width by remember { mutableIntStateOf(0) }

    Box(
        modifier = modifier
            .onSizeChanged { width = it.width }
            .pointerInput(Unit) {
                val threshold = size.width * SWIPE_THRESHOLD
                val flingVelocity = FLING_VELOCITY.toPx()
                val velocityTracker = VelocityTracker()
                detectHorizontalDragGestures(
                    onDragStart = { velocityTracker.resetTracking() },
                    onDragEnd = {
                        val velocity = velocityTracker.calculateVelocity().x
                        scope.launch {
                            val offset = offsetX.value
                            // A quick flick only counts in the direction the card was moved to.
                            val isFling = abs(velocity) > flingVelocity && velocity * offset > 0
                            if (abs(offset) < threshold && !isFling) {
                                offsetX.animateTo(0f)
                            } else {
                                // Fly out far enough that the rotated card fully leaves the screen.
                                offsetX.animateTo(if (offset > 0) size.width * 1.5f else -size.width * 1.5f)
                                if (offset > 0) currentOnSwipedRight() else currentOnSwipedLeft()
                            }
                        }
                    },
                    onDragCancel = { scope.launch { offsetX.animateTo(0f) } },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        velocityTracker.addPointerInputChange(change)
                        scope.launch { offsetX.snapTo(offsetX.value + dragAmount) }
                    },
                )
            }
            .graphicsLayer {
                translationX = offsetX.value
                rotationZ = offsetX.value / size.width * MAX_ROTATION_DEGREES
            },
    ) {
        content {
            if (width == 0) 0f else (offsetX.value / (width * SWIPE_THRESHOLD)).coerceIn(-1f, 1f)
        }
    }
}
