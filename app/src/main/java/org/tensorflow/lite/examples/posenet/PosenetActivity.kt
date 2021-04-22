/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.tensorflow.lite.examples.posenet

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.Rect
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CaptureResult
import android.hardware.camera2.TotalCaptureResult
import android.media.Image
import android.media.ImageReader
import android.media.ImageReader.OnImageAvailableListener
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import android.util.Log
import android.util.Size
import android.util.SparseIntArray
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import java.util.concurrent.Semaphore
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.Person
import org.tensorflow.lite.examples.posenet.lib.Posenet
import java.io.File
import java.io.FileWriter
import java.io.IOException

class PosenetActivity :
  Fragment(),
  ActivityCompat.OnRequestPermissionsResultCallback {
  var simpleVideoView: VideoView? = null

  var mediaControls: MediaController? = null

  private var mediaPlayer: MediaPlayer? = null
  private var mediaPlayer2: MediaPlayer? = null

  /** List of body joints that should be connected.    */
  private val bodyJoints = listOf(
    Pair(BodyPart.LEFT_WRIST, BodyPart.LEFT_ELBOW),
    Pair(BodyPart.LEFT_ELBOW, BodyPart.LEFT_SHOULDER),
    Pair(BodyPart.LEFT_SHOULDER, BodyPart.RIGHT_SHOULDER),
    Pair(BodyPart.RIGHT_SHOULDER, BodyPart.RIGHT_ELBOW),
    Pair(BodyPart.RIGHT_ELBOW, BodyPart.RIGHT_WRIST),
    Pair(BodyPart.LEFT_SHOULDER, BodyPart.LEFT_HIP),
    Pair(BodyPart.LEFT_HIP, BodyPart.RIGHT_HIP),
    Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_SHOULDER),
    Pair(BodyPart.LEFT_HIP, BodyPart.LEFT_KNEE),
    Pair(BodyPart.LEFT_KNEE, BodyPart.LEFT_ANKLE),
    Pair(BodyPart.RIGHT_HIP, BodyPart.RIGHT_KNEE),
    Pair(BodyPart.RIGHT_KNEE, BodyPart.RIGHT_ANKLE)
  )

  /** Threshold for confidence score. */
  private val minConfidence = 0.5

  /** Radius of circle used to draw keypoints.  */
  private val circleRadius = 8.0f

  /** Paint class holds the style and color information to draw geometries, text and bitmaps. */
  private var paint = Paint()

  /** A shape for extracting frame data.   */
  private val PREVIEW_WIDTH = 640

  private val PREVIEW_HEIGHT = 480

  /** An object for the Posenet library.    */
  private lateinit var posenet: Posenet
  private lateinit var posenet2: Posenet

  /** ID of the current [CameraDevice].   */
  private var cameraId: String? = null

  /** A [SurfaceView] for camera preview.   */
  private var surfaceView: SurfaceView? = null
  private var surfaceView2: SurfaceView? = null

  /** A [CameraCaptureSession] for camera preview.   */
  private var captureSession: CameraCaptureSession? = null

  /** A reference to the opened [CameraDevice].    */
  private var cameraDevice: CameraDevice? = null

  /** The [android.util.Size] of camera preview.  */
  private var previewSize: Size? = null

  /** The [android.util.Size.getWidth] of camera preview. */
  private var previewWidth = 0

  /** The [android.util.Size.getHeight] of camera preview.  */
  private var previewHeight = 0

  /** A counter to keep count of total frames.  */
  private var frameCounter = 0

  /** An IntArray to save image data in ARGB8888 format  */
  private lateinit var rgbBytes: IntArray
  private lateinit var rgbBytes2: IntArray

  /** A ByteArray to save image data in YUV format  */
  private var yuvBytes = arrayOfNulls<ByteArray>(3)
  private var yuvBytes2 = arrayOfNulls<ByteArray>(3)

  /** An additional thread for running tasks that shouldn't block the UI.   */
  private var backgroundThread: HandlerThread? = null
  private var backgroundThread2: HandlerThread? = null

  /** A [Handler] for running tasks in the background.    */
  private var backgroundHandler: Handler? = null
  private var backgroundHandler2: Handler? = null

  /** An [ImageReader] that handles preview frame capture.   */
  private var imageReader: ImageReader? = null
  private var imageReader2: ImageReader? = null

  /** [CaptureRequest.Builder] for the camera preview   */
  private var previewRequestBuilder: CaptureRequest.Builder? = null

  /** [CaptureRequest] generated by [.previewRequestBuilder   */
  private var previewRequest: CaptureRequest? = null

  /** A [Semaphore] to prevent the app from exiting before closing the camera.    */
  private val cameraOpenCloseLock = Semaphore(1)

  /** Whether the current camera device supports Flash or not.    */
  private var flashSupported = false

  /** Orientation of the camera sensor.   */
  private var sensorOrientation: Int? = null

  /** Abstract interface to someone holding a display surface.    */
  private var surfaceHolder: SurfaceHolder? = null
  private var surfaceHolder2: SurfaceHolder? = null

  /** [CameraDevice.StateCallback] is called when [CameraDevice] changes its state.   */
  private val stateCallback = object : CameraDevice.StateCallback() {

    override fun onOpened(cameraDevice: CameraDevice) {
      cameraOpenCloseLock.release()
      this@PosenetActivity.cameraDevice = cameraDevice
      //createCameraPreviewSession()
      createCameraPreviewSession2()
    }

    override fun onDisconnected(cameraDevice: CameraDevice) {
      cameraOpenCloseLock.release()
      cameraDevice.close()
      this@PosenetActivity.cameraDevice = null
    }

    override fun onError(cameraDevice: CameraDevice, error: Int) {
      onDisconnected(cameraDevice)
      this@PosenetActivity.activity?.finish()
    }

  }

  /** A [CameraCaptureSession.CaptureCallback] that handles events related to JPEG capture.  */
  private val captureCallback = object : CameraCaptureSession.CaptureCallback() {

    override fun onCaptureProgressed(session: CameraCaptureSession, request: CaptureRequest, partialResult: CaptureResult) {}

    override fun onCaptureCompleted(session: CameraCaptureSession, request: CaptureRequest, result: TotalCaptureResult) {}

  }

  /**
   * Shows a [Toast] on the UI thread.
   *
   * @param text The message to show
   */
  private fun showToast(text: String) {
    val activity = activity
    activity?.runOnUiThread { Toast.makeText(activity, text, Toast.LENGTH_SHORT).show() }
  }

  override fun onCreateView(
          inflater: LayoutInflater,
          container: ViewGroup?,
          savedInstanceState: Bundle?
  ): View? = inflater.inflate(R.layout.tfe_pn_activity_posenet2, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    surfaceView = view.findViewById(R.id.surfaceView)
    surfaceView2 = view.findViewById(R.id.surfaceView2)
    surfaceHolder = surfaceView!!.holder
    surfaceHolder2 = surfaceView2!!.holder

    /** try code*/
    activity!!.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    paint.textSize = 10F
  }

  override fun onStart() {
    super.onStart()
//  openCamera()
//  posenet = Posenet(this.context!!)

    //btnPlayVideoView()
    btnPlayPosenetVideo()
    btnPlayPosenetVideo2()
  }

  override fun onResume() {
    super.onResume()
    startBackgroundThread()
  }

  override fun onPause() {
/*  closeCamera()
    stopBackgroundThread()
    super.onPause()*/

    mediaPlayer?.pause()
    mediaPlayer2?.pause()

    imageReader!!.close()
    imageReader2!!.close()

    imageReader = null
    imageReader2 = null

    stopBackgroundThread()
    super.onPause()
  }

  override fun onDestroy() {
/*  super.onDestroy()
    posenet.close()*/

    mediaPlayer?.release()
    mediaPlayer2?.release()

    mediaPlayer = null
    mediaPlayer2 = null

    super.onDestroy()

    posenet.close()
    posenet2.close()
  }

  private fun btnPlayPosenetVideo(){
    val txt1: TextView? = view?.findViewById(R.id.txtView)
    val btn: Button? = view?.findViewById(R.id.btn2)

    btn?.setOnClickListener {
      // 1. load video
      previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
      previewHeight = previewSize!!.height
      previewWidth = previewSize!!.width
      mediaPlayer = MediaPlayer.create(context, R.raw.jump2)

      val layoutParams = surfaceView!!.layoutParams
      val fragmentWidth = surfaceView!!.width

      // 2. Resize video to PoseNet input model size
      layoutParams.height = ((mediaPlayer!!.videoHeight.toFloat() / mediaPlayer!!.videoWidth.toFloat()) * fragmentWidth.toFloat()).toInt()

      // 3. Call posenet library and define input array
      posenet = Posenet(this.context!!)
      rgbBytes = IntArray(mediaPlayer!!.videoHeight * mediaPlayer!!.videoWidth)

      // 4. Capture images from preview in YUV format.
      imageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 2)
      imageReader!!.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

      // 5. This is the surface we need to record images for processing.
      val recordingSurface = imageReader!!.surface
      mediaPlayer?.setSurface(recordingSurface)
      mediaPlayer?.start()

      txt1?.text = "Posenet video on surface."
    }

  }

  private fun btnPlayPosenetVideo2(){
    val txt1: TextView? = view?.findViewById(R.id.txtView)
    val btn: Button? = view?.findViewById(R.id.btn)

    btn?.setOnClickListener {
      // 1. load video
      previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)
      previewHeight = previewSize!!.height
      previewWidth = previewSize!!.width
      mediaPlayer2 = MediaPlayer.create(context, R.raw.jump)

      val layoutParams = surfaceView2!!.layoutParams
      val fragmentWidth = surfaceView2!!.width

      // 2. Resize video to PoseNet input model size
      layoutParams.height = ((mediaPlayer2!!.videoHeight.toFloat() / mediaPlayer2!!.videoWidth.toFloat()) * fragmentWidth.toFloat()).toInt()

      // 3. Call posenet library and define input array
      posenet2= Posenet(this.context!!)
      rgbBytes2 = IntArray(mediaPlayer2!!.videoHeight * mediaPlayer2!!.videoWidth)

      // 4. Capture images from preview in YUV format.
      imageReader2 = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 2)
      imageReader2!!.setOnImageAvailableListener(imageAvailableListener2, backgroundHandler2)

      // 5. This is the surface we need to record images for processing.
      val recordingSurface = imageReader2!!.surface
      mediaPlayer2?.setSurface(recordingSurface)
      mediaPlayer2?.start()

      txt1?.text = "Posenet video on surface."
    }

  }

  private fun btnPlayVideoView(){
    /** To load a video and set a button which can control the video to play.*/
    val txt1: TextView? = view?.findViewById(R.id.txtView)
    val btn: Button? = view?.findViewById(R.id.btn)
    simpleVideoView = view?.findViewById(R.id.videoView) as VideoView

    if (mediaControls == null) {
      mediaControls = MediaController(this.context)
      mediaControls!!.setAnchorView(this.simpleVideoView)
    }

    btn?.setOnClickListener {
      simpleVideoView!!.setMediaController(mediaControls)
      simpleVideoView!!.setVideoURI(Uri.parse("android.resource://"+ (activity?.packageName) + "/" + R.raw.jump))
      simpleVideoView!!.requestFocus()
      simpleVideoView!!.start()
      //showToast("Start to play video!").also { Toast.LENGTH_LONG }
      txt1?.text = "Src video on video view."

      simpleVideoView!!.setOnCompletionListener {
        //showToast("Video completed")
        Toast.LENGTH_LONG
        txt1?.text = "Click Play to restart."
      }
      simpleVideoView!!.setOnErrorListener {
        mp, what, extra -> showToast( "An Error Occured " + "While Playing Video !!!").also { Toast.LENGTH_LONG }
        false
      }
    }

  }

  private fun requestCameraPermission() {
    if (shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
      ConfirmationDialog().show(childFragmentManager, FRAGMENT_DIALOG)
    } else {
      requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }
  }

  override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
    if (requestCode == REQUEST_CAMERA_PERMISSION) {
      if (allPermissionsGranted(grantResults)) {
        ErrorDialog.newInstance(getString(R.string.tfe_pn_request_permission)).show(childFragmentManager, FRAGMENT_DIALOG)
      }
    } else {
      super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
  }

  private fun allPermissionsGranted(grantResults: IntArray) = grantResults.all {
    it == PackageManager.PERMISSION_GRANTED
  }

  /** Sets up member variables related to camera.   */
  private fun setUpCameraOutputs() {
    val activity = activity
    val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
      for (cameraId in manager.cameraIdList) {
        val characteristics = manager.getCameraCharacteristics(cameraId)

        // We don't use a front facing camera in this sample.
        val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
        if (cameraDirection != null && cameraDirection == CameraCharacteristics.LENS_FACING_FRONT){
          continue
        }

        previewSize = Size(PREVIEW_WIDTH, PREVIEW_HEIGHT)

        imageReader = ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, /*maxImages*/ 2)

        sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!

        previewHeight = previewSize!!.height
        previewWidth = previewSize!!.width

        // Initialize the storage bitmaps once when the resolution is known.
        rgbBytes = IntArray(previewWidth * previewHeight)

        // Check if the flash is supported.
        flashSupported = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true

        this.cameraId = cameraId

        // We've found a viable camera and finished setting up member variables,
        // so we don't need to iterate through other available cameras.
        return
      }
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())
    } catch (e: NullPointerException) {
      // Currently an NPE is thrown when the Camera2API is used but not supported on the device this code runs.
      ErrorDialog.newInstance(getString(R.string.tfe_pn_camera_error)).show(childFragmentManager, FRAGMENT_DIALOG)
    }
  }

  /** Opens the camera specified by [PosenetActivity.cameraId].   */
  private fun openCamera() {
    val permissionCamera = context!!.checkPermission(Manifest.permission.CAMERA, Process.myPid(), Process.myUid())
    if (permissionCamera != PackageManager.PERMISSION_GRANTED) {
      requestCameraPermission()
    }
    setUpCameraOutputs()
    val manager = activity!!.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    try {
      // Wait for camera to open - 2.5 seconds is sufficient
      if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
        throw RuntimeException("Time out waiting to lock camera opening.")
      }
      manager.openCamera(cameraId!!, stateCallback, backgroundHandler)
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())
    } catch (e: InterruptedException) {
      throw RuntimeException("Interrupted while trying to lock camera opening.", e)
    }
  }

  /** Closes the current [CameraDevice].   */
  private fun closeCamera() {
    if (captureSession == null) {
      return
    }

    try {
      cameraOpenCloseLock.acquire()
      captureSession!!.close()
      captureSession = null

      cameraDevice!!.close()
      cameraDevice = null

      imageReader!!.close()
      imageReader = null

      imageReader2!!.close()
      imageReader2 = null

    } catch (e: InterruptedException) {
      throw RuntimeException("Interrupted while trying to lock camera closing.", e)
    } finally {
      cameraOpenCloseLock.release()
    }
  }

  /** Starts a background thread and its [Handler].   */
  private fun startBackgroundThread() {
    backgroundThread = HandlerThread("imageAvailableListener").also { it.start() }
    backgroundHandler = Handler(backgroundThread!!.looper)

    backgroundThread2= HandlerThread("imageAvailableListener2").also { it.start() }
    backgroundHandler2 = Handler(backgroundThread2!!.looper)
  }

  /** Stops the background thread and its [Handler].   */
  private fun stopBackgroundThread() {
    backgroundThread?.quitSafely()
    backgroundThread2?.quitSafely()

    try {
      backgroundThread?.join()
      backgroundThread = null
      backgroundHandler = null

      backgroundThread2?.join()
      backgroundThread2 = null
      backgroundHandler2 = null
    } catch (e: InterruptedException) {
      Log.e(TAG, e.toString())
    }
  }

  /** Fill the yuvBytes with data from image planes.   */
  private fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {
    //stride指在內存中每行引發所佔的空間，為了實現內存分配（或者其他的什麼原因），每行偏移在內存中所佔的空間並不是圖像的寬度。
    // Row stride is the total number of bytes occupied in memory by a row of an image.
    // Because of the variable row stride it's not possible to know in
    // advance the actual necessary dimensions of the yuv planes.
    for (i in planes.indices) {
      val buffer = planes[i].buffer
      if (yuvBytes[i] == null) {
        yuvBytes[i] = ByteArray(buffer.capacity())
      }
      buffer.get(yuvBytes[i]!!)
    }
  }

  /** A [OnImageAvailableListener] to receive frames as they are available.  */
  private var imageAvailableListener = object : OnImageAvailableListener {
    override fun onImageAvailable(imageReader: ImageReader) {
      // We need wait until we have some size from onPreviewSizeChosen
      if (previewWidth == 0 || previewHeight == 0) {
        return
      }

      val image = imageReader.acquireLatestImage() ?: return
      fillBytes(image.planes, yuvBytes)

      ImageUtils.convertYUV420ToARGB8888(
              yuvBytes[0]!!,
              yuvBytes[1]!!,
              yuvBytes[2]!!,
//        previewWidth,
//        previewHeight,
              image.width,
              image.height,
              /*yRowStride=*/ image.planes[0].rowStride,
              /*uvRowStride=*/ image.planes[1].rowStride,
              /*uvPixelStride=*/ image.planes[1].pixelStride,
              rgbBytes
      )

      // Create bitmap from int array
//    val imageBitmap = Bitmap.createBitmap(rgbBytes, previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
      val imageBitmap = Bitmap.createBitmap(rgbBytes, image.width, image.height, Bitmap.Config.ARGB_8888)

      /*// Create rotated version for portrait display (source)
        val rotateMatrix = Matrix()
        rotateMatrix.postRotate(90.0f)
        val rotatedBitmap = Bitmap.createBitmap(
          imageBitmap, 0, 0, previewWidth, previewHeight,
          rotateMatrix, true
        )
        image.close()
        processImage(rotatedBitmap)*/

      /** try code */
      image.close()
      processImage(imageBitmap)
    }
  }

  private var imageAvailableListener2 = object : OnImageAvailableListener {
     override fun onImageAvailable(imageReader: ImageReader) {
      if (previewWidth == 0 || previewHeight == 0) {
        return
      }
      val image = imageReader?.acquireLatestImage() ?: return

      fillBytes(image.planes, yuvBytes2)

      ImageUtils.convertYUV420ToARGB8888(
        yuvBytes2[0]!!, yuvBytes2[1]!!, yuvBytes2[2]!!,
        image.width, image.height,
        image.planes[0].rowStride, image.planes[1].rowStride, image.planes[1].pixelStride,
        rgbBytes2
      )

      val imageBitmap = Bitmap.createBitmap(rgbBytes2, image.width, image.height, Bitmap.Config.ARGB_8888)

      image.close()

      // Crop bitmap.
      val croppedBitmap = cropBitmap(imageBitmap)

      // Created scaled version of bitmap for model input.
      val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, MODEL_WIDTH, MODEL_HEIGHT, true)

      // Perform inference.
      val person = posenet2.estimateSinglePose(scaledBitmap)
      val canvas: Canvas = surfaceHolder2!!.lockCanvas()
      draw2(canvas, person, scaledBitmap)
    }
  }

  /** Crop Bitmap to maintain aspect ratio of model input.   */
  private fun cropBitmap(bitmap: Bitmap): Bitmap {
    val bitmapRatio = bitmap.height.toFloat() / bitmap.width
    val modelInputRatio = MODEL_HEIGHT.toFloat() / MODEL_WIDTH
    var croppedBitmap = bitmap

    // Acceptable difference between the modelInputRatio and bitmapRatio to skip cropping.
    val maxDifference = 1e-5  //0.00001

    // Checks if the bitmap has similar aspect ratio as the required model input.
    croppedBitmap = when {
      abs(modelInputRatio - bitmapRatio) < maxDifference -> return croppedBitmap
      modelInputRatio < bitmapRatio -> {
        // New image is taller so we are height constrained.
        val cropHeight = bitmap.height - (bitmap.width.toFloat() / modelInputRatio)
        Bitmap.createBitmap(bitmap, 0, (cropHeight / 2).toInt(), bitmap.width, (bitmap.height - cropHeight).toInt())
      }
      else -> {
        val cropWidth = bitmap.width - (bitmap.height.toFloat() * modelInputRatio)
        Bitmap.createBitmap(bitmap, (cropWidth / 2).toInt(), 0, (bitmap.width - cropWidth).toInt(), bitmap.height)
      }
    }
    return croppedBitmap
  }

  /** Set the paint color and size.    */
  private fun setPaint() {
    paint.color = Color.YELLOW
    paint.textSize = 60.0f
    paint.strokeWidth = 8.0f
  }

  /** Draw bitmap on Canvas.   */
  private fun draw(canvas: Canvas, person: Person, bitmap: Bitmap) {
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    // Draw `bitmap` and `person` in square canvas.
    val screenWidth: Int
    val screenHeight: Int
    val left: Int
    val right: Int
    val top: Int
    val bottom: Int

    if (canvas.height > canvas.width) {
      screenWidth = canvas.width
      screenHeight = canvas.width
      left = 0
      top = (canvas.height - canvas.width) / 2
    } else {
      screenWidth = canvas.height
      screenHeight = canvas.height
      left = (canvas.width - canvas.height) / 2
      top = 0
    }

    right = left + screenWidth
    bottom = top + screenHeight

    setPaint()
    canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), Rect(left, top, right, bottom), paint)

    val widthRatio = screenWidth.toFloat() / MODEL_WIDTH
    val heightRatio = screenHeight.toFloat() / MODEL_HEIGHT

    /*// Draw all key points over the image.
    for (keyPoint in person.keyPoints) {    //Use the position of key points obtained from the Person object to draw a skeleton on the canvas.
      if (keyPoint.score > minConfidence) { //Display the key points with a confidence score above a certain threshold, which by default is 0.5.
        val position = keyPoint.position
        val adjustedX: Float = position.x.toFloat() * widthRatio + left
        val adjustedY: Float = position.y.toFloat() * heightRatio + top
        canvas.drawCircle(adjustedX, adjustedY, circleRadius, paint)
        //show key points (x, y) position
        //canvas.drawText("${adjustedX}, ${adjustedY} ", adjustedX, adjustedY,  paint)
      }
    }*/

    // Draw key points: 6-17 over the image.
    for(i in 5..16){
      if (person.keyPoints[i].score > minConfidence) {
        val adjustedX: Float = person.keyPoints[i].position.x.toFloat() * widthRatio + left
        val adjustedY: Float = person.keyPoints[i].position.y.toFloat() * heightRatio + top
        canvas.drawCircle(adjustedX, adjustedY, circleRadius, paint)
        println("(x,y): ${adjustedX}, ${adjustedY}")
        //canvas.drawText("${adjustedX}, ${adjustedY} ",adjustedX,adjustedY,  paint)  //秀出關節點座標
      }
    }

    // Draw the line to connect the body Joints points
    for (line in bodyJoints) {
      if (
              (person.keyPoints[line.first.ordinal].score > minConfidence) and
              (person.keyPoints[line.second.ordinal].score > minConfidence)
      ) {
        canvas.drawLine(
                person.keyPoints[line.first.ordinal].position.x.toFloat() * widthRatio + left,
                person.keyPoints[line.first.ordinal].position.y.toFloat() * heightRatio + top,
                person.keyPoints[line.second.ordinal].position.x.toFloat() * widthRatio + left,
                person.keyPoints[line.second.ordinal].position.y.toFloat() * heightRatio + top,
                paint
        )
      }
    }

     //draw txt device info details
    canvas.drawText(
      "Score: %.2f".format(person.score),
      //(3.0f * widthRatio),                 //i500
      //(1.0f * heightRatio + bottom),       //i500
      (5.0f * widthRatio),
      (190.0f * heightRatio + top),
      paint
    )
    canvas.drawText(
      "Device: %s".format(posenet.device),
      //(3.0f * widthRatio),                  //i500
      //(21.0f * heightRatio + bottom),       //i500
      (5.0f * widthRatio),
      (220.0f * heightRatio + top),
      paint
    )
    canvas.drawText(
      "Time: %.2f ms".format(posenet.lastInferenceTimeNanos * 1.0f / 1_000_000),
      //(3.0f * widthRatio),                 //i500
      //(41.0f * heightRatio + bottom),      //i500
      (5.0f * widthRatio),
      (250.0f * heightRatio + top),
      paint
    )

    // Draw!
    surfaceHolder!!.unlockCanvasAndPost(canvas)
  }

  private fun draw2(canvas: Canvas, person: Person, bitmap: Bitmap) {
    canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
    // Draw `bitmap` and `person` in square canvas.
    val screenWidth: Int
    val screenHeight: Int
    val left: Int
    val right: Int
    val top: Int
    val bottom: Int

    if (canvas.height > canvas.width) {
      screenWidth = canvas.width
      screenHeight = canvas.width
      left = 0
      top = (canvas.height - canvas.width) / 2
    } else {
      screenWidth = canvas.height
      screenHeight = canvas.height
      left = (canvas.width - canvas.height) / 2
      top = 0
    }

    right = left + screenWidth
    bottom = top + screenHeight

    setPaint()
    canvas.drawBitmap(bitmap, Rect(0, 0, bitmap.width, bitmap.height), Rect(left, top, right, bottom), paint)

    val widthRatio = screenWidth.toFloat() / MODEL_WIDTH
    val heightRatio = screenHeight.toFloat() / MODEL_HEIGHT

    /*// Draw all key points over the image.
    for (keyPoint in person.keyPoints) {    //Use the position of key points obtained from the Person object to draw a skeleton on the canvas.
      if (keyPoint.score > minConfidence) { //Display the key points with a confidence score above a certain threshold, which by default is 0.5.
        val position = keyPoint.position
        val adjustedX: Float = position.x.toFloat() * widthRatio + left
        val adjustedY: Float = position.y.toFloat() * heightRatio + top
        canvas.drawCircle(adjustedX, adjustedY, circleRadius, paint)
        //show key points (x, y) position
        //canvas.drawText("${adjustedX}, ${adjustedY} ", adjustedX, adjustedY,  paint)
      }
    }*/

    // Draw key points: 6-17 over the image.
    for(i in 5..16){
      if (person.keyPoints[i].score > minConfidence) {
        val adjustedX: Float = person.keyPoints[i].position.x.toFloat() * widthRatio + left
        val adjustedY: Float = person.keyPoints[i].position.y.toFloat() * heightRatio + top
        canvas.drawCircle(adjustedX, adjustedY, circleRadius, paint)
        println("(x,y): ${adjustedX}, ${adjustedY}")
        //canvas.drawText("${adjustedX}, ${adjustedY} ",adjustedX,adjustedY,  paint)  //秀出關節點座標
      }
    }

    // Draw the line to connect the body Joints points
    for (line in bodyJoints) {
      if (
              (person.keyPoints[line.first.ordinal].score > minConfidence) and
              (person.keyPoints[line.second.ordinal].score > minConfidence)
      ) {
        canvas.drawLine(
                person.keyPoints[line.first.ordinal].position.x.toFloat() * widthRatio + left,
                person.keyPoints[line.first.ordinal].position.y.toFloat() * heightRatio + top,
                person.keyPoints[line.second.ordinal].position.x.toFloat() * widthRatio + left,
                person.keyPoints[line.second.ordinal].position.y.toFloat() * heightRatio + top,
                paint
        )
      }
    }

    //draw txt device info details
    canvas.drawText(
            "Score: %.2f".format(person.score),
            //(3.0f * widthRatio),                 //i500
            //(1.0f * heightRatio + bottom),       //i500
            (5.0f * widthRatio),
            (190.0f * heightRatio + top),
            paint
    )
    canvas.drawText(
            "Device: %s".format(posenet.device),
            //(3.0f * widthRatio),                  //i500
            //(21.0f * heightRatio + bottom),       //i500
            (5.0f * widthRatio),
            (220.0f * heightRatio + top),
            paint
    )
    canvas.drawText(
            "Time: %.2f ms".format(posenet.lastInferenceTimeNanos * 1.0f / 1_000_000),
            //(3.0f * widthRatio),                 //i500
            //(41.0f * heightRatio + bottom),      //i500
            (5.0f * widthRatio),
            (250.0f * heightRatio + top),
            paint
    )

    // Draw!
    surfaceHolder2!!.unlockCanvasAndPost(canvas)
  }

  /** Process image using Posenet library.   */
  private fun processImage(bitmap: Bitmap) {
    // Crop bitmap.
    val croppedBitmap = cropBitmap(bitmap)

    // Created scaled version of bitmap for model input.
    val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, MODEL_WIDTH, MODEL_HEIGHT, true)

    // Perform inference.
    val person = posenet.estimateSinglePose(scaledBitmap)
    val canvas: Canvas = surfaceHolder!!.lockCanvas()
    draw(canvas, person, scaledBitmap)
  }

  /** Creates a new [CameraCaptureSession] for camera preview.   */
  private fun createCameraPreviewSession() {
    try {
      // We capture images from preview in YUV format.
      imageReader = ImageReader.newInstance(previewSize!!.width, previewSize!!.height, ImageFormat.YUV_420_888, 2)
      imageReader!!.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

      // This is the surface we need to record images for processing.
      val recordingSurface = imageReader!!.surface

      // We set up a CaptureRequest.Builder with the output Surface.
      previewRequestBuilder = cameraDevice!!.createCaptureRequest( CameraDevice.TEMPLATE_PREVIEW)
      previewRequestBuilder!!.addTarget(recordingSurface)

      // Here, we create a CameraCaptureSession for camera preview.
      cameraDevice!!.createCaptureSession(listOf(recordingSurface), object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
          // The camera is already closed
          if (cameraDevice == null) return

          // When the session is ready, we start displaying the preview.
          captureSession = cameraCaptureSession
          try {
            // Auto focus should be continuous for camera preview.
            previewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

            // Flash is automatically enabled when necessary.
            setAutoFlash(previewRequestBuilder!!)

            // Finally, we start displaying the camera preview.
            previewRequest = previewRequestBuilder!!.build()

            captureSession!!.setRepeatingRequest(previewRequest!!, captureCallback, backgroundHandler)

          } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
          }
        }
        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {showToast("Failed")}
        }, null
      )
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())
    }
  }

  private fun createCameraPreviewSession2() {
    // We capture images from preview in YUV format.
    imageReader = ImageReader.newInstance(previewSize!!.width, previewSize!!.height, ImageFormat.YUV_420_888, 2)
    imageReader!!.setOnImageAvailableListener(imageAvailableListener, backgroundHandler)

    // This is the surface we need to record images for processing.
    val recordingSurface = imageReader!!.surface

    try {
      // We set up a CaptureRequest.Builder with the output Surface.
      previewRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
      previewRequestBuilder!!.addTarget(recordingSurface)

      // Here, we create a CameraCaptureSession for camera preview.
      cameraDevice!!.createCaptureSession(listOf(recordingSurface), object : CameraCaptureSession.StateCallback() {
        override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {

          // The camera is already closed
          if (cameraDevice == null) return

          // When the session is ready, we start displaying the preview.
          captureSession = cameraCaptureSession

          try {
            // Auto focus should be continuous for camera preview.
            previewRequestBuilder!!.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE)

            // Flash is automatically enabled when necessary.
            setAutoFlash(previewRequestBuilder!!)

            // Finally, we start displaying the camera preview.
            previewRequest = previewRequestBuilder!!.build()
            captureSession!!.setRepeatingRequest(previewRequest!!, captureCallback, backgroundHandler)
          } catch (e: CameraAccessException) {
            Log.e(TAG, e.toString())
          }
        }

        override fun onConfigureFailed(cameraCaptureSession: CameraCaptureSession) {showToast("Failed")}
      },null
      )
    } catch (e: CameraAccessException) {
      Log.e(TAG, e.toString())
    }
  }

  private fun setAutoFlash(requestBuilder: CaptureRequest.Builder) {
    if (flashSupported) {
      requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH)
    }
  }

  /**  Shows an error message dialog.   */
  class ErrorDialog : DialogFragment() {
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
      AlertDialog.Builder(activity)
      .setMessage(arguments!!.getString(ARG_MESSAGE))
      .setPositiveButton(android.R.string.ok) { _, _ -> activity!!.finish() }
      .create()

    companion object {

      @JvmStatic
      private val ARG_MESSAGE = "message"

      @JvmStatic
      fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
        arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
      }
    }
  }

  companion object {
    /** Conversion from screen rotation to JPEG orientation. */
    private val ORIENTATIONS = SparseIntArray()
    private val FRAGMENT_DIALOG = "dialog"

    init {
      ORIENTATIONS.append(Surface.ROTATION_0, 90)
      ORIENTATIONS.append(Surface.ROTATION_90, 0)
      ORIENTATIONS.append(Surface.ROTATION_180, 270)
      ORIENTATIONS.append(Surface.ROTATION_270, 180)
    }

    /**  Tag for the [Log]. */
    private const val TAG = "PosenetActivity"
  }

}
