# Système visuel

## Intention

Une interface inspirée de Nothing OS : fond noir, cartes plates, typographie expressive sur les grands chiffres et informations ordinaires faciles à lire. Le style sert la saisie et le suivi à la salle. Le code existant présente encore des écarts ; ce document fixe la direction des prochaines modifications, sans prétendre qu'elle est déjà appliquée partout.

## Palette et composants

Centraliser les couleurs dans `ui/theme/Theme.kt`. Réutiliser `NothingCard`, `WidgetForm`, `NumText`, `NLabel` et les FAB de `ui/components/Components.kt`.

La palette conserve le contraste Nothing (noir, blanc cassé, gris et surfaces charbon) et utilise des accents fonctionnels plutôt qu'une couleur décorative par carte : orange pour la nutrition, bleu pour l'activité, menthe pour la progression positive et lavande pour la force. Les boutons d'action restent noirs/charbon avec une bordure Nothing ; les couleurs chaudes sont réservées aux données.

| Usage | Référence actuelle | Règle |
| --- | --- | --- |
| Fond | `NothingBlack`, `#000000` | Fond principal |
| Cartes | `NothingCardSurface`, `#171717` | Surface plate, sans bordure extérieure ni ombre |
| Dialogue/FAB | `NothingDeep`, `#111111` | Surface distincte des cartes |
| Champs | `NothingDark2`, `#1A1A1A` | Contrôle identifiable |
| Bordures internes | `NothingBorder`, `NothingBorderMid`, `NothingBorderStrong` | Séparations et contrôles utiles |
| Texte | `NothingWhite`, `NothingGrey1`, `NothingGrey2` | Hiérarchie lisible |
| Rouge | `NothingRed` | État, erreur, suppression ; conserver une indication textuelle |
| Nutrition | `DataOrange`, `DataAmber`, `DataCoral` | Calories et macros : accents chauds, sans colorer les boutons |
| Activité | `DataBlue` | Pas et graphiques d'activité |
| Progression | `DataMint` | Progression positive et validation |
| Force | `DataLavender` | Volume, charge et 1RM |
| Mascotte | `PetCream` et variantes | Couleurs choisies par le compagnon |

Éviter `NothingGrey3` pour une information importante. Ne pas utiliser le bleu sombre pour du petit texte. Une nouvelle couleur plus lisible doit être un token du thème, pas une valeur ajoutée dans un écran. Les couleurs macro existantes restent à harmoniser ; limiter la concurrence entre accents.

## Typographie et accessibilité

- Doto pour les chiffres héros d'au moins 24 sp ; Space Mono pour les petites valeurs et étiquettes ; Space Grotesk pour le texte courant et les titres.
- Les textes actuels de 7–9 sp sont un point à corriger, pas une règle à reproduire. Viser 12 sp ou davantage pour les informations utiles, puis vérifier le rendu à grande taille de police.
- Réserver les majuscules espacées aux libellés courts. Autoriser plusieurs lignes lorsque cela évite de masquer une information.
- Donner une description accessible aux actions représentées uniquement par une icône. Une icône décorative accompagnée d'un texte peut rester sans description.
- Prévoir des zones tactiles confortables, cible de projet : 48 dp minimum pour une action principale, même si l'icône est plus petite.
- Fournir un résumé lisible des graphiques ; ne pas transmettre une information uniquement par une couleur ou un dessin.

## Disposition

- Cartes arrondies à 24 dp, padding interne actuel de 14 dp ; `WidgetForm` fournit les 8 dp de marge horizontale externe.
- Utiliser un espacement cohérent entre widgets, cible actuelle de 10 dp. Éviter les additions de paddings et les cartes imbriquées accidentelles.
- Garder le padding de section de 24 dp pour les textes hors carte. Aligner explicitement chiffres et unités.
- Deux statistiques de même importance prennent des poids égaux. Le nombre d'or n'est pas une contrainte systématique de mise en page.
- Une nouvelle fonctionnalité peut être un écran secondaire : ne pas ajouter automatiquement une destination à la barre principale.

## États et interactions

Le compagnon est une présence partagée dans un bandeau sous la barre d'état, et non une destination à part entière. Son toucher ouvre une fiche flottante compacte pour les réactions, les objectifs et le nom. La mascotte doit rester décorative pour l'action et ne pas masquer les données.

Chaque parcours concerné doit prévoir son état vide, son chargement, son erreur et sa réussite. Les messages expliquent l'action possible. Une absence de mesure n'est pas un zéro mesuré. Après une suppression courante, préférer une possibilité d'annuler lorsque les données le permettent ; la restauration qui remplace un historique doit expliquer clairement son effet.

## Compose et animation

Donner une clé stable aux éléments dynamiques des listes. Mémoriser les transformations qui le nécessitent avec des clés correctes. `animateFloatAsState` ne redémarre pas simplement parce qu'un écran se recompose : corriger une animation sur la base du comportement observé, sans imposer `remember` ou `derivedStateOf` à chaque graphique.

Valider les modifications sur petit écran et avec une grande police. Les [maquettes HTML](../design/prototypes/README.md) sont des références historiques ; elles ne constituent pas une preuve du rendu Android.
