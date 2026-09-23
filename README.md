# GymCompanion

Application Android personnelle pour suivre l'entraînement, la nutrition, la composition corporelle et les habitudes. L'interface s'inspire de Nothing OS : fond noir, cartes sobres, chiffres de type matrice de points et accents mesurés.

## Parcours existants

- Tableau de bord, objectifs sous forme de checklist et compagnon Pixel.
- Journal alimentaire, portions, favoris, recherche Open Food Facts, scan de codes-barres et créatine.
- Journal de séances, séries, charges, records estimés et progression des exercices.
- Mesures corporelles, import Basic-Fit, calendrier et photos comparables.
- Pas saisis manuellement, coach IA via OpenRouter, sauvegardes locales et Google Drive.

La synchronisation automatique des pas est incomplète et désactivée. Le parcours de chiffrement des sauvegardes reste à terminer. Voir les limites et travaux prévus dans la [feuille de route](docs/ROADMAP.md).

## Démarrer

Ouvrir le projet dans Android Studio avec un SDK compatible avec `compileSdk` dans `app/build.gradle.kts`. La dernière compilation Kotlin a réussi le 23 septembre 2026 avec JDK 21, Gradle 9.4.1 et la plateforme Android 36. Cela ne vaut pas validation sur appareil.

Les commandes et les vérifications sont dans le [guide de développement](docs/DEVELOPMENT.md). Le projet possède actuellement le wrapper Windows ; le guide explique aussi comment appeler le wrapper Java sous Linux/macOS.

## Organisation

| Emplacement | Rôle |
| --- | --- |
| `app/src/main/` | Application Android : données, ViewModels, UI et ressources |
| `app/schemas/` | Schémas Room à conserver dans Git |
| `docs/` | Architecture, règles visuelles, plan de travail et idées |
| `design/prototypes/` | Maquettes HTML historiques, consultables dans un navigateur |
| `tools/archive/` | Anciens scripts de retouche ponctuelle, à ne pas exécuter automatiquement |
| `AGENTS.md` | Consignes communes aux agents ; précisions locales dans `data/` et `ui/` |

## Documentation

- [Architecture et responsabilités](docs/ARCHITECTURE.md)
- [Plan de travail et critères de validation](docs/ROADMAP.md)
- [Idées de fonctionnalités](docs/FEATURES.md)
- [Système visuel](docs/DESIGN.md)
- [Développer et vérifier](docs/DEVELOPMENT.md)

Les fichiers générés et les dumps mémoire déjà suivis par Git nécessitent encore un nettoyage de l'index. Le `.gitignore` couvre les futurs fichiers ; il ne retire pas les anciens.
