package com.example.tempcontacts

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tempcontacts.ui.theme.BurnerOrange
import kotlinx.coroutines.launch
import kotlin.math.abs

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val pages = listOf(
        OnboardingPage("Your Privacy, Protected", "Keep your personal number private. Burner Book stores everything locally—no clouds, no leaks.", Icons.Default.Shield),
        OnboardingPage("Self-Destructing Contacts", "Set a timer for any contact. When time is up, they vanish automatically. No manual cleanup.", Icons.Default.Timer),
        OnboardingPage("Set it and Forget it", "Perfect for online sales, dating, or temporary projects. Organize life without the commitment.", Icons.Default.CheckCircle)
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF2C2C2C), Color.Black)))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp)
                    .graphicsLayer {
                        val pageOffset = abs(pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        alpha = 1f - pageOffset.coerceIn(0f, 1f)
                        scaleX = 1f - pageOffset * 0.2f
                        scaleY = 1f - pageOffset * 0.2f
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(imageVector = pages[page].icon, contentDescription = null, modifier = Modifier.size(128.dp), tint = BurnerOrange)
                Spacer(modifier = Modifier.height(32.dp))
                Text(text = pages[page].title, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = pages[page].description, textAlign = TextAlign.Center, color = Color.Gray)
            }
        }

        // Top-right Skip button
        TextButton(
            onClick = onOnboardingComplete,
            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
        ) {
            Text("Skip", color = BurnerOrange)
        }

        // Bottom controls
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(32.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dot Indicator
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pages.indices.forEach { index ->
                    val width = animateDpAsState(targetValue = if (pagerState.currentPage == index) 24.dp else 8.dp, label = "").value
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(if (pagerState.currentPage == index) BurnerOrange else Color.Gray)
                    )
                }
            }

            // Next/Get Started Button
            Button(
                onClick = {
                    if (pagerState.currentPage < pages.size - 1) {
                        scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    } else {
                        onOnboardingComplete()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BurnerOrange)
            ) {
                Text(if (pagerState.currentPage < pages.size - 1) "Next" else "Get Started")
            }
        }
    }
}
