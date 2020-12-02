package com.testapp.speedometer.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class GenerateService : Service() {
    private var callbackFunction: IGenerateServiceCallback? = null

    override fun onBind(intent: Intent): IBinder {
        return localBinder
    }

    private val localBinder = object : IGenerateService.Stub() {
        override fun registerCallback(callback: IGenerateServiceCallback?, maxSpeed: Double) {
            callbackFunction = callback ?: return
            initGenerateNeedleMotion(needlePositionImpl, maxSpeed)
        }

        override fun startGenerate() {
            startGenerateNeedleMotion()
        }

        override fun stopGenerate() {
            stopGenerateNeedleMotion()
        }

        override fun unregisterCallback() {
            stopGenerateNeedleMotion()
        }
    }

    private val needlePositionImpl = object : NeedlePositionInterface {
        override fun onValueChanged(value: Int) {
            callbackFunction?.valueChanged(value)
        }
    }

    companion object {
        private external fun initGenerateNeedleMotion(needlePosition: NeedlePositionInterface, maxSpeed: Double)
        private external fun startGenerateNeedleMotion()
        private external fun stopGenerateNeedleMotion()

        init {
            System.loadLibrary("generateService-lib")
        }
    }
}
