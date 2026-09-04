package com.example

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import androidx.activity.compose.BackHandler
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.provider.Telephony
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close

import android.webkit.WebView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.heightIn

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.Job
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource

import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.FormBody
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import okhttp3.Request
import java.util.Calendar
import java.util.concurrent.TimeUnit

// --- Network Setup ---
object NetworkManager {
    private val cookieJar = object : CookieJar {
        val cookies = mutableListOf<Cookie>()
        
        override fun saveFromResponse(url: HttpUrl, newCookies: List<Cookie>) {
            val iterator = cookies.iterator()
            while (iterator.hasNext()) {
                val existing = iterator.next()
                for (newCookie in newCookies) {
                    if (existing.name == newCookie.name && existing.domain == newCookie.domain && existing.path == newCookie.path) {
                        iterator.remove()
                        break
                    }
                }
            }
            cookies.addAll(newCookies)
        }
        
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return cookies.filter { it.matches(url) }
        }
    }
    
    var currentUserAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun randomizeDevice() {
        val versions = listOf("120.0.0.0", "121.0.0.0", "122.0.0.0", "123.0.0.0", "124.0.0.0", "125.0.0.0", "126.0.0.0", "127.0.0.0")
        val osList = listOf(
            "Windows NT 10.0; Win64; x64",
            "Macintosh; Intel Mac OS X 10_15_7",
            "X11; Linux x86_64"
        )
        currentUserAgent = "Mozilla/5.0 (${osList.random()}) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/${versions.random()} Safari/537.36"
    }

    fun clearCookies() {
        cookieJar.cookies.clear()
        randomizeDevice()
    }

    // Optimize OkHttpClient for ultra-fast sequential connections
    val client = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .connectionPool(okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES))
        .build()

    fun buildRequest(url: String, formBody: FormBody? = null): Request {
        val builder = Request.Builder().url(url)
        if (formBody != null) {
            builder.post(formBody)
        }
        builder.addHeader("User-Agent", currentUserAgent)
        builder.addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
        builder.addHeader("Accept-Language", "en-US,en;q=0.9,gu;q=0.8,hi;q=0.7")
        builder.addHeader("Referer", "https://sarathi.parivahan.gov.in/sarathiservice/stateSelection.do")
        builder.addHeader("Connection", "keep-alive")
        return builder.build()
    }
}

// --- ViewModel ---

data class LogEntry(val text: String, val html: String? = null)

class BookingViewModel : ViewModel() {
    // Cleared defaults as requested
    var appNo by mutableStateOf("")
    var dob by mutableStateOf("")
    
    var stateCode by mutableStateOf("GJ")
    var allowStateChange by mutableStateOf(false)
    var vehicleClass by mutableStateOf("10001") // 10001: 2-W, 10002: 4-W
    var prefDate by mutableStateOf(getDynamicDefaultDate())
    var prefTime by mutableStateOf("16.00-17.00")
    var captcha by mutableStateOf("")
    var requireTrackSelection by mutableStateOf(false)
    var lastHtmlResponse by mutableStateOf<String?>(null)
    var manualTrackCode by mutableStateOf("")

    private fun getDynamicDefaultDate(): String {
        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val cal = Calendar.getInstance(tz)
        cal.add(Calendar.DAY_OF_YEAR, 29)
        val sdf = java.text.SimpleDateFormat("dd-MM-yyyy", java.util.Locale.getDefault())
        sdf.timeZone = tz
        return sdf.format(cal.time)
    }

    var hour by mutableStateOf("8")
    var minute by mutableStateOf("0")
    var second by mutableStateOf("0")
    var millisecond by mutableStateOf("0")
    var startImmediately by mutableStateOf(false)

    var statusMessage by mutableStateOf("")
    var isRunning by mutableStateOf(false)
    var isWaitingForOtp by mutableStateOf(false)
    var isCustomTime by mutableStateOf(false)
    var currentJob: Job? = null

    var otpReceived by mutableStateOf("")
    val debugLogs = androidx.compose.runtime.mutableStateListOf<LogEntry>()

    fun logDebug(msg: String, html: String? = null) {
        androidx.compose.runtime.snapshots.Snapshot.withMutableSnapshot {
            debugLogs.add(LogEntry(msg, html))
        }
    }

    private var otpDeferred = CompletableDeferred<String>()

    var lastError by mutableStateOf("")

