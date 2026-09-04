import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

state_code = """
    val context = LocalContext.current
    var showWebViewDialog by remember { mutableStateOf<String?>(null) }
    var currentTimeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        while (true) {
            currentTimeText = sdf.format(java.util.Date())
            delay(1000)
        }
    }
"""

content = content.replace("    val context = LocalContext.current\n    var showWebViewDialog by remember { mutableStateOf<String?>(null) }", state_code)

ui_code = """
            Text("Set Trigger Time (24-hour format)", fontWeight = FontWeight.Bold)
            if (currentTimeText.isNotEmpty()) {
                Text(
                    text = "Current Time: $currentTimeText",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
"""

content = content.replace("            Text(\"Set Trigger Time (24-hour format)\", fontWeight = FontWeight.Bold)", ui_code)

open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
print("done")
