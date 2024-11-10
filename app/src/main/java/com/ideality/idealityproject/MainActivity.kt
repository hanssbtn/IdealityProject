package com.ideality.idealityproject

import android.content.pm.PackageManager
import android.content.res.Configuration
import android.opengl.GLSurfaceView
import android.opengl.GLSurfaceView.RENDERMODE_CONTINUOUSLY
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.widget.Toast
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.createGraph
import com.google.ar.core.ArCoreApk
import com.google.ar.core.ArCoreApk.Availability
import com.google.ar.core.Config
import com.google.ar.core.DepthPoint
import com.google.ar.core.Plane
import com.google.ar.core.Point
import com.google.ar.core.Session
import com.google.ar.core.TrackingFailureReason
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.ideality.idealityproject.ui.theme.AppTheme
import dagger.hilt.android.AndroidEntryPoint
import dev.romainguy.kotlin.math.Float2
import io.github.sceneview.ar.ARScene
import io.github.sceneview.ar.ARSceneView
import io.github.sceneview.ar.node.AnchorNode
import io.github.sceneview.ar.rememberARCameraStream
import io.github.sceneview.ar.scene.PlaneRenderer
import io.github.sceneview.gesture.MoveGestureDetector
import io.github.sceneview.gesture.RotateGestureDetector
import io.github.sceneview.gesture.ScaleGestureDetector
import io.github.sceneview.math.toVector3
import io.github.sceneview.node.CubeNode
import io.github.sceneview.node.ModelNode
import io.github.sceneview.node.Node
import io.github.sceneview.rememberEngine
import io.github.sceneview.rememberMaterialLoader
import io.github.sceneview.rememberModelLoader
import kotlinx.coroutines.delay
import java.io.IOException
import kotlin.system.exitProcess

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        val TAG = "MainActivity"
    }


    private var userRequestedInstall = true
    private val loginVM: LoginViewModel by viewModels()
    private val normalArrow = "Pointing Arrow.glb"
    private var session: Session? = null
    private var depthModeIsSupported = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                Color.Transparent.toArgb(),
                Color.Transparent.toArgb()
            )
        )
        FirebaseApp.initializeApp(this)
        if ((Firebase.auth.currentUser != null) and Firebase.auth.currentUser!!.isAnonymous) {
            Log.d("MainActivity", "Logging out of anonymous user")
            loginVM.signOut()
        }
        Log.d("MainActivity",FirebaseApp.getInstance().options.apiKey)
        setContent {
            AppTheme {
                Navigation()
                CheckAndInstallARCore()
            }
        }
    }

    @Composable
    fun Navigation() {
        val navCtrl = rememberNavController()
        val navState = remember(navCtrl) { NavigationState(navCtrl) }
        val navGraph = remember(navCtrl) {
            navCtrl.createGraph(startDestination = SPLASH_SCREEN) {
                composable(MAIN) {
                    Main()
                }
                composable(PREVIEW_SCREEN) {
                    PreviewScreen()
                }
                composable(LOGIN_SCREEN) {
                    LoginScreen(vm = loginVM) { to, from ->
                        navState.navigateAndPopUp(to, from)
                    }
                }
                composable(SPLASH_SCREEN) {
                    SplashScreen { to, from -> navState.navigateAndPopUp(to, from) }
                }
            }
        }
        NavHost(
            modifier = Modifier.fillMaxSize().padding(WindowInsets.systemBars.asPaddingValues()),
            navController = navCtrl,
            graph = navGraph
        )
    }

    @Composable
    fun PreviewScreen() {
        val renderer = remember { PreviewScreenRenderer() }


        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = {
                GLSurfaceView(this).apply {
                    setRenderer(renderer)
                    setEGLContextClientVersion(2)
                    renderMode = RENDERMODE_CONTINUOUSLY
                    setWillNotDraw(false)
                }
            }
        )
    }
