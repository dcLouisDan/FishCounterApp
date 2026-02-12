package com.example.fishcounterapp.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fishcounterapp.FishCounterApplication
import com.example.fishcounterapp.camera.viewmodel.CameraViewModel
import com.example.fishcounterapp.camera.viewmodel.CameraViewModelFactory

@Composable
fun cameraViewModel(): CameraViewModel {
    val app = LocalContext.current.applicationContext as FishCounterApplication
    return viewModel(factory = CameraViewModelFactory(app.appContainer))
}