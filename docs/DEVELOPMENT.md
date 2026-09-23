# Développer et vérifier

## Environnement

Les versions de référence sont dans `gradle/libs.versions.toml`, `gradle/wrapper/gradle-wrapper.properties` et `app/build.gradle.kts`. Ne pas les mettre à jour uniquement pour reproduire une machine locale.

Configuration vérifiée le 23 septembre 2026 : JDK 21, Gradle 9.4.1, Android SDK Platform 36. La cible JVM des sources est 17 ; Android minimum est API 28. `local.properties` contient actuellement un chemin Windows : configurer le SDK local dans l'IDE ou via `ANDROID_HOME` selon la machine.

## Commandes

Depuis la racine, sous Windows :

```bat
gradlew.bat :app:compileDebugKotlin
gradlew.bat :app:assembleDebug
gradlew.bat :app:lintDebug
```

Sous Linux/macOS, tant que le script `gradlew` manque, définir `JAVA_HOME` et `ANDROID_HOME` vers les installations locales puis utiliser le JAR du wrapper :

```sh
"$JAVA_HOME/bin/java" -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:compileDebugKotlin
"$JAVA_HOME/bin/java" -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:assembleDebug
"$JAVA_HOME/bin/java" -cp gradle/wrapper/gradle-wrapper.jar org.gradle.wrapper.GradleWrapperMain :app:lintDebug
```

Le premier lancement nécessite le téléchargement de Gradle et des dépendances. Un échec de résolution réseau ne démontre pas une erreur Kotlin.

## Niveau de validation

| Changement | Vérification utile |
| --- | --- |
| Documentation, rangement | Liens locaux, conservation des fichiers déplacés, diff ciblé |
| Calcul métier | Tests des entrées limites, virgules décimales, unités et changement de jour |
| Données ou sauvegarde | Aller-retour complet, ancienne version, fichier invalide et échec sans perte des données existantes |
| Schéma Room | Migration depuis les versions prises en charge, données conservées et schéma exporté |
| Compose | Compilation puis scénario sur appareil/émulateur, petit écran, grande police, clavier et TalkBack selon le changement |

Aucun dossier de tests unitaires ou instrumentés n'a été trouvé lors de la première passe. Ajouter des tests ciblés sur les risques corrigés ; une tâche Gradle sans tests ne constitue pas une validation métier. Les commandes futures seront `:app:testDebugUnitTest` et `:app:connectedDebugAndroidTest` une fois les suites configurées.

## Fin d'une tâche

Relire le diff des fichiers concernés, vérifier les erreurs et mettre à jour le statut correspondant dans `ROADMAP.md`. Indiquer la commande réellement exécutée et son résultat, puis les vérifications manuelles restantes. Une compilation réussie ne suffit pas à fermer une tâche de restauration ou d'accessibilité.

## Hygiène Git

Ne pas versionner les caches, sorties de compilation, dumps mémoire ou réglages propres à une machine. Conserver le wrapper Gradle et les schémas Room. Le nettoyage de l'index doit conserver les fichiers de travail locaux ; une éventuelle réécriture de l'historique est une opération distincte. Les scripts de `tools/archive/` contiennent des chemins Windows et des remplacements globaux : ils ne sont pas des commandes de maintenance actuelles.
