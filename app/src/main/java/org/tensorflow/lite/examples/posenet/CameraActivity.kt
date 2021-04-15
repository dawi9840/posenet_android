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

class CameraActivity : AppCompatActivity(), View.OnClickListener {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    /*setContentView(R.layout.tfe_pn_activity_camera)
    savedInstanceState ?: supportFragmentManager.beginTransaction()
      .replace(R.id.container, PosenetActivity()).commit()*/

    setContentView(R.layout.tfe_pn_activity_test)
    findViewById()
    btnPlayVideoView()
    //btnShowPoseNetImg()

  }
  override fun onClick(v: View?) {
  }
  private var simpleVideoView: VideoView? = null
  private var sampleImageView: ImageView?= null
  private var mediaControls: MediaController? = null
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
  private var btnVideo: Button?= null
  private var txtVideo: TextView?= null
  private var btnImg: Button?= null
  private var txtImg: TextView?= null

  private fun findViewById(){
    simpleVideoView = findViewById<View>(R.id.videoView) as VideoView
    sampleImageView = findViewById<ImageView>(R.id.image)
    btnVideo = findViewById<Button>(R.id.btn2)
    txtVideo = findViewById<TextView>(R.id.txtView)
    btnImg = findViewById<Button>(R.id.btn)
    txtImg = findViewById(R.id.testView)
  }

  private fun btnShowPoseNetImg(){
    val drawedImage = ResourcesCompat.getDrawable(resources, R.drawable.image, null)
    val imageBitmap = drawableToBitmap(drawedImage!!)

    /** Calls the Posenet library functions. */
    val posenet = Posenet(this.applicationContext)
    val person = posenet.estimateSinglePose(imageBitmap)

    // Define parameters to draw the keypoints over the image
    val paint = Paint()
    val size = 2.0f
    val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)
    paint.color = Color.RED

    sampleImageView?.setImageBitmap(imageBitmap)  //load and show a source size image

    /** Show all 17 keypoints  */
    /*for (keypoint in person.keyPoints) {
      canvas.drawCircle(keypoint.position.x.toFloat(), keypoint.position.y.toFloat(), size, paint)
      println("P${person.keyPoints.indexOf(keypoint)}(x, y): ${keypoint.position.x}, ${keypoint.position.y} ")
      //Draw text (x, y) position on image.
      canvas.drawText("${keypoint.position.x}, ${keypoint.position.y} ", keypoint.position.x.toFloat(), keypoint.position.y.toFloat(), paint)
    }*/

    /** Show point6 ~ point17 keypoints  */
    for(i in 5..16){
      // Draw text (x, y) position on image.
      //canvas.drawText("P${i+1}", person.keyPoints[i].position.x.toFloat(), person.keyPoints[i].position.y.toFloat(), paint)
      canvas.drawCircle(
        person.keyPoints[i].position.x.toFloat(),
        person.keyPoints[i].position.y.toFloat(), size, paint)
      println("P${i+1}(x, y): ${person.keyPoints[i].position.x}, ${person.keyPoints[i].position.y}")
    }

    /** Draw lines to connect the bodyJoints points */
    for (line in bodyJoints) {
      canvas.drawLine(
        person.keyPoints[line.first.ordinal].position.x.toFloat(),
        person.keyPoints[line.first.ordinal].position.y.toFloat(),
        person.keyPoints[line.second.ordinal].position.x.toFloat(),
        person.keyPoints[line.second.ordinal].position.y.toFloat(),
        paint
      )}

    // Set a button control a text showing the image result.
    btnImg?.setOnClickListener {
      sampleImageView?.adjustViewBounds = true       //resize (257 x 257) image
      sampleImageView?.setImageBitmap(mutableBitmap)
      txtImg?.text = "Your view OuO"
    }

  }
  private fun btnPlayVideoView(){
    if (mediaControls == null) {
      mediaControls = MediaController(this)
      mediaControls!!.setAnchorView(this.simpleVideoView)
    }
    simpleVideoView!!.setVideoURI(Uri.parse("android.resource://" + packageName + "/" + R.raw.jump))
    btnVideo?.setOnClickListener{//use button to play video
      simpleVideoView!!.start()
      Toast.makeText(applicationContext, "Start to play video!", Toast.LENGTH_LONG).show()
      txtVideo?.text = "Well done!"
    }
    simpleVideoView!!.setOnCompletionListener {
      Toast.makeText(applicationContext,"Video completed", Toast.LENGTH_LONG).show()
      txtVideo?.text = "Click Play to restart."
    }
    simpleVideoView!!.setOnErrorListener {
        mp, what, extra -> Toast.makeText(applicationContext, "An Error Occured " + "While Playing Video !!!", Toast.LENGTH_LONG).show()
      false
    }
  }
  private fun drawableToBitmap(drawable: Drawable): Bitmap {
    /** Returns a resized bitmap of the drawable image.  */
    val bitmap = Bitmap.createBitmap(257, 257, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }



}
