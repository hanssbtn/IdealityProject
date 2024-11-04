package com.ideality.idealityproject

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(openAndPopUp: (String, String) -> Unit) {
    val ctx = LocalContext.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        LinearProgressIndicator(
            modifier = Modifier.fillMaxWidth(0.8f).height(20.dp)
        )
    }
    LaunchedEffect(true) {
        Toast.makeText(ctx, "Logged in as :${Firebase.auth.currentUser?.uid ?: "none"}", Toast.LENGTH_SHORT).show()
        delay(2000)
        if (Firebase.auth.currentUser != null) {
            openAndPopUp(MAIN, SPLASH_SCREEN)
        } else {
            openAndPopUp(LOGIN_SCREEN, SPLASH_SCREEN)
        }
    }
}

@Composable
@Preview(showSystemUi = true, showBackground = true)
fun SplashScreenPreview() = SplashScreen { _,_ -> }
