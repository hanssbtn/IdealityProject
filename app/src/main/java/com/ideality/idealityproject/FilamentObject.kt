package com.ideality.idealityproject

import android.content.Context
import android.view.Surface
import android.view.SurfaceView
import com.google.android.filament.Engine
import com.google.android.filament.EntityManager
import com.google.android.filament.SwapChain
import com.google.android.filament.Viewport
import com.google.android.filament.android.DisplayHelper
import com.google.android.filament.android.UiHelper
import com.google.android.filament.gltfio.AssetLoader
import com.google.android.filament.gltfio.ResourceLoader
import com.google.android.filament.gltfio.UbershaderProvider

class FilamentObject {
    val eglContext = OpenGL.createEGLContext()
    val engine = Engine.create(eglContext)
    val renderer = engine.createRenderer()
    val scene = engine.createScene()
    val view = engine.createView()
    val cameraEntity = engine.entityManager.create()
    val camera = engine.createCamera(cameraEntity).apply {
        setExposure(16f, 1/125f, 100f)
    }
    val assetLoader =
        AssetLoader(engine, UbershaderProvider(engine), EntityManager.get())

    val resourceLoader =
        ResourceLoader(engine)

    var swapChain: SwapChain? = null
    lateinit var displayHelper: DisplayHelper
    lateinit var uiHelper: UiHelper

    fun destroy() {
        uiHelper.detach()
        swapChain?.let {
            engine.destroySwapChain(it)
        }
        engine.destroyView(view)
        engine.destroyScene(scene)
        engine.destroyRenderer(renderer)
        engine.runCatching {
            this.destroyCameraComponent(cameraEntity)
            this.destroyEntity(cameraEntity)
            this.entityManager.destroy(cameraEntity)
        }
        resourceLoader.destroy()
        assetLoader.destroy()
        engine.destroy()
        eglContext.destroy()
    }

    fun create(ctx: Context): SurfaceView {
        val surfaceView = SurfaceView(ctx)
        displayHelper = DisplayHelper(ctx)
        uiHelper = UiHelper(UiHelper.ContextErrorPolicy.CHECK).apply {
            renderCallback = object : UiHelper.RendererCallback {
                override fun onNativeWindowChanged(surface: Surface) {
                    swapChain?.let {
                        engine.destroySwapChain(it)
                        swapChain = engine.createSwapChain(surface)
                    }
                    displayHelper.attach(renderer, surfaceView.display)
                }

                override fun onDetachedFromSurface() {
                    displayHelper.detach()
                    swapChain?.let {
                        engine.destroySwapChain(it)
                        engine.flushAndWait()
                        swapChain = null
                    }
                }

                override fun onResized(width: Int, height: Int) {
                    view.viewport = Viewport(0, 0, width, height)
                }
            }

            attachTo(surfaceView)
        }

        return surfaceView
    }
}