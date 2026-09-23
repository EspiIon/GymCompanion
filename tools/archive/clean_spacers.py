import os

files = [
    r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\dashboard\DashboardScreen.kt",
    r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\nutrition\NutritionScreen.kt",
    r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\goals\GoalsScreen.kt",
    r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\calendar\CalendarScreen.kt",
    r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\steps\StepsScreen.kt",
    r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\body\BodyScreen.kt",
    r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\workout\WorkoutScreen.kt",
]

for path in files:
    if not os.path.exists(path):
        print("SKIP", path)
        continue
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    # Replace large spacers between widgets by compact spacers (nombre d'or ~ 8.dp)
    content = content.replace("Spacer(Modifier.height(26.dp))", "Spacer(Modifier.height(8.dp))")
    content = content.replace("Spacer(Modifier.height(24.dp))", "Spacer(Modifier.height(8.dp))")
    content = content.replace("Spacer(Modifier.height(22.dp))", "Spacer(Modifier.height(8.dp))")
    content = content.replace("Spacer(Modifier.height(20.dp))", "Spacer(Modifier.height(8.dp))")
    with open(path, 'w', encoding='utf-8') as f:
        f.write(content)
    print("CLEANED", os.path.basename(path))
