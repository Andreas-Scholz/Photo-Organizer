package io.github.andreasscholz.photoorganizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import io.github.andreasscholz.photoorganizer.ui.navigation.AppNavHost
import io.github.andreasscholz.photoorganizer.ui.theme.PhotoOrganizerTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PhotoOrganizerTheme {
                AppNavHost()
            }
        }
    }
}
