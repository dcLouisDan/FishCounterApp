package com.example.fishcounterapp

import android.app.Application
import android.util.Log
import org.opencv.android.OpenCVLoader

class FishCounterApplication : Application() {

    lateinit var appContainer: AppContainer

    override fun onCreate() {
        super.onCreate()
        val isOpenCvInitialized = initOpenCV()
        appContainer = AppContainer(this, isOpenCvInitialized)
    }

    private fun initOpenCV(): Boolean {
        return try {
            OpenCVLoader.initLocal().also { success ->
                if (success) {
                    Log.d("OpenCV", "OpenCV loaded successfully")
                } else {
                    Log.d("OpenCV", "OpenCV failed to load")
                }
            }
        } catch (e: Exception) {
            Log.e("OpenCV", "OpenCV initialization failed", e)
            false
        }
    }
}