path = r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\nutrition\NutritionScreen.kt"
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
content = content.replace('                if (favoriteFoods.isNotEmpty() || recentFoods.isNotEmpty()) {\n                    item(key = "quickadd") {', '                if (favoriteFoods.isNotEmpty() || recentFoods.isNotEmpty()) {\n                    item(key = "quickadd") {\n                        Spacer(Modifier.height(8.dp))')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed quickadd spacer")
