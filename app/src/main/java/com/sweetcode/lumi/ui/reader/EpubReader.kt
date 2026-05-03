package com.sweetcode.lumi.ui.reader

import android.annotation.SuppressLint
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.TextDecrease
import androidx.compose.material.icons.rounded.TextIncrease
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import java.io.File

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EpubReader(
    htmlFile: File,
    title: String,
    initialProgress: Int,
    onBack: () -> Unit,
    onProgressChanged: (current: Int, total: Int) -> Unit,
    settingsViewModel: EpubReaderViewModel = hiltViewModel()
) {
    val readerSettings by settingsViewModel.settings.collectAsState()

    var currentPage by remember { mutableIntStateOf(initialProgress) }
    var totalPages by remember { mutableIntStateOf(0) }
    var showOverlay by remember { mutableStateOf(true) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    LaunchedEffect(showOverlay) {
        if (showOverlay) {
            kotlinx.coroutines.delay(3000)
            showOverlay = false
        }
    }

    LaunchedEffect(currentPage, totalPages) {
        if (totalPages > 0) onProgressChanged(currentPage, totalPages)
    }

    val bgColor = if (readerSettings.darkMode) Color(0xFF13111A) else Color(0xFFFAF7F2)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    this.settings.javaScriptEnabled = true
                    this.settings.allowFileAccess = true
                    this.settings.allowContentAccess = true
                    setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    isVerticalScrollBarEnabled = false
                    isHorizontalScrollBarEnabled = false
                    overScrollMode = View.OVER_SCROLL_NEVER
                    addJavascriptInterface(
                        EpubJsBridge(
                            onTotal = { total -> totalPages = total },
                            onPage = { page -> currentPage = page },
                            onCenterTap = { showOverlay = !showOverlay }
                        ),
                        "LumiBridge"
                    )
                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            view?.evaluateJavascript(
                                buildJs(
                                    fontSize = readerSettings.fontSize,
                                    darkMode = readerSettings.darkMode,
                                    initialPage = initialProgress
                                ),
                                null
                            )
                        }
                    }
                    loadUrl("file://${htmlFile.absolutePath}")
                    webViewRef = this
                }
            }
        )

        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            EpubTopBar(
                title = title,
                darkMode = readerSettings.darkMode,
                onBack = onBack,
                onToggleDark = {
                    settingsViewModel.toggleDarkMode()
                    webViewRef?.evaluateJavascript("Lumi.setDark(${!readerSettings.darkMode});", null)
                },
                onDecreaseFont = {
                    val newSize = readerSettings.fontSize - 2
                    settingsViewModel.setFontSize(newSize)
                    webViewRef?.evaluateJavascript("Lumi.setFont($newSize);", null)
                },
                onIncreaseFont = {
                    val newSize = readerSettings.fontSize + 2
                    settingsViewModel.setFontSize(newSize)
                    webViewRef?.evaluateJavascript("Lumi.setFont($newSize);", null)
                }
            )
        }

        AnimatedVisibility(
            visible = showOverlay,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            EpubBottomBar(
                currentPage = currentPage + 1,
                totalPages = totalPages
            )
        }
    }
}

@Composable
private fun EpubTopBar(
    title: String,
    darkMode: Boolean,
    onBack: () -> Unit,
    onToggleDark: () -> Unit,
    onDecreaseFont: () -> Unit,
    onIncreaseFont: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Atrás", tint = Color.White)
        }
        Text(
            text = title,
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(start = 4.dp)
                .weight(1f),
            maxLines = 1
        )
        IconButton(onClick = onDecreaseFont) {
            Icon(Icons.Rounded.TextDecrease, contentDescription = "Texto menor", tint = Color.White)
        }
        IconButton(onClick = onIncreaseFont) {
            Icon(Icons.Rounded.TextIncrease, contentDescription = "Texto mayor", tint = Color.White)
        }
        IconButton(onClick = onToggleDark) {
            Icon(
                imageVector = if (darkMode) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                contentDescription = "Modo noche",
                tint = Color.White
            )
        }
    }
}

