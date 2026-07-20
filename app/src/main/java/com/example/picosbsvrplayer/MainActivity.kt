package com.example.picosbsvrplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.picosbsvrplayer.theme.MyApplicationTheme
import com.example.picosbsvrplayer.ui.VrControlScreen
import com.example.picosbsvrplayer.vr.VrOutputController

class MainActivity : ComponentActivity() {
  private lateinit var outputController: VrOutputController

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    outputController = VrOutputController(this)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme { VrControlScreen(outputController) }
    }
  }

  override fun onDestroy() {
    outputController.release()
    super.onDestroy()
  }

}
