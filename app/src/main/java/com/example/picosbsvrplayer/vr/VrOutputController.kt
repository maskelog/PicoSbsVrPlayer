package com.example.picosbsvrplayer.vr

import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.hardware.display.DisplayManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.Display
import android.view.Surface
import android.widget.Toast
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VrOutputController(private val activity: Activity) : DisplayManager.DisplayListener {
  private val displayManager = activity.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
  private val mainHandler = Handler(Looper.getMainLooper())
  private val _status = MutableStateFlow(OutputStatus())
  val status: StateFlow<OutputStatus> = _status.asStateFlow()

  private var presentation: VrPresentation? = null
  private var player: ExoPlayer? = null
  private var videoView: SbsVideoView? = null
  private var decoderSurface: Surface? = null
  private var rotation = OutputRotation.AUTO
  private var inputLayout = InputLayout.SBS
  private var projectionMode = ProjectionMode.VR180
  private var lensCorrection = LensCorrection.STANDARD
  private var eyeShiftPercent = 0f
  private var viewOffsetXPercent = 0f
  private var viewOffsetYPercent = 0f
  private var scaleMode = ScaleMode.FILL
  private var viewSize = ViewSize.ORIGINAL
  private var swapEyes = false
  private var mirrorHorizontal = true

  init {
    displayManager.registerDisplayListener(this, mainHandler)
    refreshDisplay()
  }

  fun refreshDisplay() {
    val display = findExternalDisplay()
    if (display == null) {
      presentation?.dismiss()
      presentation = null
      _status.value = OutputStatus()
      return
    }
    val mode = display.mode
    _status.value =
      OutputStatus(
        connected = true,
        displayName = display.name,
        resolution = "${mode.physicalWidth} × ${mode.physicalHeight}",
        refreshRate = "%.1f Hz".format(mode.refreshRate),
      )
    if (presentation?.display?.displayId != display.displayId) {
      presentation?.dismiss()
      presentation =
        runCatching {
          VrPresentation(activity, display).also {
            it.show()
            it.showContent(CalibrationView(it.context))
          }
        }.getOrElse {
          _status.value = _status.value.copy(connected = false, displayName = "${display.name} · 출력 창 열기 실패")
          Toast.makeText(activity, "외부 화면을 독립 모드로 열 수 없습니다.", Toast.LENGTH_LONG).show()
          null
        }
    }
    applyRotation()
  }

  fun showCalibration() {
    val output = requirePresentation() ?: return
    player?.pause()
    releaseVideoView()
    output.showContent(CalibrationView(output.context))
    applyRotation()
  }

  fun playLocal(uri: Uri) {
    val output = requirePresentation() ?: return
    releaseVideoView()
    val glView =
      SbsVideoView(output.context) { surface ->
        mainHandler.post {
          decoderSurface = surface
          player?.setVideoSurface(surface)
        }
      }.also {
        it.inputLayout = inputLayout
        it.projectionMode = projectionMode
        it.lensCenterScale = lensCorrection.centerScale
        it.eyeShift = eyeShiftPercent / 100f
        it.viewOffsetX = viewOffsetXPercent / 100f
        it.viewOffsetY = viewOffsetYPercent / 100f
        it.scaleMode = scaleMode
        it.viewScale = viewSize.scale
        it.swapEyes = swapEyes
        it.mirrorHorizontal = mirrorHorizontal
      }
    videoView = glView
    output.showContent(glView)
    applyRotation()

    val exoPlayer =
      (player ?: ExoPlayer.Builder(activity).build().also { created ->
        created.addListener(
          object : Player.Listener {
            override fun onVideoSizeChanged(videoSize: VideoSize) {
              videoView?.setVideoSize(videoSize.width, videoSize.height)
            }
          },
        )
        player = created
      })
    decoderSurface?.let(exoPlayer::setVideoSurface)
    exoPlayer.setMediaItem(MediaItem.fromUri(uri))
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
  }

  fun showYouTube(url: String): Boolean {
    val videoId = parseYouTubeVideoId(url) ?: return false
    val output = requirePresentation() ?: return false
    player?.pause()
    releaseVideoView()
    output.showYouTube(videoId)
    applyRotation()
    return true
  }

  fun play() {
    player?.play()
    presentation?.playYouTube()
  }

  fun pause() {
    player?.pause()
    presentation?.pauseYouTube()
  }

  fun seekBy(milliseconds: Long) {
    player?.let { it.seekTo((it.currentPosition + milliseconds).coerceAtLeast(0)) }
  }

  fun updateSettings(
    rotation: OutputRotation,
    inputLayout: InputLayout,
    projectionMode: ProjectionMode,
    lensCorrection: LensCorrection,
    eyeShiftPercent: Float,
    viewOffsetXPercent: Float,
    viewOffsetYPercent: Float,
    scaleMode: ScaleMode,
    viewSize: ViewSize,
    swapEyes: Boolean,
    mirrorHorizontal: Boolean,
  ) {
    this.rotation = rotation
    this.inputLayout = inputLayout
    this.projectionMode = projectionMode
    this.lensCorrection = lensCorrection
    this.eyeShiftPercent = eyeShiftPercent.coerceIn(-3f, 3f)
    this.viewOffsetXPercent = viewOffsetXPercent.coerceIn(-15f, 15f)
    this.viewOffsetYPercent = viewOffsetYPercent.coerceIn(-15f, 15f)
    this.scaleMode = scaleMode
    this.viewSize = viewSize
    this.swapEyes = swapEyes
    this.mirrorHorizontal = mirrorHorizontal
    videoView?.run {
      this.inputLayout = inputLayout
      this.projectionMode = projectionMode
      this.lensCenterScale = lensCorrection.centerScale
      this.eyeShift = this@VrOutputController.eyeShiftPercent / 100f
      this.viewOffsetX = this@VrOutputController.viewOffsetXPercent / 100f
      this.viewOffsetY = this@VrOutputController.viewOffsetYPercent / 100f
      this.scaleMode = scaleMode
      this.viewScale = viewSize.scale
      this.swapEyes = swapEyes
      this.mirrorHorizontal = mirrorHorizontal
      requestRender()
    }
    applyRotation()
  }

  fun launchYouTubeAppOnExternalDisplay(url: String): Boolean {
    val display = findExternalDisplay() ?: return false
    return runCatching {
      val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
      val options = ActivityOptions.makeBasic().apply { launchDisplayId = display.displayId }
      activity.startActivity(intent, options.toBundle())
      true
    }.getOrElse {
      Toast.makeText(activity, "YouTube 앱을 PICO 화면에서 열 수 없습니다.", Toast.LENGTH_LONG).show()
      false
    }
  }

  private fun applyRotation() {
    val output = presentation ?: return
    val mode = output.display.mode
    // PICO advertises a portrait 2160x3840 panel, but its optical/viewing
    // orientation is the clockwise landscape direction on the Samsung host.
    val degrees = rotation.degrees ?: if (mode.physicalHeight > mode.physicalWidth) 90 else 0
    videoView?.let {
      // SurfaceView layers do not reliably inherit their parent's transform on
      // Samsung external Presentation displays. Rotate and pack the two eyes
      // in GL instead, against the display's physical portrait coordinates.
      output.rotatingLayout.outputRotation = 0
      it.outputRotation = degrees
      it.requestRender()
    } ?: run {
      output.rotatingLayout.outputRotation = degrees
    }
  }

  private fun requirePresentation(): VrPresentation? {
    refreshDisplay()
    val output = presentation
    if (output == null) Toast.makeText(activity, "PICO 외부 화면이 감지되지 않았습니다.", Toast.LENGTH_LONG).show()
    return output
  }

  private fun findExternalDisplay(): Display? =
    displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION).firstOrNull {
      it.displayId != Display.DEFAULT_DISPLAY && it.isValid
    } ?: displayManager.displays.firstOrNull { it.displayId != Display.DEFAULT_DISPLAY && it.isValid }

  private fun releaseVideoView() {
    player?.setVideoSurface(null)
    decoderSurface = null
    videoView?.releaseVideoSurface()
    videoView = null
  }

  fun release() {
    displayManager.unregisterDisplayListener(this)
    releaseVideoView()
    player?.release()
    player = null
    presentation?.dismiss()
    presentation = null
  }

  override fun onDisplayAdded(displayId: Int) = refreshDisplay()
  override fun onDisplayRemoved(displayId: Int) = refreshDisplay()
  override fun onDisplayChanged(displayId: Int) = refreshDisplay()

  companion object {
    fun parseYouTubeVideoId(value: String): String? {
      val text = value.trim()
      if (text.matches(Regex("[A-Za-z0-9_-]{11}"))) return text
      val uri = runCatching { Uri.parse(text) }.getOrNull() ?: return null
      val host = uri.host.orEmpty().lowercase()
      val id =
        when {
          host == "youtu.be" -> uri.pathSegments.firstOrNull()
          host.endsWith("youtube.com") && uri.pathSegments.firstOrNull() in listOf("embed", "shorts", "live") ->
            uri.pathSegments.getOrNull(1)
          host.endsWith("youtube.com") -> uri.getQueryParameter("v")
          else -> null
        }
      return id?.takeIf { it.matches(Regex("[A-Za-z0-9_-]{11}")) }
    }
  }
}
