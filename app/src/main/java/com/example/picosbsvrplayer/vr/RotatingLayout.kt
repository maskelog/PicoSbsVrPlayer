package com.example.picosbsvrplayer.vr

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup

/** Gives its child a landscape canvas even when the HMD EDID is portrait. */
class RotatingLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
) : ViewGroup(context, attrs) {
  var outputRotation: Int = 0
    set(value) {
      field = ((value % 360) + 360) % 360
      requestLayout()
    }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    val width = MeasureSpec.getSize(widthMeasureSpec)
    val height = MeasureSpec.getSize(heightMeasureSpec)
    val quarterTurn = outputRotation == 90 || outputRotation == 270
    val childWidth = if (quarterTurn) height else width
    val childHeight = if (quarterTurn) width else height
    getChildAt(0)?.measure(
      MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
      MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY),
    )
    setMeasuredDimension(width, height)
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    val child = getChildAt(0) ?: return
    val width = right - left
    val height = bottom - top
    val quarterTurn = outputRotation == 90 || outputRotation == 270
    child.layout(0, 0, if (quarterTurn) height else width, if (quarterTurn) width else height)
    child.pivotX = 0f
    child.pivotY = 0f
    child.rotation = outputRotation.toFloat()
    when (outputRotation) {
      90 -> {
        child.translationX = width.toFloat()
        child.translationY = 0f
      }
      180 -> {
        child.translationX = width.toFloat()
        child.translationY = height.toFloat()
      }
      270 -> {
        child.translationX = 0f
        child.translationY = height.toFloat()
      }
      else -> {
        child.translationX = 0f
        child.translationY = 0f
      }
    }
  }

  fun replaceContent(view: View) {
    removeAllViews()
    addView(view, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    requestLayout()
  }
}
