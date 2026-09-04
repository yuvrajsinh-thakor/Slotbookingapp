import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target = 'logDebug("\\u2b05\\ufe0f HTTP ${response.code}")'
replacement = '''val timeStamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }.format(java.util.Date())
            logDebug("\\u2b05\\ufe0f HTTP ${response.code} - [$timeStamp]")'''

if target in content:
    content = content.replace(target, replacement)
    open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
    print("Log updated successfully")
else:
    print("Target not found")
