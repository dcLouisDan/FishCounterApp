package com.example.fishcounterapp.camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fishcounterapp.AppContainer
import com.example.fishcounterapp.camera.data.CameraRepository

class CameraViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(
                cameraRepository = container.cameraRepository,
                isOpenCvInitialized = container.isOpenCvInitialized
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}