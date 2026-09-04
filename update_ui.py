import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target = """                OutlinedButton(
                    onClick = { viewModel.clearData(context) },
                    modifier = Modifier.weight(1f).height(50.dp)
                ) {
                    Text("Clear")
                }
            }

            Button(
                onClick = { 
                    focusManager.clearFocus()"""

replacement = """                OutlinedButton(
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
                    focusManager.clearFocus()"""

if target in content:
    content = content.replace(target, replacement)
    open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
    print("Updated UI with switch")
else:
    print("Target not found.")

