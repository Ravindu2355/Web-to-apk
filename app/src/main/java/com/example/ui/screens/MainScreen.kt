package com.example.ui.screens

import android.annotation.SuppressLint
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.view.ViewGroup
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.AndroidBridge
import com.example.LogEntry
import com.example.WebToAppViewModel

// Visual Constants for our Cosmic Space theme
val SpaceBg = Color(0xFF090B11)
val SlateCardBg = Color(0xFF141724)
val SlateBorder = Color(0xFF262F45)
val AccentCyan = Color(0xFF00F0FF)
val AccentPurple = Color(0xFFBF5AF2)
val AccentGreen = Color(0xFF30D158)
val AccentOrange = Color(0xFFFF9F0A)
val AccentRed = Color(0xFFFF453A)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
  viewModel: WebToAppViewModel,
  vibrator: Vibrator,
  tts: TextToSpeech,
  onToggleImmersive: () -> Unit,
  onSetStatusBarColor: (String) -> Unit,
  onSetNavigationBarColor: (String) -> Unit,
  onSetFullScreen: (Boolean) -> Unit,
  onShowStatusBar: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activeTab by viewModel.activeTab.collectAsStateWithLifecycle()
  val htmlCode by viewModel.htmlCode.collectAsStateWithLifecycle()
  val cssCode by viewModel.cssCode.collectAsStateWithLifecycle()
  val jsCode by viewModel.jsCode.collectAsStateWithLifecycle()
  val editorSubTab by viewModel.editorSubTab.collectAsStateWithLifecycle()
  val reloadFlag by viewModel.sandboxReloadFlag.collectAsStateWithLifecycle()
  val selectedTemplateIndex by viewModel.selectedTemplateIndex.collectAsStateWithLifecycle()
  val isFullscreen = viewModel.isFullscreen.value

  var webViewRef by remember { mutableStateOf<WebView?>(null) }

  // Create the AndroidBridge once
  val bridgeInstance = remember(webViewInstanceRefKey(reloadFlag)) {
    AndroidBridge(
      context = context,
      viewModel = viewModel,
      vibrator = vibrator,
      tts = tts,
      onToggleImmersive = {
        viewModel.isFullscreen.value = !viewModel.isFullscreen.value
        onToggleImmersive()
      },
      onSetStatusBarColor = onSetStatusBarColor,
      onSetNavigationBarColor = onSetNavigationBarColor,
      onGoBack = { webViewRef?.goBack() },
      onGoForward = { webViewRef?.goForward() },
      onReload = { webViewRef?.reload() },
      onLoadUrl = { url -> webViewRef?.loadUrl(url) },
      onCanGoBack = { webViewRef?.canGoBack() ?: false },
      onCanGoForward = { webViewRef?.canGoForward() ?: false },
      onSetFullScreen = { enabled ->
        viewModel.isFullscreen.value = enabled
        onSetFullScreen(enabled)
      },
      onShowStatusBar = onShowStatusBar
    )
  }

  // Handle the hardware back button press, passing full control to JavaScript
  BackHandler(enabled = webViewRef != null) {
    webViewRef?.let { webv ->
      webv.evaluateJavascript(
        """
        (function() {
          if (typeof window.onAndroidBack === 'function') {
            try {
              window.onAndroidBack();
              return "custom_handler";
            } catch(e) {
              return "error: " + e.message;
            }
          }
          var event = new CustomEvent('androidback', { cancelable: true });
          var cancelled = !window.dispatchEvent(event);
          if (cancelled) {
            return "event_cancelled";
          }
          return "not_handled";
        })()
        """.trimIndent()
      ) { result ->
        val cleanResult = result?.replace("\"", "") ?: "not_handled"
        if (cleanResult == "not_handled" || cleanResult.startsWith("error:")) {
          if (webv.canGoBack()) {
            webv.goBack()
          } else {
            // No history and JS didn't handle it, so exit the app
            (context as? android.app.Activity)?.finish()
          }
        }
      }
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(SpaceBg)
  ) {
    if (!com.example.AppConfig.enableSandboxMode) {
      // Standalone Production mode: Edge-to-edge full web screen
      Column(modifier = Modifier.fillMaxSize()) {
        if (!isFullscreen) {
          val statusBarColorHex = com.example.AppConfig.statusBarColor
          val parsedColor = remember(statusBarColorHex) {
            try {
              Color(android.graphics.Color.parseColor(statusBarColorHex))
            } catch (e: Exception) {
              Color.Transparent
            }
          }
          Spacer(
            modifier = Modifier
              .fillMaxWidth()
              .windowInsetsTopHeight(WindowInsets.statusBars)
              .background(parsedColor)
          )
        }
        WebViewComponent(
          viewModel = viewModel,
          html = com.example.AppConfig.startUrl,
          bridge = bridgeInstance,
          onWebViewCreated = { webViewRef = it },
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        )
      }
    } else if (isFullscreen) {
      // True immersive full-screen display of running static site
      Box(modifier = Modifier.fillMaxSize()) {
        WebViewComponent(
          viewModel = viewModel,
          html = viewModel.getMergedHtml(),
          bridge = bridgeInstance,
          onWebViewCreated = { webViewRef = it },
          modifier = Modifier.fillMaxSize()
        )

        // Elegant floating overlay buttons to escape fullscreen
        var showMenuOverlay by remember { mutableStateOf(false) }

        Box(
          modifier = Modifier
            .align(Alignment.BottomEnd)
            .padding(24.dp)
        ) {
          if (showMenuOverlay) {
            Column(
              horizontalAlignment = Alignment.End,
              verticalArrangement = Arrangement.spacedBy(10.dp),
              modifier = Modifier.padding(bottom = 60.dp)
            ) {
              SmallFloatingActionButton(
                onClick = {
                  viewModel.triggerReload()
                  showMenuOverlay = false
                },
                containerColor = SlateCardBg,
                contentColor = AccentCyan,
                shape = CircleShape
              ) {
                Icon(Icons.Default.Refresh, contentDescription = "Reload Page")
              }

              SmallFloatingActionButton(
                onClick = {
                  viewModel.setActiveTab(2) // Jump to dev logs
                  viewModel.isFullscreen.value = false
                  onToggleImmersive()
                  showMenuOverlay = false
                },
                containerColor = SlateCardBg,
                contentColor = AccentPurple,
                shape = CircleShape
              ) {
                Icon(Icons.Default.Warning, contentDescription = "Show Web Logs")
              }

              Button(
                onClick = {
                  viewModel.isFullscreen.value = false
                  onToggleImmersive()
                  showMenuOverlay = false
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentRed),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
              ) {
                Icon(
                  Icons.Default.Close,
                  contentDescription = "Exit Fullscreen",
                  modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Exit Immersive Mode", fontSize = 12.sp, fontWeight = FontWeight.Bold)
              }
            }
          }

          FloatingActionButton(
            onClick = { showMenuOverlay = !showMenuOverlay },
            containerColor = if (showMenuOverlay) AccentRed else AccentCyan.copy(alpha = 0.85f),
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = if (showMenuOverlay) Icons.Default.Close else Icons.Default.Settings,
              contentDescription = "Menu Overlay"
            )
          }
        }
      }
    } else {
      // Normal design layout: Top branding headers + Tab Navigation row + Content sheets
      Scaffold(
        topBar = {
          TopAppBar(
            title = {
              Column {
                Text(
                  text = "WEBTOAPP SANDBOX",
                  style = TextStyle(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    brush = Brush.linearGradient(listOf(AccentCyan, AccentPurple))
                  )
                )
                Text(
                  text = "Native Android JS Bridge Runtime",
                  fontSize = 10.sp,
                  color = Color.Gray,
                  fontWeight = FontWeight.Medium
                )
              }
            },
            colors = TopAppBarDefaults.topAppBarColors(
              containerColor = SpaceBg,
              titleContentColor = Color.White
            ),
            actions = {
              IconButton(onClick = { viewModel.triggerReload() }) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Reload web page Container",
                  tint = AccentCyan
                )
              }
            }
          )
        },
        bottomBar = {
          NavigationBar(
            containerColor = SpaceBg,
            tonalElevation = 8.dp,
            modifier = Modifier.border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
          ) {
            val items = listOf(
              Triple("Runtime", Icons.Default.PlayArrow, 0),
              Triple("Sandbox IDE", Icons.Default.Build, 1),
              Triple("Bridge Logs", Icons.Default.Warning, 2),
              Triple("Export Guide", Icons.Default.List, 3)
            )
            items.forEach { (label, icon, index) ->
              NavigationBarItem(
                selected = activeTab == index,
                onClick = { viewModel.setActiveTab(index) },
                icon = { Icon(icon, contentDescription = label) },
                label = { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                colors = NavigationBarItemDefaults.colors(
                  selectedIconColor = Color.Black,
                  selectedTextColor = AccentCyan,
                  indicatorColor = AccentCyan,
                  unselectedIconColor = Color.Gray,
                  unselectedTextColor = Color.Gray
                )
              )
            }
          }
        },
        containerColor = SpaceBg,
        modifier = Modifier.fillMaxSize()
      ) { innerPadding ->
        Column(
          modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
        ) {
          // Horizontal divider
          HorizontalDivider(color = SlateBorder, thickness = 0.5.dp)

          when (activeTab) {
            0 -> WebViewSandboxTab(
              viewModel = viewModel,
              html = viewModel.getMergedHtml(),
              bridge = bridgeInstance,
              onWebViewCreated = { webViewRef = it }
            )
            1 -> CodeEditorTab(
              viewModel = viewModel,
              htmlCode = htmlCode,
              cssCode = cssCode,
              jsCode = jsCode,
              editorSubTab = editorSubTab,
              selectedTemplate = selectedTemplateIndex
            )
            2 -> LogsMonitorTab(viewModel = viewModel)
            3 -> BundleGuideTab()
          }
        }
      }
    }
  }
}

