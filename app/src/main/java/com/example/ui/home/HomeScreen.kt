package com.example.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ui.auth.AuthState
import com.example.ui.auth.AuthViewModel
import com.example.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val successState = authState as? AuthState.Success
    val email = successState?.email ?: ""
    val displayName = successState?.profile?.displayName?.ifBlank { null } ?: email.substringBefore("@")

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Woolly Pasture",
                        fontFamily = FontFamily.Serif,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.signOut()
                        onSignOut()
                    }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = WoollyPrimaryGreen
                )
            )
        }
    ) { paddingValues ->
        WoollySceneryBackground(modifier = Modifier.padding(paddingValues)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    border = BorderStroke(3.dp, WoollyCardBorder),
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(130.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_app_icon_1787697277604),
                        contentDescription = "App Icon",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = WoollyCardBg,
                    border = BorderStroke(1.dp, WoollyCardBorder),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Welcome, $displayName! 🌾",
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = WoollyDarkGreen,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "You are signed in as:",
                            fontSize = 13.sp,
                            color = WoollyMutedText
                        )

                        Text(
                            text = email,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = WoollyPrimaryGreen,
                            modifier = Modifier.padding(top = 4.dp)
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFEFF5F0),
                            border = BorderStroke(1.dp, Color(0xFFD3E4D6)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Your sheep profile has been synced with Firestore. Everything is safe and cozy in your farm!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = WoollyPrimaryGreen,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}
