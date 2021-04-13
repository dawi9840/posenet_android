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

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.media.Image
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.Posenet
import kotlin.math.abs

class CameraActivity : AppCompatActivity() {
  private var simpleVideoView: VideoView? = null
  private var simpleVideoView2: VideoView? = null
  private var mediaControls: MediaController? = null /** load Video for VideoView. */
  private val minConfidence = 0.5                    /** Threshold for confidence score. */
  private val circleRadius = 8.0f                    /** Radius of circle used to draw keypoints.  */
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
  )               /** List of body joints that should be connected.    */
  private fun drawableToBitmap(drawable: Drawable): Bitmap {
    val bitmap = Bitmap.createBitmap(257, 257, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }  /** Returns a resized bitmap of the drawable image.  */

  /** Crop Bitmap to maintain aspect ratio of model input.   */
  private fun cropBitmap(bitmap: Bitmap): Bitmap {
    val bitmapRatio = bitmap.height.toFloat() / bitmap.width
    val modelInputRatio = MODEL_HEIGHT.toFloat() / MODEL_WIDTH
    var croppedBitmap = bitmap

    // Acceptable difference between the modelInputRatio and bitmapRatio to skip cropping.
    val maxDifference = 1e-5  //0.00001

    // Checks if the bitmap has similar aspect ratio as the required model input.
    when {
      abs(modelInputRatio - bitmapRatio) < maxDifference -> return croppedBitmap
      modelInputRatio < bitmapRatio -> {
        // New image is taller so we are height constrained.
        val cropHeight = bitmap.height - (bitmap.width.toFloat() / modelInputRatio)
        croppedBitmap = Bitmap.createBitmap(
                bitmap,
                0,
                (cropHeight / 2).toInt(),
                bitmap.width,
                (bitmap.height - cropHeight).toInt()
        )
      }
      else -> {
        val cropWidth = bitmap.width - (bitmap.height.toFloat() * modelInputRatio)
        croppedBitmap = Bitmap.createBitmap(
                bitmap,
                (cropWidth / 2).toInt(),
                0,
                (bitmap.width - cropWidth).toInt(),
                bitmap.height
        )
      }
    }
    return croppedBitmap
  }
  /** Process image using Posenet library.   */
  private fun processImage(bitmap: Bitmap) {
    // Crop bitmap.
    val croppedBitmap = cropBitmap(bitmap)

    // Created scaled version of bitmap for model input.
    val scaledBitmap = Bitmap.createScaledBitmap(croppedBitmap, MODEL_WIDTH, MODEL_HEIGHT, true)

    // Perform inference.
    //val person2 = posenet.estimateSinglePose(scaledBitmap)
    //val canvas: Canvas = surfaceHolder!!.lockCanvas()
    //draw(canvas, person2, scaledBitmap)
  }
  /** Fill the yuvBytes with data from image planes.   */
  private fun fillBytes(planes: Array<Image.Plane>, yuvBytes: Array<ByteArray?>) {
    for (i in planes.indices) {
      val buffer = planes[i].buffer
      if (yuvBytes[i] == null) {
        yuvBytes[i] = ByteArray(buffer.capacity())
      }
      buffer.get(yuvBytes[i]!!)
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    /*setContentView(R.layout.tfe_pn_activity_camera) //呼叫PosenetAxtivity.kt
    savedInstanceState ?: supportFragmentManager.beginTransaction()
      .replace(R.id.container, PosenetActivity()).commit()*/

    setContentView(R.layout.tfe_pn_activity_test)
    val sampleImageView = findViewById<ImageView>(R.id.image)
    val drawedImage = ResourcesCompat.getDrawable(resources, R.drawable.image, null)
    val imageBitmap = drawableToBitmap(drawedImage!!)
    val posenet = Posenet(this.applicationContext)
    /** Calls the Posenet library functions. */
    val person = posenet.estimateSinglePose(imageBitmap)

    // Draw the keypoints over the image.
    val paint = Paint()
    paint.color = Color.RED
    val size = 2.0f

    val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    //my code, to show point6 ~ point17 keypoint
    for(i in 5..16){
      canvas.drawCircle(
              person.keyPoints[i].position.x.toFloat(),
              person.keyPoints[i].position.y.toFloat(), size, paint)
      /* //show text keypoint posiyion(x, y)
      canvas.drawText(  "${person.keyPoints[i].position.x},${person.keyPoints[i].position.y}",
      person.keyPoints[i].position.x.toFloat(), person.keyPoints[i].position.y.toFloat(), paint)*/
      println("point${i}(x, y): ${person.keyPoints[i].position.x}, ${person.keyPoints[i].position.y}")
    }               //畫點關節6~17點
    for (line in bodyJoints) {
      canvas.drawLine(
              person.keyPoints[line.first.ordinal].position.x.toFloat(),
              person.keyPoints[line.first.ordinal].position.y.toFloat(),
              person.keyPoints[line.second.ordinal].position.x.toFloat(),
              person.keyPoints[line.second.ordinal].position.y.toFloat(),
              paint
      )}     //畫線把關節點連起來

    /*for (keypoint in person.keyPoints) {  /** (source code) show all 17 keypoints  */
      canvas.drawCircle(
              keypoint.position.x.toFloat(),
              keypoint.position.y.toFloat(), size, paint
      )
      canvas.drawText(    //show keypoint posiyion(x, y)
         "${keypoint.position.x}, ${keypoint.position.y} ",
              keypoint.position.x.toFloat(),
              keypoint.position.y.toFloat(),  paint
      )
      print("point:${person.keyPoints.indexOf(keypoint)}, ")
      println("(x, y): ${keypoint.position.x}, ${keypoint.position.y}")
    }
    for (line in bodyJoints) {   //畫線把關節點連起來
      canvas.drawLine(
              person.keyPoints[line.first.ordinal].position.x.toFloat(),
              person.keyPoints[line.first.ordinal].position.y.toFloat(),
              person.keyPoints[line.second.ordinal].position.x.toFloat(),
              person.keyPoints[line.second.ordinal].position.y.toFloat(),
              paint
      )
    }*/

    // Set a button control a text showing the image result.
    val btnImage: Button? = findViewById(R.id.btn)
    val txt: TextView? = findViewById(R.id.testView)
    btnImage?.setOnClickListener {
      sampleImageView.setImageBitmap(imageBitmap)   //load a source size image
      sampleImageView.adjustViewBounds = true       //resize a image
      sampleImageView.setImageBitmap(mutableBitmap)
      txt?.text = "Your view OuO"
    }

    /** load a video to videoView */
    val txt1: TextView? = findViewById(R.id.txtView)
    val btnVideo: Button? = findViewById(R.id.btn2)
    simpleVideoView = findViewById<View>(R.id.videoView) as VideoView
    simpleVideoView2 = simpleVideoView
    if (mediaControls == null) {
      mediaControls = MediaController(this)
      mediaControls!!.setAnchorView(this.simpleVideoView)
    }
    simpleVideoView2!!.setMediaController(mediaControls)
    simpleVideoView!!.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.jump))

    simpleVideoView2!!.requestFocus()
    //use button to play video
    btnVideo?.setOnClickListener(View.OnClickListener { view ->
      simpleVideoView2!!.start()
      Toast.makeText(applicationContext,"Start to play video!", Toast.LENGTH_LONG).show()
      txt1?.text = "Well done!"
    })
    simpleVideoView2!!.setOnCompletionListener {
      Toast.makeText(applicationContext,"Video completed", Toast.LENGTH_LONG).show()
      txt1?.text = "\uD83D\uDE02 Click Play to restart. \uD83D\uDE02"
    }
    simpleVideoView2!!.setOnErrorListener {
      mp, what, extra -> Toast.makeText(applicationContext, "An Error Occured " + "While Playing Video !!!", Toast.LENGTH_LONG).show()
      false
    }

  }
}