// -------------------------------------------------------------
// WEB VIEW RUNTIME SCREEN
// -------------------------------------------------------------
@Composable
fun WebViewSandboxTab(
  viewModel: WebToAppViewModel,
  html: String,
  bridge: AndroidBridge,
  onWebViewCreated: (WebView) -> Unit
) {
  Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Mini state bar inside runtime tab
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(SlateCardBg.copy(alpha = 0.5f))
          .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(8.dp)
              .clip(CircleShape)
              .background(AccentGreen)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text("Sandbox Environment: ACTIVE", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = AccentGreen)
        }
        Text(
          text = "window.AndroidApp injected",
          fontSize = 10.sp,
          fontFamily = FontFamily.Monospace,
          color = AccentCyan
        )
      }

      WebViewComponent(
        viewModel = viewModel,
        html = html,
        bridge = bridge,
        onWebViewCreated = onWebViewCreated,
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
      )
    }

    // Floating Button to launch True Immersive View
    FloatingActionButton(
      onClick = {
        viewModel.isFullscreen.value = true
        // trigger bridge logs trigger
        viewModel.addLog("SYSTEM", "INFO", "Entered TRUE Fullscreen mode.")
      },
      containerColor = AccentPurple,
      contentColor = Color.White,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(20.dp)
        .size(54.dp)
    ) {
      Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = "Open Fullscreen Web App",
        modifier = Modifier.size(28.dp)
      )
    }
  }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebViewComponent(
  viewModel: WebToAppViewModel,
  html: String,
  bridge: AndroidBridge,
  onWebViewCreated: (WebView) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current

  AndroidView(
    factory = { ctx ->
      WebView(ctx).apply {
        layoutParams = ViewGroup.LayoutParams(
          ViewGroup.LayoutParams.MATCH_PARENT,
          ViewGroup.LayoutParams.MATCH_PARENT
        )

        // Enable complete features to maximize power of static site wrapper
        settings.run {
          javaScriptEnabled = true
          domStorageEnabled = true
          databaseEnabled = true
          useWideViewPort = true
          loadWithOverviewMode = true
          allowFileAccess = true
          allowContentAccess = true
        }

        // Bridge Injection
        addJavascriptInterface(bridge, "AndroidApp")

        webViewClient = object : WebViewClient() {
          override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            viewModel.addLog("SYSTEM", "DEBUG", "WebView page loaded: $url")
          }
        }

        webChromeClient = object : WebChromeClient() {
          override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
            if (consoleMessage != null) {
              val level = consoleMessage.messageLevel().name
              val msg = consoleMessage.message()
              viewModel.addLog("CONSOLE", level, "$msg [Line ${consoleMessage.lineNumber()}]")
            }
            return true
          }
        }

        onWebViewCreated(this)

        if (html.startsWith("http://") || html.startsWith("https://") || html.startsWith("file://")) {
          loadUrl(html)
        } else {
          loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
        }
      }
    },
    update = { webView ->
      onWebViewCreated(webView)
      if (html.startsWith("http://") || html.startsWith("https://") || html.startsWith("file://")) {
        if (webView.url != html) {
          webView.loadUrl(html)
        }
      } else {
        webView.loadDataWithBaseURL("https://localhost/", html, "text/html", "UTF-8", null)
      }
    },
    modifier = modifier
  )
}


