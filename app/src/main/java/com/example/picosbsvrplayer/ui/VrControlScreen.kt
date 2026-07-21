package com.example.picosbsvrplayer.ui

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
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

  val backgroundBrush =
    Brush.verticalGradient(
      colors =
        listOf(
          MaterialTheme.colorScheme.background,
          MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.72f),
          MaterialTheme.colorScheme.background,
        ),
    )

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    containerColor = Color.Transparent,
    contentColor = MaterialTheme.colorScheme.onBackground,
    contentWindowInsets = WindowInsets.safeDrawing,
  ) { padding ->
    Box(modifier = Modifier.fillMaxSize().background(backgroundBrush)) {
      Column(
        modifier =
          Modifier.padding(padding)
            .padding(horizontal = 20.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        Spacer(Modifier.height(2.dp))
        AppHeader()
        ConnectionCard(
          connected = status.connected,
          displayName = status.displayName,
          resolution = status.resolution,
          refreshRate = status.refreshRate,
        )
        SectionTabs(
          values = ControlSection.entries,
          selected = selectedSection,
          onSelect = { selectedSection = it },
        )

        Crossfade(
          targetState = selectedSection,
          animationSpec = tween(durationMillis = 160),
          label = "control section",
        ) { section ->
          when (section) {
            ControlSection.PLAYER ->
              PlayerSection(
                connected = status.connected,
                inputLayout = inputLayout,
                onInputLayoutChange = { inputLayout = it },
                projectionMode = projectionMode,
                onProjectionModeChange = { projectionMode = it },
                lensCorrection = lensCorrection,
                onLensCorrectionChange = { lensCorrection = it },
                scaleMode = scaleMode,
                onScaleModeChange = { scaleMode = it },
                viewSize = viewSize,
                onViewSizeChange = { viewSize = it },
                swapEyes = swapEyes,
                onSwapEyesChange = { swapEyes = it },
                mirrorHorizontal = mirrorHorizontal,
                onMirrorHorizontalChange = { mirrorHorizontal = it },
                eyeShiftPercent = eyeShiftPercent,
                onEyeShiftChange = { eyeShiftPercent = it },
                onPickVideo = { videoPicker.launch(arrayOf("video/*")) },
                onSeekBack = { controller.seekBy(-10_000) },
                onPlay = controller::play,
                onPause = controller::pause,
                onSeekForward = { controller.seekBy(10_000) },
              )

            ControlSection.TRACKPAD ->
              TrackpadSection(
                viewOffsetXPercent = viewOffsetXPercent,
                viewOffsetYPercent = viewOffsetYPercent,
                onOffsetChange = { x, y ->
                  viewOffsetXPercent = x
                  viewOffsetYPercent = y
                },
                onReset = {
                  viewOffsetXPercent = 0f
                  viewOffsetYPercent = 0f
                },
              )

            ControlSection.DISPLAY ->
              DisplaySection(
                connected = status.connected,
                rotation = rotation,
                onRotationChange = { rotation = it },
                onCalibration = controller::showCalibration,
              )

            ControlSection.YOUTUBE ->
              YoutubeSection(
                connected = status.connected,
                youtubeUrl = youtubeUrl,
                onYoutubeUrlChange = { youtubeUrl = it },
                onOpenEmbedded = {
                  message =
                    if (controller.showYouTube(youtubeUrl)) "YouTube 영상을 PICO에서 엽니다."
                    else "올바른 YouTube 영상 주소를 입력하세요."
                },
                onOpenOfficial = {
                  message =
                    if (controller.launchYouTubeAppOnExternalDisplay(youtubeUrl)) {
                      "YouTube 앱을 PICO 화면에서 실행했습니다."
                    } else {
                      "외부 화면에서 YouTube 앱을 실행하지 못했습니다."
                    }
                },
              )
          }
        }

        if (message.isNotEmpty()) StatusBanner(message)
        Spacer(Modifier.height(28.dp))
      }
    }
  }
}

@Composable
private fun AppHeader() {
  Text(
    text = "PICO VR",
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.primary,
    fontWeight = FontWeight.Bold,
  )
}