    init {
        generateCaptcha()
    }

    fun generateCaptcha() {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        captcha = (1..6).map { chars.random() }.joinToString("")
    }

    fun setOtp(otp: String) {
        val cleanOtp = otp.trim()
        otpReceived = cleanOtp
        statusMessage = "✅ OTP Auto-detected: $cleanOtp"
        if (!otpDeferred.isCompleted) {
            otpDeferred.complete(cleanOtp)
        }
    }
    
    fun resetSession() {
        currentJob?.cancel()
        isRunning = false
        isWaitingForOtp = false
        statusMessage = ""
        otpReceived = ""
        debugLogs.clear()
        lastHtmlResponse = null
    }

    fun submitManualOtp(otp: String) {
        val cleanOtp = otp.trim()
        if (!otpDeferred.isCompleted) {
            otpReceived = cleanOtp
            statusMessage = "✅ Manual OTP submitted: $cleanOtp"
            otpDeferred.complete(cleanOtp)
        }
    }

    fun startBooking() {
        if (isRunning) return
        isRunning = true
        isWaitingForOtp = false
        otpDeferred = CompletableDeferred()

        currentJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) { 
                    statusMessage = "🔄 Preparing session..." 
                    debugLogs.clear()
        lastHtmlResponse = null
                }
                NetworkManager.clearCookies()
                

                if (!step1SelectState()) {
                    withContext(Dispatchers.Main) { statusMessage = "❌ State selection failed: $lastError" }
                    return@launch
                }
                
                if (!step2LoadBookingPage()) {
                    withContext(Dispatchers.Main) { statusMessage = "❌ Failed to load booking page: $lastError" }
                    return@launch
                }

                withContext(Dispatchers.Main) { statusMessage = "✅ Session prepared. Scheduling..." }