// -------------------------------------------------------------
// SANDBOX WRITING & TEMPLATE SCREEN
// -------------------------------------------------------------
@Composable
fun CodeEditorTab(
  viewModel: WebToAppViewModel,
  htmlCode: String,
  cssCode: String,
  jsCode: String,
  editorSubTab: Int,
  selectedTemplate: Int
) {
  var showPresetsDialog by remember { mutableStateOf(false) }

  Box(modifier = Modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Template Preset header bar
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .background(SlateCardBg)
          .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column(modifier = Modifier.weight(1f)) {
          Text("Editing Preset Template", fontSize = 10.sp, color = Color.Gray)
          Text(
            viewModel.templates[selectedTemplate].name,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = AccentCyan
          )
        }

        Button(
          onClick = { showPresetsDialog = true },
          colors = ButtonDefaults.buttonColors(containerColor = SlateBorder),
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Icon(Icons.Default.Star, contentDescription = null, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text("Presets", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }

      // Tab selection headers (HTML, CSS, JS)
      TabRow(
        selectedTabIndex = editorSubTab,
        containerColor = SpaceBg,
        contentColor = Color.White,
        indicator = { tabPositions ->
          TabRowDefaults.SecondaryIndicator(
            modifier = Modifier.tabIndicatorOffset(tabPositions[editorSubTab]),
            color = AccentCyan
          )
        }
      ) {
        val subtabs = listOf("index.html" to Icons.Default.Build, "style.css" to Icons.Default.Edit, "script.js" to Icons.Default.PlayArrow)
        subtabs.forEachIndexed { i, (name, icon) ->
          Tab(
            selected = editorSubTab == i,
            onClick = { viewModel.setEditorSubTab(i) },
            text = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  icon,
                  contentDescription = null,
                  modifier = Modifier.size(15.dp),
                  tint = if (editorSubTab == i) AccentCyan else Color.Gray
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  name,
                  fontSize = 11.sp,
                  fontWeight = if (editorSubTab == i) FontWeight.Bold else FontWeight.Normal,
                  fontFamily = FontFamily.Monospace,
                  color = if (editorSubTab == i) AccentCyan else Color.Gray
                )
              }
            }
          )
        }
      }

      // Input TextField with full edge-to-edge support representing IDE Editor
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .background(SpaceBg)
          .padding(8.dp)
      ) {
        val currentCodeText = when (editorSubTab) {
          0 -> htmlCode
          1 -> cssCode
          else -> jsCode
        }

        val onValueChange: (String) -> Unit = { updated ->
          when (editorSubTab) {
            0 -> viewModel.updateHtml(updated)
            1 -> viewModel.updateCss(updated)
            2 -> viewModel.updateJs(updated)
          }
        }

        OutlinedTextField(
          value = currentCodeText,
          onValueChange = onValueChange,
          textStyle = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            color = Color(0xFFD4D4D4)
          ),
          modifier = Modifier
            .fillMaxSize()
            .border(width = 1.dp, color = SlateBorder, shape = RoundedCornerShape(12.dp)),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = AccentCyan,
            unfocusedBorderColor = SlateBorder,
            focusedContainerColor = Color(0xFF0F111A),
            unfocusedContainerColor = Color(0xFF0C0E14)
          ),
          shape = RoundedCornerShape(12.dp),
          placeholder = {
            Text("Write clean client side syntax code here... window.AndroidApp handles bridges.", fontSize = 11.sp, color = Color.DarkGray)
          }
        )
      }
    }

    // Floating Button to triggers compilation reload
    FloatingActionButton(
      onClick = {
        viewModel.triggerReload()
        viewModel.setActiveTab(0) // Swap to live render
      },
      containerColor = AccentCyan,
      contentColor = Color.Black,
      shape = CircleShape,
      modifier = Modifier
        .align(Alignment.BottomEnd)
        .padding(20.dp)
        .size(56.dp)
    ) {
      Icon(
        imageVector = Icons.Default.PlayArrow,
        contentDescription = "Compile & Run Web App",
        modifier = Modifier.size(28.dp)
      )
    }
  }

  // Preset Template Dialog picker
  if (showPresetsDialog) {
    AlertDialog(
      onDismissRequest = { showPresetsDialog = false },
      title = { Text("Available Application Presets", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Select a ready preset template loaded complete with beautiful Material and HTML5 specs to view live examples of Javascript to Android bridge integrations:", fontSize = 12.sp, color = Color.Gray)
          
          viewModel.templates.forEachIndexed { idx, template ->
            val isSelected = selectedTemplate == idx
            Card(
              colors = CardDefaults.cardColors(
                containerColor = if (isSelected) SlateBorder else SlateCardBg
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  viewModel.selectTemplate(idx)
                  showPresetsDialog = false
                }
                .border(
                  width = 2.dp,
                  color = if (isSelected) AccentCyan else Color.Transparent,
                  shape = RoundedCornerShape(12.dp)
                ),
              shape = RoundedCornerShape(12.dp)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = template.name,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = if (isSelected) AccentCyan else Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = template.description, fontSize = 10.sp, color = Color.Gray, lineHeight = 1.3.sp)
              }
            }
          }
        }
      },
      confirmButton = {
        Button(
          onClick = { showPresetsDialog = false },
          colors = ButtonDefaults.buttonColors(containerColor = SlateBorder)
        ) {
          Text("Close")
        }
      },
      containerColor = SpaceBg,
      shape = RoundedCornerShape(16.dp)
    )
  }
}


