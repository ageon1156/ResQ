

package org.meshtastic.core.ui.component

import android.annotation.SuppressLint
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.horizontalDrag
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow.Companion.Ellipsis
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

private const val NO_OPTION_INDEX = -1

private val TRACK_PADDING = 2.dp

private val TRACK_COLOR = Color.LightGray.copy(alpha = .5f)

private val PRESSED_TRACK_PADDING = 1.dp

private val OPTION_PADDING = 5.dp

private const val PRESSED_UNSELECTED_ALPHA = .6f

private val BACKGROUND_SHAPE = RoundedCornerShape(8.dp)

@Composable
fun <T : Any> SlidingSelector(
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val state = remember { SelectorState() }
    state.optionCount = options.size
    state.selectedOption = options.indexOf(selectedOption)
    state.onOptionSelected = { onOptionSelected(options[it]) }

    val selectedIndexOffset by animateFloatAsState(state.selectedOption.toFloat(), label = "Selected Index Offset")

    Layout(
        content = {
            SelectedIndicator(state)
            Dividers(state)
            Options(state, options, content)
        },
        modifier =
        modifier
            .fillMaxWidth()
            .then(state.inputModifier)
            .background(TRACK_COLOR, BACKGROUND_SHAPE)
            .padding(TRACK_PADDING),
    ) { measurables, constraints ->
        val (indicatorMeasurable, dividersMeasurable, optionsMeasurable) = measurables

        val optionsPlaceable = optionsMeasurable.measure(constraints)
        state.updatePressedScale(optionsPlaceable.height, this)

        val indicatorPlaceable =
            indicatorMeasurable.measure(
                Constraints.fixed(width = optionsPlaceable.width / options.size, height = optionsPlaceable.height),
            )

        val dividersPlaceable =
            dividersMeasurable.measure(
                Constraints.fixed(width = optionsPlaceable.width, height = optionsPlaceable.height),
            )

        layout(optionsPlaceable.width, optionsPlaceable.height) {
            val optionWidth = optionsPlaceable.width / options.size

            indicatorPlaceable.placeRelative(x = (selectedIndexOffset * optionWidth).toInt(), y = 0)
            dividersPlaceable.placeRelative(IntOffset.Zero)
            optionsPlaceable.placeRelative(IntOffset.Zero)
        }
    }
}

@Composable
fun OptionLabel(text: String) {
    Text(text, maxLines = 1, overflow = Ellipsis)
}

@Composable
private fun SelectedIndicator(state: SelectorState) {
    Box(
        Modifier.then(
            state.optionScaleModifier(
                pressed = state.pressedOption == state.selectedOption,
                option = state.selectedOption,
            ),
        )
            .shadow(4.dp, BACKGROUND_SHAPE)
            .background(MaterialTheme.colorScheme.background, BACKGROUND_SHAPE),
    )
}

@Composable
private fun Dividers(state: SelectorState) {
    
    val alphas =
        (0 until state.optionCount).map { i ->
            val selectionAdjacent = i == state.selectedOption || i - 1 == state.selectedOption
            animateFloatAsState(if (selectionAdjacent) 0f else 1f, label = "Dividers")
        }

    Canvas(Modifier.fillMaxSize()) {
        val optionWidth = size.width / state.optionCount
        val dividerPadding = TRACK_PADDING + PRESSED_TRACK_PADDING

        alphas.forEachIndexed { i, alpha ->
            val x = i * optionWidth
            drawLine(
                Color.White,
                alpha = alpha.value,
                start = Offset(x, dividerPadding.toPx()),
                end = Offset(x, size.height - dividerPadding.toPx()),
            )
        }
    }
}

