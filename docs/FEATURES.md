# Idées de fonctionnalités

Ces idées constituent un carnet produit, pas une liste de fonctionnalités déjà implémentées ni un engagement à tout ajouter. Les efforts sont relatifs au dépôt actuel et incluent stockage, UI et vérification.

| Idée | Usage concret | Première version utile | Effort estimé |
| --- | --- | --- | --- |
| Modèles de séances | Préparer un Push/Pull/Legs puis démarrer sans tout ressaisir | Exercices et séries préremplis ; dernières charges visibles | Moyen |
| Séance en cours récupérable | Reprendre après fermeture de l'app ou appel téléphonique | Brouillon persistant, validation série par série, repos basé sur une heure de fin | Important |
| Tâches récurrentes | Répéter une habitude certains jours sans recréer la tâche | Fréquence, cases quotidiennes et historique de réalisation | Moyen |
| Repas enregistrés et recettes | Ajouter un petit-déjeuner habituel ou une portion d'un plat | Groupe d'aliments réutilisable ; rendement et portion pour les recettes | Moyen |
| Copier un repas | Réutiliser rapidement un repas d'hier | Copie vers une date et un repas choisis, portions modifiables | Faible |
| Bilan hebdomadaire | Comprendre sa régularité d'un coup d'œil | Séances, volume, moyennes nutritionnelles et tendance du poids ; jours manquants explicites | Moyen |
| Accueil personnalisable | Montrer les modules réellement utilisés | Choisir et ordonner les widgets ; masquer Pixel ou l'IA | Moyen |
| Suivi de progression lisible | Voir une évolution sans surinterpréter une mesure isolée | Courbes filtrables, tendance du poids et notes attachées aux mesures | Moyen |
| Photos guidées | Rendre les comparaisons plus cohérentes | Repères d'alignement, angle de vue et superposition réglable | Moyen |
| Objectifs reliés aux actions | Cocher automatiquement « trois séances cette semaine » | Règles simples basées sur les données, avec historique et correction manuelle | Important |
| Widget Android | Ajouter un repas ou ouvrir la séance depuis l'écran d'accueil | Deux raccourcis et progression du jour, style monochrome | Moyen |
| Coach IA explicable | Comprendre les données utilisées dans une réponse | Aperçu des données envoyées, résumé hebdomadaire explicite et historique maîtrisé | Important |

## Les trois priorités produit proposées

### F01 — Modèles de séances

Créer, renommer, dupliquer et supprimer un modèle. Démarrer une séance en copiant le modèle : une modification ultérieure du modèle ne doit pas modifier l'historique. Afficher les dernières charges comme aide à la saisie, sans les appliquer silencieusement. Inclure les modèles dans les migrations et les sauvegardes.

Validation : démarrer deux séances du même modèle, modifier le modèle ensuite et vérifier que les séances enregistrées restent identiques.

### F02 — Tâches récurrentes

Séparer la définition de l'habitude de ses réalisations datées. Prévoir les jours de semaine, la possibilité de suspendre une habitude et de corriger une réalisation passée. Ne pas perdre les anciennes coches lorsque le jour change. Conserver les objectifs ponctuels existants.

Validation : passage à minuit, consultation d'une ancienne date, modification du planning et restauration du suivi.

### F03 — Copier puis enregistrer des repas

Commencer par copier un repas existant. Étendre ensuite aux groupes d'aliments nommés, puis aux recettes. Toujours copier les valeurs vers le journal pour que l'édition d'une recette ne change pas les repas passés.

Validation : modifier la portion de la copie, vérifier les totaux et confirmer que le repas source reste intact.

## Direction produit

Donner la priorité à une saisie rapide utilisable à la salle, à l'historique fiable et à la lecture de la progression. Garder les fonctions IA et le compagnon facultatifs. Les estimations doivent être identifiées comme telles ; une donnée absente ne doit pas être affichée comme une valeur mesurée de zéro.
