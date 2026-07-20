package com.example.picosbsvrplayer.vr

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.view.Surface
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicBoolean
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt

/**
 * Renders one decoded video texture into two eye viewports. The input may already
 * be left/right SBS, top/bottom stereo, or mono. This avoids decoding twice.
 */
@SuppressLint("ViewConstructor")
class SbsVideoView(
  context: Context,
  private val onSurfaceReady: (Surface) -> Unit,
) : GLSurfaceView(context), GLSurfaceView.Renderer, SurfaceTexture.OnFrameAvailableListener {
  @Volatile var inputLayout: InputLayout = InputLayout.SBS
  @Volatile var projectionMode: ProjectionMode = ProjectionMode.VR180
  @Volatile var lensCenterScale: Float = LensCorrection.STANDARD.centerScale
  @Volatile var eyeShift: Float = 0f
  @Volatile var viewOffsetX: Float = 0f
  @Volatile var viewOffsetY: Float = 0f
  @Volatile var swapEyes: Boolean = false
  @Volatile var mirrorHorizontal: Boolean = true
  @Volatile var scaleMode: ScaleMode = ScaleMode.FILL
  @Volatile var viewScale: Float = 1f
  @Volatile var outputRotation: Int = 0

  @Volatile private var videoWidth = 16
  @Volatile private var videoHeight = 9
  private val frameAvailable = AtomicBoolean(false)
  private val textureMatrix = FloatArray(16)
  private var program = 0
  private var textureId = 0
  private var surfaceTexture: SurfaceTexture? = null
  private var decoderSurface: Surface? = null
  private var viewWidth = 1
  private var viewHeight = 1

  private val vertices: FloatBuffer =
    ByteBuffer.allocateDirect(VERTICES.size * 4).order(ByteOrder.nativeOrder()).asFloatBuffer().apply {
      put(VERTICES)
      position(0)
    }

  init {
    setEGLContextClientVersion(2)
    setRenderer(this)
    renderMode = RENDERMODE_WHEN_DIRTY
    preserveEGLContextOnPause = true
  }

  fun setVideoSize(width: Int, height: Int) {
    if (width > 0 && height > 0) {
      videoWidth = width
      videoHeight = height
      requestRender()
    }
  }

  override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
    program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
    textureId = createExternalTexture()
    surfaceTexture = SurfaceTexture(textureId).also { it.setOnFrameAvailableListener(this) }
    decoderSurface = Surface(surfaceTexture).also(onSurfaceReady)
    Matrix.setIdentityM(textureMatrix, 0)
    GLES20.glClearColor(0f, 0f, 0f, 1f)
  }

  override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
    viewWidth = width.coerceAtLeast(1)
    viewHeight = height.coerceAtLeast(1)
  }

  override fun onDrawFrame(gl: GL10?) {
    if (frameAvailable.compareAndSet(true, false)) {
      surfaceTexture?.updateTexImage()
      surfaceTexture?.getTransformMatrix(textureMatrix)
    }
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
    if (program == 0) return

    GLES20.glUseProgram(program)
    val position = GLES20.glGetAttribLocation(program, "aPosition")
    val texCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
    val texMatrix = GLES20.glGetUniformLocation(program, "uTexMatrix")
    val crop = GLES20.glGetUniformLocation(program, "uCrop")
    val rotation = GLES20.glGetUniformLocation(program, "uRotation")
    val projection = GLES20.glGetUniformLocation(program, "uProjection")
    val lensScale = GLES20.glGetUniformLocation(program, "uLensCenterScale")
    val eyeShiftLocation = GLES20.glGetUniformLocation(program, "uEyeShift")
    val viewOffsetLocation = GLES20.glGetUniformLocation(program, "uViewOffset")
    val mirrorHorizontalLocation = GLES20.glGetUniformLocation(program, "uMirrorHorizontal")
    val sampler = GLES20.glGetUniformLocation(program, "uTexture")

    vertices.position(0)
    GLES20.glEnableVertexAttribArray(position)
    GLES20.glVertexAttribPointer(position, 2, GLES20.GL_FLOAT, false, 16, vertices)
    vertices.position(2)
    GLES20.glEnableVertexAttribArray(texCoord)
    GLES20.glVertexAttribPointer(texCoord, 2, GLES20.GL_FLOAT, false, 16, vertices)
    GLES20.glUniformMatrix4fv(texMatrix, 1, false, textureMatrix, 0)
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
    GLES20.glUniform1i(sampler, 0)

    GLES20.glUniform1f(rotation, (outputRotation / 90).toFloat())
    GLES20.glUniform1f(
      projection,
      if (projectionMode == ProjectionMode.FLAT_SBS_CORRECTED) 1f else 0f,
    )
    GLES20.glUniform1f(lensScale, lensCenterScale.coerceIn(0.6f, 1f))
    GLES20.glUniform2f(
      viewOffsetLocation,
      viewOffsetX.coerceIn(-0.15f, 0.15f),
      viewOffsetY.coerceIn(-0.15f, 0.15f),
    )
    GLES20.glUniform1f(mirrorHorizontalLocation, if (mirrorHorizontal) 1f else 0f)
    drawEye(0, if (swapEyes) 1 else 0, crop, eyeShiftLocation)
    drawEye(1, if (swapEyes) 0 else 1, crop, eyeShiftLocation)

    GLES20.glDisableVertexAttribArray(position)
    GLES20.glDisableVertexAttribArray(texCoord)
  }

  private fun drawEye(outputEye: Int, sourceEye: Int, cropUniform: Int, eyeShiftUniform: Int) {
    val viewport = calculateViewport(outputEye)
    GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3])
    when (inputLayout) {
      InputLayout.SBS -> GLES20.glUniform4f(cropUniform, sourceEye * 0.5f, 0f, 0.5f, 1f)
      InputLayout.TOP_BOTTOM -> GLES20.glUniform4f(cropUniform, 0f, sourceEye * 0.5f, 1f, 0.5f)
      InputLayout.MONO -> GLES20.glUniform4f(cropUniform, 0f, 0f, 1f, 1f)
    }
    val shift = if (inputLayout == InputLayout.MONO) 0f else eyeShift.coerceIn(-0.03f, 0.03f)
    GLES20.glUniform1f(eyeShiftUniform, if (outputEye == 0) shift else -shift)
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
  }

  private fun calculateViewport(eye: Int): IntArray {
    val rotation = ((outputRotation % 360) + 360) % 360
    val quarterTurn = rotation == 90 || rotation == 270
    val slotWidth = if (quarterTurn) viewWidth else viewWidth / 2
    val slotHeight = if (quarterTurn) viewHeight / 2 else viewHeight
    val slotX = if (quarterTurn) 0 else eye * slotWidth
    val slotY =
      when (rotation) {
        90 -> (1 - eye) * slotHeight
        270 -> eye * slotHeight
        else -> 0
      }
    val baseViewport =
      if (scaleMode == ScaleMode.FILL) {
        intArrayOf(slotX, slotY, slotWidth, slotHeight)
      } else {
        calculateFitViewport(slotX, slotY, slotWidth, slotHeight, quarterTurn)
      }
    val scale = viewScale.coerceIn(0.5f, 1f)
    val scaledWidth = (baseViewport[2] * scale).roundToInt().coerceAtLeast(1)
    val scaledHeight = (baseViewport[3] * scale).roundToInt().coerceAtLeast(1)
    return intArrayOf(
      baseViewport[0] + (baseViewport[2] - scaledWidth) / 2,
      baseViewport[1] + (baseViewport[3] - scaledHeight) / 2,
      scaledWidth,
      scaledHeight,
    )
  }

  private fun calculateFitViewport(
    slotX: Int,
    slotY: Int,
    slotWidth: Int,
    slotHeight: Int,
    quarterTurn: Boolean,
  ): IntArray {
    val sourceAspect =
      when (inputLayout) {
        InputLayout.SBS -> (videoWidth / 2f) / videoHeight
        InputLayout.TOP_BOTTOM -> videoWidth / (videoHeight / 2f)
        InputLayout.MONO -> videoWidth.toFloat() / videoHeight
      }
    val rotatedSourceAspect = if (quarterTurn) 1f / sourceAspect else sourceAspect
    val destinationAspect = slotWidth.toFloat() / slotHeight
    return if (rotatedSourceAspect > destinationAspect) {
      val height = (slotWidth / rotatedSourceAspect).toInt().coerceAtLeast(1)
      intArrayOf(slotX, slotY + (slotHeight - height) / 2, slotWidth, height)
    } else {
      val width = (slotHeight * rotatedSourceAspect).toInt().coerceAtLeast(1)
      intArrayOf(slotX + (slotWidth - width) / 2, slotY, width, slotHeight)
    }
  }

  override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
    frameAvailable.set(true)
    requestRender()
  }

  fun releaseVideoSurface() {
    queueEvent {
      surfaceTexture?.setOnFrameAvailableListener(null)
      decoderSurface?.release()
      surfaceTexture?.release()
      decoderSurface = null
      surfaceTexture = null
    }
  }

  private fun createExternalTexture(): Int {
    val textures = IntArray(1)
    GLES20.glGenTextures(1, textures, 0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0])
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    return textures[0]
  }

  private fun createProgram(vertex: String, fragment: String): Int {
    val vertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertex)
    val fragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragment)
    return GLES20.glCreateProgram().also {
      GLES20.glAttachShader(it, vertexShader)
      GLES20.glAttachShader(it, fragmentShader)
      GLES20.glLinkProgram(it)
    }
  }

  private fun compileShader(type: Int, source: String): Int =
    GLES20.glCreateShader(type).also {
      GLES20.glShaderSource(it, source)
      GLES20.glCompileShader(it)
    }

  companion object {
    private val VERTICES = floatArrayOf(
      -1f, -1f, 0f, 1f,
      1f, -1f, 1f, 1f,
      -1f, 1f, 0f, 0f,
      1f, 1f, 1f, 0f,
    )

    private const val VERTEX_SHADER = """
      attribute vec4 aPosition;
      attribute vec2 aTexCoord;
      varying vec2 vLocalCoord;
      void main() {
        gl_Position = aPosition;
        vLocalCoord = aTexCoord;
      }
    """

    private const val FRAGMENT_SHADER = """
      #extension GL_OES_EGL_image_external : require
      precision mediump float;
      uniform samplerExternalOES uTexture;
      uniform mat4 uTexMatrix;
      uniform vec4 uCrop;
      uniform float uRotation;
      uniform float uProjection;
      uniform float uLensCenterScale;
      uniform float uEyeShift;
      uniform vec2 uViewOffset;
      uniform float uMirrorHorizontal;
      varying vec2 vLocalCoord;
      void main() {
        vec2 local = vLocalCoord;
        local.x += uEyeShift;
        if (local.x < 0.0 || local.x > 1.0) {
          gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
          return;
        }

        // Flat stereo footage has no optical pre-warp. Expand its centre and
        // compress its edges into an approximately 180-degree fisheye image,
        // independently for each eye. Native VR180 frames pass through unchanged.
        if (uProjection > 0.5) {
          vec2 lens = local * 2.0 - 1.0;
          float radius2 = dot(lens, lens);
          float factor = uLensCenterScale + (1.0 - uLensCenterScale) * radius2;
          local = lens * factor * 0.5 + 0.5;
          if (local.x < 0.0 || local.x > 1.0 || local.y < 0.0 || local.y > 1.0) {
            gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
            return;
          }
        }

        // Mirror in the final landscape viewing direction. This must happen
        // before the panel rotation so 90/270-degree outputs flip horizontally.
        if (uMirrorHorizontal > 0.5) {
          local.x = 1.0 - local.x;
        }

        if (uRotation == 1.0) {
          local = vec2(local.y, 1.0 - local.x);
        } else if (uRotation == 2.0) {
          local = vec2(1.0 - local.x, 1.0 - local.y);
        } else if (uRotation == 3.0) {
          local = vec2(1.0 - local.y, local.x);
        }
        local += uViewOffset;
        if (local.x < 0.0 || local.x > 1.0 || local.y < 0.0 || local.y > 1.0) {
          gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
          return;
        }
        vec2 cropped = local * uCrop.zw + uCrop.xy;
        vec2 textureCoord = (uTexMatrix * vec4(cropped, 0.0, 1.0)).xy;
        gl_FragColor = texture2D(uTexture, textureCoord);
      }
    """
  }
}
