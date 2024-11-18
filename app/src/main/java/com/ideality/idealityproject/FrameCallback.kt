package com.ideality.idealityproject

import android.util.Log
import android.view.Choreographer
import com.google.ar.core.Frame
import com.ideality.idealityproject.arcore.ARCoreObject

class FrameCallback(
    val arCoreObject: ARCoreObject,
    val doFrame: (frame: Frame) -> Unit
): Choreographer.FrameCallback {

    private val choreographer: Choreographer = Choreographer.getInstance()

    override fun doFrame(frameTimeNanos: Long) {
        choreographer.postFrameCallback(this)

        if (// only render if we have an ar frame
            arCoreObject.frame.timestamp != 0L &&
            arCoreObject.filament.uiHelper.isReadyToRender &&
            // This means you are sending frames too quickly to the GPU
            arCoreObject.filament.renderer.beginFrame(arCoreObject.filament.swapChain!!, frameTimeNanos)
        ) {
            arCoreObject.filament.renderer.render(arCoreObject.filament.view)
            arCoreObject.filament.renderer.endFrame()
        }

        if (arCoreObject.frame.timestamp != 0L) {
            doFrame(arCoreObject.frame)
        }
    }

    fun stop() {
        Log.d("FrameCallback", "stopping callback")
        choreographer.removeFrameCallback(this)
    }


    fun start() {
        Log.d("FrameCallback", "starting callback")
        choreographer.postFrameCallback(this)
    }
}