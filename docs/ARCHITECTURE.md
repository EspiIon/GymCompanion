# Architecture

## Organisation actuelle

Un seul module Android `:app`, en Kotlin/Compose. Room conserve les données structurées ; DataStore conserve les préférences ; les photos sont des fichiers du stockage interne. Hilt fournit les dépendances, dont un client OkHttp partagé. WorkManager programme les sauvegardes.

Flux principal : écran Compose → ViewModel → `GymRepository` → DAO Room. Les `Flow` exposés par les DAO alimentent les `StateFlow` des ViewModels, collectés avec le cycle de vie dans les écrans.

| Dossier dans `app/src/main/java/com/gymcompanion/app/` | Responsabilité |
| --- | --- |
| `data/model/` | Entités, types métier et petits calculs purs |
| `data/db/` | Requêtes, schéma Room et migrations |
| `data/repository/` | Accès aux données et transactions reliant plusieurs DAO |
| `data/backup/` | Export, restauration, chiffrement existant et Drive |
| `data/remote/`, `data/importer/` | Services externes et import Basic-Fit |
| `data/sync/` | Ébauche du suivi automatique des pas, actuellement inactive |
| `viewmodel/` | État des écrans et actions utilisateur |
| `ui/screens/`, `ui/components/`, `ui/theme/` | Écrans, composants partagés et tokens visuels |
| `common/`, `notification/` | Horloge applicative et rappels |

`GymCompanionApp.kt` contient l'initialisation et les fournisseurs Hilt. `MainActivity.kt` héberge le `NavHost`. Les routes et les destinations de navigation principale sont dans `ui/navigation/Navigation.kt`. Un écran secondaire n'a pas à être ajouté à la barre principale.

## Invariants

- Les dates persistées utilisent le format ISO `yyyy-MM-dd`. Une date consultée dans l'historique doit rester distincte de la date du jour.
- Les macros de `FoodEntry` représentent la portion enregistrée ; celles de `FoodItem` représentent 100 g. Le poids et le nombre d'unités doivent décrire la même portion.
- Une séance et ses séries doivent être écrites ou supprimées ensemble. Le lien `sessionId` reste actuellement logique, sans clé étrangère Room.
- Une nouvelle donnée persistée doit être prise en compte dans les sauvegardes et les migrations nécessaires.
- Room, DataStore et les fichiers ne forment pas une transaction commune. Une restauration doit gérer explicitement les échecs entre ces étapes.
- Une sauvegarde locale créée n'implique pas un envoi Drive réussi. Exposer ces états séparément.

## Évolution souhaitée

Conserver le module unique tant qu'un découpage ne répond pas à une difficulté concrète. Extraire progressivement les calculs de portions, les objectifs nutritionnels et la validation de sauvegarde dans des fonctions ou classes testables. Découper les longs écrans par composants fonctionnels ; éviter les réorganisations globales mélangées aux corrections métier.

La [feuille de route](ROADMAP.md) porte l'état du travail ; ce document décrit les responsabilités et les invariants.
