

package org.meshtastic.core.ui.component

import androidx.compose.foundation.lazy.LazyListState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private const val SCROLL_TO_TOP_INDEX = 0
private const val FAST_SCROLL_THRESHOLD = 10

fun LazyListState.smartScrollToTop(coroutineScope: CoroutineScope) {
    smartScrollToIndex(coroutineScope = coroutineScope, targetIndex = SCROLL_TO_TOP_INDEX)
}

fun LazyListState.smartScrollToIndex(coroutineScope: CoroutineScope, targetIndex: Int) {
    if (targetIndex < 0 || firstVisibleItemIndex == targetIndex) {
        return
    }
    coroutineScope.launch {
        val totalItems = layoutInfo.totalItemsCount
        if (totalItems == 0) {
            return@launch
        }
        val clampedTarget = targetIndex.coerceIn(0, totalItems - 1)
        val difference = firstVisibleItemIndex - clampedTarget
        val jumpIndex =
            when {
                difference > FAST_SCROLL_THRESHOLD ->
                    (clampedTarget + FAST_SCROLL_THRESHOLD).coerceAtMost(totalItems - 1)
                difference < -FAST_SCROLL_THRESHOLD -> (clampedTarget - FAST_SCROLL_THRESHOLD).coerceAtLeast(0)
                else -> null
            }
        jumpIndex?.let { scrollToItem(it) }
        animateScrollToItem(index = clampedTarget)
    }
}