// -------------------------------------------------------------
// LIVE CONSOLE & INTERCEPTED BRIDGE LOGS MONITOR SCREEN
// -------------------------------------------------------------
@Composable
fun LogsMonitorTab(viewModel: WebToAppViewModel) {
  val listState = rememberLazyListState()

  // Auto-scroll logic as logs stream
  LaunchedEffect(viewModel.logs.size) {
    if (viewModel.logs.isNotEmpty()) {
      listState.animateScrollToItem(viewModel.logs.size - 1)
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column {
        Text("Debugging Terminal Logs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Text("Displays console.log() interceptions and bridge ticks", fontSize = 11.sp, color = Color.Gray)
      }

      IconButton(
        onClick = { viewModel.clearLogs() },
        modifier = Modifier.background(SlateCardBg, shape = RoundedCornerShape(8.dp))
      ) {
        Icon(Icons.Default.Delete, contentDescription = "Clear Session Logs", tint = AccentRed)
      }
    }

    if (viewModel.logs.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(12.dp))
          .background(Color(0xFF04060C)),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(36.dp))
          Spacer(modifier = Modifier.height(10.dp))
          Text("Terminal Standing By", fontSize = 12.sp, color = Color.Gray)
          Text("Trigger bridge actions or log scripts to see them stream", fontSize = 10.sp, color = Color.DarkGray)
        }
      }
    } else {
      LazyColumn(
        state = listState,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(12.dp))
          .background(Color(0xFF04060C))
          .padding(8.dp)
      ) {
        items(viewModel.logs) { log ->
          LogItemLayout(log)
        }
      }
    }
  }
}

