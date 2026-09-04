import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

dialog = """
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
"""

old_end = """            Button(
                onClick = { checkPassword() }, 
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = lockoutTimer == 0
            ) {
                Text("Unlock App")
            }
        }
    }
}"""

new_end = """            Button(
                onClick = { checkPassword() }, 
                modifier = Modifier.fillMaxWidth().height(50.dp),
                enabled = lockoutTimer == 0
            ) {
                Text("Unlock App")
            }
        }
    }
""" + dialog

content = content.replace(old_end, new_end)
open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
print("done")
