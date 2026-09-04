import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target = """            val html = viewModel.lastHtmlResponse
            if (html != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text("Latest Server Response", style = MaterialTheme.typography.titleMedium)
                AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            setBackgroundColor(android.graphics.Color.WHITE)
                        }
                    },
                    update = { webView ->
                        webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height((LocalConfiguration.current.screenHeightDp * 1.0).dp)
                        .background(Color.White)
                )
            }"""

replacement = """            val html = viewModel.lastHtmlResponse
            if (html != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showWebViewDialog = html },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Latest Server Response HTML")
                }
            }"""

if target in content:
    content = content.replace(target, replacement)
    open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
    print("Fixed WebView inline render!")
else:
    print("Target not found.")

