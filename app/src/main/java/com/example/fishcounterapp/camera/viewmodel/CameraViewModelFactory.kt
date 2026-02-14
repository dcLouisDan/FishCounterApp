package com.example.fishcounterapp.camera.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.fishcounterapp.AppContainer

class CameraViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CameraViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CameraViewModel(
                cameraRepository = container.cameraRepository,
                isOpenCvInitialized = container.isOpenCvInitialized,
                imageProcessor = container.imageProcessor
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}