@Composable
private fun ConnectionCard(
  connected: Boolean,
  displayName: String,
  resolution: String,
  refreshRate: String,
) {
  val container =
    if (connected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.66f)
    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.72f)
  val content =
    if (connected) MaterialTheme.colorScheme.onPrimaryContainer
    else MaterialTheme.colorScheme.onErrorContainer
  val indicator = if (connected) Color(0xFF30D158) else MaterialTheme.colorScheme.error

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = container,
    contentColor = content,
    border = BorderStroke(1.dp, content.copy(alpha = 0.12f)),
    tonalElevation = 2.dp,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(Modifier.size(11.dp).clip(CircleShape).background(indicator))
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
          if (connected) "PICO 연결됨" else "PICO 연결 필요",
          style = MaterialTheme.typography.titleMedium,
        )
        Text(
          if (connected) displayName.ifBlank { "외부 VR 디스플레이" }
          else "USB-C로 연결하면 재생 기능이 활성화됩니다.",
          style = MaterialTheme.typography.bodySmall,
          color = content.copy(alpha = 0.78f),
        )
      }
      if (connected) {
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = content.copy(alpha = 0.1f),
          contentColor = content,
        ) {
          Column(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.End,
          ) {
            Text(resolution, style = MaterialTheme.typography.labelMedium)
            Text(refreshRate, style = MaterialTheme.typography.bodySmall, color = content.copy(alpha = 0.75f))
          }
        }
      }
    }
  }
}

@Composable
private fun PlayerSection(
  connected: Boolean,
  inputLayout: InputLayout,
  onInputLayoutChange: (InputLayout) -> Unit,
  projectionMode: ProjectionMode,
  onProjectionModeChange: (ProjectionMode) -> Unit,
  lensCorrection: LensCorrection,
  onLensCorrectionChange: (LensCorrection) -> Unit,
  scaleMode: ScaleMode,
  onScaleModeChange: (ScaleMode) -> Unit,
  viewSize: ViewSize,
  onViewSizeChange: (ViewSize) -> Unit,
  swapEyes: Boolean,
  onSwapEyesChange: (Boolean) -> Unit,
  mirrorHorizontal: Boolean,
  onMirrorHorizontalChange: (Boolean) -> Unit,
  eyeShiftPercent: Float,
  onEyeShiftChange: (Float) -> Unit,
  onPickVideo: () -> Unit,
  onSeekBack: () -> Unit,
  onPlay: () -> Unit,
  onPause: () -> Unit,
  onSeekForward: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader("VR 재생", "재생 중인 영상을 조작하거나 새 영상을 준비하세요.")

    GlassSurface {
      SettingGroupTitle("재생 제어", "현재 재생 중인 로컬 영상을 바로 조작합니다.")
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
      ) {
        PlaybackButton("−10초", onSeekBack, Modifier.weight(1f))
        PlaybackButton("재생", onPlay, Modifier.weight(1f))
        PlaybackButton("정지", onPause, Modifier.weight(1f))
        PlaybackButton("+10초", onSeekForward, Modifier.weight(1f))
      }
    }

    SectionHeader("재생 전 설정", "영상 형식을 맞춘 뒤 로컬 파일을 선택하세요.")

    GlassSurface {
      SettingGroupTitle("영상 형식", "원본 영상의 눈 배치와 투영 방식을 지정합니다.")
      ChoiceLabel("눈 배치")
      ChoiceGrid(InputLayout.entries, inputLayout, { it.label }, onInputLayoutChange)
      ChoiceLabel("투영 방식")
      ChoiceGrid(ProjectionMode.entries, projectionMode, { it.label }, onProjectionModeChange)
      InfoNote("VR180은 180° 원본에, 평면 SBS 보정은 일반 직사각형 3D 영상에 사용합니다.")

      if (projectionMode == ProjectionMode.FLAT_SBS_CORRECTED) {
        ChoiceLabel("렌즈 보정 강도")
        ChoiceGrid(LensCorrection.entries, lensCorrection, { it.label }, onLensCorrectionChange)
      }

      ChoiceLabel("화면 맞춤")
      ChoiceGrid(ScaleMode.entries, scaleMode, { it.label }, onScaleModeChange)
      ChoiceLabel("시야 크기")
      ChoiceGrid(ViewSize.entries, viewSize, { it.label }, onViewSizeChange)
    }

    GlassSurface {
      SettingGroupTitle("눈과 화면", "필요할 때만 조정하고 기본값으로 먼저 확인하세요.")
      ToggleSetting(
        title = "좌우 눈 바꾸기",
        supporting = "입체 초점이 반대로 느껴지는 상하 3D 영상에 사용합니다.",
        checked = swapEyes,
        onCheckedChange = onSwapEyesChange,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
      ToggleSetting(
        title = "수평 반전 보정",
        supporting = "글자와 영상이 거울처럼 보일 때 켭니다.",
        checked = mirrorHorizontal,
        onCheckedChange = onMirrorHorizontalChange,
      )
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.65f))
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text("입체 초점", style = MaterialTheme.typography.titleSmall)
          Text(
            "가까운 물체의 좌우 시차를 미세 조절합니다.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        ValuePill("${if (eyeShiftPercent > 0) "+" else ""}${eyeShiftPercent.toInt()}%")
      }
      Slider(
        value = eyeShiftPercent,
        onValueChange = onEyeShiftChange,
        valueRange = -3f..3f,
        steps = 5,
      )
      InfoNote("0%에서 시작해 한 단계씩 조절하세요.")
    }

    FluidPrimaryButton(
      text = "로컬 영상 선택",
      onClick = onPickVideo,
      enabled = connected,
    )
  }
}

