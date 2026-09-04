import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target = """    private suspend fun scheduleCritical() {
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")"""

replacement = """    private suspend fun scheduleCritical() {
        if (startImmediately) {
            withContext(Dispatchers.Main) {
                statusMessage = "🚀 Starting critical sequence immediately..."
            }
            criticalSequence()
            return
        }

        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")"""

if target in content:
    content = content.replace(target, replacement)
    open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
    print("Updated scheduleCritical")
else:
    print("Target not found.")