                scheduleCritical()
            } finally {
                isRunning = false
            }
        }
    }

    private suspend fun scheduleCritical() {
        if (startImmediately) {
            withContext(Dispatchers.Main) {
                statusMessage = "🚀 Starting critical sequence immediately..."
            }
            criticalSequence()
            return
        }

        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        val now = Calendar.getInstance(tz)
        val target = Calendar.getInstance(tz).apply {
            set(Calendar.HOUR_OF_DAY, hour.toIntOrNull() ?: 8)
            set(Calendar.MINUTE, minute.toIntOrNull() ?: 0)
            set(Calendar.SECOND, second.toIntOrNull() ?: 0)
            set(Calendar.MILLISECOND, millisecond.toIntOrNull() ?: 0)
        }

        if (target.before(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        val delayMs = target.timeInMillis - now.timeInMillis

        val sdfDisplay = java.text.SimpleDateFormat("EEE MMM dd, HH:mm:ss z", java.util.Locale.getDefault())
        sdfDisplay.timeZone = tz
        val formattedTime = sdfDisplay.format(target.time)

        withContext(Dispatchers.Main) {
            statusMessage = "⏳ Scheduled for $formattedTime. Waiting ${delayMs / 1000}s..."
        }

        if (delayMs > 0) {
            delay(delayMs)
        }

        criticalSequence()
    }

    private suspend fun criticalSequence() {
        withContext(Dispatchers.Main) { statusMessage = "🚀 Fast sequence started..." }
        // Do NOT clear cookies here! It would destroy the session established in Steps 1 & 2.

        // BATCH EXECUTION: Execute step 3 through 7 sequentially without jumping back to Main thread
        // This eliminates coroutine context switching overhead, making the sequence ultra-fast
        val preOtpSuccess = try {
            if (!step3SubmitApplication()) throw Exception("Step 3 Failed")
            
            // PARALLEL EXECUTION: Run independent network requests simultaneously
            // This slashes the waiting time by overlapping the server response delays
            kotlinx.coroutines.coroutineScope {
                val jobA = async(Dispatchers.IO) { 
                    val r3b = step3bFetchTrkDet()
                    val r4 = step4SelectCov()
                    r3b && r4
                }
                val jobB = async(Dispatchers.IO) { 
                    val r5 = step5FetchSlotInfo()
                    val r6 = if (r5) step6FetchSlotDetails() else false
                    r5 && r6
                }
                
                val resA = jobA.await()
                val resB = jobB.await()
                
                if (!resA) throw Exception("Step 3b or 4 Failed")
                if (!resB) throw Exception("Step 5 or 6 Failed")
            }
            
            if (!step7BookSlot()) throw Exception("Step 7 Failed")
            true
        } catch(e: Exception) {
            false
        }

        if (!preOtpSuccess) {
            withContext(Dispatchers.Main) { statusMessage = "❌ Network failure in Step 3-7: $lastError" }
            return
        }

        isWaitingForOtp = true
        withContext(Dispatchers.Main) { statusMessage = "⏳ Waiting for OTP (Auto SMS or Manual)..." }

        val otp = withTimeoutOrNull(90_000) {
            otpDeferred.await()
        }
        isWaitingForOtp = false

        if (otp == null) {
            withContext(Dispatchers.Main) { statusMessage = "❌ OTP timeout" }
            return
        }

        withContext(Dispatchers.Main) { statusMessage = "✅ Submitting OTP: $otp..." }

        // BATCH EXECUTION: Execute step 8 and 9 rapidly
        val finalSuccess = try {
            
            if (!step8SubmitOtp(otp)) throw Exception("Step 8 Failed")
            
            if (!step9Confirm()) throw Exception("Step 9 Failed")
            true
        } catch(e: Exception) {
            false
        }

        if (!finalSuccess) {
            withContext(Dispatchers.Main) { statusMessage = "❌ Final confirmation failed: $lastError" }
            return
        }

        withContext(Dispatchers.Main) { statusMessage = "🎉 Booking SUCCESSFUL!" }
    }

    // --- HTTP Steps ---
    private fun executeRequestWithResponse(req: Request): Pair<Boolean, String> {
        return try {
            val response = NetworkManager.client.newCall(req).execute()
            var success = response.isSuccessful
            val bodyString = response.body?.string() ?: ""
            val finalUrl = response.request.url.toString()
            
            val method = req.method
            logDebug("\n\u27a1\ufe0f [$method] ${req.url}")
            if (req.body is okhttp3.FormBody) {
                val fb = req.body as okhttp3.FormBody
                val params = mutableListOf<String>()
                for (i in 0 until fb.size) {
                    params.add("${fb.name(i)}=${fb.value(i)}")
                }
                logDebug("Payload: ${params.joinToString("&")}")
            }
            val timeStamp = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()).apply { timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata") }.format(java.util.Date())
            logDebug("\u2b05\ufe0f HTTP ${response.code} - [$timeStamp]")
            logDebug("Response received. Check UI for HTML.", bodyString)
            lastHtmlResponse = bodyString

            // Check for specific parivahan error pages masquerading as 200 OK
            if (bodyString.contains("Error 503", ignoreCase = true) || bodyString.contains("SSL1001", ignoreCase = true)) {
                val errorMsg = if (bodyString.contains("SSL1001", ignoreCase = true)) "SSL1001: Invalid Portal Request" else "Portal Error Page (503)"
                lastError = errorMsg
                response.close()
                return Pair(false, bodyString)
            }

            // If we were unexpectedly redirected to the start page, it's a session drop/failure
            if (success && !req.url.toString().contains("dlslotbook.do") && (finalUrl.contains("dlslotbook.do") || finalUrl.contains("stateSelection.do"))) {
                success = false
                lastError = "Session dropped - Redirected to start page"
            }
            // Also check for Invalid OTP message
            if (success && req.url.toString().contains("insdlSlotdet.do") && (bodyString.contains("Invalid OTP", ignoreCase = true) || bodyString.contains("expired", ignoreCase = true) || bodyString.contains("not match", ignoreCase = true))) {
                success = false
                lastError = "Invalid or Expired OTP"
            }

            if (!success && lastError.isBlank()) {
                lastError = "HTTP ${response.code}"
            }
            response.close()
            Pair(success, bodyString)
        } catch (e: Exception) {
            e.printStackTrace()
            lastError = e.javaClass.simpleName + ": " + e.message
            Pair(false, "")
        }
    }

    private fun executeRequest(req: Request): Boolean {
        return executeRequestWithResponse(req).first
    }

    private fun step1SelectState(): Boolean {
        // Step 0: Initialize session cookies
        try {
            val initReq = NetworkManager.buildRequest("https://sarathi.parivahan.gov.in/sarathiservice/stateSelection.do")
            NetworkManager.client.newCall(initReq).execute().close()
        } catch (e: Exception) {
            // Ignore initialization errors, proceed to POST
        }

        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/sarathiservice/stateSelectBean.do",
            FormBody.Builder().add("stName", stateCode).build()
        )
        return executeRequest(req)
    }

    private fun step2LoadBookingPage(): Boolean {
        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/dlslotbook.do"
        )
        return executeRequest(req)
    }

    private var trackCode: String = ""
    private var actualSlotString = ""

    private fun step3SubmitApplication(): Boolean {
        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/dldetsubmit.do",
            FormBody.Builder()
                .add("subtype", "1")
                .add("applno", appNo)
                .add("llno", "")
                .add("dob", dob)
                .add("uName", "")
                .add("hexUsrid", "")
                .add("captcha", captcha)
                .add("+++SAVE+++", "++SUBMIT++")
                .build()
        )
        val (success, body) = executeRequestWithResponse(req)
        if (success) {


            // Mimic Python's BeautifulSoup logic for track selection
            val selectRegex = Regex("""<select[^>]*id=["']trackName["'][^>]*>(.*?)</select>""", RegexOption.DOT_MATCHES_ALL)
            val selectMatch = selectRegex.find(body)
            if (selectMatch != null) {
                val optionsStr = selectMatch.groupValues[1]
                val optionRegex = Regex("""<option[^>]*value=["']([^"]+)["'][^>]*>""")
                val options = optionRegex.findAll(optionsStr).toList()
                if (options.size > 1) {
                    trackCode = options[1].groupValues[1]
                }
            } else {
                val regex2 = Regex("""trckCd=([A-Z0-9]+)""")
                val match2 = regex2.find(body)
                if (match2 != null) {
                    trackCode = match2.groupValues[1] + "   "
                }
            }
        }
        return success
    }

    private fun step3bFetchTrkDet(): Boolean {
        if (trackCode.isBlank() || trackCode == "-1") return true
        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/fetchtrkdet.do?trckCd=${trackCode.trim()}",
            FormBody.Builder()
                .add("trackCode", trackCode.trim() + "+++")
                .build()
        )
        return executeRequest(req)
    }

    private fun step4SelectCov(): Boolean {
        val builder = FormBody.Builder()
        builder.add("iscov", vehicleClass)
        builder.add("__checkbox_iscov", vehicleClass)
        builder.add("covcd", "$vehicleClass,")
        builder.add("trkcd", trackCode.trim())
        builder.add("method:proceedBookslot", "++PROCEED TO BOOK++")

        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/proceeddlapmnt.do",
            builder.build()
        )
        return executeRequest(req)
    }

    private fun step5FetchSlotInfo(): Boolean {
        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/fetchdlslotinfo.do?date=$prefDate"
        )
        val (success, body) = executeRequestWithResponse(req)
        if (success) {
            val prefCovStr = if (vehicleClass == "10001") "2-WHEELER" else "4-WHEELER"
            val regex = Regex("""$prefTime,$prefCovStr,\d+""")
            val match = regex.find(body)
            if (match != null) {
                actualSlotString = match.value
                logDebug("🎯 Found matching available slot quota: $actualSlotString")
            } else {
                actualSlotString = "$prefTime,$prefCovStr,20"
                logDebug("ℹ️ Defaulted slot quota string: $actualSlotString")
            }
        }
        return success
    }

    private fun step6FetchSlotDetails(): Boolean {
        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/fetchdlslotdetinfo.do"
        )
        return executeRequest(req)
    }

    private fun step7BookSlot(): Boolean {
        val prefCovStr = if (vehicleClass == "10001") "2-WHEELER" else "4-WHEELER"
        val slotStr = if (actualSlotString.isNotBlank()) actualSlotString else "$prefTime,$prefCovStr,20"
        
        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/dlsltprev.do",
            FormBody.Builder()
                .add(prefCovStr, slotStr)
                .add("bookslotstr", "$slotStr;")
                .add("save", "++BOOK SLOT++")
                .build()
        )
        return executeRequest(req)
    }

    private fun step8SubmitOtp(otp: String): Boolean {
        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/insdlSlotdet.do",
            FormBody.Builder()
                .add("smsCode", otp)
                .add("slotcnfrmbtn", "Submit")
                .build()
        )
        val (success, body) = executeRequestWithResponse(req)
        if (success) {
            if (body.contains("Invalid OTP", ignoreCase = true) || 
                body.contains("expired", ignoreCase = true) || 
                body.contains("not match", ignoreCase = true) ||
                body.contains("No Slot Available", ignoreCase = true)) {
                lastError = "OTP verification failed or no slot available"
                return false
            }
        }
        return success
    }

    private fun step9Confirm(): Boolean {
        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/slots/viewDlSlotBookDet.do"
        )
        val (success, body) = executeRequestWithResponse(req)
        if (success) {
            if (body.contains("Error", ignoreCase = true) && !body.contains("Dear", ignoreCase = true)) {
                lastError = "Final appointment confirmation failed on portal"
                return false
            }
        }
        return success
    }

    fun saveData(context: Context) {
        val prefs = context.getSharedPreferences("BookingPrefs", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("appNo", appNo)
            .putString("dob", dob)
            .putString("stateCode", stateCode)
            .putBoolean("allowStateChange", allowStateChange)
            .putString("vehicleClass", vehicleClass)
            .putString("prefDate", prefDate)
            .putString("prefTime", prefTime)
            .putString("hour", hour)
            .putString("minute", minute)
            .putString("second", second)
            .putString("millisecond", millisecond)
            .putBoolean("requireTrackSelection", requireTrackSelection)
            .putString("manualTrackCode", manualTrackCode)
            .apply()
        statusMessage = "✅ Data saved locally"
    }

    fun loadData(context: Context) {
        val prefs = context.getSharedPreferences("BookingPrefs", Context.MODE_PRIVATE)
        stateCode = prefs.getString("stateCode", "GJ") ?: "GJ"
        allowStateChange = prefs.getBoolean("allowStateChange", false)
        appNo = prefs.getString("appNo", "") ?: ""
        dob = prefs.getString("dob", "") ?: ""
        vehicleClass = prefs.getString("vehicleClass", "10001") ?: "10001"
        prefDate = prefs.getString("prefDate", getDynamicDefaultDate()) ?: getDynamicDefaultDate()
        prefTime = prefs.getString("prefTime", "16.00-17.00") ?: "16.00-17.00"
        hour = prefs.getString("hour", "8") ?: "8"
        minute = prefs.getString("minute", "0") ?: "0"
        second = prefs.getString("second", "0") ?: "0"
        millisecond = prefs.getString("millisecond", "0") ?: "0"
        requireTrackSelection = prefs.getBoolean("requireTrackSelection", false)
        manualTrackCode = prefs.getString("manualTrackCode", "") ?: ""
    }

    fun clearData(context: Context) {
        val prefs = context.getSharedPreferences("BookingPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefs.edit().putString("stateCode", stateCode).putBoolean("allowStateChange", allowStateChange).apply()
        appNo = ""
        dob = ""
        vehicleClass = "10001"
        prefDate = getDynamicDefaultDate()
        prefTime = "16.00-17.00"
        hour = "8"
        minute = "0"
        second = "0"
        millisecond = "0"
        statusMessage = "🧹 Form cleared to defaults"
    }
}

// --- Compose UI ---

@Composable
fun LoginScreen(onLoginSuccess: () -> Unit) {

    val context = LocalContext.current
    var showWebViewDialog by remember { mutableStateOf<String?>(null) }

    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var wrongAttemptCount by remember { mutableIntStateOf(0) }
    var lockoutTimer by remember { mutableIntStateOf(0) }
    var waitingForApproval by remember { mutableStateOf(false) }
    var deviceId by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }
    var showNameDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deviceId = android.provider.Settings.Secure.getString(context.contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
    }

    LaunchedEffect(lockoutTimer) {
        if (lockoutTimer > 0) {
            delay(1000)
            lockoutTimer--
        }
    }

    LaunchedEffect(waitingForApproval) {
        if (waitingForApproval) {
            val url = "https://script.google.com/macros/s/AKfycbwA7vFL7ptynJu5ZAEGniNaFzO7xyajDz-9qU2D6GbTzi2ZBBY9BmwzH9roYkACn_CB/exec"
            val mediaType = "application/json; charset=utf-8".toMediaType()

            // Step 1: Register the device ID
            try {
                val json = JSONObject().apply {
                    put("deviceId", deviceId)
                    put("userName", userName)
                    put("action", "register")
                }
                val body = json.toString().toRequestBody(mediaType)
                val request = Request.Builder().url(url).post(body).build()
                val response = withContext(Dispatchers.IO) {
                    NetworkManager.client.newCall(request).execute()
                }
                val responseBody = response.body?.string() ?: "{}"
                response.close()
                
                val status = JSONObject(responseBody).optString("status", "")
                if (status == "APPROVED") {
                    context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                        .edit().putBoolean("isUnlocked", true).apply()
                    onLoginSuccess()
                    return@LaunchedEffect
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // Step 2: Poll for status check every 15 seconds
            while (waitingForApproval) {
                delay(60_000)
                try {
                    val json = JSONObject().apply {
                        put("deviceId", deviceId)
                        put("userName", userName)
                        put("action", "check")
                    }
                    val body = json.toString().toRequestBody(mediaType)
                    val request = Request.Builder().url(url).post(body).build()
                    val response = withContext(Dispatchers.IO) {
                        NetworkManager.client.newCall(request).execute()
                    }
                    val responseBody = response.body?.string() ?: "{}"
                    response.close()
                    
                    val status = JSONObject(responseBody).optString("status", "")
                    if (status == "APPROVED") {
                        context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                            .edit().putBoolean("isUnlocked", true).apply()
                        onLoginSuccess()
                        break
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    fun checkPassword() {
        if (lockoutTimer > 0) return

        if (password == "260211091507") {
            context.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                .edit().putBoolean("isUnlocked", true).apply()
            onLoginSuccess()
            return
        }

        if (password == "202026") {
            showNameDialog = true
            return
        }

        val tz = java.util.TimeZone.getTimeZone("Asia/Kolkata")
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
        
        val dailyPassword = finalPasswordStr

        if (password == "15071980" || password == dailyPassword) {
            wrongAttemptCount = 0
            onLoginSuccess()
        } else {
            error = true
            wrongAttemptCount++
            if (wrongAttemptCount >= 3) {
                lockoutTimer = 20
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (waitingForApproval) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Waiting for Admin Approval...", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Device ID: $deviceId\nName: $userName", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Wait for 1 minutes", style = MaterialTheme.typography.bodySmall)
        } else {
            Text("Security Gateway", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Enter today's daily code to access.", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; error = false },
                label = { Text("Password") },
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = error || lockoutTimer > 0,
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = lockoutTimer == 0
            )
            if (error && lockoutTimer == 0) {
                Text(
                    "Invalid Password",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            } else if (lockoutTimer > 0) {
                Text(
                    "Too many attempts. Try again in ${lockoutTimer}s",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = { checkPassword() }, 
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = lockoutTimer == 0
            ) {
                Text("Unlock App")
            }
        }
    }

    if (showNameDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Enter Your Name") },
            text = {
                OutlinedTextField(
                    value = userName,
                    onValueChange = { userName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (userName.isNotBlank()) {
                            showNameDialog = false
                            waitingForApproval = true
                        }
                    }
                ) {
                    Text("Submit")
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showNameDialog = false }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownMenuField(
    label: String,
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption) },
                    onClick = {
                        onOptionSelected(selectionOption)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun SmsReceiverEffect(onOtpReceived: (String) -> Unit) {

    val context = LocalContext.current
    var showWebViewDialog by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
                    val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
                    for (sms in messages) {
                        val body = sms.displayMessageBody
                        if (body != null && (body.contains("parivahan", ignoreCase = true) || body.contains("MoRTH", ignoreCase = true) || body.contains("OTP", ignoreCase = true))) {
                            val match = Regex("\\b\\d{6}\\b").find(body)
                            match?.value?.let { otp ->
                                onOtpReceived(otp)
                            }
                        }
                    }
                }
            }
        }
        val filter = IntentFilter(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: BookingViewModel = viewModel()) {
    BackHandler(enabled = viewModel.isRunning) {
        // Prevent accidental back press during sequence
    }

    val context = LocalContext.current
    var showWebViewDialog by remember { mutableStateOf<String?>(null) }
    var currentTimeText by remember { mutableStateOf("") }
    LaunchedEffect(Unit) {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
        sdf.timeZone = java.util.TimeZone.getTimeZone("Asia/Kolkata")
        while (true) {
            currentTimeText = sdf.format(java.util.Date())
            delay(1000)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadData(context)
    }

    // Request SMS Permissions
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            )
        )
    }

    // Register SMS Broadcast Receiver
    SmsReceiverEffect { otp ->
        viewModel.setOtp(otp)
    }

    val timeSlots = listOf(
        "10.30-11.30", "11.30-12.30", "12.30-13.30",
        "14.00-15.00", "15.00-16.00", "16.00-17.00", "17.00-18.00"
    )
    val vehicleOptions = listOf("2-Wheeler" to "10001", "4-Wheeler" to "10002")
    var manualOtpInput by remember { mutableStateOf("") }
    

    val focusManager = LocalFocusManager.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slot Booker Ultra-Fast") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = { focusManager.clearFocus() })
                }
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Enable State Change",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = viewModel.allowStateChange,
                    onCheckedChange = { viewModel.allowStateChange = it },
                    enabled = !viewModel.isRunning
                )
            }
            
            val ALL_STATES_MAP = mapOf("AN" to "Andaman and Nicobar", "AP" to "Andhra Pradesh", "AR" to "Arunachal Pradesh", "AS" to "Assam", "BR" to "Bihar", "CH" to "Chandigarh", "CG" to "Chhattisgarh", "DL" to "Delhi", "GA" to "Goa", "GJ" to "Gujarat", "HR" to "Haryana", "HP" to "Himachal Pradesh", "JK" to "Jammu and Kashmir", "JH" to "Jharkhand", "KA" to "Karnataka", "KL" to "Kerala", "LA" to "Ladakh", "LD" to "Lakshadweep(UT)", "MP" to "Madhya Pradesh", "MH" to "Maharashtra", "MN" to "Manipur", "ML" to "Meghalaya", "MZ" to "Mizoram", "NL" to "Nagaland", "OD" to "Odisha", "PY" to "Pondicherry", "PB" to "Punjab", "RJ" to "Rajasthan", "SK" to "Sikkim", "TN" to "Tamil Nadu", "TG" to "Telangana", "TR" to "Tripura", "DD" to "UT of DNH and DD", "UK" to "Uttarakhand", "UP" to "Uttar Pradesh", "WB" to "West Bengal")
            val stateNames = ALL_STATES_MAP.values.toList()
            val selectedStateName = ALL_STATES_MAP[viewModel.stateCode] ?: "Gujarat"
            
            if (viewModel.allowStateChange && !viewModel.isRunning) {
                DropdownMenuField(
                    label = "State",
                    options = stateNames,
                    selectedOption = selectedStateName,
                    onOptionSelected = { name -> 
                        val code = ALL_STATES_MAP.entries.firstOrNull { it.value == name }?.key ?: "GJ"
                        viewModel.stateCode = code 
                    }
                )
            } else {
                OutlinedTextField(
                    value = selectedStateName,
                    onValueChange = {},
                    label = { Text("State") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    enabled = false,
                    colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }

            OutlinedTextField(
                value = viewModel.appNo,
                onValueChange = { viewModel.appNo = it },
                label = { Text("Application Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            OutlinedTextField(
                value = viewModel.dob,
                onValueChange = { viewModel.dob = it },
                label = { Text("Date of Birth (DD-MM-YYYY)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            DropdownMenuField(
                label = "Vehicle Class",
                options = vehicleOptions.map { it.first },
                selectedOption = vehicleOptions.find { it.second == viewModel.vehicleClass }?.first ?: "2-Wheeler",
                onOptionSelected = { selected ->
                    viewModel.vehicleClass = vehicleOptions.find { it.first == selected }?.second ?: "10001"
                }
            )
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = viewModel.requireTrackSelection,
                    onCheckedChange = { viewModel.requireTrackSelection = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Requires Track Selection?")
            }
            
            if (viewModel.requireTrackSelection) {
                OutlinedTextField(
                    value = viewModel.manualTrackCode,
                    onValueChange = { viewModel.manualTrackCode = it.uppercase() },
                    label = { Text("Manual Track Code (Optional, e.g. GJ19TRK)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            OutlinedTextField(
                value = viewModel.prefDate,
                onValueChange = { viewModel.prefDate = it },
                label = { Text("Preferred Booking Date (DD-MM-YYYY)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = viewModel.isCustomTime,
                    onCheckedChange = { viewModel.isCustomTime = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Custom Time Input")
            }
            if (viewModel.isCustomTime) {
                OutlinedTextField(
                    value = viewModel.prefTime,
                    onValueChange = { viewModel.prefTime = it },
                    label = { Text("Preferred Time Slot (e.g. 10.30-11.30)") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            } else {
                DropdownMenuField(
                    label = "Preferred Time Slot",
                    options = timeSlots,
                    selectedOption = viewModel.prefTime,
                    onOptionSelected = { 
                        viewModel.prefTime = it
                        focusManager.clearFocus()
                    }
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))


            Text("Set Trigger Time (24-hour format)", fontWeight = FontWeight.Bold)
            if (currentTimeText.isNotEmpty()) {
                Text(
                    text = "Current Time: $currentTimeText",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }


            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = viewModel.hour,
                    onValueChange = { viewModel.hour = it },
                    label = { Text("HH") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = viewModel.minute,
                    onValueChange = { viewModel.minute = it },
                    label = { Text("MM") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = viewModel.second,
                    onValueChange = { viewModel.second = it },
                    label = { Text("SS") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                OutlinedTextField(
                    value = viewModel.millisecond,
                    onValueChange = { viewModel.millisecond = it },
                    label = { Text("ms") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = viewModel.captcha,
                    onValueChange = { viewModel.captcha = it },
                    label = { Text("CAPTCHA") },
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = { viewModel.generateCaptcha() }) {
                    Text("Randomize")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { viewModel.saveData(context) },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Save")
                }
                OutlinedButton(
                    onClick = { viewModel.clearData(context) },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Clear")
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (viewModel.startImmediately) "Mode: Start Immediately" else "Mode: Start at Scheduled Time",
                    style = MaterialTheme.typography.bodyMedium
                )
                Switch(
                    checked = viewModel.startImmediately,
                    onCheckedChange = { viewModel.startImmediately = it },
                    enabled = !viewModel.isRunning
                )
            }

            Button(
                onClick = { 
                    focusManager.clearFocus()
                    viewModel.startBooking() 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .height(50.dp),
                enabled = !viewModel.isRunning
            ) {
                Text("Start Sequence")
            }
            if (viewModel.isRunning || viewModel.statusMessage.isNotEmpty()) {
                OutlinedButton(
                    onClick = { 
                        focusManager.clearFocus()
                        viewModel.resetSession() 
                    },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp).height(50.dp)
                ) {
                    Text("Start Over")
                }
            }

            if (viewModel.statusMessage.isNotEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = viewModel.statusMessage,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            Text("Manual OTP Entry", fontWeight = FontWeight.Bold)
            Text(
                "Use this if booking for someone else and you receive the OTP manually.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = manualOtpInput,
                    onValueChange = { newValue -> 
                        val filtered = newValue.filter { it.isDigit() }
                        if (filtered.length <= 6) {
                            manualOtpInput = filtered
                        }
                    },
                    label = { Text("Enter OTP here") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Button(
                    onClick = { 
                        viewModel.submitManualOtp(manualOtpInput) 
                        manualOtpInput = ""
                    },
                    enabled = viewModel.isRunning && manualOtpInput.isNotBlank()
                ) {
                    Text("Submit OTP")
                }
            }

Spacer(modifier = Modifier.height(16.dp))
            Text("Debug Console", style = MaterialTheme.typography.titleMedium)
            androidx.compose.foundation.text.selection.SelectionContainer {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .background(Color.Black, shape = RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(viewModel.debugLogs.size) { index ->
                            val log = viewModel.debugLogs[index]
                            Column {
                                Text(
                                    text = log.text,
                                    color = Color.Green,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                if (log.html != null) {
                                    Button(
                                        onClick = { showWebViewDialog = log.html },
                                        modifier = Modifier.padding(top = 4.dp).height(32.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                                    ) {
                                        Text("Preview Step", fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val html = viewModel.lastHtmlResponse
            if (html != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { showWebViewDialog = html },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Latest Server Response HTML")
                }
            }
        }
    }

    if (showWebViewDialog != null) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showWebViewDialog = null },
            properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Step Preview",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                        IconButton(onClick = { showWebViewDialog = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }
                    AndroidView(
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
                    )
                }
            }
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(Unit) {
                        val prefs = applicationContext.getSharedPreferences("AuthPrefs", Context.MODE_PRIVATE)
                        if (prefs.getBoolean("isUnlocked", false)) {
                            isAuthenticated = true
                        }
                    }

                    if (isAuthenticated) {
                        MainScreen()
                    } else {
                        LoginScreen(onLoginSuccess = { isAuthenticated = true })
                    }
                }
            }
        }
    }
}
