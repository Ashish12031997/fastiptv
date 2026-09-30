package com.fastiptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.fastiptv.ui.navigation.AppNavigation
import com.fastiptv.ui.navigation.Screen
import com.fastiptv.ui.theme.FastIptvTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val startRoute = intent?.getStringExtra("startRoute") ?: Screen.Movies.route
        setContent {
            FastIptvTheme {
                AppNavigation(startDestination = startRoute)
            }
        }
    }
}

