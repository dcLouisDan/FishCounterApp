package com.example.fishcounterapp

import android.content.Context
import com.example.fishcounterapp.camera.data.CameraRepository
import com.example.fishcounterapp.domain.processing.ImageProcessor


class AppContainer(private val context: Context, val isOpenCvInitialized: Boolean) {
    val cameraRepository: CameraRepository by lazy {
        CameraRepository(context)
    }

    val imageProcessor: ImageProcessor? by lazy {
        if (isOpenCvInitialized) ImageProcessor() else null
    }
}