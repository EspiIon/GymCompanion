# Consignes communes aux agents

## Repères

Application Android Kotlin/Compose, Room, Hilt et DataStore. Lire le [README](README.md), puis les documents utiles : [architecture](docs/ARCHITECTURE.md), [développement](docs/DEVELOPMENT.md), [design](docs/DESIGN.md). L'état du travail est dans [ROADMAP.md](docs/ROADMAP.md) ; les propositions produit sont dans [FEATURES.md](docs/FEATURES.md).

Les instructions de l'utilisateur déterminent le périmètre courant. Le backlog ne constitue pas une autorisation d'implémenter toutes les idées. Les fichiers de `docs/archive/` et `tools/archive/` décrivent un historique, pas des consignes actives.

## Intervention

- Examiner le statut Git des chemins concernés et lire les sources avant de modifier. Préserver les modifications locales antérieures.
- Définir un résultat observable et choisir une tâche cohérente. Éviter de mêler déplacement massif et correction métier.
- Éditer les sources, jamais les sorties de `build/`, `app/build/` ou `.gradle/`. Conserver les schémas Room et le wrapper dans Git.
- Réutiliser les composants et dépendances existants. Ajouter une abstraction ou une dépendance pour un besoin concret.
- Séparer les règles métier des Composables. Garder disque, réseau et calculs lourds hors du thread UI.
- Préserver les données et la compatibilité des sauvegardes ; suivre les consignes locales de `data/AGENTS.md` pour ces changements.
- Utiliser le français pour les textes utilisateur et des noms de code cohérents avec le Kotlin existant.

## Coordination des agents

Si une délégation est utilisée dans le cadre autorisé :

1. Confier une tâche bornée, avec objectif, fichiers attribués, dépendances et résultat attendu.
2. Éviter deux agents en écriture sur le même fichier. Un propriétaire intègre les modifications communes : modèles, DAO, thème, navigation et Gradle.
3. Les tâches indépendantes peuvent avancer en parallèle. Définir le contrat des changements de données avant d'intégrer l'UI qui en dépend.
4. Demander un retour factuel : fichiers changés, vérifications, erreurs et limites. L'agent principal relit l'intégration et effectue les contrôles communs.

Ne pas maintenir des rapports d'agents dupliquant la feuille de route ; mettre à jour la tâche correspondante à la fin.

## Fin d'une intervention

- Exécuter les contrôles adaptés au risque selon `docs/DEVELOPMENT.md`. La documentation se vérifie par les liens et le diff ; une restauration exige des scénarios sur les données.
- Distinguer « implémenté », « compilé » et « validé sur appareil ». Ne pas décrire une vérification prévue comme déjà exécutée.
- Mettre à jour le statut dans `docs/ROADMAP.md` lorsque le comportement ou le travail restant change. Modifier les autres documents si leurs règles ou responsabilités changent.
- Résumer le résultat, les vérifications et les limites concrètes. Ne pas réécrire l'historique Git dans une tâche de code ordinaire.
