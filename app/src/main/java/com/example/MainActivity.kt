package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.example.data.AppDatabase
import com.example.data.WarpConfigRepository
import com.example.ui.WarpDashboard
import com.example.ui.WarpViewModel
import com.example.ui.WarpViewModelFactory
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Room Database and Repository
        val database = AppDatabase.getDatabase(this)
        val repository = WarpConfigRepository(database.warpConfigDao())
        
        // Initialize ViewModel using the custom factory
        val factory = WarpViewModelFactory(application, repository)
        val viewModel = androidx.lifecycle.ViewModelProvider(this, factory)[WarpViewModel::class.java]
        
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme(darkTheme = true) { // Force Dark Mode for ultra-premium Cyberpunk feel
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Box(modifier = Modifier.padding(innerPadding)) {
                        WarpDashboard(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
