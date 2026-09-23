path = r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\dashboard\DashboardScreen.kt"
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace("; DottedDivider(Modifier.padding(horizontal = PAD))", "")
content = content.replace("DottedDivider(Modifier.padding(horizontal = PAD))", "")
content = content.replace("; DottedDivider(Modifier.padding(horizontal = 24.dp))", "")
content = content.replace("DottedDivider(Modifier.padding(horizontal = 24.dp))", "")
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("DashboardScreen cleaned")
