package com.chlqudco.randomtour

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.activity.compose.setContent
import com.chlqudco.randomtour.ui.theme.RandomTourTheme

class MainActivity : ComponentActivity() {
    private val viewModel by viewModels<RandomTourViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            RandomTourTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    RandomTourApp(viewModel)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        viewModel.onHostStarted()
    }

    override fun onStop() {
        viewModel.onHostStopped()
        super.onStop()
    }
}
