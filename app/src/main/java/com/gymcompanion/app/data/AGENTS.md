# Consignes pour les données

Ces règles complètent le `AGENTS.md` racine.

- Un changement de schéma exige une version Room, une migration ajoutée à `AppMigrations.ALL`, un schéma exporté et une vérification des données conservées. Une modification de requête seule n'exige pas de nouvelle version.
- Inclure toute nouvelle donnée utilisateur dans `BackupData`, `BackupDao`, l'export et la restauration. Définir explicitement le comportement pour les anciennes versions.
- Valider l'archive avant de remplacer les données. Ne pas écraser les photos existantes pendant la préparation ; tester aussi les échecs et le rollback.
- Room, fichiers et DataStore n'ont pas de transaction commune. Prévoir les états partiels, la récupération et un message exact.
- Placer les écritures liées dans une transaction. Préserver les conventions date ISO et macros par portion/pour 100 g.
- Différencier absence de résultat, erreur réseau, autorisation refusée et format invalide. Ne pas avaler une annulation de coroutine dans un `catch` général.
- Éviter les lectures complètes de l'historique si une requête ciblée répond au besoin ; vérifier avec un volume réaliste.
- Ne pas considérer le parcours de chiffrement actuel comme terminé ; suivre R03 dans la feuille de route avant de le présenter comme sécurisé.
