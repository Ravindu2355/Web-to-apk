package com.example

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
  val timestamp: String,
  val tag: String, // "BRIDGE" or "CONSOLE"
  val level: String, // "INFO", "DEBUG", "ERROR"
  val message: String
)

data class HtmlTemplate(
  val id: String,
  val name: String,
  val description: String,
  val html: String,
  val css: String,
  val js: String
)

class WebToAppViewModel : ViewModel() {

  // Current HTML/CSS/JS content in active editor
  private val _htmlCode = MutableStateFlow(TEMPLATE_DASHBOARD_HTML)
  val htmlCode: StateFlow<String> = _htmlCode.asStateFlow()

  private val _cssCode = MutableStateFlow(TEMPLATE_DASHBOARD_CSS)
  val cssCode: StateFlow<String> = _cssCode.asStateFlow()

  private val _jsCode = MutableStateFlow(TEMPLATE_DASHBOARD_JS)
  val jsCode: StateFlow<String> = _jsCode.asStateFlow()

  // Tracking current active tabs
  // Tabs: 0: WebView Runtime, 1: Sandbox Editor, 2: Console Log, 3: Bundle Guide
  private val _activeTab = MutableStateFlow(0)
  val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

  // HTML Editor Subtab: 0: HTML, 1: CSS, 2: JS
  private val _editorSubTab = MutableStateFlow(0)
  val editorSubTab: StateFlow<Int> = _editorSubTab.asStateFlow()

  // Track Immersive Fullscreen State
  val isFullscreen = mutableStateOf(false)

  // Track Sandbox Load Trigger (incremented to force reload)
  private val _sandboxReloadFlag = MutableStateFlow(0L)
  val sandboxReloadFlag: StateFlow<Long> = _sandboxReloadFlag.asStateFlow()

  // Live Console & Bridge logs list
  val logs = mutableStateListOf<LogEntry>()

  // Available Presets
  val templates = listOf(
    HtmlTemplate(
      id = "dashboard",
      name = "Android Controller Dash",
      description = "Full control panel with haptics, TTS voice, persistent Storage engine, visual light customizer, status bar recoloring, and battery meters.",
      html = TEMPLATE_DASHBOARD_HTML,
      css = TEMPLATE_DASHBOARD_CSS,
      js = TEMPLATE_DASHBOARD_JS
    ),
    HtmlTemplate(
      id = "soundboard",
      name = "Haptic Popper & Soundboard",
      description = "Stress relief bubble board showing tactile haptic pulses of variable wave lengths and multi-oscillator system triggers.",
      html = TEMPLATE_SOUNDBOARD_HTML,
      css = TEMPLATE_SOUNDBOARD_CSS,
      js = TEMPLATE_SOUNDBOARD_JS
    ),
    HtmlTemplate(
      id = "canvas",
      name = "Tactile Paint Sandbox",
      description = "Adaptive custom HTML5 Canvas drawing board. Triggers vibrating drawing drag friction, local background saves, and image copy exports.",
      html = TEMPLATE_CANVAS_HTML,
      css = TEMPLATE_CANVAS_CSS,
      js = TEMPLATE_CANVAS_JS
    )
  )

  private val _selectedTemplateIndex = MutableStateFlow(0)
  val selectedTemplateIndex: StateFlow<Int> = _selectedTemplateIndex.asStateFlow()

  init {
    addLog("SYSTEM", "INFO", "WebToApp Sandbox Engine initialized. Ready to execute static sites!")
  }

  fun updateHtml(code: String) {
    _htmlCode.value = code
  }

  fun updateCss(code: String) {
    _cssCode.value = code
  }

  fun updateJs(code: String) {
    _jsCode.value = code
  }

  fun setActiveTab(index: Int) {
    _activeTab.value = index
  }

  fun setEditorSubTab(index: Int) {
    _editorSubTab.value = index
  }

  fun selectTemplate(index: Int) {
    if (index in templates.indices) {
      _selectedTemplateIndex.value = index
      val t = templates[index]
      _htmlCode.value = t.html
      _cssCode.value = t.css
      _jsCode.value = t.js
      addLog("SYSTEM", "INFO", "Loaded preset template: ${t.name}")
      triggerReload()
    }
  }

  fun triggerReload() {
    _sandboxReloadFlag.value = System.currentTimeMillis()
    addLog("SYSTEM", "INFO", "Reloading WebView runtime container...")
  }

  fun addLog(tag: String, level: String, message: String) {
    val time = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
    // Bound the logs count to prevent runaway memory
    if (logs.size > 200) {
      logs.removeAt(0)
    }
    logs.add(LogEntry(time, tag, level, message))
  }

  fun clearLogs() {
    logs.clear()
    addLog("SYSTEM", "INFO", "Console and Bridge logs cleared.")
  }

  // Combined web code bundled as a standalone string incorporating CSS and JS scripts
  fun getMergedHtml(): String {
    return """
      <!DOCTYPE html>
      <html>
      <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no, viewport-fit=cover">
          <style>
              /* Core responsive reset */
              * {
                  box-sizing: border-box;
                  -webkit-tap-highlight-color: transparent;
              }
              body {
                  margin: 0;
                  padding: 0;
                  font-family: system-ui, -apple-system, sans-serif;
                  background-color: #12141c;
                  color: #f1f3f9;
                  overflow-x: hidden;
              }
              ${_cssCode.value}
          </style>
      </head>
      <body>
          ${_htmlCode.value}
          
          <script>
              // Intercept window console logs securely and feed back to App console logs
              (function() {
                  const originalLog = console.log;
                  const originalError = console.error;
                  const originalWarn = console.warn;
                  
                  console.log = function(...args) {
                      const msg = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
                      originalLog.apply(console, args);
                      if (window.AndroidApp && window.AndroidApp.logConsole) {
                          window.AndroidApp.logConsole("INFO", msg);
                      }
                  };
                  
                  console.error = function(...args) {
                      const msg = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
                      originalError.apply(console, args);
                      if (window.AndroidApp && window.AndroidApp.logConsole) {
                          window.AndroidApp.logConsole("ERROR", msg);
                      }
                  };
                  
                  console.warn = function(...args) {
                      const msg = args.map(a => typeof a === 'object' ? JSON.stringify(a) : String(a)).join(' ');
                      originalWarn.apply(console, args);
                      if (window.AndroidApp && window.AndroidApp.logConsole) {
                          window.AndroidApp.logConsole("WARN", msg);
                      }
                  };
              })();
              
              // Helper wrapper in case user calls either window.AndroidApp or window.AndroidBridge
              window.AndroidBridge = window.AndroidApp;
              
              ${_jsCode.value}
          </script>
      </body>
      </html>
    """.trimIndent()
  }

