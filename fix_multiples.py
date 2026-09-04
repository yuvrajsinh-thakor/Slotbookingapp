import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

# Replace in LoginScreen
login_code = """@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {

    val context = LocalContext.current
    var showWebViewDialog by remember { mutableStateOf<String?>(null) }
    var currentTimeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        while (true) {
            currentTimeText = sdf.format(java.util.Date())
            delay(1000)
        }
    }"""

login_replacement = """@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {

    val context = LocalContext.current
    var showWebViewDialog by remember { mutableStateOf<String?>(null) }"""

content = content.replace(login_code, login_replacement)

# Replace in SmsReceiverEffect
sms_code = """@Composable
fun SmsReceiverEffect(onOtpReceived: (String) -> Unit) {

    val context = LocalContext.current
    var showWebViewDialog by remember { mutableStateOf<String?>(null) }
    var currentTimeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        while (true) {
            currentTimeText = sdf.format(java.util.Date())
            delay(1000)
        }
    }"""

sms_replacement = """@Composable
fun SmsReceiverEffect(onOtpReceived: (String) -> Unit) {

    val context = LocalContext.current
    var showWebViewDialog by remember { mutableStateOf<String?>(null) }"""

content = content.replace(sms_code, sms_replacement)

open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
print("fixed")
