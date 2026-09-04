import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target_vm_vars = """    var vehicleClass by mutableStateOf("10001") // 10001: 2-W, 10002: 4-W
    var prefDate by mutableStateOf(getDynamicDefaultDate())"""

replacement_vm_vars = """    var stateCode by mutableStateOf("GJ")
    var allowStateChange by mutableStateOf(false)
    var vehicleClass by mutableStateOf("10001") // 10001: 2-W, 10002: 4-W
    var prefDate by mutableStateOf(getDynamicDefaultDate())"""

if target_vm_vars in content:
    content = content.replace(target_vm_vars, replacement_vm_vars)
    print("Replaced ViewModel vars.")
else:
    print("Could not find ViewModel vars.")

target_save = """            .putString("vehicleClass", vehicleClass)"""
replacement_save = """            .putString("stateCode", stateCode)
            .putBoolean("allowStateChange", allowStateChange)
            .putString("vehicleClass", vehicleClass)"""

if target_save in content:
    content = content.replace(target_save, replacement_save)
    print("Replaced saveData")
else:
    print("Could not find saveData")

target_load = """        appNo = prefs.getString("appNo", "") ?: ""
"""
replacement_load = """        stateCode = prefs.getString("stateCode", "GJ") ?: "GJ"
        allowStateChange = prefs.getBoolean("allowStateChange", false)
        appNo = prefs.getString("appNo", "") ?: ""
"""

if target_load in content:
    content = content.replace(target_load, replacement_load)
    print("Replaced loadData")
else:
    print("Could not find loadData")

target_clear = """    fun clearData(context: Context) {
        val prefs = context.getSharedPreferences("BookingPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()"""

replacement_clear = """    fun clearData(context: Context) {
        val prefs = context.getSharedPreferences("BookingPrefs", Context.MODE_PRIVATE)
        prefs.edit().clear().apply()
        prefs.edit().putString("stateCode", stateCode).putBoolean("allowStateChange", allowStateChange).apply()"""

if target_clear in content:
    content = content.replace(target_clear, replacement_clear)
    print("Replaced clearData")
else:
    print("Could not find clearData")

target_req = """        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/sarathiservice/stateSelectBean.do",
            FormBody.Builder().add("stName", "GJ").build()
        )"""

replacement_req = """        val req = NetworkManager.buildRequest(
            "https://sarathi.parivahan.gov.in/sarathiservice/stateSelectBean.do",
            FormBody.Builder().add("stName", stateCode).build()
        )"""

if target_req in content:
    content = content.replace(target_req, replacement_req)
    print("Replaced state req")
else:
    print("Could not find state req")

open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)