@Composable
fun LogItemLayout(log: LogEntry) {
  val levelColor = when (log.level.uppercase()) {
    "ERROR" -> AccentRed
    "WARN" -> AccentOrange
    "DEBUG" -> AccentCyan
    else -> AccentGreen
  }

  val tagBg = when (log.tag.uppercase()) {
    "CONSOLE" -> AccentCyan.copy(alpha = 0.15f)
    "BRIDGE" -> AccentPurple.copy(alpha = 0.15f)
    else -> SlateBorder
  }

  val tagTextTint = when (log.tag.uppercase()) {
    "CONSOLE" -> AccentCyan
    "BRIDGE" -> AccentPurple
    else -> Color.White
  }

  Card(
    colors = CardDefaults.cardColors(containerColor = SlateCardBg.copy(alpha = 0.4f)),
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(6.dp)
  ) {
    Row(
      modifier = Modifier
        .padding(8.dp)
        .fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(bottom = 4.dp)
        ) {
          Text(
            text = log.timestamp,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
            color = Color.DarkGray
          )

          Box(
            modifier = Modifier
              .background(tagBg, shape = RoundedCornerShape(4.dp))
              .padding(horizontal = 5.dp, vertical = 2.dp)
          ) {
            Text(
              text = log.tag,
              fontSize = 8.sp,
              fontWeight = FontWeight.Black,
              fontFamily = FontFamily.Monospace,
              color = tagTextTint
            )
          }

          Text(
            text = log.level,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = levelColor
          )
        }

        Text(
          text = log.message,
          fontSize = 11.sp,
          fontFamily = FontFamily.Monospace,
          color = Color(0xFFE2E8F0),
          modifier = Modifier.padding(start = 2.dp)
        )
      }
    }
  }
}