@Composable
private fun <T> Options(state: SelectorState, options: List<T>, content: @Composable (T) -> Unit) {
    CompositionLocalProvider(LocalTextStyle provides TextStyle(fontWeight = FontWeight.Medium)) {
        Row(horizontalArrangement = spacedBy(TRACK_PADDING), modifier = Modifier.fillMaxWidth().selectableGroup()) {
            options.forEachIndexed { i, timeFrame ->
                val isSelected = i == state.selectedOption
                val isPressed = i == state.pressedOption

                val alpha by
                    animateFloatAsState(
                        if (!isSelected && isPressed) PRESSED_UNSELECTED_ALPHA else 1f,
                        label = "Unselected",
                    )

                val semanticsModifier =
                    Modifier.semantics(mergeDescendants = true) {
                        selected = isSelected
                        role = Role.Button
                        onClick {
                            state.onOptionSelected(i)
                            true
                        }
                        stateDescription = if (isSelected) "Selected" else "Not selected"
                    }

                Box(
                    Modifier
                        
                        .weight(1f)
                        .then(semanticsModifier)
                        .padding(OPTION_PADDING)
                        
                        .alpha(alpha)
                        
                        .then(state.optionScaleModifier(isPressed && isSelected, i))
                        
                        .wrapContentWidth(),
                ) {
                    content(timeFrame)
                }
            }
        }
    }
}

private class SelectorState {
    var optionCount by mutableIntStateOf(0)
    var selectedOption by mutableIntStateOf(0)
    var onOptionSelected: (Int) -> Unit by mutableStateOf({})
    var pressedOption by mutableIntStateOf(NO_OPTION_INDEX)

    var pressedSelectedScale by mutableFloatStateOf(1f)
        private set

    fun updatePressedScale(controlHeight: Int, density: Density) {
        with(density) {
            val pressedPadding = PRESSED_TRACK_PADDING * 2
            val pressedHeight = controlHeight - pressedPadding.toPx()
            pressedSelectedScale = pressedHeight / controlHeight
        }
    }

    @SuppressLint("ModifierFactoryExtensionFunction")
    fun optionScaleModifier(pressed: Boolean, option: Int): Modifier = Modifier.composed {
        val scale by animateFloatAsState(if (pressed) pressedSelectedScale else 1f, label = "Scale")
        val xOffset by animateDpAsState(if (pressed) PRESSED_TRACK_PADDING else 0.dp, label = "x Offset")

        graphicsLayer {
            this.scaleX = scale
            this.scaleY = scale

            this.transformOrigin =
                TransformOrigin(
                    pivotFractionX =
                    when (option) {
                        0 -> 0f
                        optionCount - 1 -> 1f
                        else -> .5f
                    },
                    pivotFractionY = .5f,
                )

            this.translationX =
                when (option) {
                    0 -> xOffset.toPx()
                    optionCount - 1 -> -xOffset.toPx()
                    else -> 0f
                }
        }
    }

    val inputModifier =
        Modifier.pointerInput(optionCount) {
            val optionWidth = size.width / optionCount

            fun optionIndex(change: PointerInputChange): Int =
                ((change.position.x / size.width.toFloat()) * optionCount).toInt().coerceIn(0, optionCount - 1)

            awaitEachGesture {
                val down = awaitFirstDown()

                pressedOption = optionIndex(down)
                val downOnSelected = pressedOption == selectedOption
                val optionBounds =
                    Rect(
                        left = pressedOption * optionWidth.toFloat(),
                        right = (pressedOption + 1) * optionWidth.toFloat(),
                        top = 0f,
                        bottom = size.height.toFloat(),
                    )

                if (downOnSelected) {
                    horizontalDrag(down.id) { change ->
                        pressedOption = optionIndex(change)

                        if (pressedOption != selectedOption) {
                            onOptionSelected(pressedOption)
                        }
                    }
                } else {
                    waitForUpOrCancellation(inBounds = optionBounds)
                        
                        ?.let { onOptionSelected(pressedOption) }
                }
                pressedOption = NO_OPTION_INDEX
            }
        }
}

@Suppress("ReturnCount")
private suspend fun AwaitPointerEventScope.waitForUpOrCancellation(inBounds: Rect): PointerInputChange? {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Main)
        if (event.changes.all { it.changedToUp() }) {
            
            return event.changes[0]
        }

        if (event.changes.any { it.isConsumed || !inBounds.contains(it.position) }) {
            
            return null
        }

        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.any { it.isConsumed }) {
            return null
        }
    }
}