@Composable
private fun TrackpadSection(
  viewOffsetXPercent: Float,
  viewOffsetYPercent: Float,
  onOffsetChange: (Float, Float) -> Unit,
  onReset: () -> Unit,
) {
  var isDragging by remember { mutableStateOf(false) }
  val padScale by
    animateFloatAsState(
      targetValue = if (isDragging) 0.985f else 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioNoBouncy,
          stiffness = Spring.StiffnessMediumLow,
        ),
      label = "trackpad press",
    )
  val padBackground = MaterialTheme.colorScheme.surfaceContainerHighest
  val padBorder = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)
  val padGrid = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.28f)
  val padIndicator = MaterialTheme.colorScheme.primary

  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader("시점 조정", "손가락을 움직인 만큼 PICO 화면의 시점이 따라옵니다.")
    GlassSurface {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Text("직접 조작", style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Surface(
          shape = CircleShape,
          color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
          contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
          Text("1 : 1", modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelMedium)
        }
      }

      Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Canvas(
          modifier =
            Modifier.fillMaxWidth()
              .widthIn(max = 310.dp)
              .aspectRatio(1f)
              .graphicsLayer {
                scaleX = padScale
                scaleY = padScale
              }
              .semantics { contentDescription = "원형 VR 시점 트랙패드" }
              .pointerInput(Unit) {
                detectDragGestures(
                  onDragStart = { isDragging = true },
                  onDragEnd = { isDragging = false },
                  onDragCancel = { isDragging = false },
                ) { change, dragAmount ->
                  change.consume()
                  if (size.width > 0 && size.height > 0) {
                    val x = (viewOffsetXPercent + dragAmount.x / size.width * 30f).coerceIn(-15f, 15f)
                    val y = (viewOffsetYPercent + dragAmount.y / size.height * 30f).coerceIn(-15f, 15f)
                    onOffsetChange(x, y)
                  }
                }
              },
        ) {
          val center = Offset(size.width / 2f, size.height / 2f)
          val radius = size.minDimension / 2f - 3f
          val indicatorRange = radius - 30f
          drawCircle(color = padBackground, radius = radius, center = center)
          drawCircle(color = padBorder, radius = radius, center = center, style = Stroke(width = 2.5f))
          drawCircle(color = padGrid.copy(alpha = 0.45f), radius = radius * 0.55f, center = center, style = Stroke(width = 1.5f))
          drawLine(
            color = padGrid,
            start = Offset(center.x, center.y - radius),
            end = Offset(center.x, center.y + radius),
            strokeWidth = 1.5f,
          )
          drawLine(
            color = padGrid,
            start = Offset(center.x - radius, center.y),
            end = Offset(center.x + radius, center.y),
            strokeWidth = 1.5f,
          )
          val indicatorCenter =
            Offset(
              x = center.x + viewOffsetXPercent / 15f * indicatorRange,
              y = center.y + viewOffsetYPercent / 15f * indicatorRange,
            )
          drawCircle(color = padIndicator.copy(alpha = 0.18f), radius = 31f, center = indicatorCenter)
          drawCircle(color = padIndicator, radius = 18f, center = indicatorCenter)
          drawCircle(color = Color.White.copy(alpha = 0.72f), radius = 5f, center = indicatorCenter)
        }
      }

      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        MetricPill("좌우", viewOffsetXPercent, Modifier.weight(1f))
        MetricPill("상하", viewOffsetYPercent, Modifier.weight(1f))
      }
      OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
        Text("시점 중앙으로")
      }
      InfoNote("두 눈의 시점을 함께 이동하며 입체 초점 설정에는 영향을 주지 않습니다.")
    }
  }
}