  companion object {
    // ==========================================
    // TEMPLATE 1: DEV CONTROLLER
    // ==========================================
    private val TEMPLATE_DASHBOARD_HTML = """
      <div class="app-wrap">
          <header class="app-header">
              <div class="hdr-logo">⚡ WEBTOAPP</div>
              <div class="hdr-tag">BRIDGE CONTROLLER v1.0</div>
          </header>
          
          <div class="main-container">
              <!-- Live Device Status Dashboard Card -->
              <div class="panel-card hero-gradient">
                  <h3><i class="icon">📱</i> Live Device Profile</h3>
                  <div class="status-grid" id="profile-container">
                      <div class="stat-box">
                          <label>Battery</label>
                          <span id="stat-battery" class="accent-text">Checking...</span>
                      </div>
                      <div class="stat-box">
                          <label>Device Model</label>
                          <span id="stat-model">Checking...</span>
                      </div>
                      <div class="stat-box">
                          <label>Android Version</label>
                          <span id="stat-version">Checking...</span>
                      </div>
                      <div class="stat-box">
                          <label>Orientation</label>
                          <span id="stat-orientation" class="accent-text">Portrait</span>
                      </div>
                  </div>
                  <button class="btn btn-secondary full-w" id="btn-refresh-profile" style="margin-top: 14px;">
                      Refresh Raw Device Spec
                  </button>
              </div>

              <!-- Vibration Tactile Engine Card -->
              <div class="panel-card">
                  <h3><i class="icon">📳</i> Haptic Tactile Engine</h3>
                  <p class="description">Test local physical tactile motor vibration signals directly from Javascript.</p>
                  
                  <div class="control-row">
                      <label for="vib-range">Pulse Length: <span id="val-vib">80</span>ms</label>
                      <input type="range" id="vib-range" min="10" max="600" value="80" class="styled-slider">
                  </div>
                  
                  <div class="btn-group-row">
                      <button class="btn btn-primary" id="btn-vibrate">Trigger Pulse</button>
                      <button class="btn btn-outline" id="btn-vibrate-pattern">Zapper Pattern</button>
                  </div>
              </div>

              <!-- Voice Synthesis Engine Card -->
              <div class="panel-card">
                  <h3><i class="icon">🗣️</i> TTS Speech Synthesizer</h3>
                  <p class="description">Type below to prompt Android's native Text-to-Speech synthesizer directly.</p>
                  
                  <div class="text-input-wrapper">
                      <input type="text" id="tts-input" value="Welcome to Web To App. All native Android resources are now at your control." placeholder="Say something...">
                  </div>
                  <button class="btn btn-primary full-w" id="btn-speak">Speak Text</button>
              </div>

              <!-- Storage Key-Value Card -->
              <div class="panel-card">
                  <h3><i class="icon">💾</i> Sandbox Storage Engine</h3>
                  <p class="description">Persist variables inside native storage. They will survive app restructures and reloads!</p>
                  
                  <div class="form-grid">
                      <div class="field-item">
                          <label>Key Name</label>
                          <input type="text" id="store-key" value="user_name" class="mini-input">
                      </div>
                      <div class="field-item">
                          <label>Value String</label>
                          <input type="text" id="store-value" value="Stardust Traveler" class="mini-input">
                      </div>
                  </div>
                  
                  <div class="action-grid" style="margin-top: 10px;">
                      <button class="btn btn-primary" id="btn-save-data">Save Key</button>
                      <button class="btn btn-secondary" id="btn-load-data">Load Key</button>
                  </div>
                  <div class="output-box" id="storage-output">Saved variables appear here.</div>
              </div>

              <!-- Interface Customization Card -->
              <div class="panel-card">
                  <h3><i class="icon">🎨</i> System UI Palette</h3>
                  <p class="description">Command high-level Android display attributes, system bar tints, and full-screen states.</p>
                  
                  <div class="color-picker-grid">
                      <div class="color-preset" style="background-color: #12141c;" data-hex="#12141c" title="Space Slate"></div>
                      <div class="color-preset" style="background-color: #1a0f30;" data-hex="#1a0f30" title="Celestial Purple"></div>
                      <div class="color-preset" style="background-color: #0b212f;" data-hex="#0b212f" title="Ocean Abyss"></div>
                      <div class="color-preset" style="background-color: #3b0d11;" data-hex="#3b0d11" title="Mars Sunrise"></div>
                      <div class="color-preset" style="background-color: #1a3a2d;" data-hex="#1a3a2d" title="Deep Forest"></div>
                  </div>
                  
                  <div class="btn-group-row" style="margin-top: 14px;">
                      <button class="btn btn-outline" id="btn-toggle-immersive">Toggle Fullscreen View</button>
                      <button class="btn id-sound-click" id="btn-play-sound">Play Beep</button>
                  </div>
              </div>

              <!-- Clipboard Engine Card -->
              <div class="panel-card">
                  <h3><i class="icon">📋</i> Clipboard Utility</h3>
                  <p class="description">Copy and fetch strings securely from the native target clipboard.</p>
                  <div class="text-input-wrapper">
                      <input type="text" id="clipboard-input" value="Jetpack Compose + HTML5 = Love" placeholder="Clipboard content...">
                  </div>
                  <div class="action-grid" style="margin-top: 10px;">
                      <button class="btn btn-secondary" id="btn-copy">Copy Content</button>
                      <button class="btn btn-outline" id="btn-paste">Fetch Paste</button>
                  </div>
              </div>
          </div>
          
          <footer style="text-align: center; color: #64748b; padding: 20px 0; font-size: 11px;">
              WebToApp Container Engine • Connected to Native Android Client
          </footer>
      </div>
    """.trimIndent()

    private val TEMPLATE_DASHBOARD_CSS = """
      /* Elegant dark cyberpunk styling */
      :root {
          --primary-color: #a855f7;
          --primary-glow: rgba(168, 85, 247, 0.35);
          --secondary-color: #2563eb;
          --bg-color: #0d0e12;
          --card-bg: #161a26;
          --text-color: #e2e8f0;
          --accent-cyan: #06b6d4;
      }
      
      body {
          background-color: var(--bg-color);
          color: var(--text-color);
          margin: 0;
          padding: 0;
          font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Helvetica, Arial, sans-serif;
      }
      
      .app-wrap {
          max-width: 600px;
          margin: 0 auto;
          padding: 16px;
      }
      
      .app-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          padding: 16px 0;
          border-bottom: 1px solid #1e293b;
          margin-bottom: 20px;
      }
      
      .hdr-logo {
          font-size: 18px;
          font-weight: 800;
          letter-spacing: 1px;
          color: transparent;
          background: linear-gradient(135deg, #a855f7, #6366f1, #06b6d4);
          -webkit-background-clip: text;
      }
      
      .hdr-tag {
          font-size: 10px;
          background: rgba(168, 85, 247, 0.15);
          color: #d8b4fe;
          padding: 3px 8px;
          border-radius: 99px;
          font-family: monospace;
          border: 1px solid rgba(168, 85, 247, 0.3);
      }
      
      .main-container {
          display: flex;
          flex-direction: column;
          gap: 16px;
      }
      
      .panel-card {
          background-color: var(--card-bg);
          border-radius: 16px;
          padding: 18px;
          border: 1px solid #232a3d;
          box-shadow: 0 4px 12px rgba(0,0,0,0.25);
          position: relative;
          overflow: hidden;
      }
      
      .panel-card::before {
          content: '';
          position: absolute;
          top: 0;
          left: 0;
          width: 100%;
          height: 3px;
          background: linear-gradient(90deg, transparent, #232e4d, transparent);
      }
      
      .hero-gradient {
          background: linear-gradient(155deg, #181d2f 0%, #111524 100%);
          border: 1px solid rgba(168, 85, 247, 0.18);
      }
      
      .hero-gradient::before {
          background: linear-gradient(90deg, #a855f7 0%, #06b6d4 100%);
      }
      
      h3 {
          margin-top: 0;
          margin-bottom: 8px;
          font-size: 15px;
          letter-spacing: 0.5px;
          display: flex;
          align-items: center;
          gap: 8px;
          color: #f8fafc;
      }
      
      h3 .icon {
          font-style: normal;
          font-size: 16px;
      }
      
      .description {
          font-size: 12px;
          color: #94a3b8;
          margin-bottom: 14px;
          margin-top: 0;
          line-height: 1.5;
      }
      
      .status-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 10px;
          margin-top: 12px;
      }
      
      .stat-box {
          background: #0f121d;
          border-radius: 10px;
          padding: 10px;
          border: 1px solid #1a2233;
          display: flex;
          flex-direction: column;
          gap: 4px;
      }
      
      .stat-box label {
          font-size: 9px;
          color: #64748b;
          text-transform: uppercase;
          font-family: monospace;
      }
      
      .stat-box span {
          font-size: 13px;
          font-weight: 600;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
      }
      
      .accent-text {
          color: var(--accent-cyan);
          text-shadow: 0 0 8px rgba(6, 182, 212, 0.4);
      }
      
      .control-row {
          margin-bottom: 14px;
          display: flex;
          flex-direction: column;
          gap: 6px;
      }
      
      .control-row label {
          font-size: 12px;
          color: #94a3b8;
          display: flex;
          justify-content: space-between;
      }
      
      .styled-slider {
          -webkit-appearance: none;
          width: 100%;
          height: 6px;
          border-radius: 99px;
          background: #1e293b;
          outline: none;
      }
      
      .styled-slider::-webkit-slider-thumb {
          -webkit-appearance: none;
          width: 18px;
          height: 18px;
          border-radius: 50%;
          background: var(--primary-color);
          box-shadow: 0 0 10px var(--primary-glow);
          cursor: pointer;
      }
      
      .btn-group-row {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 10px;
      }
      
      .btn {
          font-family: inherit;
          font-size: 13px;
          font-weight: 600;
          padding: 10px 16px;
          border-radius: 10px;
          border: none;
          cursor: pointer;
          transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
          outline: none;
          display: inline-flex;
          align-items: center;
          justify-content: center;
          min-height: 40px;
          user-select: none;
      }
      
      .btn-primary {
          background-color: var(--primary-color);
          color: #ffffff;
          box-shadow: 0 4px 10px var(--primary-glow);
      }
      
      .btn-primary:active {
          transform: scale(0.96);
          background-color: #9333ea;
      }
      
      .btn-secondary {
          background-color: var(--secondary-color);
          color: #ffffff;
          box-shadow: 0 4px 10px rgba(37, 99, 235, 0.25);
      }
      
      .btn-secondary:active {
          transform: scale(0.96);
          background-color: #1d4ed8;
      }
      
      .btn-outline {
          background: transparent;
          border: 1.5px solid #334155;
          color: #cbd5e1;
      }
      
      .btn-outline:active {
          background: rgba(255,255,255,0.05);
          transform: scale(0.96);
      }
      
      .full-w {
          width: 100%;
      }
      
      .text-input-wrapper {
          background: #0f121d;
          border: 1px solid #1e293b;
          border-radius: 10px;
          padding: 2px;
          display: flex;
          align-items: center;
          transition: border-color 0.2s;
      }
      
      .text-input-wrapper:focus-within {
          border-color: var(--primary-color);
      }
      
      .text-input-wrapper input {
          width: 100%;
          background: transparent;
          outline: none;
          border: none;
          padding: 8px 12px;
          color: #ffffff;
          font-size: 13px;
          font-family: inherit;
      }
      
      .form-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 10px;
      }
      
      .field-item {
          display: flex;
          flex-direction: column;
          gap: 5px;
      }
      
      .field-item label {
          font-size: 10px;
          color: #64748b;
          text-transform: uppercase;
      }
      
      .mini-input {
          background: #0f121d;
          border: 1px solid #1e293b;
          border-radius: 8px;
          padding: 8px 10px;
          font-size: 13px;
          color: #ffffff;
          outline: none;
          transition: border-color 0.2s;
      }
      .mini-input:focus {
          border-color: var(--primary-color);
      }
      
      .action-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 10px;
      }
      
      .output-box {
          background: #0d0f17;
          border-radius: 8px;
          border: 1px dashed #222a44;
          padding: 10px;
          margin-top: 10px;
          font-size: 11px;
          color: #a855f7;
          font-family: monospace;
          min-height: 20px;
          word-break: break-all;
      }
      
      .color-picker-grid {
          display: flex;
          gap: 10px;
          justify-content: space-around;
      }
      
      .color-preset {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          border: 2.5px solid #2d3748;
          cursor: pointer;
          transition: transform 0.2s, border-color 0.2s;
      }
      
      .color-preset:hover, .color-preset.active {
          transform: scale(1.15);
          border-color: #ffffff;
          box-shadow: 0 0 10px rgba(255,255,255,0.3);
      }
    """.trimIndent()

    private val TEMPLATE_DASHBOARD_JS = """
      document.addEventListener("DOMContentLoaded", () => {
          console.log("Universal controller JS initialized successfully.");
          
          const labelVib = document.getElementById("val-vib");
          const rangeVib = document.getElementById("vib-range");
          const btnVib = document.getElementById("btn-vibrate");
          const btnVibPattern = document.getElementById("btn-vibrate-pattern");
          const btnSpeak = document.getElementById("btn-speak");
          const ttsInput = document.getElementById("tts-input");
          
          const optKey = document.getElementById("store-key");
          const optValue = document.getElementById("store-value");
          const btnSave = document.getElementById("btn-save-data");
          const btnLoad = document.getElementById("btn-load-data");
          const storageOutput = document.getElementById("storage-output");
          
          const clipVal = document.getElementById("clipboard-input");
          const btnCopy = document.getElementById("btn-copy");
          const btnPaste = document.getElementById("btn-paste");
          
          const btnRefreshSpec = document.getElementById("btn-refresh-profile");
          const btnToggleFullScreen = document.getElementById("btn-toggle-immersive");
          const btnPlaySound = document.getElementById("btn-play-sound");
          
          // Connect haptic vibration
          rangeVib.addEventListener("input", (e) => {
              labelVib.innerText = e.target.value;
          });
          
          btnVib.addEventListener("click", () => {
              const ms = parseInt(rangeVib.value);
              console.log("Triggering client haptic vibrate signal: " + ms + "ms");
              if (window.AndroidApp) {
                  window.AndroidApp.vibrate(ms);
                  window.AndroidApp.showToast("Tactile pulse triggered: " + ms + "ms");
              } else {
                  console.warn("Android client bridge not detected.");
              }
          });
          
          btnVibPattern.addEventListener("click", () => {
              console.log("Triggering customized pattern haptic array");
              if (window.AndroidApp) {
                  // Send a multi-pulse patterns or run individual steps
                  window.AndroidApp.vibrate(100);
                  setTimeout(() => window.AndroidApp.vibrate(60), 200);
                  setTimeout(() => window.AndroidApp.vibrate(60), 320);
                  setTimeout(() => window.AndroidApp.vibrate(250), 500);
                  window.AndroidApp.showToast("Rhythm tactile combo executed!");
              } else {
                  console.warn("Android client bridge not detected.");
              }
          });
          
          // Voice feedback speak
          btnSpeak.addEventListener("click", () => {
              const txt = ttsInput.value || "No feedback typed";
              console.log("Triggering synthesized speaking cue: " + txt);
              if (window.AndroidApp) {
                  window.AndroidApp.speak(txt);
              } else {
                  console.warn("Android client bridge not active.");
              }
          });
          
          // SharedPreferences save / load
          btnSave.addEventListener("click", () => {
              const key = optKey.value.trim();
              const val = optValue.value;
              if(!key) return;
              console.log("Saving client preferences: " + key + " = " + val);
              if (window.AndroidApp) {
                  window.AndroidApp.saveData(key, val);
                  storageOutput.innerText = "Key '" + key + "' committed securely!";
                  window.AndroidApp.showToast("Local storage key saved!");
              } else {
                  storageOutput.innerText = "Error: Bridge not connected. Values saved locally in session.";
                  localStorage.setItem(key, val);
              }
          });
          
          btnLoad.addEventListener("click", () => {
              const key = optKey.value.trim();
              if(!key) return;
              console.log("Retrieving client preferences: " + key);
              if (window.AndroidApp) {
                  const savedVal = window.AndroidApp.getData(key);
                  console.log("Loaded: " + savedVal);
                  storageOutput.innerText = "[DB Response] " + key + " = \"" + (savedVal || " (empty) ") + "\"";
              } else {
                  const savedVal = localStorage.getItem(key);
                  storageOutput.innerText = "[Session Storage] " + key + " = \"" + (savedVal || " (empty) ") + "\"";
              }
          });
          
          // Custom Clipboard
          btnCopy.addEventListener("click", () => {
              const txt = clipVal.value;
              console.log("Copying to standard clipboard: " + txt);
              if (window.AndroidApp) {
                  window.AndroidApp.copyToClipboard(txt);
                  window.AndroidApp.showToast("Copied content!");
              } else {
                  navigator.clipboard.writeText(txt);
              }
          });
          
          btnPaste.addEventListener("click", () => {
              console.log("Fetching target clipboard state");
              if (window.AndroidApp) {
                  const p = window.AndroidApp.getFromClipboard();
                  clipVal.value = p || " (Clipboard Empty) ";
                  window.AndroidApp.showToast("Clipboard Fetched!");
              } else {
                  navigator.clipboard.readText().then(t => {
                      clipVal.value = t;
                  });
              }
          });
          
          // Display system bars re-theming
          document.querySelectorAll('.color-preset').forEach(el => {
              el.addEventListener('click', () => {
                  document.querySelectorAll('.color-preset').forEach(c => c.classList.remove('active'));
                  el.classList.add('active');
                  const hex = el.getAttribute('data-hex');
                  console.log("Instructing system theme change: " + hex);
                  if (window.AndroidApp) {
                      window.AndroidApp.setStatusBarColor(hex);
                      window.AndroidApp.setNavigationBarColor(hex);
                      window.AndroidApp.showToast("Top/Bottom bar themes modified!");
                  }
              });
          });
          
          // Play system audible click
          btnPlaySound.addEventListener("click", () => {
              console.log("Dispatching play audio click sound ID");
              if (window.AndroidApp) {
                  window.AndroidApp.playSystemSound(1); // 1 = Standard Click
              }
          });
          
          // Full Screen toggling
          btnToggleFullScreen.addEventListener("click", () => {
              console.log("Triggering layout immersive fullscreen toggle");
              if (window.AndroidApp) {
                  window.AndroidApp.toggleFullScreen();
              }
          });
          
          // Fetch status metrics
          function updateSpecs() {
              console.log("Querying target client specification logs...");
              if (window.AndroidApp) {
                  const raw = window.AndroidApp.getDeviceInfo();
                  console.log("Device metric raw response: " + raw);
                  try {
                      const spec = JSON.parse(raw);
                      document.getElementById("stat-battery").innerText = spec.batteryLevel + "%";
                      document.getElementById("stat-model").innerText = spec.model;
                      document.getElementById("stat-version").innerText = "API " + spec.sdkInt;
                      document.getElementById("stat-orientation").innerText = spec.orientation;
                  } catch(e) {
                      console.error("Failed to compile device metadata structure: " + e);
                  }
              } else {
                  document.getElementById("stat-battery").innerText = "100% AC";
                  document.getElementById("stat-model").innerText = "Web Browser Sandbox";
                  document.getElementById("stat-version").innerText = "Chrome/v8";
                  document.getElementById("stat-orientation").innerText = "Landscape Preview";
              }
          }
          
          btnRefreshSpec.addEventListener("click", updateSpecs);
          // Auto run once
          setTimeout(updateSpecs, 400);
      });
    """.trimIndent()


    // ==========================================
    // TEMPLATE 2: HAPTIC POPPER & SOUNDBOARD
    // ==========================================
    private val TEMPLATE_SOUNDBOARD_HTML = """
      <div class="popper-universe">
          <header class="pop-header">
              <h2>📳 Haptic Popper & Synth</h2>
              <div class="pop-tag">Tactile Playground</div>
          </header>
          
          <div class="card">
              <div class="meter-panel">
                  <div>Tactile Tension: <span id="pop-tension-lbl" class="cyan">Medium</span></div>
                  <div class="grid-indicators">
                      <div class="ind" id="ind-l1"></div>
                      <div class="ind" id="ind-l2"></div>
                      <div class="ind" id="ind-l3"></div>
                  </div>
              </div>
              
              <!-- Pop grid -->
              <div class="popper-grid" id="popper-grid">
                  <!-- JS will dynamically populate 12 bouncy cells -->
              </div>
              
              <div class="btn-grid">
                  <button class="btn btn-outline" id="btn-reset-bubbles">Reset Bubbles</button>
                  <button class="btn btn-primary" id="btn-haptic-pulse">Full Shock</button>
              </div>
          </div>
          
          <div class="card soundboard-panel">
              <h3>🎵 Native Tone Synth</h3>
              <p class="desc">Play Android system ring tones or notification sounds from JS.</p>
              <div class="synth-layout">
                  <button class="synth-pad" data-sound="1">
                      <span class="pad-icon">🔔</span>
                      <label>Click</label>
                  </button>
                  <button class="synth-pad" data-sound="5">
                      <span class="pad-icon">⚡</span>
                      <label>Focus</label>
                  </button>
                  <button class="synth-pad" data-sound="6">
                      <span class="pad-icon">💨</span>
                      <label>Return</label>
                  </button>
                  <button class="synth-pad" data-sound="7">
                      <span class="pad-icon">🛸</span>
                      <label>Launch</label>
                  </button>
              </div>
          </div>
      </div>
    """.trimIndent()

    private val TEMPLATE_SOUNDBOARD_CSS = """
      .popper-universe {
          max-width: 500px;
          margin: 0 auto;
          padding: 16px;
          font-family: system-ui, -apple-system, sans-serif;
      }
      .pop-header {
          display: flex;
          justify-content: space-between;
          align-items: center;
          margin-bottom: 20px;
      }
      .pop-header h2 {
          margin: 0;
          font-size: 18px;
          color: #f1f5f9;
          font-weight: 700;
      }
      .pop-tag {
          font-size: 10px;
          padding: 3px 8px;
          background: rgba(6, 182, 212, 0.15);
          color: #22d3ee;
          border-radius: 6px;
          text-transform: uppercase;
          font-family: monospace;
          border: 1px solid rgba(6, 182, 212, 0.3);
      }
      .card {
          background-color: #1a1e2e;
          border-radius: 16px;
          border: 1px solid #2a344d;
          padding: 16px;
          box-shadow: 0 4px 15px rgba(0,0,0,0.3);
          margin-bottom: 16px;
      }
      .meter-panel {
          display: flex;
          justify-content: space-between;
          align-items: center;
          background: #0f1220;
          padding: 10px 14px;
          border-radius: 10px;
          font-family: monospace;
          font-size: 11px;
          margin-bottom: 20px;
          border: 1px solid #232b45;
      }
      .grid-indicators {
          display: flex;
          gap: 6px;
      }
      .ind {
          width: 8px;
          height: 8px;
          border-radius: 50%;
          background-color: #334155;
      }
      .ind.active {
          background-color: #22d3ee;
          box-shadow: 0 0 6px #22d3ee;
      }
      .cyan { color: #22d3ee; }
      
      /* Popper Bubble Grid */
      .popper-grid {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 14px;
          margin-bottom: 20px;
      }
      .bubble {
          aspect-ratio: 1;
          border-radius: 50%;
          background: radial-gradient(circle at 35% 35%, #3b82f6 0%, #1d4ed8 70%);
          border: none;
          box-shadow: 0 4px 10px rgba(29, 78, 216, 0.4), inset -2px -2px 6px rgba(0,0,0,0.5), inset 2px 2px 4px rgba(255,255,255,0.4);
          cursor: pointer;
          transform: scale(1);
          transition: transform 0.15s cubic-bezier(0.175, 0.885, 0.32, 1.275);
      }
      .bubble:active {
          transform: scale(0.9);
      }
      .bubble.popped {
          background: radial-gradient(circle at 50% 50%, #1e293b 0%, #0f172a 100%);
          box-shadow: inset 1px 1px 5px rgba(0,0,0,0.7), inset -1px -1px 2px rgba(255,255,255,0.1);
          transform: scale(0.95);
          pointer-events: none;
      }
      .btn-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 12px;
      }
      .btn {
          font-family: inherit;
          font-size: 13px;
          font-weight: 600;
          padding: 10px;
          border-radius: 10px;
          border: none;
          cursor: pointer;
          min-height: 40px;
      }
      .btn-primary {
          background: #ec4899;
          color: #fff;
          box-shadow: 0 4px 8px rgba(236, 72, 153, 0.3);
      }
      .btn-outline {
          background: transparent;
          border: 1.5px solid #2a344d;
          color: #f1f5f9;
      }
      
      /* Synth Pad board */
      .synth-layout {
          display: grid;
          grid-template-columns: repeat(4, 1fr);
          gap: 10px;
          margin-top: 10px;
      }
      .synth-pad {
          background-color: #111422;
          border: 1px solid #2d3752;
          color: #fff;
          border-radius: 12px;
          display: flex;
          flex-direction: column;
          align-items: center;
          padding: 12px 6px;
          cursor: pointer;
          transition: all 0.2s;
      }
      .synth-pad:active {
          background-color: #22d3ee;
          color: #0f1220;
          transform: translateY(2dp);
      }
      .pad-icon {
          font-size: 20px;
          margin-bottom: 4px;
      }
      .synth-pad label {
          font-size: 10px;
          font-weight: 600;
      }
    """.trimIndent()

    private val TEMPLATE_SOUNDBOARD_JS = """
      document.addEventListener("DOMContentLoaded", () => {
          console.log("Tactile popper initialized.");
          const grid = document.getElementById("popper-grid");
          const tensionLbl = document.getElementById("pop-tension-lbl");
          const ind1 = document.getElementById("ind-l1");
          const ind2 = document.getElementById("ind-l2");
          const ind3 = document.getElementById("ind-l3");
          
          let poppedCount = 0;
          
          // Generate Bubbles
          const bubbleConfigs = [
              { duration: 25, label: "Tingle", level: 1 },
              { duration: 35, label: "Tingle", level: 1 },
              { duration: 55, label: "Click", level: 2 },
              { duration: 55, label: "Click", level: 2 },
              { duration: 80, label: "Thud", level: 2 },
              { duration: 80, label: "Thud", level: 2 },
              { duration: 120, label: "Slam", level: 3 },
              { duration: 120, label: "Slam", level: 3 },
              { duration: 30, label: "Nudge", level: 1 },
              { duration: 60, label: "Impact", level: 2 },
              { duration: 90, label: "Bouncer", level: 3 },
              { duration: 150, label: "Explode", level: 3 }
          ];
          
          function renderBubbles() {
              grid.innerHTML = "";
              bubbleConfigs.forEach((cfg, idx) => {
                  const b = document.createElement("button");
                  b.className = "bubble";
                  b.addEventListener("click", () => {
                      popBubble(b, cfg);
                  });
                  grid.appendChild(b);
              });
          }
          
          function popBubble(element, cfg) {
              element.classList.add("popped");
              poppedCount++;
              
              // Vibration Call
              console.log("Popping bubble with duration: " + cfg.duration + "ms (" + cfg.label + ")");
              if (window.AndroidApp) {
                  window.AndroidApp.vibrate(cfg.duration);
                  // Play standard feedback tick sound 
                  window.AndroidApp.playSystemSound(5); // focus feedback sound
              }
              
              // Highlight indicator levels
              tensionLbl.innerText = cfg.label;
              ind1.className = "ind" + (cfg.level >= 1 ? " active" : "");
              ind2.className = "ind" + (cfg.level >= 2 ? " active" : "");
              ind3.className = "ind" + (cfg.level >= 3 ? " active" : "");
              
              if(poppedCount === bubbleConfigs.length) {
                  if(window.AndroidApp) {
                      window.AndroidApp.speak("Grid cleared! Congratulations.");
                      window.AndroidApp.vibrate(200);
                  }
              }
          }
          
          document.getElementById("btn-reset-bubbles").addEventListener("click", () => {
              poppedCount = 0;
              renderBubbles();
              tensionLbl.innerText = "Ready";
              ind1.className = "ind";
              ind2.className = "ind";
              ind3.className = "ind";
              console.log("Bubble board reset.");
              if(window.AndroidApp) {
                  window.AndroidApp.vibrate(40);
                  window.AndroidApp.showToast("Bubbles reset! Let's pop.");
              }
          });
          
          document.getElementById("btn-haptic-pulse").addEventListener("click", () => {
              console.log("Triggering 400ms physical feedback surge");
              if (window.AndroidApp) {
                  window.AndroidApp.vibrate(400);
                  window.AndroidApp.showToast("⚡ MAX POWER SURGE DISPATCHED!");
              }
          });
          
          // Audio Synth Pads
          document.querySelectorAll(".synth-pad").forEach(pad => {
              pad.addEventListener("click", () => {
                  const id = parseInt(pad.getAttribute("data-sound"));
                  console.log("Synth triggered sound ID: " + id);
                  if (window.AndroidApp) {
                      window.AndroidApp.playSystemSound(id);
                      window.AndroidApp.vibrate(50);
                  }
              });
          });
          
          renderBubbles();
      });
    """.trimIndent()


    // ==========================================
    // TEMPLATE 3: BRUTALIST CANVAS
    // ==========================================
    private val TEMPLATE_CANVAS_HTML = """
      <div class="canvas-app">
          <header class="app-header">
              <h3>🎨 Feel-The-Paint Board</h3>
              <p>Every stroke of your finger activates customized continuous Android tactile drags.</p>
          </header>
          
          <div class="canvas-container">
              <canvas id="paint-canvas"></canvas>
          </div>
          
          <div class="canvas-controls">
              <div class="control-section select-row">
                  <button class="color-dot active" style="background:#00f0ff;" data-color="#00f0ff"></button>
                  <button class="color-dot" style="background:#ff007f;" data-color="#ff007f"></button>
                  <button class="color-dot" style="background:#ffb700;" data-color="#ffb700"></button>
                  <button class="color-dot" style="background:#10b981;" data-color="#10b981"></button>
                  <button class="color-dot" style="background:#ffffff;" data-color="#ffffff"></button>
              </div>
              
              <div class="control-section slider-column">
                  <label>Brush Tactile Weight: <span id="lbl-weight">15</span></label>
                  <input type="range" id="size-slider" min="5" max="40" value="15">
              </div>
              
              <div class="btn-grid">
                  <button class="btn btn-outline" id="btn-clear">Reset Screen</button>
                  <button class="btn btn-primary" id="btn-save-canvas">Save Spec State</button>
              </div>
          </div>
          <div class="canvas-toast" id="can-logs">Brush state standing by. Try drawing!</div>
      </div>
    """.trimIndent()

    private val TEMPLATE_CANVAS_CSS = """
      .canvas-app {
          max-width: 500px;
          margin: 0 auto;
          padding: 12px;
          font-family: monospace;
          display: flex;
          flex-direction: column;
          gap: 12px;
      }
      .app-header h3 {
          margin: 0 0 4px 0;
          color: #00f0ff;
          font-weight: 800;
          letter-spacing: 0.5px;
      }
      .app-header p {
          margin: 0;
          font-size: 11px;
          color: #8fa1c4;
          line-height: 1.4;
      }
      .canvas-container {
          background-color: #070913;
          border: 2px solid #232a4a;
          border-radius: 12px;
          aspect-ratio: 1.2;
          overflow: hidden;
          width: 100%;
          display: flex;
          position: relative;
      }
      #paint-canvas {
          width: 100%;
          height: 100%;
          touch-action: none; /* Crucial to prevent android scroll pull downs */
      }
      .canvas-controls {
          background-color: #121626;
          border: 1px solid #242c4c;
          border-radius: 12px;
          padding: 12px;
          display: flex;
          flex-direction: column;
          gap: 12px;
      }
      .control-section {
          padding-bottom: 6px;
      }
      .select-row {
          display: flex;
          justify-content: center;
          gap: 14px;
          border-bottom: 1px dashed #1d2542;
          padding-bottom: 10px;
      }
      .color-dot {
          width: 28px;
          height: 28px;
          border-radius: 50%;
          cursor: pointer;
          border: 2.5px solid transparent;
          transition: transform 0.2s;
      }
      .color-dot.active {
          transform: scale(1.2);
          border-color: #ffffff;
      }
      .slider-column {
          display: flex;
          flex-direction: column;
          gap: 6px;
          font-size: 11px;
          color: #a0aec0;
      }
      .slider-column input {
          -webkit-appearance: none;
          width: 100%;
          height: 5px;
          background: #1c2340;
          border-radius: 4px;
          outline: none;
      }
      .slider-column input::-webkit-slider-thumb {
          -webkit-appearance: none;
          width: 16px;
          height: 16px;
          border-radius: 50%;
          background: #00f0ff;
          cursor: pointer;
      }
      .btn-grid {
          display: grid;
          grid-template-columns: 1fr 1fr;
          gap: 10px;
      }
      .btn {
          font-family: inherit;
          font-size: 12px;
          padding: 8px 12px;
          border-radius: 8px;
          font-weight: 700;
          cursor: pointer;
          border: none;
          min-height: 38px;
      }
      .btn-primary {
          background-color: #00f0ff;
          color: #000;
          box-shadow: 0 0 10px rgba(0,240,255,0.3);
      }
      .btn-outline {
          background-color: transparent;
          border: 1.5px solid #283359;
          color: #a0aec0;
      }
      .canvas-toast {
          background: #070913;
          border: 1px solid #1a2038;
          border-radius: 8px;
          padding: 8px 12px;
          font-size: 10px;
          color: #00f0ff;
          text-align: center;
          word-break: break-all;
      }
    """.trimIndent()

    private val TEMPLATE_CANVAS_JS = """
      document.addEventListener("DOMContentLoaded", () => {
          console.log("Feel-the-paint canvas scripts loaded.");
          const canvas = document.getElementById("paint-canvas");
          const ctx = canvas.getContext("2d");
          const toast = document.getElementById("can-logs");
          const thicknessSlider = document.getElementById("size-slider");
          const thicknessLbl = document.getElementById("lbl-weight");
          
          let color = "#00f0ff";
          let thickness = parseInt(thicknessSlider.value);
          let painting = false;
          let drawPointsHistoryCount = 0;
          
          // Setup sizes on load
          function resizeCanvas() {
              const bounds = canvas.getBoundingClientRect();
              canvas.width = bounds.width;
              canvas.height = bounds.height;
              
              // Re-fill dark canvas
              ctx.fillStyle = "#070913";
              ctx.fillRect(0,0, canvas.width, canvas.height);
              
              // Load historical canvas data, if any
              if (window.AndroidApp) {
                  const state = window.AndroidApp.getData("canvas_auto_save");
                  if (state && state.length > 5) {
                      const img = new Image();
                      img.onload = function() {
                          ctx.drawImage(img, 0, 0);
                      };
                      img.src = state;
                      console.log("Canvas spec restored successfully!");
                      toast.innerText = "Previous canvas layout loaded from SharedPreferences!";
                  }
              }
          }
          
          // Wire slider
          thicknessSlider.addEventListener("input", (e) => {
              thickness = parseInt(e.target.value);
              thicknessLbl.innerText = thickness;
          });
          
          // Color Selectors
          document.querySelectorAll(".color-dot").forEach(dot => {
              dot.addEventListener("click", () => {
                  document.querySelectorAll(".color-dot").forEach(d => d.classList.remove("active"));
                  dot.classList.add("active");
                  color = dot.getAttribute("data-color");
                  console.log("Brush tint shifted to: " + color);
                  if (window.AndroidApp) {
                      window.AndroidApp.vibrate(20);
                  }
              });
          });
          
          // Draw handlers
          function getXY(e) {
              const rect = canvas.getBoundingClientRect();
              if (e.touches && e.touches.length > 0) {
                  return {
                      x: e.touches[0].clientX - rect.left,
                      y: e.touches[0].clientY - rect.top
                  };
              }
              return {
                  x: e.clientX - rect.left,
                  y: e.clientY - rect.top
              };
          }
          
          function startDraw(e) {
              painting = true;
              ctx.beginPath();
              const pt = getXY(e);
              ctx.moveTo(pt.x, pt.y);
              if (window.AndroidApp) {
                  // Low haptic bump on draw touch start
                  window.AndroidApp.vibrate(35);
              }
          }
          
          function stopDraw() {
              painting = false;
              ctx.closePath();
              drawPointsHistoryCount = 0;
          }
          
          function draw(e) {
              if(!painting) return;
              e.preventDefault();
              const pt = getXY(e);
              
              ctx.lineWidth = thickness;
              ctx.lineCap = "round";
              ctx.strokeStyle = color;
              
              ctx.lineTo(pt.x, pt.y);
              ctx.stroke();
              ctx.beginPath();
              ctx.moveTo(pt.x, pt.y);
              
              drawPointsHistoryCount++;
              
              // Spark tactile haptic ticks as user drags to match drawing resistance
              if (drawPointsHistoryCount % 4 === 0) {
                  if (window.AndroidApp) {
                      // Alternate between tiny vibration pulses mimicking paint resistance
                      const pulse = Math.min(12 + Math.floor(thickness / 2), 35);
                      window.AndroidApp.vibrate(pulse);
                  }
              }
          }
          
          // Drag listeners
          canvas.addEventListener("pointerdown", startDraw);
          canvas.addEventListener("pointerup", stopDraw);
          canvas.addEventListener("pointermove", draw);
          canvas.addEventListener("touchstart", startDraw);
          canvas.addEventListener("touchend", stopDraw);
          canvas.addEventListener("touchmove", draw);
          
          // Form commands
          document.getElementById("btn-clear").addEventListener("click", () => {
              ctx.fillStyle = "#070913";
              ctx.fillRect(0,0, canvas.width, canvas.height);
              toast.innerText = "Screen wiped. Ready empty canvas.";
              if (window.AndroidApp) {
                  window.AndroidApp.vibrate(60);
                  window.AndroidApp.saveData("canvas_auto_save", ""); // reset
              }
          });
          
          document.getElementById("btn-save-canvas").addEventListener("click", () => {
              const dataUrl = canvas.toDataURL("image/png");
              console.log("Broadcasting canvas payload capture to Android local cache!");
              if (window.AndroidApp) {
                  window.AndroidApp.saveData("canvas_auto_save", dataUrl);
                  window.AndroidApp.showToast("Spec layout saved in native SharedPreferences!");
                  toast.innerText = "Successfully saved " + Math.round(dataUrl.length / 1024) + " KB drawing state.";
              } else {
                  localStorage.setItem("canvas_auto_save", dataUrl);
                  toast.innerText = "Saved to local storage (Web Preview).";
              }
          });
          
          // Initialize sizes
          setTimeout(resizeCanvas, 300);
      });
    """.trimIndent()
  }
}
