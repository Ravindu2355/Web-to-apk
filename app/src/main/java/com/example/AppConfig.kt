package com.example

import android.content.Context
import org.json.JSONObject
import java.io.InputStream

object AppConfig {
    var appName: String = "WebToApp Sandbox"
    var startUrl: String = "https://localhost/"
    var applicationId: String = "com.aistudio.webtoapp.vshrt"
    var statusBarColor: String = "#090B11"
    var navigationBarColor: String = "#141724"
    var immersiveMode: Boolean = false
    var enableSandboxMode: Boolean = true

    fun load(context: Context) {
        try {
            val inputStream: InputStream = context.assets.open("app-config.json")
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            val jsonStr = String(buffer, Charsets.UTF_8)
            val json = JSONObject(jsonStr)

            appName = json.optString("appName", appName)
            startUrl = json.optString("startUrl", startUrl)
            applicationId = json.optString("applicationId", applicationId)
            statusBarColor = json.optString("statusBarColor", statusBarColor)
            navigationBarColor = json.optString("navigationBarColor", navigationBarColor)
            immersiveMode = json.optBoolean("immersiveMode", immersiveMode)
            enableSandboxMode = json.optBoolean("enableSandboxMode", enableSandboxMode)
        } catch (e: Exception) {
            e.printStackTrace()
            // Keep default values if loading fails
        }
    }
}
