package com.skylake.skytv.jgorunner.ui.tvhome.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ShimmerChannelGrid(columns: Int = 5) {
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.1f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    val shimmerBrush = Color.LightGray.copy(alpha = alpha)

    Column(modifier = Modifier.fillMaxSize()) {
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(7) {
                Box(
                    modifier = Modifier
                        .height(32.dp)
                        .width(90.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(shimmerBrush)
                )
            }
        }

//        Box(
//            modifier = Modifier
//                .fillMaxWidth()
//                .padding(horizontal = 12.dp, vertical = 4.dp)
//                .height(120.dp)
//                .clip(RoundedCornerShape(16.dp))
//                .background(Color.DarkGray.copy(alpha = 0.1f))
//        ) {
//            Row(
//                modifier = Modifier
//                    .fillMaxSize()
//                    .padding(16.dp),
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Column(modifier = Modifier.weight(1f)) {
//                    Box(modifier = Modifier.height(14.dp).fillMaxWidth(0.3f).background(shimmerBrush))
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Box(modifier = Modifier.height(24.dp).fillMaxWidth(0.6f).background(shimmerBrush))
//                    Spacer(modifier = Modifier.height(8.dp))
//                    Box(modifier = Modifier.height(12.dp).fillMaxWidth(0.8f).background(shimmerBrush))
//                }
//                Box(
//                    modifier = Modifier
//                        .height(90.dp)
//                        .width(160.dp)
//                        .clip(RoundedCornerShape(12.dp))
//                        .background(shimmerBrush)
//                )
//            }
//        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 120.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(20) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.DarkGray.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(0.8f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(shimmerBrush)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Box(
                            modifier = Modifier
                                .height(14.dp)
                                .fillMaxWidth(0.9f)
                                .clip(RoundedCornerShape(4.dp))
                                .background(shimmerBrush)
                        )
                    }
                }
            }
        }
    }
}