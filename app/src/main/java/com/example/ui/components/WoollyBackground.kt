package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

// Modern Farm Pastel Color Palette
val FarmSkyTop = Color(0xFFC7DEE9)
val FarmSkyMid = Color(0xFFDEECF2)
val FarmSkyBottom = Color(0xFFF2F7F4)
val FarmHillLight = Color(0xFF8CAE8E)
val FarmHillMid = Color(0xFF5E8B65)
val FarmHillDeep = Color(0xFF355E40)
val FarmSun = Color(0xFFECCB73)

val WoollyCardBg = Color(0xFFFBF8F2)
val WoollyCardBorder = Color(0xFFEDE7DC)
val WoollyPrimaryGreen = Color(0xFF264232)
val WoollyDarkGreen = Color(0xFF1B3125)
val WoollySageLabel = Color(0xFF2C493A)
val WoollyMutedText = Color(0xFF6B8274)
val WoollyFieldBorder = Color(0xFFE4DFD5)
val WoollyLinkGreen = Color(0xFF457766)

@Composable
fun WoollySceneryBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(FarmSkyTop, FarmSkyMid, FarmSkyBottom)
                )
            )
    ) {
        // Scenery Canvas (Sun, Clouds, Rolling Hills)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Warm Glowing Sun (top right)
            drawCircle(
                color = FarmSun.copy(alpha = 0.35f),
                radius = 80.dp.toPx(),
                center = Offset(width * 0.82f, height * 0.08f)
            )
            drawCircle(
                color = FarmSun,
                radius = 42.dp.toPx(),
                center = Offset(width * 0.82f, height * 0.08f)
            )

            // Puffy background clouds
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = 45.dp.toPx(),
                center = Offset(width * 0.22f, height * 0.05f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.55f),
                radius = 35.dp.toPx(),
                center = Offset(width * 0.35f, height * 0.06f)
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.45f),
                radius = 50.dp.toPx(),
                center = Offset(width * 0.60f, height * 0.05f)
            )

            // Rolling Hills at the bottom
            // Hill 1 (Back, soft sage)
            val path1 = Path().apply {
                moveTo(0f, height * 0.82f)
                cubicTo(
                    width * 0.35f, height * 0.74f,
                    width * 0.7f, height * 0.88f,
                    width, height * 0.78f
                )
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(path1, color = FarmHillLight)

            // Hill 2 (Mid, rich meadow)
            val path2 = Path().apply {
                moveTo(0f, height * 0.87f)
                cubicTo(
                    width * 0.3f, height * 0.94f,
                    width * 0.65f, height * 0.80f,
                    width, height * 0.86f
                )
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(path2, color = FarmHillMid)

            // Hill 3 (Foreground, deep forest hill)
            val path3 = Path().apply {
                moveTo(0f, height * 0.94f)
                cubicTo(
                    width * 0.4f, height * 0.88f,
                    width * 0.8f, height * 0.96f,
                    width, height * 0.91f
                )
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(path3, color = FarmHillDeep)
        }

        // Main content on top
        content()
    }
}

@Composable
fun WoollyTopHeader(
    modifier: Modifier = Modifier,
    showBaaBubble: Boolean = true
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // Cute Lamb Avatar
        Surface(
            shape = CircleShape,
            color = Color.White,
            shadowElevation = 3.dp,
            modifier = Modifier.size(44.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_app_icon_1787697277604),
                contentDescription = "Woolly Lamb",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }

        if (showBaaBubble) {
            Spacer(modifier = Modifier.width(6.dp))
            // Baa! Speech Bubble
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.White.copy(alpha = 0.95f),
                border = androidx.compose.foundation.BorderStroke(1.dp, WoollyCardBorder),
                modifier = Modifier.offset(y = (-10).dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Baa! 🐑",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = WoollyDarkGreen
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Brand Name Woolly in Serif
        Text(
            text = "Woolly",
            fontFamily = FontFamily.Serif,
            fontWeight = FontWeight.Bold,
            fontSize = 32.sp,
            color = WoollyDarkGreen
        )
    }
}
