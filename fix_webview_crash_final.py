import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target = """                    AndroidView(
                        factory = { context ->
                            WebView(context).apply {
                                setLayerType(android.view.View.LAYER_TYPE_SOFTWARE, null)
                                settings.javaScriptEnabled = true
                                settings.builtInZoomControls = true
                                settings.displayZoomControls = false
                                settings.loadWithOverviewMode = true
                                settings.useWideViewPort = true
                                setBackgroundColor(android.graphics.Color.WHITE)
                            }
                        },
                        update = { webView ->
                            webView.loadDataWithBaseURL(null, showWebViewDialog ?: "", "text/html", "UTF-8", null)
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                    )"""

replacement = """                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White)
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        Text(
                            text = showWebViewDialog ?: "",
                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                            color = Color.Black
                        )
                    }"""

if target in content:
    content = content.replace(target, replacement)
    open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
    print("Replaced WebView with Text completely!")
else:
    print("Target not found.")

