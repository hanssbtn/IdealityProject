package com.ideality.idealityproject

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.constraintlayout.compose.ConstraintLayout
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Config
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.arcore.createAnchorOrNull
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.RotateGestureDetector
import io.github.sceneview.gesture.ScaleGestureDetector
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {
    private var userRequestedInstall = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            Main()
            CheckAndInstallARCore()
        }
    }

    @Composable
    fun Main() {
        // Define your composable functions here
        ConstraintLayout(
            Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // Add your UI elements here
            val (left, right, up, down) = createRefs()
            // Model list
            val (list) = createRefs()

            var listenerCreator by remember {
                mutableStateOf<io.github.sceneview.gesture.GestureDetector.OnGestureListener?>(null)
            }

            val anchorNodes = remember {
                mutableStateOf<AnchorNode?>(null)
            }

            ARScene(
                modifier = Modifier
                    .fillMaxSize(),
                onGestureListener = listenerCreator,
                sessionConfiguration = { sess, config ->
                    config.depthMode =
                        when (sess.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                            true -> {
                                Log.d("ARScene", "DepthMode = Automatic")
                                Config.DepthMode.AUTOMATIC
                            }

                            false -> {
                                Log.d("ARScene", "DepthMode = Disabled")
                                Config.DepthMode.DISABLED
                            }
                        }
                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
                    config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
                    config.semanticMode = Config.SemanticMode.DISABLED
                },
                onSessionCreated = { session ->
                    Log.d("ARScene", "Session created ($session)")
                },
                onSessionResumed = { session ->
                    val orientation = when (this@MainActivity.resources.configuration.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                        Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                        Configuration.ORIENTATION_UNDEFINED -> "Undefined"
                        else -> ""
                    }
                    Log.d("ARScene", "Session resumed, orientation: $orientation")
                },
                onViewCreated = {
                    Log.d("ARScene", "View created ($this)")

                    val handleSingleTapConfirmed = { e: MotionEvent, node: Node? ->
                        val result = this.hitTestAR(
                            e.x,
                            e.y,
                            depthPoint = this.session?.isDepthModeSupported(Config.DepthMode.AUTOMATIC)
                                ?: false
                        ) { hitRes ->
                            val anchor = hitRes.createAnchorOrNull()
                            if (anchor != null) {
                                val anchorNode = AnchorNode(engine = this.engine, anchor = anchor)
                                this@ARScene.clearChildNodes()
                                anchorNodes.value = anchorNode
                                this@ARScene.addChildNode(anchorNode)
                                Log.d("ARScene", "added anchor node ${anchorNode.position}")
                            }

                            false
                        }
                        val ht = this.session?.frame?.hitTestInstantPlacement(e.xPrecision, e.yPrecision, 0.5F)!!.getOrNull(0)
                        if (ht != null) {
                            Log.d("ARScene","Distance: ${ht.distance}")
                        }
                        result
                    }


                    listenerCreator =
                        object : io.github.sceneview.gesture.GestureDetector.OnGestureListener {
                            override fun onContextClick(e: MotionEvent, node: Node?) {
                                Log.d("ARScene", "onContextClick: Not yet implemented")
                            }

                            override fun onDoubleTap(e: MotionEvent, node: Node?) {
                                Log.d("ARScene", "onDoubleTap: Not yet implemented")
                            }

                            override fun onDoubleTapEvent(e: MotionEvent, node: Node?) {
                                val filePath = "Modern_Club_Chair.glb"
                                try {
                                    val testModel = ModelNode(
                                        modelLoader.createModelInstance(filePath)
                                    ).apply {
                                        isEditable = true
                                    }
                                    val boundingBoxNode = CubeNode(
                                        engine,
                                        size = testModel.extents,
                                        center = testModel.center,
                                        materialInstance = materialLoader.createColorInstance(Color.White.copy(alpha = 0.5f))
                                    ).apply {
                                        isVisible = false
                                    }
                                    testModel.addChildNode(boundingBoxNode)
                                    if (anchorNodes.value != null) {
                                        Log.d("ARScene", "Added model node $testModel. size: ${testModel.size}")
                                        anchorNodes.value!!.addChildNode(testModel)
                                    }
//                                    System.gc()
//                                    Runtime.getRuntime().runFinalization()
                                    Log.d("ARScene", "Opened file $filePath")
                                } catch (ioe: IOException) {
                                    Log.e("ARScene", "Cannot open sample file $filePath")
                                } finally {
                                    Toast.makeText(context, "OnDoubleTapUpEvent", Toast.LENGTH_SHORT).show()
                                }
                            }

                            override fun onDown(e: MotionEvent, node: Node?) {
                                Log.d("ARScene", "onDown: Not yet implemented")
                            }

                            override fun onFling(
                                e1: MotionEvent?,
                                e2: MotionEvent,
                                node: Node?,
                                velocity: Float2
                            ) {
                                Log.d("ARScene", "onFling: Not yet implemented")
                            }

                            override fun onLongPress(e: MotionEvent, node: Node?) {
                                Log.d("ARScene", "onLongPress: Not yet implemented")
                            }

                            override fun onMove(
                                detector: MoveGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onMove: Not yet implemented")
                            }

                            override fun onMoveBegin(
                                detector: MoveGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onMoveBegin: Not yet implemented")
                            }

                            override fun onMoveEnd(
                                detector: MoveGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onMoveEnd: Not yet implemented")
                            }

                            override fun onRotate(
                                detector: RotateGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onRotate: Not yet implemented")
                            }

                            override fun onRotateBegin(
                                detector: RotateGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onRotateBegin Not yet implemented")
                            }

                            override fun onRotateEnd(
                                detector: RotateGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onRotateEnd: Not yet implemented")
                            }

                            override fun onScale(
                                detector: ScaleGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onScale: Not yet implemented")
                            }

                            override fun onScaleBegin(
                                detector: ScaleGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onScaleBegin: Not yet implemented")
                            }

                            override fun onScaleEnd(
                                detector: ScaleGestureDetector,
                                e: MotionEvent,
                                node: Node?
                            ) {
                                Log.d("ARScene", "onScaleEnd: Not yet implemented")
                            }

                            override fun onScroll(
                                e1: MotionEvent?,
                                e2: MotionEvent,
                                node: Node?,
                                distance: Float2
                            ) {
                                Log.d("ARScene", "onScroll: Not yet implemented")
                            }

                            override fun onShowPress(e: MotionEvent, node: Node?) {
                                Log.d("ARScene", "onShowPress: Not yet implemented")
                            }

                            override fun onSingleTapConfirmed(e: MotionEvent, node: Node?) {
                                var res = handleSingleTapConfirmed(e, node)
                                Toast.makeText(
                                    context,
                                    "onSingleTapUpConfirmed",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }

                            override fun onSingleTapUp(e: MotionEvent, node: Node?) {
                                Log.d("ARScene", "onSingleTapUp: Not yet implemented")
                            }
                        }
                    onGestureListener = listenerCreator

                    Log.d("ARScene", "onGestureListener: ${this.onGestureListener.toString()}")
                }
            )
        }
    }

    private fun getARCoreVersion(): String? {
        return try {
            val packageInfo = packageManager.getPackageInfo("com.google.ar.core", 0)
            packageInfo.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    @Composable
    private fun CheckAndInstallARCore() {
        val localContext = LocalContext.current
        var availability = ArCoreApk.getInstance().checkAvailability(localContext)
        if (availability == Availability.SUPPORTED_INSTALLED) return
        val maxRetries = 10
        val shouldRetry = remember { mutableStateOf(true) }
        val title = "Error"
        val showDialog = remember { mutableStateOf(true) }
        val text = remember { mutableStateOf("") }
        val confirmAction = remember { mutableStateOf<(() -> Unit)?>(null) }
        while (true) {
            LaunchedEffect(Unit) {
                var currentRetryCount = 0
                while (true) {
                    availability = ArCoreApk.getInstance().checkAvailability(localContext)
                    when (availability) {
                        Availability.UNKNOWN_CHECKING -> {
                            delay(250)
                        }

                        Availability.UNSUPPORTED_DEVICE_NOT_CAPABLE -> {
                            text.value = "This device does not have adequate ARCore support."
                            break
                        }

                        Availability.SUPPORTED_INSTALLED -> {
                            Toast.makeText(
                                localContext,
                                "Successfully installed ARCore",
                                Toast.LENGTH_SHORT
                            ).show()
                            showDialog.value = false
                            break
                        }

                        Availability.UNKNOWN_TIMED_OUT -> {
                            currentRetryCount++
                            if (currentRetryCount == maxRetries) {
                                text.value =
                                    "Failed to check device compatibility (timed out). Would you like to try again?"
                                confirmAction.value = {
                                    Toast.makeText(localContext, "Retrying", Toast.LENGTH_SHORT)
                                        .show()
                                    shouldRetry.value = true
                                }
                                break
                            }
                        }

                        Availability.SUPPORTED_NOT_INSTALLED -> {
                            var resp = requestARCoreInstall()
                            while (!resp) {
                                if (!userRequestedInstall) {
                                    text.value = "User declined installation."
                                    return@LaunchedEffect
                                }
                                resp = requestARCoreInstall()
                            }
                            break
                        }

                        Availability.SUPPORTED_APK_TOO_OLD -> {
                            text.value =
                                "The current ARCore version is not supported (version ${getARCoreVersion()})"
                            break
                        }

                        Availability.UNKNOWN_ERROR -> {
                            text.value = "An unknown error occurred. Try again?"
                            confirmAction.value = {
                                shouldRetry.value = true
                            }
                            break
                        }
                    }
                }
            }
            if (!showDialog.value) return
            AlertDialog(
                icon = null,
                title = { Text(title) },
                text = { Text(text.value) },
                onDismissRequest = {
                    showDialog.value = false
                    cleanup()
                    exitProcess(1)
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            confirmAction.value?.invoke()
                            showDialog.value = false
                        }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDialog.value = false
                            cleanup()
                            exitProcess(1)
                        }) {
                        Text("No")
                    }
                },
            )
            if (!shouldRetry.value) return
        }
    }

    private fun cleanup() {

    }

    private fun requestARCoreInstall(): Boolean {
        try {
            when (ArCoreApk.getInstance().requestInstall(this, userRequestedInstall)) {
                ArCoreApk.InstallStatus.INSTALLED -> {
                    // ARCore is installed, proceed with AR functionality
                    return true
                }

                ArCoreApk.InstallStatus.INSTALL_REQUESTED -> {
                    // ARCore installation requested, pause the app until installation completes
                    userRequestedInstall = false
                }
            }
        } catch (e: Exception) {
            return false
        }
        return false
    }
}