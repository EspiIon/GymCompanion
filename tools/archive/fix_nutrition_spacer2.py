path = r"C:\Users\Loic\Desktop\Dev\GymCompanion\app\src\main\java\com\gymcompanion\app\ui\screens\nutrition\NutritionScreen.kt"
with open(path, 'r', encoding='utf-8') as f:
    content = f.read()
# Add spacer after macros widget (before quickadd block)
content = content.replace('                        }\n                    }\n                }\n\n                // ── Quick-add', '                        }\n                    }\n                }\n                \n                Spacer(Modifier.height(8.dp))\n\n                // ── Quick-add')
# Add spacer after quickadd (before meals)
content = content.replace('                        }\n                    }\n                }\n\n                // ── Meals', '                        }\n                    }\n                }\n                \n                Spacer(Modifier.height(8.dp))\n\n                // ── Meals')
with open(path, 'w', encoding='utf-8') as f:
    f.write(content)
print("Fixed nutrition spacers")
