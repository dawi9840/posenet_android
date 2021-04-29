package org.tensorflow.lite.examples.posenet

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.Posenet as Posenet

class Posenet01ImageTest :
  Fragment(),
  ActivityCompat.OnRequestPermissionsResultCallback {

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
  private var ImgeView1: ImageView?= null
  private var ImgeView2: ImageView?= null
  private var btnImg1: Button?= null
  private var btnImg2: Button?= null
  private var txtImg: TextView?= null
  private var paint = Paint()

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):
    View? = inflater.inflate(R.layout.tfe_pn_img_test, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?){
    ImgeView1 = view.findViewById(R.id.image)
    ImgeView2 = view.findViewById(R.id.image2)
    btnImg1 = view.findViewById(R.id.btn)
    btnImg2 = view.findViewById(R.id.btn2)
    txtImg = view.findViewById(R.id.txtViewImg)
  }

  override fun onStart(){
    super.onStart()
    btnShowPoseNetImg1()
    btnShowPoseNetImg2()
  }

  private fun setPaint() {
    // Set the paint color and size.
    paint.color = Color.GREEN
    paint.textSize = 60.0f
    paint.strokeWidth = 2.0f
  }

  @SuppressLint("SetTextI18n")
  private fun btnShowPoseNetImg1(){
    val drawedImage = ResourcesCompat.getDrawable(resources, R.drawable.pose1, null)
    val imageBitmap = drawableToBitmap(drawedImage!!)
    val posenet = Posenet(this.context!!)
    val person = posenet.estimateSinglePose(imageBitmap)

    // Define size parameters to draw the keypoints over the image
    val size = 4.0f

    val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    setPaint()

    //load and show a source size image
    ImgeView1?.setImageBitmap(imageBitmap)

    // Show point6 ~ point17 keypoints
    for(i in 5..16){
      // Draw text (x, y) position on image.
      //canvas.drawText("P${i+1}", person.keyPoints[i].position.x.toFloat(), person.keyPoints[i].position.y.toFloat(), paint)
      canvas.drawCircle(
        person.keyPoints[i].position.x.toFloat(),
        person.keyPoints[i].position.y.toFloat(), size, paint)
      println("A(${i}): ${person.keyPoints[i].position.x}, ${person.keyPoints[i].position.y}")
    }

    // Draw lines to connect the bodyJoints points.
    for (line in bodyJoints) {
      canvas.drawLine(
        person.keyPoints[line.first.ordinal].position.x.toFloat(),
        person.keyPoints[line.first.ordinal].position.y.toFloat(),
        person.keyPoints[line.second.ordinal].position.x.toFloat(),
        person.keyPoints[line.second.ordinal].position.y.toFloat(),
        paint)
    }

    btnImg1?.setOnClickListener{
      ImgeView1?.adjustViewBounds = true
      ImgeView1?.setImageBitmap(mutableBitmap)
      txtImg?.text = "Posenet Img1."
    }

  }

  private fun btnShowPoseNetImg2(){
    val drawedImage = ResourcesCompat.getDrawable(resources, R.drawable.pose2, null)
    val imageBitmap = drawableToBitmap(drawedImage!!)
    val posenet = Posenet(this.context!!)
    val person = posenet.estimateSinglePose(imageBitmap)
    val size = 4.0f
    val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    paint.color = Color.CYAN

    ImgeView2?.setImageBitmap(imageBitmap)

    for(i in 5..16){
      canvas.drawCircle(
        person.keyPoints[i].position.x.toFloat(),
        person.keyPoints[i].position.y.toFloat(), size, paint)
      println("B(${i}): ${person.keyPoints[i].position.x}, ${person.keyPoints[i].position.y}")
    }

    for (line in bodyJoints) {
      canvas.drawLine(
        person.keyPoints[line.first.ordinal].position.x.toFloat(),
        person.keyPoints[line.first.ordinal].position.y.toFloat(),
        person.keyPoints[line.second.ordinal].position.x.toFloat(),
        person.keyPoints[line.second.ordinal].position.y.toFloat(),
        paint)
    }

    btnImg2?.setOnClickListener{
      ImgeView2?.adjustViewBounds = true
      ImgeView2?.setImageBitmap(mutableBitmap)
      txtImg?.text = "Posenet Img2."
    }

  }

  private fun drawableToBitmap(drawable: Drawable): Bitmap {
    // Returns a resized bitmap of the drawable image.
    val bitmap = Bitmap.createBitmap(257, 257, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
  }

}
