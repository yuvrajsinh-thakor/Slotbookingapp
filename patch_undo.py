import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

dialog = """    if (showNameDialog) {
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
"""

content = content.replace("        }\n    }\n" + dialog + "\n@OptIn(ExperimentalMaterial3Api::class)", "        }\n    }\n}\n\n@OptIn(ExperimentalMaterial3Api::class)")
open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
print("undone")
