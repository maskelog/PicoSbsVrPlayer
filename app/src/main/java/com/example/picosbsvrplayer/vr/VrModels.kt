package com.example.picosbsvrplayer.vr

enum class InputLayout(val label: String) {
  SBS("좌우 SBS"),
  TOP_BOTTOM("상하 3D"),
  MONO("일반 영상"),
}

enum class ProjectionMode(val label: String) {
  VR180("VR180 (원본)"),
  FLAT_SBS_CORRECTED("평면 SBS 보정"),
}

enum class LensCorrection(val label: String, val centerScale: Float) {
  WEAK("약하게", 0.90f),
  STANDARD("표준", 0.84f),
  STRONG("강하게", 0.76f),
}

enum class OutputRotation(val label: String, val degrees: Int?) {
  AUTO("자동", null),
  DEG_0("0°", 0),
  DEG_90("90°", 90),
  DEG_270("270°", 270),
}

enum class ScaleMode(val label: String) {
  FILL("화면 채우기"),
  FIT("비율 유지"),
}

enum class ViewSize(val label: String, val scale: Float) {
  ORIGINAL("기존 100%", 1f),
  WIDE("넓게 90%", 0.9f),
  EXTRA_WIDE("더 넓게 80%", 0.8f),
}

data class OutputStatus(
  val connected: Boolean = false,
  val displayName: String = "PICO를 연결하세요",
  val resolution: String = "-",
  val refreshRate: String = "-",
)
