package com.fastiptv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.fastiptv.domain.model.Channel
import com.fastiptv.ui.theme.AccentBlue
import com.fastiptv.ui.theme.DarkBackground
import com.fastiptv.ui.theme.DarkSurfaceElevated
import com.fastiptv.ui.theme.GlassBorder
import com.fastiptv.ui.theme.LiveRed
import com.fastiptv.ui.theme.TextMuted
import com.fastiptv.ui.theme.TextWhite

@Composable
fun HeroSpotlightBanner(
    channel: Channel?,
    categoryName: String? = null,
    currentProgramTitle: String? = null,
    onWatchClick: (Channel) -> Unit,
    onFavoriteClick: (Channel) -> Unit = {},
    modifier: Modifier = Modifier,
    topNavFocusRequester: FocusRequester? = null
) {
    if (channel == null) return

    Crossfade(
        targetState = channel,
        animationSpec = tween(durationMillis = 350),
        label = "hero_crossfade",
        modifier = modifier
            .fillMaxWidth()
            .height(280.dp)
            .clip(RoundedCornerShape(20.dp))
    ) { current ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Ambient Backdrop Artwork (Logo scaled and blurred/faded)
            if (!current.logoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = current.logoUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .align(Alignment.CenterEnd)
                        .padding(start = 200.dp),
                    contentScale = ContentScale.Fit,
                    alpha = 0.15f
                )
            }

            // Cinematic Gradient Scrim (Left-to-Right and Top-to-Bottom for optimal readability)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                DarkBackground,
                                DarkBackground.copy(alpha = 0.95f),
                                DarkBackground.copy(alpha = 0.70f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Content Details
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(28.dp)
            ) {
                // Channel Logo Card
                Box(
                    modifier = Modifier
                        .size(140.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceElevated)
                        .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!current.logoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = current.logoUrl,
                            contentDescription = current.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Text(
                            text = current.name.take(3).uppercase(),
                            color = AccentBlue,
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Metadata Column
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Badges Row
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // LIVE HD Badge
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(LiveRed.copy(alpha = 0.15f))
                                .border(1.dp, LiveRed.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(LiveRed)
                            )
                            Text(
                                text = "LIVE HD",
                                color = LiveRed,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black
                            )
                        }

                        // Category Badge
                        if (!categoryName.isNullOrBlank()) {
                            Text(
                                text = categoryName.uppercase(),
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(DarkSurfaceElevated)
                                    .border(1.dp, GlassBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Favorite Badge if already starred
                        if (current.isFavorite) {
                            Text(
                                text = "★ FAVORITE",
                                color = AccentBlue,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(AccentBlue.copy(alpha = 0.15f))
                                    .border(1.dp, AccentBlue.copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // Channel Title
                    Text(
                        text = current.name,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // On-Air Program
                    if (!currentProgramTitle.isNullOrBlank()) {
                        Text(
                            text = "Now Playing: $currentProgramTitle",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AccentBlue,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    } else {
                        Text(
                            text = "Broadcast stream online • 1080p high quality low-latency feed",
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = { onWatchClick(current) },
                            modifier = if (topNavFocusRequester != null) Modifier.focusProperties { up = topNavFocusRequester } else Modifier,
                            colors = ButtonDefaults.colors(
                                containerColor = AccentBlue,
                                focusedContainerColor = TextWhite
                            ),
                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = "▶  Watch Live",
                                color = DarkBackground,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }

                        Button(
                            onClick = { onFavoriteClick(current) },
                            modifier = if (topNavFocusRequester != null) Modifier.focusProperties { up = topNavFocusRequester } else Modifier,
                            colors = ButtonDefaults.colors(
                                containerColor = DarkSurfaceElevated,
                                focusedContainerColor = DarkSurfaceElevated
                            ),
                            border = ButtonDefaults.border(
                                border = Border(border = BorderStroke(1.dp, GlassBorder)),
                                focusedBorder = Border(border = BorderStroke(1.5.dp, AccentBlue))
                            ),
                            shape = ButtonDefaults.shape(shape = RoundedCornerShape(10.dp))
                        ) {
                            Text(
                                text = if (current.isFavorite) "★  Favorited" else "☆  Add Favorite",
                                color = if (current.isFavorite) AccentBlue else TextWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