@Composable
private fun EpubBottomBar(currentPage: Int, totalPages: Int) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black.copy(alpha = 0.7f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = if (totalPages > 0) "$currentPage / $totalPages" else "Calculando…",
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
            )
        }
        if (totalPages > 0) {
            LinearProgressIndicator(
                progress = { currentPage.toFloat() / totalPages.toFloat() },
                color = MaterialTheme.colorScheme.primary,
                trackColor = Color.White.copy(alpha = 0.2f),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}

class EpubJsBridge(
    private val onTotal: (Int) -> Unit,
    private val onPage: (Int) -> Unit,
    private val onCenterTap: () -> Unit
) {
    @android.webkit.JavascriptInterface
    fun setTotalPages(total: Int) {
        android.os.Handler(android.os.Looper.getMainLooper()).post { onTotal(total) }
    }

    @android.webkit.JavascriptInterface
    fun setCurrentPage(page: Int) {
        android.os.Handler(android.os.Looper.getMainLooper()).post { onPage(page) }
    }

    @android.webkit.JavascriptInterface
    fun centerTap() {
        android.os.Handler(android.os.Looper.getMainLooper()).post { onCenterTap() }
    }
}

private fun buildJs(fontSize: Int, darkMode: Boolean, initialPage: Int): String = """
    (function() {
        var style = document.createElement('style');
        style.id = 'lumi-style';
        document.head.appendChild(style);
        
        window.Lumi = {
            currentPage: 0,
            totalPages: 0,
            fontSize: $fontSize,
            dark: $darkMode,
            
            applyStyle: function() {
                var w = window.innerWidth;
                var h = window.innerHeight - 32;
                style.innerHTML = `
                    html, body {
                        margin: 0;
                        padding: 0;
                        height: ${'$'}{h}px;
                        overflow: hidden;
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        font-size: ${'$'}{Lumi.fontSize}px;
                        line-height: 1.6;
                        color: ${'$'}{Lumi.dark ? '#EDE9F7' : '#1A1625'};
                        background: ${'$'}{Lumi.dark ? '#13111A' : '#FAF7F2'};
                    }
                    body {
                        column-width: ${'$'}{w - 48}px;
                        column-gap: 48px;
                        column-fill: auto;
                        padding: 16px 24px;
                        height: ${'$'}{h - 32}px;
                    }
                    img {
                        max-width: 100%;
                        max-height: 80vh;
                        height: auto;
                        display: block;
                        margin: 1em auto;
                    }
                    h1, h2, h3 { break-after: avoid; }
                    p { break-inside: avoid-column; orphans: 2; widows: 2; }
                    a { color: ${'$'}{Lumi.dark ? '#B5A0FF' : '#6B4EFF'}; }
                `;
                Lumi.recalculate();
            },
            
            recalculate: function() {
                var body = document.body;
                var w = window.innerWidth;
                Lumi.totalPages = Math.ceil(body.scrollWidth / w);
                LumiBridge.setTotalPages(Lumi.totalPages);
                Lumi.goToPage(Lumi.currentPage);
            },
            
            goToPage: function(page) {
                Lumi.currentPage = Math.max(0, Math.min(page, Lumi.totalPages - 1));
                document.body.scrollLeft = Lumi.currentPage * window.innerWidth;
                LumiBridge.setCurrentPage(Lumi.currentPage);
            },
            
            next: function() { Lumi.goToPage(Lumi.currentPage + 1); },
            prev: function() { Lumi.goToPage(Lumi.currentPage - 1); },
            
            setFont: function(size) {
                Lumi.fontSize = size;
                var ratio = Lumi.totalPages > 0 ? Lumi.currentPage / Lumi.totalPages : 0;
                Lumi.applyStyle();
                setTimeout(function() {
                    Lumi.goToPage(Math.round(ratio * Lumi.totalPages));
                }, 50);
            },
            
            setDark: function(dark) {
                Lumi.dark = dark;
                Lumi.applyStyle();
            }
        };
        
        Lumi.applyStyle();
        setTimeout(function() {
            Lumi.goToPage($initialPage);
        }, 100);
        
        document.addEventListener('click', function(e) {
            var w = window.innerWidth;
            if (e.clientX < w * 0.3) {
                Lumi.prev();
            } else if (e.clientX > w * 0.7) {
                Lumi.next();
            } else {
                LumiBridge.centerTap();
            }
        }, true);
        
        window.addEventListener('resize', function() {
            setTimeout(function() { Lumi.recalculate(); }, 100);
        });
    })();
""".trimIndent()