//
//    override fun onResume() {
//        super.onResume()
//        try {
//            if (session == null) {
//                session = Session(this).apply {
//                    config.lightEstimationMode = Config.LightEstimationMode.ENVIRONMENTAL_HDR
//                    this@MainActivity.depthModeIsSupported = isDepthModeSupported(Config.DepthMode.AUTOMATIC)
//                    config.depthMode = if (depthModeIsSupported) {
//                        Config.DepthMode.AUTOMATIC
//                    } else {
//                        Config.DepthMode.DISABLED
//                    }
//                    config.planeFindingMode = Config.PlaneFindingMode.HORIZONTAL_AND_VERTICAL
//                    config.focusMode = Config.FocusMode.AUTO
//                    config.instantPlacementMode = Config.InstantPlacementMode.LOCAL_Y_UP
//                    config.textureUpdateMode = Config.TextureUpdateMode.EXPOSE_HARDWARE_BUFFER
//                    config.updateMode = Config.UpdateMode.LATEST_CAMERA_IMAGE
//                }
//
//            }
//        } catch (e: Exception) {
//            Log.e("MainActivity", "Got error trying to create session", e)
//        }
//        session?.resume()
//    }

    @Preview(showBackground = true, showSystemUi = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
    @Composable
    fun MainPreview() {
        val trackingStatus = TrackingFailureReason.NONE
        val listState = rememberLazyListState()
        AppTheme {
            ConstraintLayout(Modifier.fillMaxSize().padding(WindowInsets.systemBars.asPaddingValues())) {
                val (left, right, up, down) = createRefs()
                // Model list
                val (list, msg) = createRefs()
                Box(Modifier.fillMaxSize()) {

                }
                Text(
                    modifier = Modifier.wrapContentHeight().fillMaxWidth(0.75f).constrainAs(msg) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(list.top, margin = 50.dp)
                    },
                    textAlign = TextAlign.Center,
                    color = Color.White,
                    softWrap = true,
                    text = when (trackingStatus) {
                        TrackingFailureReason.NONE -> "Tracking"
                        TrackingFailureReason.BAD_STATE -> "ERR: Bad state."
                        TrackingFailureReason.EXCESSIVE_MOTION -> "ERR: Excessive movement."
                        TrackingFailureReason.CAMERA_UNAVAILABLE -> "ERR: Camera unavailable."
                        TrackingFailureReason.INSUFFICIENT_LIGHT -> "ERR: The environment is too dark."
                        TrackingFailureReason.INSUFFICIENT_FEATURES -> "ERR: No identifiable features."
                    }
                )
                Column(
                    modifier = Modifier
                        .constrainAs(list) {
                            start.linkTo(parent.start)
                            end.linkTo(parent.end)
                            bottom.linkTo(parent.bottom, margin = 20.dp)
                        }.fillMaxWidth(0.9f).wrapContentHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                    Box(
                        Modifier.defaultMinSize(20.dp,10.dp).fillMaxWidth(0.25f)
                            .height(10.dp).background(Color.White, shape = RoundedCornerShape(50))
                    )
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth().wrapContentHeight()
                            .defaultMinSize(300.dp, 60.dp).border(color = Color.White, width = 2.dp)
                            .padding(0.dp),
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        items(20, ) {
                            Box(Modifier.size(80.dp, 80.dp).background(Color.White)) {

                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun Main() {
        var trackingStatus by remember { mutableStateOf(TrackingFailureReason.NONE) }
        // Define your composable functions here
        ConstraintLayout(
            Modifier
                .fillMaxSize()
                .padding(WindowInsets.systemBars.asPaddingValues())
        ) {
            // Add your UI elements here
            val (left, right, up, down) = createRefs()
            // Model list
            val (list, model, msg) = createRefs()
            val display = false
            if (display) {
                LazyRow (
                    Modifier.constrainAs(list) {
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom, margin = 20.dp)
                    }.fillMaxSize(0.8f).defaultMinSize(300.dp, 60.dp)
                ) {
                    itemsIndexed(listOf(0,1)) { idx, item ->

                    }
                }
            }

            var gestureListener by remember {
                mutableStateOf<io.github.sceneview.gesture.GestureDetector.OnGestureListener?>(null)
            }

            val anchorNodes = remember {
                mutableStateOf<AnchorNode?>(null)
            }
            val engine = rememberEngine()
            val modelLoader = rememberModelLoader(engine)
            val materialLoader = rememberMaterialLoader(engine)
            val cameraStream = rememberARCameraStream(materialLoader, creator = {
                ARSceneView.createARCameraStream(materialLoader).apply {
                    isDepthOcclusionEnabled = depthModeIsSupported

                }
            })
//            val normalArrowNodes = remember {
//                modelLoader.createInstancedModel(normalArrow, 10).map {
//                        instance -> ModelNode(instance)
//                }.toMutableList()
//            }
            val normalArrowNode = remember {
                ModelNode(modelLoader.createModelInstance(normalArrow)).apply {
                    name = "Normal arrow"
                    isEditable = true
                    Log.d("AnchorNode", "Volume: ${(this.volume())}")

                }
            }
            var arrowsCount by remember { mutableIntStateOf(0) }

            ARScene(
                modifier = Modifier
                    .fillMaxSize(),
                onGestureListener = gestureListener,
                engine = engine,
                modelLoader = modelLoader,
                materialLoader = materialLoader,
                cameraStream = cameraStream,
                sessionConfiguration = { sess, config ->
                    config.depthMode =
                        when (sess.isDepthModeSupported(Config.DepthMode.AUTOMATIC)) {
                            true -> {
                                Log.d("ARScene", "DepthMode = Automatic")
                                depthModeIsSupported = true
                                Config.DepthMode.AUTOMATIC
                            }

                            false -> {
                                Log.d("ARScene", "DepthMode = Disabled")
                                depthModeIsSupported = false
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
                onTrackingFailureChanged = { reason ->
                    trackingStatus = reason ?: TrackingFailureReason.NONE
                },
                onSessionResumed = { session ->
                    val orientation = when (this@MainActivity.resources.configuration.orientation) {
                        Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
                        Configuration.ORIENTATION_PORTRAIT -> "Portrait"
                        Configuration.ORIENTATION_UNDEFINED -> "Undefined"
                        else -> "Unknown"
                    }
                    Log.d("ARScene", "Session resumed, orientation: $orientation")
                },
                onViewCreated = {
                    Log.d("ARScene", "View created ($this)")
                    Log.d("ARScene", "isDepthOcclusionEnabled: ${this.cameraStream?.isDepthOcclusionEnabled ?: false}")
                    this.cameraStream?.isDepthOcclusionEnabled = depthModeIsSupported
                    val handleSingleTapConfirmed = { e: MotionEvent, node: Node? ->
                        val hitList = frame?.hitTest(e)
                        if (hitList != null) {
                            for (t in hitList) {
                                val isPlane = t.trackable is Plane
                                Log.d("ARScene","Trackable distance: ${t.distance} m")
                                Log.d("ARScene", "Trackable type: ${t.trackable.javaClass.name}")
                                if (isPlane) {
                                    val plane = t.trackable as Plane
                                    Log.d("HitTest", "Pose in polygon: ${plane.isPoseInPolygon(t.hitPose)}")
                                    if (plane.isPoseInPolygon(t.hitPose) and (t.distance > 0)) {
                                        Log.d("HitTest", "Hit plane with type ${plane.type}")
//                                        val normal = floatArrayOf(plane.centerPose.position)
                                        val normal = plane.centerPose.yAxis
//                                        plane.centerPose.getTransformedAxis(1, 1f,  normal, 0)
                                        Log.d("HitTest", "plane normal: [${normal.contentToString()}]")
                                        val anchor = t.createAnchor()
                                        if (anchorNodes.value == null) anchorNodes.value = AnchorNode(engine, anchor)
                                        else {
                                            Log.d("AnchorNode", "Detaching anchor ${anchorNodes.value!!.anchor.pose}")
                                            anchorNodes.value!!.detachAnchor()
                                            Log.d("AnchorNode", "Attaching new anchor ${anchor.pose}")
                                            anchorNodes.value!!.anchor = anchor
                                        }
                                        Log.d("HitTest", "Added anchor at ${anchor.pose}, Anchor node: ${anchorNodes.value!!.pose}")
    //                                    arrowsCount++
    //                                    if (arrowsCount == 10) {
    //                                        arrowsCount--
    //                                    }

                                        anchorNodes.value!!.apply {
                                            onUpdated = {
                                                this.childNodes.forEach { child ->
                                                    Log.d("AnchorNode", "child: ${child.name} ")
                                                    child.worldTransform = this.worldTransform
                                                }
                                            }
                                            onTrackingStateChanged = {
                                                Log.d("AnchorNode", "Current state: ${this.trackingState.name}")
                                            }
                                            onAddedToScene = { scene ->
                                                Log.d("AnchorNode", "Added to scene: ${scene.renderableCount}")
                                            }
                                            onRemovedFromScene = { scene ->
                                                Log.d("AnchorNode", "Removed from scene $scene")
                                            }
                                            addChildNode(normalArrowNode)
                                            this@ARScene.addChildNode(this)
                                        }

                                    }
                                }
                                if (t.trackable is Point) {
                                    Log.d("HitTest", "Hit point with orientation ${(t.trackable as Point).orientationMode}")
                                }
                                if (t.trackable is DepthPoint) {
                                    Log.d("HitTest", "Hit depth point at depth ${t.hitPose}")
                                }
                            }
                        }

                    }

                    planeRenderer.planeRendererMode = PlaneRenderer.PlaneRendererMode.RENDER_ALL
                    planeRenderer.maxHitTestPerSecond = 20

                    gestureListener =
                        object : io.github.sceneview.gesture.GestureDetector.OnGestureListener {
                            override fun onContextClick(e: MotionEvent, node: Node?) {
                                Log.d("ARScene", "onContextClick: Not yet implemented")
                            }

                            override fun onDoubleTap(e: MotionEvent, node: Node?) {
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

                            override fun onDoubleTapEvent(e: MotionEvent, node: Node?) {

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
                    onGestureListener = gestureListener

                    Log.d("ARScene", "onGestureListener: ${this.onGestureListener.toString()}")
                    this.onSessionUpdated = { _, _ ->
                        Log.d("ARScene", "Main light color: ${this.lightEstimation?.mainLightColor ?: "none"}")
                        Log.d("ARScene", "Main light direction: ${this.lightEstimation?.mainLightDirection ?: "none"}")
                        Log.d("ARScene", "Main light intensity: ${this.lightEstimation?.mainLightIntensity ?: "none"}")
                        Log.d("ARScene", "Reflections: ${this.lightEstimation?.reflections ?: "none"}")
                        Log.d("ARScene", "Irradiance: ${this.lightEstimation?.irradiance ?: "none"}")
                    }
                }
            )

            Text(
                modifier = Modifier.wrapContentHeight().fillMaxWidth(0.75f).constrainAs(msg) {
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    bottom.linkTo(parent.bottom, margin = 50.dp)
                },
                textAlign = TextAlign.Center,
                color = Color.White,
                softWrap = true,
                text = when (trackingStatus) {
                    TrackingFailureReason.NONE -> "Tracking"
                    TrackingFailureReason.BAD_STATE -> "ERR: Bad state."
                    TrackingFailureReason.EXCESSIVE_MOTION -> "ERR: Excessive movement."
                    TrackingFailureReason.CAMERA_UNAVAILABLE -> "ERR: Camera unavailable."
                    TrackingFailureReason.INSUFFICIENT_LIGHT -> "ERR: The environment is too dark."
                    TrackingFailureReason.INSUFFICIENT_FEATURES -> "ERR: No identifiable features."
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
        if (Firebase.auth.currentUser != null) {
            loginVM.signOut()
        }
    }

    override fun onDestroy() {
        cleanup()
        super.onDestroy()
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

fun ModelNode.volume(): Float {
    return this.extents.toVector3().length()
}