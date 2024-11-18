package com.ideality.idealityproject.arcore

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.opengl.Matrix
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.google.android.filament.Entity
import com.google.android.filament.EntityManager
import com.google.android.filament.IndexBuffer
import com.google.android.filament.Material
import com.google.android.filament.MaterialInstance
import com.google.android.filament.RenderableManager
import com.google.android.filament.RenderableManager.PrimitiveType
import com.google.android.filament.Stream
import com.google.android.filament.Texture
import com.google.android.filament.Texture.PixelBufferDescriptor
import com.google.android.filament.TextureSampler
import com.google.android.filament.VertexBuffer
import com.google.android.filament.VertexBuffer.AttributeType
import com.google.android.filament.VertexBuffer.VertexAttribute
import com.google.android.filament.utils.Mat4
import com.google.ar.core.Config
import com.google.ar.core.Frame
import com.google.ar.core.Session
import com.google.ar.core.exceptions.CameraNotAvailableException
import com.google.ar.core.exceptions.MissingGlContextException
import com.google.ar.core.exceptions.SessionPausedException
import com.ideality.idealityproject.FilamentObject
import com.ideality.idealityproject.OpenGL
import com.ideality.idealityproject.setUV
import com.ideality.idealityproject.setXY
import java.nio.ByteBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import java.nio.channels.Channels
import kotlin.math.roundToInt

