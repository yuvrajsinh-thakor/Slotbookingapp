import sys

content = open('app/src/main/java/com/example/MainActivity.kt').read()

target = """    var millisecond by mutableStateOf("0")

    var statusMessage by mutableStateOf(\"\")"""

replacement = """    var millisecond by mutableStateOf("0")
    var startImmediately by mutableStateOf(false)

    var statusMessage by mutableStateOf(\"\")"""

if target in content:
    content = content.replace(target, replacement)
    open('app/src/main/java/com/example/MainActivity.kt', 'w').write(content)
    print("Added startImmediately to ViewModel")
else:
    print("Target not found.")

