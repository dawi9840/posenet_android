package org.tensorflow.lite.examples.posenet

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.Drawable
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.core.app.ActivityCompat
import androidx.core.content.res.ResourcesCompat
import androidx.fragment.app.Fragment
import org.tensorflow.lite.examples.posenet.lib.BodyPart
import org.tensorflow.lite.examples.posenet.lib.Posenet

class Posenet02VideoTest :
  Fragment(),
  ActivityCompat.OnRequestPermissionsResultCallback {

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
  )
  private var btnVideo: Button?= null
  private var txtVideo: TextView?= null
  private var btnImg: Button?= null
  private var txtImg: TextView?= null
  private var btnSurf: Button?= null
  private var mediaPlayer: MediaPlayer? = null
  private var surfaceview: SurfaceView? = null

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):
    View? = inflater.inflate(R.layout.tfe_pn_activity_test, container, false)

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    simpleVideoView = view.findViewById(R.id.videoView) as VideoView
    surfaceview = view.findViewById(R.id.surfViewTest) as SurfaceView
    txtVideo = view.findViewById(R.id.txtViewVideo)
    btnVideo = view.findViewById(R.id.btn2)

    sampleImageView = view.findViewById(R.id.image)
    txtImg = view.findViewById(R.id.txtViewImg)
    btnImg = view.findViewById(R.id.btn)

    surfaceview!!.holder.setKeepScreenOn(true)
    surfaceview!!.holder.addCallback(SurfaceViewLis())
    btnSurf = view.findViewById(R.id.btn3)

  }

  private inner class SurfaceViewLis : SurfaceHolder.Callback {
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
      if (mediaPlayer != null && mediaPlayer!!.isPlaying) {
        mediaPlayer!!.stop()
      }
    }
  }

  override fun onStart() {
    super.onStart()
    btnShowPoseNetImg()
    btnPlayVideoView()
    btnPlaySurfaceView()
  }

  private fun btnPlaySurfaceView(){
    btnSurf?.setOnClickListener{
      video_SurfaceView()
    }
  }

  private fun btnPlayVideoView(){
    btnVideo?.setOnClickListener{
      video_VideoView()
    }

  }

  @SuppressLint("SetTextI18n")
  private fun video_VideoView(){
    if (mediaControls == null) {
      mediaControls = MediaController(this.context)
      mediaControls!!.setAnchorView(this.simpleVideoView)
    }

    simpleVideoView!!.setVideoURI(Uri.parse("android.resource://" +  activity?.packageName + "/" + R.raw.jump))
    simpleVideoView!!.start()
    txtVideo?.text = "Src video on video view."

    simpleVideoView!!.setOnCompletionListener {txtVideo?.text = "Click Play to restart."}
    simpleVideoView!!.setOnErrorListener {
      mp, what, extra -> Toast.makeText(this.context, "An Error Occured " + "While Playing Video !!!", Toast.LENGTH_LONG).show()
      false
    }
  }

  @SuppressLint("SetTextI18n")
  private fun video_SurfaceView(){
    mediaPlayer = MediaPlayer()
    mediaPlayer!!.setDataSource(this.context!!, Uri.parse("android.resource://" + activity?.packageName + "/" + R.raw.jump))

    // Set SurfaceView object to display video.
    mediaPlayer!!.setDisplay(surfaceview!!.holder)

    // prepare async to not block main thread
    mediaPlayer!!.prepareAsync()

    mediaPlayer!!.setOnPreparedListener {
      mediaPlayer!!.start()
      txtVideo?.text = "Src video on Surface View."
    }

    mediaPlayer!!.setOnCompletionListener {txtVideo?.text = "play done!"}
    mediaPlayer!!.setOnErrorListener {mp, what, extra -> false}
  }

  @SuppressLint("SetTextI18n")
  private fun btnShowPoseNetImg(){
    val drawedImage = ResourcesCompat.getDrawable(resources, R.drawable.image2, null)
    val imageBitmap = drawableToBitmap(drawedImage!!)

    // Calls the Posenet library functions.
    val posenet = Posenet(this.context!!)
    val person = posenet.estimateSinglePose(imageBitmap)

    // Define parameters to draw the keypoints over the image
    val paint = Paint()
    val size = 2.0f
    val mutableBitmap = imageBitmap.copy(Bitmap.Config.ARGB_8888, true)
    val canvas = Canvas(mutableBitmap)

    paint.color = Color.RED

    //load and show a source size image
    sampleImageView?.setImageBitmap(imageBitmap)

    // Show point6 ~ point17 keypoints
    for(i in 5..16){
      // Draw text (x, y) position on image.
      //canvas.drawText("P${i+1}", person.keyPoints[i].position.x.toFloat(), person.keyPoints[i].position.y.toFloat(), paint)
      canvas.drawCircle(
              person.keyPoints[i].position.x.toFloat(),
              person.keyPoints[i].position.y.toFloat(), size, paint)
      println("P${i+1}(x, y): ${person.keyPoints[i].position.x}, ${person.keyPoints[i].position.y}")
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

    btnImg?.setOnClickListener{
      sampleImageView?.adjustViewBounds = true
      sampleImageView?.setImageBitmap(mutableBitmap)
      txtImg?.text = "Posenet Img."
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