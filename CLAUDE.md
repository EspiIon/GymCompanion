# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Open the project root in **Android Studio Ladybug or newer**. Gradle syncs automatically.

```bash
# Assemble debug APK
./gradlew assembleDebug

# Install on connected device/emulator
./gradlew installDebug

# Run all tests
./gradlew test

# Lint
./gradlew lint
```

Minimum SDK: 28 — always test on API 28+ emulators.

## Architecture

**MVVM + Repository + Hilt DI**, fully reactive with Kotlin `Flow` / `StateFlow`.

```
data/model/     → Room @Entity classes (FoodEntry, WorkoutSession, StepRecord, BodyRecord, Streak, DailyGoal)
data/db/        → Room DAOs returning Flow<T> + AppDatabase
data/repository/GymRepository.kt → single source of truth, injected @Singleton
viewmodel/      → one HiltViewModel per screen, exposes StateFlow<UiState>
ui/screens/     → one Composable screen per tab (dashboard, nutrition, workout, steps, body)
ui/components/  → shared Compose components (GlassCard, CircularProgressCard, MacroBar, StreakBadge…)
ui/theme/       → Material 3 dark color scheme (Theme.kt) + typography (Type.kt)
ui/navigation/  → sealed Screen class + bottomNavItems list
```

**DI wiring:** `GymCompanionApp.kt` holds `@HiltAndroidApp` and the `@Module` that provides the Room database and all DAOs. ViewModels use `@HiltViewModel` + `@Inject constructor`.

**Navigation:** single `NavHost` in `MainActivity.kt` with fade transitions. Bottom nav is a custom floating pill built from `NavigationBar` + `GlassCard` styling — not a standard `NavigationBar` scaffold placement.

**Streak logic** lives inside each ViewModel (`NutritionViewModel`, `WorkoutViewModel`) — it compares `lastActivityDate` to today/yesterday and increments or resets `Streak.currentStreak`.

## Key design tokens

| Token | Value | Usage |
|---|---|---|
| `Background` | `#0D0D14` | Screen background |
| `Primary` | `#6C63FF` | Nutrition, primary actions |
| `Secondary` | `#00D4AA` | Steps, confirmations |
| `StreakColor` | `#FF8C42` | Streak badge, workout calories |
| `CalorieColor` | `Secondary` alias | Calorie ring/values |

All cards use `GlassCard` (gradient background + subtle border). Avoid plain `Card` or `Surface` for content cards.

## Adding a new screen

1. Add a `Screen` object in `ui/navigation/Navigation.kt` and append to `bottomNavItems`.
2. Create `ui/screens/<name>/<Name>Screen.kt` with a `@Composable` function.
3. Create `viewmodel/<Name>ViewModel.kt` annotated `@HiltViewModel`.
4. Register the `composable(Screen.<Name>.route)` in the `NavHost` inside `MainActivity.kt`.
5. Add any new DAOs/entities: update `AppDatabase.kt` entities list and bump the version (use `fallbackToDestructiveMigration` for dev).
