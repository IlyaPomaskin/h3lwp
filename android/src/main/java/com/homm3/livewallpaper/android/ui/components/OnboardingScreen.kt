package com.homm3.livewallpaper.android.ui.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.rememberPagerState
import com.homm3.livewallpaper.android.ui.theme.H3lwpnextTheme
import kotlinx.coroutines.launch

enum class Pages(val value: Int) {
    Start(0),
    Explain(1),
    Parse(2);

    companion object {
        fun fromInt(value: Int) = values().find { it.value == value }

        val count = values().size
    }
}

@Composable
fun DotsIndicator(
    totalDots: Int,
    selectedIndex: Int,
    selectedColor: Color,
    unSelectedColor: Color,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = Modifier
            .wrapContentWidth()
            .wrapContentHeight()
            .then(modifier)
    ) {
        items(totalDots) { index ->
            if (index == selectedIndex) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(selectedColor)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(unSelectedColor)
                )
            }

            if (index != totalDots - 1) {
                Spacer(modifier = Modifier.padding(horizontal = 2.dp))
            }
        }
    }
}

@OptIn(ExperimentalPagerApi::class, ExperimentalAnimationApi::class)
@Composable
fun OnboardingScreen() {
    val pagerState = rememberPagerState(initialPage = Pages.Start.value)
    val scope = rememberCoroutineScope()
    val prevPage = Pages.fromInt(pagerState.currentPage - 1)
    val nextPage = Pages.fromInt(pagerState.currentPage + 1)

    val handlePrevClick = {
        scope.launch {
            if (prevPage != null) {
                pagerState.animateScrollToPage(prevPage.value)
            }
        }
    }
    val handleNextClick = {
        scope.launch {
            if (nextPage != null) {
                pagerState.animateScrollToPage(nextPage.value)
            }
        }
    }

    H3lwpnextTheme {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                HorizontalPager(
                    count = Pages.count, state = pagerState, modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth()
                ) { page ->
                    when (Pages.fromInt(page)) {
                        Pages.Start -> Text(text = "start")
                        Pages.Explain -> Text(text = "explain")
                        Pages.Parse -> Text(text = "parse")
                        null -> Text(text = "null")
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Button(
                    modifier = Modifier.padding(12.dp),
                    enabled = prevPage != null,
                    onClick = { handleNextClick() }
                ) { Text(text = "Prev") }

                DotsIndicator(
                    totalDots = Pages.count,
                    selectedIndex = pagerState.currentPage,
                    selectedColor = Color.Black,
                    unSelectedColor = Color.Gray,
                )

                Button(
                    modifier = Modifier.padding(12.dp),
                    enabled = nextPage != null,
                    onClick = { handleNextClick() }
                ) { Text(text = "Next") }
            }
        }
    }
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OnboardingScreenPreview() {
    OnboardingScreen()
}