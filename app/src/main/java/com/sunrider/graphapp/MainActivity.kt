package com.sunrider.graphapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.sunrider.graphapp.ui.screens.MainScreen
import com.sunrider.graphapp.ui.screens.MatrixInputScreen
import com.sunrider.graphapp.ui.theme.GraphAppTheme
import com.sunrider.graphapp.viewmodel.GraphViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: GraphViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            var showMatrixInput by rememberSaveable { mutableStateOf(false) }

            GraphAppTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (showMatrixInput) {
                        MatrixInputScreen(
                            onApply = { size, matrix, isWeighted, isDirected ->
                                viewModel.applyAdjacencyMatrix(size, matrix, isWeighted, isDirected)
                                showMatrixInput = false
                            },
                            onBack = { showMatrixInput = false },
                            modifier = Modifier.padding(innerPadding)
                        )
                    } else {
                        MainScreen(
                            viewModel = viewModel,
                            onOpenMatrixInput = { showMatrixInput = true },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}
