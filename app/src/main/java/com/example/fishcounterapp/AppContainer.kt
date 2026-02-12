package com.example.fishcounterapp

import android.content.Context
import com.example.fishcounterapp.camera.data.CameraRepository


class AppContainer(private val context: Context, val isOpenCvInitialized: Boolean) {
    val cameraRepository: CameraRepository by lazy {
        CameraRepository(context)
    }
}