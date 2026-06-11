package com.example

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Configuration
import android.os.BatteryManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.webkit.JavascriptInterface
import android.widget.Toast

class AndroidBridge(
  private val context: Context,
  private val viewModel: WebToAppViewModel,
  private val vibrator: Vibrator,
  private val tts: TextToSpeech,
  private val onToggleImmersive: () -> Unit,
  private val onSetStatusBarColor: (String) -> Unit,
  private val onSetNavigationBarColor: (String) -> Unit,
  private val onGoBack: (() -> Unit)? = null,
  private val onGoForward: (() -> Unit)? = null,
  private val onReload: (() -> Unit)? = null,
  private val onLoadUrl: ((String) -> Unit)? = null,
  private val onCanGoBack: (() -> Boolean)? = null,
  private val onCanGoForward: (() -> Boolean)? = null,
  private val onSetFullScreen: ((Boolean) -> Unit)? = null,
  private val onShowStatusBar: ((Boolean) -> Unit)? = null
) {

  @JavascriptInterface
  fun goBack() {
    viewModel.addLog("BRIDGE", "INFO", "goBack()")
    (context as? Activity)?.runOnUiThread {
      onGoBack?.invoke()
    }
  }

  @JavascriptInterface
  fun goForward() {
    viewModel.addLog("BRIDGE", "INFO", "goForward()")
    (context as? Activity)?.runOnUiThread {
      onGoForward?.invoke()
    }
  }

  @JavascriptInterface
  fun reload() {
    viewModel.addLog("BRIDGE", "INFO", "reload()")
    (context as? Activity)?.runOnUiThread {
      onReload?.invoke()
    }
  }

  @JavascriptInterface
  fun loadUrl(url: String) {
    viewModel.addLog("BRIDGE", "INFO", "loadUrl(\"$url\")")
    (context as? Activity)?.runOnUiThread {
      onLoadUrl?.invoke(url)
    }
  }

  @JavascriptInterface
  fun canGoBack(): Boolean {
    val result = onCanGoBack?.invoke() ?: false
    viewModel.addLog("BRIDGE", "INFO", "canGoBack() -> return $result")
    return result
  }

  @JavascriptInterface
  fun canGoForward(): Boolean {
    val result = onCanGoForward?.invoke() ?: false
    viewModel.addLog("BRIDGE", "INFO", "canGoForward() -> return $result")
    return result
  }

  @JavascriptInterface
  fun showToast(message: String) {
    viewModel.addLog("BRIDGE", "INFO", "showToast(\"$message\")")
    (context as? Activity)?.runOnUiThread {
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
  }

  @JavascriptInterface
  fun vibrate(durationMs: Long) {
    viewModel.addLog("BRIDGE", "INFO", "vibrate(${durationMs}ms)")
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
      } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(durationMs)
      }
    } catch (e: Exception) {
      viewModel.addLog("BRIDGE", "ERROR", "Vibration failed: ${e.message}")
    }
  }

  @JavascriptInterface
  fun saveData(key: String, value: String) {
    val trimmedValue = if (value.length > 40) value.take(40) + "..." else value
    viewModel.addLog("BRIDGE", "INFO", "saveData(\"$key\", \"$trimmedValue\")")
    val prefs = context.getSharedPreferences("webtoapp_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString(key, value).apply()
  }

  @JavascriptInterface
  fun getData(key: String): String {
    val prefs = context.getSharedPreferences("webtoapp_prefs", Context.MODE_PRIVATE)
    val value = prefs.getString(key, "") ?: ""
    val trimmedValue = if (value.length > 40) value.take(40) + "..." else value
    viewModel.addLog("BRIDGE", "INFO", "getData(\"$key\") -> return \"$trimmedValue\"")
    return value
  }

  @JavascriptInterface
  fun speak(text: String) {
    viewModel.addLog("BRIDGE", "INFO", "speak(\"$text\")")
    (context as? Activity)?.runOnUiThread {
      try {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "webtoapp_tts")
      } catch (e: Exception) {
        viewModel.addLog("BRIDGE", "ERROR", "TTS speak failed: ${e.message}")
      }
    }
  }

  @JavascriptInterface
  fun copyToClipboard(text: String) {
    viewModel.addLog("BRIDGE", "INFO", "copyToClipboard(\"$text\")")
    (context as? Activity)?.runOnUiThread {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      val clip = ClipData.newPlainText("webtoapp", text)
      clipboard.setPrimaryClip(clip)
    }
  }

  @JavascriptInterface
  fun getFromClipboard(): String {
    var text = ""
    try {
      val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
      if (clipboard.hasPrimaryClip()) {
        val item = clipboard.primaryClip?.getItemAt(0)
        text = item?.text?.toString() ?: ""
      }
    } catch (e: Exception) {
      viewModel.addLog("BRIDGE", "ERROR", "Clipboard copy error: ${e.message}")
    }
    viewModel.addLog("BRIDGE", "INFO", "getFromClipboard() -> return \"$text\"")
    return text
  }

  @JavascriptInterface
  fun logConsole(level: String, message: String) {
    val logLevel = level.uppercase()
    viewModel.addLog("CONSOLE", logLevel, message)
  }

  @JavascriptInterface
  fun playSystemSound(soundId: Int) {
    viewModel.addLog("BRIDGE", "INFO", "playSystemSound($soundId)")
    (context as? Activity)?.runOnUiThread {
      val view = (context as? Activity)?.window?.decorView
      view?.playSoundEffect(soundId)
    }
  }

  @JavascriptInterface
  fun toggleFullScreen() {
    viewModel.addLog("BRIDGE", "INFO", "toggleFullScreen()")
    (context as? Activity)?.runOnUiThread {
      onToggleImmersive()
    }
  }

  @JavascriptInterface
  fun setFullScreen(enabled: Boolean) {
    viewModel.addLog("BRIDGE", "INFO", "setFullScreen($enabled)")
    (context as? Activity)?.runOnUiThread {
      onSetFullScreen?.invoke(enabled)
    }
  }

  @JavascriptInterface
  fun showStatusBar(visible: Boolean) {
    viewModel.addLog("BRIDGE", "INFO", "showStatusBar($visible)")
    (context as? Activity)?.runOnUiThread {
      onShowStatusBar?.invoke(visible)
    }
  }

  @JavascriptInterface
  fun setStatusBarColor(hexColor: String) {
    viewModel.addLog("BRIDGE", "INFO", "setStatusBarColor(\"$hexColor\")")
    (context as? Activity)?.runOnUiThread {
      onSetStatusBarColor(hexColor)
    }
  }

  @JavascriptInterface
  fun setNavigationBarColor(hexColor: String) {
    viewModel.addLog("BRIDGE", "INFO", "setNavigationBarColor(\"$hexColor\")")
    (context as? Activity)?.runOnUiThread {
      onSetNavigationBarColor(hexColor)
    }
  }

  @JavascriptInterface
  fun getDeviceInfo(): String {
    val batteryPct = getBatteryPercentage()
    val orientation = if (context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
      "Landscape"
    } else {
      "Portrait"
    }

    val json = """
      {
        "model": "${Build.MODEL}",
        "manufacturer": "${Build.MANUFACTURER}",
        "sdkInt": ${Build.VERSION.SDK_INT},
        "release": "${Build.VERSION.RELEASE}",
        "batteryLevel": $batteryPct,
        "orientation": "$orientation"
      }
    """.trimIndent()
    viewModel.addLog("BRIDGE", "INFO", "getDeviceInfo() triggered specs compile success")
    return json
  }

  private fun getBatteryPercentage(): Int {
    return try {
      val batteryStatusReceiver = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
      val level = batteryStatusReceiver?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
      val scale = batteryStatusReceiver?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
      if (level >= 0 && scale > 0) (level * 100 / scale) else 78
    } catch (e: Exception) {
      78
    }
  }
}
