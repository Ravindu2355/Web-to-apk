package com.example

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.remember
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

  private lateinit var tts: TextToSpeech

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    // Initialize the Android Standard Text To Speech Engine
    tts = TextToSpeech(this) { status ->
      if (status == TextToSpeech.SUCCESS) {
        tts.language = Locale.US
      }
    }

    AppConfig.load(this)
    enableEdgeToEdge()

    // Apply initial colors from configuration
    changeStatusBarColor(AppConfig.statusBarColor)
    changeNavigationBarColor(AppConfig.navigationBarColor)

    setContent {
      MyApplicationTheme {
        val viewModel: WebToAppViewModel = viewModel()
        val vibrator = remember { getSystemService(Context.VIBRATOR_SERVICE) as Vibrator }

        // Initialize state on first launch
        androidx.compose.runtime.LaunchedEffect(Unit) {
          if (AppConfig.immersiveMode) {
            viewModel.isFullscreen.value = true
            toggleImmersiveFullscreen(true)
          }
        }

        MainScreen(
          viewModel = viewModel,
          vibrator = vibrator,
          tts = tts,
          onToggleImmersive = {
            toggleImmersiveFullscreen(viewModel.isFullscreen.value)
          },
          onSetStatusBarColor = { hex ->
            changeStatusBarColor(hex)
          },
          onSetNavigationBarColor = { hex ->
            changeNavigationBarColor(hex)
          },
          onSetFullScreen = { enabled ->
            toggleImmersiveFullscreen(enabled)
          },
          onShowStatusBar = { visible ->
            showStatusBar(visible)
          },
          onShowNavigationBar = { visible ->
            showNavigationBar(visible)
          }
        )
      }
    }
  }

  private fun showStatusBar(visible: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    if (visible) {
      windowInsetsController.show(WindowInsetsCompat.Type.statusBars())
    } else {
      windowInsetsController.hide(WindowInsetsCompat.Type.statusBars())
    }
  }

  private fun showNavigationBar(visible: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    if (visible) {
      windowInsetsController.show(WindowInsetsCompat.Type.navigationBars())
    } else {
      windowInsetsController.hide(WindowInsetsCompat.Type.navigationBars())
      windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }
  }

  private fun toggleImmersiveFullscreen(hideBars: Boolean) {
    val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
    if (hideBars) {
      windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
      windowInsetsController.systemBarsBehavior =
        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
      windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
    }
  }

  private fun changeStatusBarColor(hex: String) {
    try {
      val colorInt = Color.parseColor(hex)
      window.statusBarColor = colorInt
    } catch (e: Exception) {
      // Ignore parsing exceptions
    }
  }

  private fun changeNavigationBarColor(hex: String) {
    try {
      val colorInt = Color.parseColor(hex)
      window.navigationBarColor = colorInt
    } catch (e: Exception) {
      // Ignore parsing exceptions
    }
  }

  override fun onDestroy() {
    if (::tts.isInitialized) {
      tts.stop()
      tts.shutdown()
    }
    super.onDestroy()
  }
}
