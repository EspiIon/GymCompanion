# Consignes pour l'interface

Ces règles complètent le `AGENTS.md` racine. La référence visuelle est `docs/DESIGN.md` à la racine du dépôt.

- Réutiliser les tokens, cartes et composants partagés ; corriger les écarts progressivement dans le périmètre de la tâche.
- Collecter les états avec le cycle de vie. Garder les calculs métier et écritures hors des Composables.
- Distinguer état éphémère, état à conserver lors d'une recréation et brouillon qui doit survivre à l'arrêt du processus. Choisir `remember`, `rememberSaveable`, ViewModel ou stockage en conséquence.
- Prévoir les états vide, chargement, erreur et réussite ; empêcher les doubles validations pendant une écriture.
- Vérifier clavier, grandes polices, petit écran et accessibilité pour le parcours modifié. Une compilation ne prouve pas la qualité visuelle.
- Donner des clés stables aux listes dynamiques ; extraire les composants lorsqu'ils ont une responsabilité claire, sans imposer un découpage mécanique.
- Déclarer les routes secondaires dans le NavHost ; une nouvelle route ne nécessite pas un nouvel onglet principal.
- Distinguer les estimations et les mesures enregistrées dans les libellés.
