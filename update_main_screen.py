import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = viewModel.appNo,"""

replacement = """        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
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
                    enabled = false
                )
            }

            OutlinedTextField(
                value = viewModel.appNo,"""

if target in content:
    content = content.replace(target, replacement)
    open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
    print("Replaced UI properly!")
else:
    print("Target not found.")