// -------------------------------------------------------------
// BUNDLE & EXPORT GUIDE SCREEN
// -------------------------------------------------------------
@Composable
fun BundleGuideTab() {
  val clipboardManager = LocalClipboardManager.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
      .verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    Column {
      Text("Bundle & Export Your App", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color.White)
      Text("Simple offline conversion instructions", fontSize = 11.sp, color = Color.Gray)
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateCardBg),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(12.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📁 Step 1: Resource Setup", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 13.sp)
        Text("Bundle your static files directly in your Android assets directory so the app works on-device completely offline with zero web server cost.", fontSize = 11.sp, color = Color.Gray)
        
        Text("Move index.html, style.css, and script.js files into your Android bundle:", fontSize = 11.sp, color = Color.White)
        CodeContainer(code = "app/src/main/assets/index.html\napp/src/main/assets/style.css\napp/src/main/assets/script.js", clipboardManager = clipboardManager)
      }
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateCardBg),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(12.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🖥️ Step 2: Configure Android WebView", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 13.sp)
        Text("Inside your MainActivity or Jetpack Compose screen containing the WebView, enable local file reading by replacing data string renders with local assets loaders:", fontSize = 11.sp, color = Color.Gray)
        
        val codeText = """
          // Load HTML from Assets folder
          webView.loadUrl("file:///android_asset/index.html")
          
          // Inject Bridge class object securely
          webView.addJavascriptInterface(
              AndroidBridge(context, viewModel, vibrator, tts, ...), 
              "AndroidApp"
          )
        """.trimIndent()
        CodeContainer(code = codeText, clipboardManager = clipboardManager)
      }
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateCardBg),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(12.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🛡️ Step 3: Hardware Permissions", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 13.sp)
        Text("Include standard user features (such as haptic buzzing) inside your Android Manifest so the platform authorizes Javascript callbacks:", fontSize = 11.sp, color = Color.Gray)
        
        val manifestXml = """
          <!-- AndroidManifest.xml -->
          <manifest ...>
              <uses-permission android:name="android.permission.VIBRATE" />
              
              <application ...>
                  <!-- Add hardware configurations -->
              </application>
          </manifest>
        """.trimIndent()
        CodeContainer(code = manifestXml, clipboardManager = clipboardManager)
      }
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateCardBg),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(12.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("🔙 Step 4: Intercepting hardware BACK Button", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 13.sp)
        Text("Your web app can intercept when the user clicks the physical device back button using either a standard event listener or a global hook function.", fontSize = 11.sp, color = Color.Gray)
        
        Text("Option A: Standard Custom Event (Recommended)", fontSize = 11.sp, color = Color.White)
        val jsEventCode = """
          window.addEventListener('androidback', (event) => {
              // preventDefault stops native WebView back step and lets you run custom JS
              event.preventDefault(); 
              
              alert("Hardware back button clicked!");
              // Example: Custom navigation back or close navigation drawer
          });
        """.trimIndent()
        CodeContainer(code = jsEventCode, clipboardManager = clipboardManager)

        Text("Option B: Global function hook", fontSize = 11.sp, color = Color.White)
        val jsFuncCode = """
          window.onAndroidBack = function() {
              alert("window.onAndroidBack was triggered!");
              // Run any custom back action you desire here
          };
        """.trimIndent()
        CodeContainer(code = jsFuncCode, clipboardManager = clipboardManager)
      }
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = SlateCardBg),
      shape = RoundedCornerShape(12.dp),
      modifier = Modifier.fillMaxWidth().border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(12.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("📱 Step 5: Fullscreen & Status Bar Control", fontWeight = FontWeight.Bold, color = AccentCyan, fontSize = 13.sp)
        Text("Pass dynamic layout attributes directly from your JavaScript application to command immersive states, notches, and status overlays:", fontSize = 11.sp, color = Color.Gray)
        
        Text("Immersive Fullscreen API", fontSize = 11.sp, color = Color.White)
        val jsFullscreenCode = """
          // Toggle full screen mode on or off
          window.AndroidBridge.toggleFullScreen();

          // Explicitly enter/exit full screen model
          window.AndroidBridge.setFullScreen(true); // true to hide status/navigation bars, false to show
        """.trimIndent()
        CodeContainer(code = jsFullscreenCode, clipboardManager = clipboardManager)

        Text("Dynamic Status Bar Control", fontSize = 11.sp, color = Color.White)
        val jsStatusBarCode = """
          // Explicitly show or hide the status/clock bar at the top (preserving the notch)
          window.AndroidBridge.showStatusBar(true); // true to show, false to hide

          // Adjust colors of system bars dynamically
          window.AndroidBridge.setStatusBarColor("#1A0F30");
          window.AndroidBridge.setNavigationBarColor("#1A0F30");
        """.trimIndent()
        CodeContainer(code = jsStatusBarCode, clipboardManager = clipboardManager)
      }
    }
    
    Spacer(modifier = Modifier.height(20.dp))
  }
}

@Composable
fun CodeContainer(code: String, clipboardManager: androidx.compose.ui.platform.ClipboardManager) {
  Box(
    modifier = Modifier
      .fillMaxWidth()
      .background(Color(0xFF070912), shape = RoundedCornerShape(8.dp))
      .border(width = 0.5.dp, color = SlateBorder, shape = RoundedCornerShape(8.dp))
      .padding(10.dp)
  ) {
    Text(
      text = code,
      fontFamily = FontFamily.Monospace,
      fontSize = 10.sp,
      color = Color(0xFF9EADCA),
      modifier = Modifier.fillMaxWidth()
    )

    IconButton(
      onClick = { clipboardManager.setText(AnnotatedString(code)) },
      modifier = Modifier
        .align(Alignment.TopEnd)
        .size(24.dp)
        .background(SlateCardBg, shape = RoundedCornerShape(4.dp))
    ) {
      Icon(Icons.Default.Share, contentDescription = "Copy code block", tint = AccentCyan, modifier = Modifier.size(12.dp))
    }
  }
}

// Support key to recreate bridge on reload trigger
private fun webViewInstanceRefKey(flag: Long): String {
  return "bridge_ref_$flag"
}
