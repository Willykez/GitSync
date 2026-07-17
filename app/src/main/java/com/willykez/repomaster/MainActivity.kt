package com.willykez.repomaster

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.willykez.repomaster.navigation.RepoMasterApp
import com.willykez.repomaster.ui.theme.RepoMasterTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            RepoMasterTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RepoMasterApp()
                }
            }
        }
    }
}
