package com.example.picosbsvrplayer.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.picosbsvrplayer.vr.InputLayout
import com.example.picosbsvrplayer.vr.LensCorrection
import com.example.picosbsvrplayer.vr.OutputRotation
import com.example.picosbsvrplayer.vr.ProjectionMode
import com.example.picosbsvrplayer.vr.ScaleMode
import com.example.picosbsvrplayer.vr.ViewSize
import com.example.picosbsvrplayer.vr.VrOutputController

@Composable
fun VrControlScreen(controller: VrOutputController) {
  val context = LocalContext.current
  val status by controller.status.collectAsStateWithLifecycle()
  var rotation by remember { mutableStateOf(OutputRotation.AUTO) }
  var inputLayout by remember { mutableStateOf(InputLayout.SBS) }
  var projectionMode by remember { mutableStateOf(ProjectionMode.VR180) }
  var lensCorrection by remember { mutableStateOf(LensCorrection.STANDARD) }
  var eyeShiftPercent by remember { mutableStateOf(0f) }
  var viewOffsetXPercent by remember { mutableStateOf(0f) }
  var viewOffsetYPercent by remember { mutableStateOf(0f) }
  var scaleMode by remember { mutableStateOf(ScaleMode.FILL) }
  var viewSize by remember { mutableStateOf(ViewSize.ORIGINAL) }
  var swapEyes by remember { mutableStateOf(false) }
  var mirrorHorizontal by remember { mutableStateOf(true) }
  var selectedSection by remember { mutableStateOf(ControlSection.PLAYER) }
  var youtubeUrl by remember { mutableStateOf("") }
  var message by remember { mutableStateOf("") }

  LaunchedEffect(
    rotation,
    inputLayout,
    projectionMode,
    lensCorrection,
    eyeShiftPercent,
    viewOffsetXPercent,
    viewOffsetYPercent,
    scaleMode,
    viewSize,
    swapEyes,
    mirrorHorizontal,
  ) {
    controller.updateSettings(
      rotation,
      inputLayout,
      projectionMode,
      lensCorrection,
      eyeShiftPercent,
      viewOffsetXPercent,
      viewOffsetYPercent,
      scaleMode,
      viewSize,
      swapEyes,
      mirrorHorizontal,
    )
  }

  val videoPicker =
    rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
      if (uri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        controller.playLocal(uri)
        message = "로컬 영상을 PICO에서 재생합니다."
      }
    }

  Scaffold(modifier = Modifier.fillMaxSize()) { padding ->
    Column(
      modifier =
        Modifier.padding(padding).padding(horizontal = 18.dp).verticalScroll(rememberScrollState()),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Spacer(Modifier.height(4.dp))
      Text("PICO SBS VR Player", style = MaterialTheme.typography.headlineSmall)
      Text("휴대폰은 리모컨, PICO는 독립 VR 화면으로 사용합니다.", style = MaterialTheme.typography.bodyMedium)

      Card(
        colors =
          CardDefaults.cardColors(
            containerColor =
              if (status.connected) MaterialTheme.colorScheme.primaryContainer
              else MaterialTheme.colorScheme.errorContainer,
          ),
      ) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
          Text(if (status.connected) "외부 화면 연결됨" else "외부 화면 없음", style = MaterialTheme.typography.titleMedium)
          Text(status.displayName)
          Text("${status.resolution} · ${status.refreshRate}")
        }
      }

      SectionTabs(ControlSection.entries, selectedSection) { selectedSection = it }

      Card(modifier = Modifier.fillMaxWidth()) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          when (selectedSection) {
            ControlSection.DISPLAY -> {
              SectionTitle("화면 설정")
              Text("PICO 출력 방향과 눈 구분 테스트를 설정합니다.", style = MaterialTheme.typography.bodySmall)
              ChipRows(OutputRotation.entries, rotation, { it.label }) { rotation = it }
              OutlinedButton(onClick = controller::showCalibration, enabled = status.connected) {
                Text("L/R 전체화면 테스트 패턴")
              }
            }

            ControlSection.PLAYER -> {
      SectionTitle("VR 재생")
      Text("영상 형식에 맞춰 눈 배치와 투영 방식을 선택한 뒤 로컬 영상을 재생하세요.")
      ChipRows(InputLayout.entries, inputLayout, { it.label }) { inputLayout = it }
      Text("영상 투영 방식")
      ChipRows(ProjectionMode.entries, projectionMode, { it.label }) { projectionMode = it }
      Text(
        "VR180은 VR 영상 원본에, 평면 SBS 보정은 일반 직사각형 3D 영상에 사용합니다.",
        style = MaterialTheme.typography.bodySmall,
      )
      if (projectionMode == ProjectionMode.FLAT_SBS_CORRECTED) {
        Text(
          "평면 영상의 렌즈 왜곡을 보정합니다.",
          style = MaterialTheme.typography.bodySmall,
        )
        Text("렌즈 보정 강도")
        ChipRows(LensCorrection.entries, lensCorrection, { it.label }) { lensCorrection = it }
      }
      ChipRows(ScaleMode.entries, scaleMode, { it.label }) { scaleMode = it }
      Text("시야 크기")
      ChipRows(ViewSize.entries, viewSize, { it.label }) { viewSize = it }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("좌우 눈 바꾸기", modifier = Modifier.weight(1f))
        Switch(checked = swapEyes, onCheckedChange = { swapEyes = it })
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("수평 반전 보정", modifier = Modifier.weight(1f))
        Switch(checked = mirrorHorizontal, onCheckedChange = { mirrorHorizontal = it })
      }
      Text(
        "글자와 영상이 거울처럼 보이면 켜세요. 좌우 눈 배치에는 영향을 주지 않습니다.",
        style = MaterialTheme.typography.bodySmall,
      )
      Text("입체 초점 보정: ${if (eyeShiftPercent > 0) "+" else ""}${eyeShiftPercent.toInt()}%")
      Slider(
        value = eyeShiftPercent,
        onValueChange = { eyeShiftPercent = it },
        valueRange = -3f..3f,
        steps = 5,
      )
      Text(
        "+ 방향은 가까운 물체의 과도한 좌우 시차를 줄입니다. 0%에서 시작해 1%씩 조절하세요.",
        style = MaterialTheme.typography.bodySmall,
      )
      Button(onClick = { videoPicker.launch(arrayOf("video/*")) }, enabled = status.connected) { Text("로컬 영상 선택") }

      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { controller.seekBy(-10_000) }) { Text("-10초") }
        OutlinedButton(onClick = controller::play) { Text("재생") }
        OutlinedButton(onClick = controller::pause) { Text("일시정지") }
        OutlinedButton(onClick = { controller.seekBy(10_000) }) { Text("+10초") }
      }

            }

            ControlSection.TRACKPAD -> {
              SectionTitle("시점 조정")
              Text("패드 위를 드래그하면 PICO 화면의 시점이 같은 방향으로 움직입니다.")
              val trackpadBackground = MaterialTheme.colorScheme.surfaceVariant
              val trackpadGrid = MaterialTheme.colorScheme.outline
              val trackpadIndicator = MaterialTheme.colorScheme.primary
              Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Canvas(
                  modifier =
                    Modifier.size(240.dp)
                      .semantics { contentDescription = "원형 VR 시점 트랙패드" }
                      .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                          change.consume()
                          if (size.width > 0 && size.height > 0) {
                            viewOffsetXPercent =
                              (viewOffsetXPercent + dragAmount.x / size.width * 30f).coerceIn(-15f, 15f)
                            viewOffsetYPercent =
                              (viewOffsetYPercent + dragAmount.y / size.height * 30f).coerceIn(-15f, 15f)
                          }
                        }
                      },
                ) {
                  val centre = Offset(size.width / 2f, size.height / 2f)
                  val radius = size.minDimension / 2f
                  val indicatorRange = radius - 28f
                  drawCircle(color = trackpadBackground, radius = radius, center = centre)
                  drawLine(
                    color = trackpadGrid,
                    start = Offset(centre.x, centre.y - radius),
                    end = Offset(centre.x, centre.y + radius),
                    strokeWidth = 2f,
                  )
                  drawLine(
                    color = trackpadGrid,
                    start = Offset(centre.x - radius, centre.y),
                    end = Offset(centre.x + radius, centre.y),
                    strokeWidth = 2f,
                  )
                  drawCircle(
                    color = trackpadIndicator,
                    radius = 22f,
                    center =
                      Offset(
                        x = centre.x + viewOffsetXPercent / 15f * indicatorRange,
                        y = centre.y + viewOffsetYPercent / 15f * indicatorRange,
                      ),
                  )
                }
              }
              Text("좌우 ${viewOffsetXPercent.toInt()}% · 상하 ${viewOffsetYPercent.toInt()}%")
              OutlinedButton(
                onClick = {
                  viewOffsetXPercent = 0f
                  viewOffsetYPercent = 0f
                },
              ) { Text("시점 중앙으로") }
              Text(
                "두 눈의 시점을 함께 이동하며 입체 초점 설정에는 영향을 주지 않습니다.",
                style = MaterialTheme.typography.bodySmall,
              )
            }

            ControlSection.YOUTUBE -> {
      SectionTitle("YouTube VR")
      OutlinedTextField(
        value = youtubeUrl,
        onValueChange = { youtubeUrl = it },
        label = { Text("YouTube 영상 주소") },
        placeholder = { Text("https://youtu.be/...") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
      )
      Button(
        onClick = {
          message =
            if (controller.showYouTube(youtubeUrl)) "YouTube 영상을 PICO에서 엽니다."
            else "올바른 YouTube 영상 주소를 입력하세요."
        },
        enabled = status.connected,
      ) { Text("PICO 내장 플레이어에서 열기") }
      Text(
        "내장 플레이어는 공식 YouTube 웹 플레이어를 사용합니다. 앱은 YouTube 영상을 다운로드하거나 변환하지 않습니다.",
        style = MaterialTheme.typography.bodySmall,
      )
      OutlinedButton(
        onClick = {
          message =
            if (controller.launchYouTubeAppOnExternalDisplay(youtubeUrl)) "YouTube 앱을 PICO 화면에서 실행했습니다."
            else "외부 화면에서 YouTube 앱을 실행하지 못했습니다."
        },
        enabled = status.connected && youtubeUrl.isNotBlank(),
      ) { Text("공식 YouTube 앱을 PICO에서 열기") }
      Text(
        "이미 SBS로 업로드된 영상은 내장 플레이어로 볼 수 있습니다. YouTube 180°/360° 영상의 시점 제어와 Cardboard 모드는 공식 YouTube 앱에서 제공될 때만 사용할 수 있습니다.",
        style = MaterialTheme.typography.bodySmall,
      )
            }
          }
        }
      }

      if (message.isNotEmpty()) Text(message, color = MaterialTheme.colorScheme.primary)
      Spacer(Modifier.height(24.dp))
    }
  }
}

private enum class ControlSection(val label: String) {
  DISPLAY("화면 설정"),
  PLAYER("VR 재생"),
  TRACKPAD("시점 조정"),
  YOUTUBE("YouTube"),
}

@Composable
private fun SectionTabs(
  values: List<ControlSection>,
  selected: ControlSection,
  onSelect: (ControlSection) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    values.chunked(2).forEach { rowValues ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rowValues.forEach { value ->
          FilterChip(
            selected = value == selected,
            onClick = { onSelect(value) },
            label = { Text(value.label) },
            modifier = Modifier.weight(1f),
          )
        }
      }
    }
  }
}

@Composable
private fun SectionTitle(text: String) {
  Text(text, style = MaterialTheme.typography.titleMedium)
}

@Composable
private fun <T> ChipRows(
  values: List<T>,
  selected: T,
  label: (T) -> String,
  onSelect: (T) -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
    values.chunked(3).forEach { rowValues ->
      Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        rowValues.forEach { value ->
          FilterChip(selected = value == selected, onClick = { onSelect(value) }, label = { Text(label(value)) })
        }
      }
    }
  }
}
