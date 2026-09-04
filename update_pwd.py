import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target = """        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(tz)
        val dd = cal.get(Calendar.DAY_OF_MONTH)
        val mm = cal.get(Calendar.MONTH) + 1 // 0-indexed month
        val yy = cal.get(Calendar.YEAR) % 100

        val p1 = (dd * 7) % 100
        val p2 = ((dd + mm + yy) * 2) % 100
        val p3 = (dd % 10) * 10 + (dd / 10)
        val p4 = 100 - dd

        val dailyPassword = String.format("%02d%02d%02d%02d", p1, p2, p3, p4)"""

replacement = """        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(tz)
        val dd = cal.get(Calendar.DAY_OF_MONTH)
        val mm = cal.get(Calendar.MONTH) + 1 // 0-indexed month
        val yy = cal.get(Calendar.YEAR) % 100

        val p1 = (dd * 7) % 100
        val p2 = ((dd + mm + yy) * 2) % 100
        val p3 = (dd % 10) * 10 + (dd / 10)
        val p4 = 100 - dd

        val basePasswordStr = String.format("%02d%02d%02d%02d", p1, p2, p3, p4)
        val basePasswordNum = basePasswordStr.toLongOrNull() ?: 0L
        
        val prefix = deviceId.take(4).lowercase()
        var numStr = ""
        for (char in prefix) {
            if (char in '0'..'9') {
                numStr += char
            } else if (char in 'a'..'z') {
                numStr += (char.code - 96).toString()
            }
        }
        val deviceOffset = numStr.toLongOrNull() ?: 0L
        
        val finalPasswordNum = basePasswordNum + deviceOffset
        var finalPasswordStr = finalPasswordNum.toString()
        
        if (finalPasswordStr.length > 8) {
            finalPasswordStr = finalPasswordStr.takeLast(8)
        } else if (finalPasswordStr.length < 8) {
            finalPasswordStr = finalPasswordStr.padStart(8, '0')
        }
        
        val dailyPassword = finalPasswordStr"""

if target in content:
    content = content.replace(target, replacement)
    open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
    print("Updated password logic")
else:
    print("Target not found")