@Composable
private fun DisplaySection(
  connected: Boolean,
  rotation: OutputRotation,
  onRotationChange: (OutputRotation) -> Unit,
  onCalibration: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader("화면 설정", "PICO 출력 방향을 맞추고 좌우 눈을 확인합니다.")
    GlassSurface {
      SettingGroupTitle("출력 방향", "영상이 옆으로 보이면 회전값을 선택하세요.")
      ChoiceGrid(OutputRotation.entries, rotation, { it.label }, onRotationChange)
      InfoNote("자동 설정이 맞지 않을 때만 90°·180°·270°를 직접 선택하세요.")
    }
    FluidPrimaryButton(
      text = "L / R 전체화면 테스트",
      onClick = onCalibration,
      enabled = connected,
    )
    InfoNote("왼쪽 눈에는 L, 오른쪽 눈에는 R이 보이면 눈 배치가 정상입니다.")
  }
}

@Composable
private fun YoutubeSection(
  connected: Boolean,
  youtubeUrl: String,
  onYoutubeUrlChange: (String) -> Unit,
  onOpenEmbedded: () -> Unit,
  onOpenOfficial: () -> Unit,
) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    SectionHeader("YouTube VR", "공식 재생 방식으로 PICO 화면에서 영상을 엽니다.")
    GlassSurface {
      SettingGroupTitle("영상 주소", "공유 링크와 일반 YouTube 주소를 모두 사용할 수 있습니다.")
      OutlinedTextField(
        value = youtubeUrl,
        onValueChange = onYoutubeUrlChange,
        label = { Text("YouTube 영상 주소") },
        placeholder = { Text("https://youtu.be/…") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.large,
      )
      FluidPrimaryButton(
        text = "PICO 내장 플레이어에서 열기",
        onClick = onOpenEmbedded,
        enabled = connected && youtubeUrl.isNotBlank(),
      )
      InfoNote("공식 YouTube 웹 플레이어를 사용하며 앱은 영상을 다운로드하거나 변환하지 않습니다.")
    }

    GlassSurface {
      SettingGroupTitle("공식 앱", "지원되는 영상은 YouTube의 VR 기능을 사용할 수 있습니다.")
      OutlinedButton(
        onClick = onOpenOfficial,
        enabled = connected && youtubeUrl.isNotBlank(),
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
      ) { Text("공식 YouTube 앱을 PICO에서 열기") }
      InfoNote("SBS 영상은 내장 플레이어에서 볼 수 있으며 180°·360° 시점 제어는 공식 앱이 지원할 때만 사용할 수 있습니다.")
    }
  }
}

private enum class ControlSection(val label: String) {
  PLAYER("재생"),
  TRACKPAD("시점"),
  DISPLAY("화면"),
  YOUTUBE("YouTube"),
}

