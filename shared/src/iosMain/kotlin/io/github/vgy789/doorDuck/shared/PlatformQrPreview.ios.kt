package io.github.vgy789.doorDuck.shared

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import platform.WebKit.WKWebView

@Composable
actual fun PlatformQrPreview(
    base64: String?,
    emptyText: String,
    contentDescription: String,
    modifier: Modifier,
) {
    UIKitView(
        factory = {
            WKWebView().apply {
                scrollView.scrollEnabled = false
                opaque = false
                backgroundColor = platform.UIKit.UIColor.clearColor
            }
        },
        modifier = modifier,
        update = { webView ->
            val html = if (base64.isNullOrBlank()) {
                """
                <html>
                <body style='margin:0;width:100vw;height:100vh;display:flex;align-items:center;justify-content:center;font-family:-apple-system;color:#465062;text-align:center;'>
                $emptyText
                </body>
                </html>
                """.trimIndent()
            } else {
                """
                <html>
                <body style='margin:0;width:100vw;height:100vh;display:flex;align-items:center;justify-content:center;background:transparent;overflow:hidden;'>
                <img
                    alt='$contentDescription'
                    style='display:block;width:84%;height:84%;object-fit:contain;image-rendering:pixelated;'
                    src='data:image/png;base64,$base64'
                />
                </body>
                </html>
                """.trimIndent()
            }
            webView.loadHTMLString(html, baseURL = null)
        },
    )
}
