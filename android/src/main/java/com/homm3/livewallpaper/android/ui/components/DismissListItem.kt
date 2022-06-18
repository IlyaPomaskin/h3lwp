package com.homm3.livewallpaper.android.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.ExperimentalTransitionApi
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp

@OptIn(
    ExperimentalMaterialApi::class, ExperimentalAnimationApi::class,
    ExperimentalTransitionApi::class
)
@Composable
fun DismissListItem(
    disabled: Boolean = false,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val dismissState = rememberDismissState(initialValue = DismissValue.Default)
    val isDismissed = dismissState.isDismissed(DismissDirection.EndToStart)
    val directions = when (disabled) {
        true -> emptySet()
        false -> setOf(DismissDirection.EndToStart)
    }

    SwipeToDismiss(
        state = dismissState,
        directions = directions,
        dismissThresholds = { FractionalThreshold(0.25f) },
        background = {
            val color by animateColorAsState(
                when (dismissState.targetValue) {
                    DismissValue.DismissedToStart -> MaterialTheme.colors.error
                    else -> MaterialTheme.colors.background
                },
            )

            Box(
                Modifier
                    .fillMaxSize()
                    .background(color)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                val scale by animateFloatAsState(
                    if (dismissState.targetValue == DismissValue.Default) 0.75f else 1f
                )

                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete map",
                    modifier = Modifier.scale(scale)
                )
            }
        },
        dismissContent = {
            val elevation by animateDpAsState(
                if (dismissState.dismissDirection == DismissDirection.EndToStart) 4.dp else 0.dp
            )

            val state = remember { MutableTransitionState(!isDismissed) }
            state.targetState = !isDismissed

            if (state.isIdle && !state.currentState) {
                SideEffect { onDismiss() }
            }

            AnimatedVisibility(
                visibleState = state,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Surface(elevation = elevation) {
                    content()
                }
            }
        }
    )
}