class ARCoreObject(
    activity: Activity,
    val session: Session,
    val occlusionMaterialFile: String = "materials/depth_material.filamat",
    val flatMaterialFile: String = "materials/flat_material.filamat"
) {
    companion object {
        const val TAG = "ARCoreObject"
        const val NEAR = 0.1f
        const val FAR = 30.0f
        const val NEAR_DOUBLE = 0.1
        const val FAR_DOUBLE = 30.0
        private const val POSITION_BUFFER_INDEX: Int = 0
        private const val UV_BUFFER_INDEX: Int = 1
    }

    data class ModelBuffers(val clipPosition: FloatBuffer, val textureUV: FloatBuffer, val triangleIndices: ShortBuffer)

    val filament: FilamentObject = FilamentObject()
    val surfaceView: SurfaceView
    val cameraTextureIDs: IntArray = IntArray(1).apply {
        OpenGL.createExternalTextureID()
    }

    lateinit var frame: Frame

    lateinit var depthTexture: Texture
    lateinit var flatTexture: Texture

    lateinit var stream: Stream

    lateinit var occlusionMaterialInstance: MaterialInstance
    lateinit var flatMaterialInstance: MaterialInstance

    private var currentDisplayRotation: Int = 0

    @Entity
    var depthRenderable: Int = 0
    @Entity
    var flatRenderable: Int = 0

    init {
        surfaceView = filament.create(activity)
    }

    private val cameraId: String = session.cameraConfig.cameraId

    private val cameraManager: CameraManager =
        ContextCompat.getSystemService(activity, CameraManager::class.java)!!

    fun configChange(activity: Activity) {
        val intrinsics = frame.camera.textureIntrinsics
        val dimensions = intrinsics.imageDimensions

        val displayWidth: Int
        val displayHeight: Int
        val displayRotation: Int

        DisplayMetrics()
            .also { displayMetrics ->
                @Suppress("DEPRECATION")
                (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) activity.display
                else activity.windowManager.defaultDisplay)!!
                    .also { display ->
                        display.getRealMetrics(displayMetrics)
                        displayRotation = display.rotation
                    }

                displayWidth = displayMetrics.widthPixels
                displayHeight = displayMetrics.heightPixels
            }

        currentDisplayRotation =
            when (displayRotation) {
                Surface.ROTATION_0 -> 0
                Surface.ROTATION_90 -> 90
                Surface.ROTATION_180 -> 180
                Surface.ROTATION_270 -> 270
                else -> throw RuntimeException("Invalid Display Rotation")
            }

        // camera width and height relative to display
        val cameraWidth: Int
        val cameraHeight: Int

        when (cameraManager
            .getCameraCharacteristics(cameraId)
            .get(CameraCharacteristics.SENSOR_ORIENTATION)!!) {
            0, 180 -> when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    cameraWidth = dimensions[0]
                    cameraHeight = dimensions[1]
                }

                else -> {
                    cameraWidth = dimensions[1]
                    cameraHeight = dimensions[0]
                }
            }

            else -> when (displayRotation) {
                Surface.ROTATION_0, Surface.ROTATION_180 -> {
                    cameraWidth = dimensions[1]
                    cameraHeight = dimensions[0]
                }

                else -> {
                    cameraWidth = dimensions[0]
                    cameraHeight = dimensions[1]
                }
            }
        }

        val cameraRatio: Float = cameraWidth.toFloat() / cameraHeight.toFloat()
        val displayRatio: Float = displayWidth.toFloat() / displayHeight.toFloat()

        val viewWidth: Int
        val viewHeight: Int

        if (displayRatio < cameraRatio) {
            // width constrained
            viewWidth = displayWidth
            viewHeight = (displayWidth.toFloat() / cameraRatio).roundToInt()
        } else {
            // height constrained
            viewWidth = (displayHeight.toFloat() * cameraRatio).roundToInt()
            viewHeight = displayHeight
        }

        surfaceView.updateLayoutParams<FrameLayout.LayoutParams> {
            width = viewWidth
            height = viewHeight
        }

        session.setDisplayGeometry(displayRotation, viewWidth, viewHeight)
    }

    fun update() {
        try {
            val tmp = session.update()
            if (tmp.timestamp == 0L || tmp.timestamp == this.frame.timestamp) {
                this.frame = tmp
            } else return

            val depthImage = when (session.config.depthMode) {
                Config.DepthMode.AUTOMATIC -> {
                    try {
                        frame.acquireDepthImage16Bits()
                    } catch (e: Exception) {
                        Log.e(TAG, "update: got error ", e)
                        null
                    }
                }
                Config.DepthMode.RAW_DEPTH_ONLY -> {
                    try {
                        frame.acquireRawDepthImage16Bits()
                    } catch (e: Exception) {
                        Log.e(TAG, "update: got error ", e)
                        null
                    }
                }
                Config.DepthMode.DISABLED -> null
            }
            if (depthImage != null) {
                depthTexture.setImage(
                    filament.engine,
                    0,
                    PixelBufferDescriptor(
                        depthImage.planes[0].buffer,
                        Texture.Format.RG,
                        Texture.Type.UBYTE,
                        1,
                        0,
                        0,
                        0,
                        Handler(Looper.getMainLooper()),
                    ) {
                        depthImage.close()
                    }
                )
                occlusionMaterialInstance.setParameter(
                    "uvTransform",
                    MaterialInstance.FloatElement.FLOAT4,
                    uvTransform(),
                    0,
                    4,
                )
                filament.scene.removeEntity(flatRenderable)
                filament.scene.addEntity(depthRenderable)
            } else {
                flatMaterialInstance.setParameter(
                    /* name = */ "uvTransform",
                    /* type = */ MaterialInstance.FloatElement.FLOAT4,
                    /* v = */ uvTransform(),
                    /* offset = */ 0,
                    /* count = */ 4,
                )

                filament.scene.removeEntity(depthRenderable)
                filament.scene.addEntity(flatRenderable)
            }

            // update camera projection
            filament.camera.setCustomProjection(
                /* inProjection = */
                FloatArray(16).let {
                    frame.camera.getProjectionMatrix(it, 0, NEAR, FAR)
                    DoubleArray(16) { idx ->
                        it[idx].toDouble()
                    }
                },
                /* near = */ NEAR_DOUBLE,
                /* far = */ FAR_DOUBLE,
            )

            val cameraTransform = FloatArray(16).apply { frame.camera.displayOrientedPose.toMatrix(this, 0) }
            filament.camera.setModelMatrix(cameraTransform)
            val instance = filament.engine.transformManager.create(depthRenderable)
            filament.engine.transformManager.setTransform(instance, cameraTransform)
        } catch (cnae: CameraNotAvailableException) {
            Log.e(TAG, "update: Camera is not available. ", cnae)
        } catch (spe: SessionPausedException) {
            Log.e(TAG, "update: Session is paused. ", spe)
        } catch (mgce: MissingGlContextException) {
            Log.e(TAG, "update: GL context is missing. ", mgce)
        }
    }

    private fun initMaterialInstances(activity: Activity) {
        try {
            if (this::stream.isInitialized.not()) {
                val intrinsics = frame.camera.imageIntrinsics.imageDimensions
                val width = intrinsics[0]
                val height = intrinsics[1]
                stream = Stream.Builder()
                    .stream(SurfaceTexture(cameraTextureIDs[0]))
                    .width(width)
                    .height(height)
                    .build(filament.engine)
            }

            flatMaterialInstance = activity.assets.let {
                val buf = it.openFd(flatMaterialFile).use { fd ->
                    val stream = fd.createInputStream()
                    val buf = ByteBuffer.allocate(fd.length.toInt())

                    stream.channel?.read(buf) ?: Channels.newChannel(stream).let { chan ->
                        chan.read(buf)
                        chan.close()
                    }

                    buf.apply { rewind() }
                }
                Material.Builder().payload(buf, buf.remaining()).build(filament.engine).also { mat ->
                    Log.d(TAG, "Material.isDepthCullingEnabled: ${mat.isDepthCullingEnabled}")
                    Log.d(TAG, "Material.isDepthWriteEnabled: ${mat.isDepthWriteEnabled}")
                    Log.d(TAG, "Material.isColorWriteEnabled: ${mat.isColorWriteEnabled}")
                }.createInstance("flat_texture").apply {
                    setParameter("cameraTexture",
                        Texture.Builder()
                            .sampler(Texture.Sampler.SAMPLER_EXTERNAL)
                            .importTexture(cameraTextureIDs[0].toLong())
                            .build(filament.engine).apply {
                                setExternalStream(filament.engine, this@ARCoreObject.stream)
                                flatTexture = this
                            }, TextureSampler(
                            TextureSampler.MinFilter.LINEAR,
                            TextureSampler.MagFilter.LINEAR,
                            TextureSampler.WrapMode.CLAMP_TO_EDGE).apply {
                        }
                    )

                }
            }
            initFlat()

            // Loop until valid frame is retrieved.
            while (this::frame.isInitialized.not()) {
                session.update().let {
                    // If the timestamp is not equal to 0, then the frame is new and valid.
                    if (it.timestamp != 0L) {
                        this.frame = it
                    }
                }
            }

            configChange(activity)
            if (session.config.depthMode == Config.DepthMode.RAW_DEPTH_ONLY || session.config.depthMode == Config.DepthMode.AUTOMATIC) {
                val depthImage = frame.acquireDepthImage16Bits()
                occlusionMaterialInstance = activity.assets.let {
                    val buf = it.openFd(occlusionMaterialFile).use { fd ->
                        val stream = fd.createInputStream()
                        val buf = ByteBuffer.allocate(fd.length.toInt())

                        stream.channel?.read(buf) ?: Channels.newChannel(stream).let { chan ->
                            chan.read(buf)
                            chan.close()
                        }

                        buf.apply { rewind() }
                    }
                    Material.Builder().payload(buf, buf.remaining()).build(filament.engine)
                        .also { mat ->
                            Log.d(TAG, "Material.isDepthCullingEnabled: ${mat.isDepthCullingEnabled}")
                            Log.d(TAG, "Material.isDepthWriteEnabled: ${mat.isDepthWriteEnabled}")
                            Log.d(TAG, "Material.isColorWriteEnabled: ${mat.isColorWriteEnabled}")
                        }.createInstance("depth_texture").apply {
                            setParameter("depthTexture", Texture.Builder()
                                .sampler(Texture.Sampler.SAMPLER_2D)
                                .format(Texture.InternalFormat.RG8)
                                .width(depthImage.width)
                                .height(depthImage.height)
                                .levels(1).build(filament.engine).also { texture ->
                                    depthTexture = texture
                                }, TextureSampler())
                            setParameter("uvTransform", MaterialInstance.FloatElement.FLOAT4, Mat4.identity().toFloatArray(), 0, 4)
                        }
                }
                initDepth()
            }
        } catch (cnae: CameraNotAvailableException) {
            Log.e(TAG, "initMaterialInstances: Camera is not available. ", cnae)
        } catch (spe: SessionPausedException) {
            Log.e(TAG, "initMaterialInstances: Session is paused. ", spe)
        } catch (mgce: MissingGlContextException) {
            Log.e(TAG, "initMaterialInstances: GL context is missing. ", mgce)
        }
    }

    private fun initFlat() {
        val tes = tessellation()
        RenderableManager
            .Builder(1)
            .castShadows(false)
            .receiveShadows(false)
            .culling(false)
            .geometry(
                /* index = */ 0,
                /* type = */ PrimitiveType.TRIANGLES,
                /* vertices = */
                VertexBuffer
                    .Builder()
                    .vertexCount(tes.clipPosition.array().count())
                    .bufferCount(2)
                    .attribute(
                        /* attribute = */ VertexAttribute.POSITION,
                        /* bufferIndex = */ POSITION_BUFFER_INDEX,
                        /* attributeType = */ AttributeType.FLOAT2,
                        /* byteOffset = */ 0,
                        /* byteStride = */ 0,
                    )
                    .attribute(
                        /* attribute = */ VertexAttribute.UV0,
                        /* bufferIndex = */ UV_BUFFER_INDEX,
                        /* attributeType = */ AttributeType.FLOAT2,
                        /* byteOffset = */ 0,
                        /* byteStride = */ 0,
                    )
                    .build(filament.engine)
                    .also { vertexBuffer ->
                        vertexBuffer.setBufferAt(
                            /* engine = */ filament.engine,
                            /* bufferIndex = */ POSITION_BUFFER_INDEX,
                            /* buffer = */ tes.clipPosition,
                        )

                        vertexBuffer.setBufferAt(
                            /* engine = */ filament.engine,
                            /* bufferIndex = */ UV_BUFFER_INDEX,
                            /* buffer = */ tes.textureUV,
                        )
                    },
                /* indices = */
                IndexBuffer
                    .Builder()
                    .indexCount(tes.triangleIndices.array().count())
                    .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                    .build(filament.engine)
                    .apply { setBuffer(filament.engine, tes.triangleIndices) },
            )
            .material(0, flatMaterialInstance)
            .build(filament.engine, EntityManager.get().create().also { flatRenderable = it })
    }

    private fun tessellation(): ModelBuffers {
        val tesWidth = 1
        val tesHeight = 1

        val clipPosition = FloatArray((((tesWidth * tesHeight) + tesWidth + tesHeight + 1) * 2))

        val uvs = FloatArray((((tesWidth * tesHeight) + tesWidth + tesHeight + 1) * 2))

        for (k in 0..tesHeight) {
            val v = k.toFloat() / tesHeight.toFloat()
            val y = (k.toFloat() / tesHeight.toFloat()) * 2f - 1f

            for (i in 0..tesWidth) {
                val u = i.toFloat() / tesWidth.toFloat()
                val x = (i.toFloat() / tesWidth.toFloat()) * 2f - 1f
                clipPosition.setXY(k * (tesWidth + 1) + i, x, y)
                uvs.setUV(k * (tesWidth + 1) + i, u, v)
            }
        }

        val triangleIndices = ShortArray(tesWidth * tesHeight * 6)

        for (k in 0 until tesHeight) {
            for (i in 0 until tesWidth) {
                triangleIndices[((k * tesWidth + i) * 6) + 0] =
                    ((k * (tesWidth + 1)) + i + 0).toShort()
                triangleIndices[((k * tesWidth + i) * 6) + 1] =
                    ((k * (tesWidth + 1)) + i + 1).toShort()
                triangleIndices[((k * tesWidth + i) * 6) + 2] =
                    ((k + 1) * (tesWidth + 1) + i).toShort()

                triangleIndices[((k * tesWidth + i) * 6) + 3] =
                    ((k + 1) * (tesWidth + 1) + i).toShort()
                triangleIndices[((k * tesWidth + i) * 6) + 4] =
                    ((k * (tesWidth + 1)) + i + 1).toShort()
                triangleIndices[((k * tesWidth + i) * 6) + 5] =
                    ((k + 1) * (tesWidth + 1) + i + 1).toShort()
            }
        }

        return ModelBuffers(FloatBuffer.wrap(clipPosition).also {
            Log.d("ModelBuffer", "clipPosition: [position: ${it.position()}, limit: ${it.limit()}, order: ${it.order()}, remaining: ${it.remaining()}]")
        }, FloatBuffer.wrap(uvs).also {
            Log.d("ModelBuffer", "uvs: [position: ${it.position()}, limit: ${it.limit()}, order: ${it.order()}, remaining: ${it.remaining()}]")
        }, ShortBuffer.wrap(triangleIndices).also {
            Log.d("ModelBuffer", "triangleIndices: [position: ${it.position()}, limit: ${it.limit()}, order: ${it.order()}, remaining: ${it.remaining()}]")
        })
    }

    private fun initDepth() {
        val tes = tessellation()
        RenderableManager
            .Builder(1)
            .castShadows(false)
            .receiveShadows(false)
            .culling(false)
            .geometry(
                0,
                PrimitiveType.TRIANGLES,
                VertexBuffer
                    .Builder()
                    .vertexCount(tes.clipPosition.array().count())
                    .bufferCount(2)
                    .attribute(
                        /* attribute = */ VertexAttribute.POSITION,
                        /* bufferIndex = */ POSITION_BUFFER_INDEX,
                        /* attributeType = */ AttributeType.FLOAT2,
                        /* byteOffset = */ 0,
                        /* byteStride = */ 0,
                    )
                    .attribute(
                        /* attribute = */ VertexAttribute.UV0,
                        /* bufferIndex = */ UV_BUFFER_INDEX,
                        /* attributeType = */ AttributeType.FLOAT2,
                        /* byteOffset = */ 0,
                        /* byteStride = */ 0,
                    )
                    .build(filament.engine)
                    .also { vertexBuffer ->
                        vertexBuffer.setBufferAt(
                            /* engine = */ filament.engine,
                            /* bufferIndex = */ POSITION_BUFFER_INDEX,
                            /* buffer = */ tes.clipPosition
                        )

                        vertexBuffer.setBufferAt(
                            /* engine = */ filament.engine,
                            /* bufferIndex = */ UV_BUFFER_INDEX,
                            /* buffer = */ tes.textureUV
                        )
                    },
                IndexBuffer
                    .Builder()
                    .indexCount(tes.triangleIndices.array().count())
                    .bufferType(IndexBuffer.Builder.IndexType.USHORT)
                    .build(filament.engine)
                    .apply { setBuffer(filament.engine, tes.triangleIndices) },
            )
            .material(0, occlusionMaterialInstance)
            .build(filament.engine, EntityManager.get().create().also { depthRenderable = it })
    }

    fun uvTransform() = FloatArray(16).apply {
        Matrix.setIdentityM(this, 0)
        Matrix.translateM(this, 0, -0.5f, -0.5f, 0f)
        Matrix.rotateM(this, 0, imageRotation().toFloat(), 0f, 0f, 0f)
        Matrix.translateM(this, 0, -0.5f, -0.5f, -0.5f)
    }

    private fun imageRotation(): Int = (cameraManager
        .getCameraCharacteristics(cameraId)
        .get(CameraCharacteristics.SENSOR_ORIENTATION)!! +
            when (currentDisplayRotation) {
                0 -> 90
                90 -> 0
                180 -> 270
                270 -> 180
                else -> throw RuntimeException("Unreachable")
            } + 270) % 360

    fun destroy() {
        depthTexture.let {
            filament.engine.destroyTexture(it)
        }
        flatTexture.let {
            filament.engine.destroyTexture(it)
        }
        filament.destroy()
    }
}