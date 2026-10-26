package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.SerialLabMainScreen
import com.example.ui.SerialViewModel
import com.example.ui.theme.SerialLabTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SerialViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SerialLabTheme {
                SerialLabMainScreen(viewModel = viewModel)
            }
        }
    }
}
