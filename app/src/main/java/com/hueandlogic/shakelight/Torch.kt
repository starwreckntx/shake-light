package com.hueandlogic.shakelight

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.os.Looper

/** Torch control via CameraManager. Tracks real state so the quick-settings tile and this app stay in sync. */
class Torch(context: Context) {
    private val cm = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraId: String? = findFlashCamera()
    @Volatile var isOn = false
        private set

    private val callback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(id: String, enabled: Boolean) {
            if (id == cameraId) isOn = enabled
        }
        override fun onTorchModeUnavailable(id: String) {
            if (id == cameraId) isOn = false
        }
    }

    fun start() = cm.registerTorchCallback(callback, Handler(Looper.getMainLooper()))
    fun stop() = cm.unregisterTorchCallback(callback)

    /** Returns false if no flash or the camera is busy (e.g. camera app open). */
    fun set(on: Boolean): Boolean {
        val id = cameraId ?: return false
        return try {
            cm.setTorchMode(id, on)
            isOn = on
            true
        } catch (e: Exception) {
            false
        }
    }

    fun toggle(): Boolean = set(!isOn)

    private fun findFlashCamera(): String? {
        val ids = cm.cameraIdList
        fun hasFlash(id: String) =
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
        fun isBack(id: String) =
            cm.getCameraCharacteristics(id).get(CameraCharacteristics.LENS_FACING) ==
                CameraCharacteristics.LENS_FACING_BACK
        return ids.firstOrNull { hasFlash(it) && isBack(it) } ?: ids.firstOrNull { hasFlash(it) }
    }
}
