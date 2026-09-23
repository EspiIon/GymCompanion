# Plan de travail

État au 23 septembre 2026. Ce fichier est la source unique du statut des tâches. « Implémenté » décrit le code ; « validé » exige les vérifications indiquées. Les idées produit sont détaillées dans [FEATURES.md](FEATURES.md).

## Première passe réalisée

Implémenté et compilé, sans validation sur appareil : export/import de la créatine ; préparation des photos avant restauration ; vérifications de version et d'entrées ZIP ; transaction séance/séries ; correction de date et de portions dans l'ajout rapide ; garde-fou sur l'import galerie. Le worker de pas sans persistance est désactivé. `.gitignore` est ajouté, mais les fichiers déjà suivis restent dans l'index.

Vérifications exécutées : `:app:compileDebugKotlin` et `git diff --check` sur les sources modifiées. Les migrations et un aller-retour réel de sauvegarde restent à vérifier.

Réorganisation documentaire réalisée : README, architecture, guide de développement, référence visuelle unique, carnet de fonctionnalités et consignes d'agents communes/locales. Les maquettes et scripts sont rangés dans `design/prototypes/` et `tools/archive/`. Vérification : 33 liens locaux valides et contenu des 11 fichiers déplacés conservé à l'identique ; diff documentaire sans erreur d'espacement. Le nettoyage de l'index Git reste D01.

## Fiabilité — à faire avant une diffusion

| ID | Travail | Critères de fin |
| --- | --- | --- |
| R01 | Tester la restauration et les anciennes sauvegardes | Toutes les tables et photos reviennent ; une archive invalide laisse les données existantes intactes ; champs absents interprétés selon la version, sans masquer une archive tronquée |
| R02 | Rendre les sauvegardes et leur publication robustes | Instantané cohérent des tables, écriture temporaire puis remplacement du fichier ; échecs Drive visibles ; échecs entre Room, fichiers et réglages traités explicitement |
| R03 | Terminer le chiffrement et la récupération | Format authentifié et versionné ; export manuel, automatique et Drive cohérents ; restauration sur nouvel appareil avec mot de passe ; erreur claire si mot de passe incorrect ; compatibilité ancienne explicitée |
| R04 | Tester et unifier les portions nutritionnelles | Poids, unités et macros cohérents à l'ajout et à l'édition, y compris virgules décimales, zéro, valeurs négatives et changement de mode |
| R05 | Vérifier Room et les données liées | Migrations testées, classement fiable de plusieurs relevés du même jour, pas de séance partielle ni de série orpheline ; revoir le fallback destructif au downgrade |
| R06 | Fournir de vrais états d'erreur | Distinguer panne réseau et aliment introuvable ; import galerie et enregistrement échoués visibles ; aucun message de succès prématuré |

## Usage, performance et interface

État au 23 septembre 2026 : première passe U02/U03 implémentée et compilée, sans validation sur appareil. Un bandeau persistant remplace la page du compagnon ; il ouvre une fiche flottante et utilise une mascotte Canvas. Les graphiques principaux du poids et de progression sont désormais tactiles et plus hauts. Les petites libellés du parcours modifié visent 12 sp. Le rendu sur appareil, le contraste, la grande police et TalkBack restent à valider.

| ID | Travail | Critères de fin |
| --- | --- | --- |
| U01 | Implémenter le suivi automatique des pas | Source choisie et documentée, permissions, disponibilité, doublons, redémarrage et changement de jour gérés ; saisie manuelle toujours explicite |
| U02 | Vérifier la lisibilité Nothing | Contrastes, textes importants, grandes polices, petit écran, clavier et TalkBack vérifiés sur les parcours principaux |
| U03 | Simplifier les écrans et calculs | Décomposer les longs écrans lors de leurs modifications ; centraliser les objectifs nutritionnels et calculs communs ; éviter les cartes imbriquées et les couleurs hors thème |
| U04 | Limiter le travail sur l'historique | Requêtes ciblées pour les records et les séries ; transformations lourdes hors du thread UI ; mesurer avant/après avec un historique conséquent |
| U05 | Sécuriser l'état d'une séance | Brouillon récupérable et minuteur fiable après navigation, mise en arrière-plan et recréation de l'activité |

## Dépôt et vérification

| ID | Travail | Critères de fin |
| --- | --- | --- |
| D01 | Nettoyer l'index Git | Caches, sorties, dumps et réglages locaux retirés de l'index sans effacer les fichiers de travail ; vérifier séparément si l'historique doit être nettoyé |
| D02 | Compléter le démarrage et l'automatisation | Wrapper Unix restauré, environnement documenté, tests ciblés configurés ; CI de compilation, tests et lint après résolution des problèmes existants |
| D03 | Valider sur Android | Parcours ajout/édition/suppression, scan, photo, import et sauvegarde testés sur API 28+ et une version récente ; résultats consignés |

## Ordre proposé

1. R01–R03 : garantir la récupération des données.
2. R04–R06, D02–D03 : couvrir les erreurs et les régressions.
3. U01–U02 : fiabiliser les pas et la lecture des écrans.
4. F01 puis F02 puis F03 : modèles de séances, habitudes, repas réutilisables.

D01 peut être traité indépendamment. U03–U05 peuvent accompagner les parcours concernés. Une demande explicite de l'utilisateur peut modifier cet ordre.

## Mise à jour d'une tâche

Conserver l'identifiant, décrire le comportement obtenu et consigner les vérifications réellement exécutées. Si un test sur appareil manque, le signaler plutôt que marquer la tâche entièrement validée. Ajouter une dépendance ou une limite dans la tâche concernée au lieu de créer une deuxième liste concurrente.