@Composable
private fun SectionTabs(
  values: List<ControlSection>,
  selected: ControlSection,
  onSelect: (ControlSection) -> Unit,
) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.88f),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
  ) {
    Row(modifier = Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
      values.forEach { value ->
        val isSelected = value == selected
        val background by
          animateColorAsState(
            targetValue =
              if (isSelected) MaterialTheme.colorScheme.primary
              else Color.Transparent,
            animationSpec = tween(140),
            label = "tab background",
          )
        val content by
          animateColorAsState(
            targetValue =
              if (isSelected) MaterialTheme.colorScheme.onPrimary
              else MaterialTheme.colorScheme.onSurfaceVariant,
            animationSpec = tween(140),
            label = "tab content",
          )
        Box(
          modifier =
            Modifier.weight(1f)
              .height(42.dp)
              .clip(RoundedCornerShape(14.dp))
              .background(background)
              .clickable { onSelect(value) },
          contentAlignment = Alignment.Center,
        ) {
          Text(value.label, style = MaterialTheme.typography.labelLarge, color = content)
        }
      }
    }
  }
}

@Composable
private fun SectionHeader(title: String, description: String) {
  Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
    Text(title, style = MaterialTheme.typography.titleLarge)
    Text(
      description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun GlassSurface(content: @Composable ColumnScope.() -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLarge,
    color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.9f),
    contentColor = MaterialTheme.colorScheme.onSurface,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.62f)),
    tonalElevation = 1.dp,
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(18.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      content = content,
    )
  }
}

@Composable
private fun SettingGroupTitle(title: String, supporting: String) {
  Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Text(
      supporting,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun ChoiceLabel(text: String) {
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

@Composable
private fun <T> ChoiceGrid(
  values: List<T>,
  selected: T,
  label: (T) -> String,
  onSelect: (T) -> Unit,
) {
  val columnCount = if (values.size == 2) 2 else 3
  Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
    values.chunked(columnCount).forEach { rowValues ->
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        rowValues.forEach { value ->
          FilterChip(
            selected = value == selected,
            onClick = { onSelect(value) },
            label = { Text(label(value), maxLines = 1) },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(13.dp),
            colors =
              FilterChipDefaults.filterChipColors(
                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
              ),
          )
        }
        repeat(columnCount - rowValues.size) { Spacer(Modifier.weight(1f)) }
      }
    }
  }
}

@Composable
private fun ToggleSetting(
  title: String,
  supporting: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier = Modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleSmall)
      Text(
        supporting,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
private fun FluidPrimaryButton(
  text: String,
  onClick: () -> Unit,
  enabled: Boolean,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val pressed by interactionSource.collectIsPressedAsState()
  val scale by
    animateFloatAsState(
      targetValue = if (pressed && enabled) 0.975f else 1f,
      animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
      label = "button press",
    )
  Button(
    onClick = onClick,
    enabled = enabled,
    modifier =
      Modifier.fillMaxWidth()
        .heightIn(min = 54.dp)
        .graphicsLayer {
          scaleX = scale
          scaleY = scale
        },
    shape = MaterialTheme.shapes.large,
    interactionSource = interactionSource,
    colors =
      ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary,
      ),
  ) {
    Text(text, style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
private fun PlaybackButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
  OutlinedButton(
    onClick = onClick,
    modifier = modifier.heightIn(min = 46.dp),
    shape = RoundedCornerShape(14.dp),
    contentPadding = ButtonDefaults.ContentPadding,
  ) {
    Text(text, maxLines = 1, style = MaterialTheme.typography.labelMedium)
  }
}

@Composable
private fun ValuePill(value: String) {
  Surface(
    shape = CircleShape,
    color = MaterialTheme.colorScheme.primaryContainer,
    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
  ) {
    Text(value, modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp), style = MaterialTheme.typography.labelLarge)
  }
}

@Composable
private fun MetricPill(label: String, value: Float, modifier: Modifier = Modifier) {
  Surface(
    modifier = modifier,
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHighest,
  ) {
    Row(
      modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      Spacer(Modifier.weight(1f))
      Text(
        "${if (value > 0) "+" else ""}${value.toInt()}%",
        style = MaterialTheme.typography.labelLarge,
      )
    }
  }
}

@Composable
private fun InfoNote(text: String) {
  Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Top) {
    Box(
      Modifier.padding(top = 7.dp)
        .size(5.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
    )
    Text(
      text,
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
  }
}

@Composable
private fun StatusBanner(message: String) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.large,
    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.82f),
    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.12f)),
  ) {
    Text(message, modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp), style = MaterialTheme.typography.bodyMedium)
  }
}
