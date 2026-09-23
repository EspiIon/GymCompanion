package com.gymcompanion.app.data.db

import androidx.room.migration.Migration

/**
 * Migrations Room versionnées — garantissent qu'aucune mise à jour n'efface les données.
 *
 * La base est actuellement en **version 7**. À partir de maintenant, tout changement de
 * schéma DOIT :
 *   1. incrémenter `DB_VERSION` dans [AppDatabase] (ex. 7 → 8),
 *   2. ajouter un objet `Migration(7, 8)` ci-dessous avec le SQL `ALTER TABLE …`,
 *   3. l'ajouter à [ALL].
 *
 * Exemple :
 * ```
 * val MIGRATION_7_8 = Migration(7, 8) { db ->
 *     db.execSQL("ALTER TABLE food_entries ADD COLUMN note TEXT")
 * }
 * ```
 *
 * NE PLUS utiliser `fallbackToDestructiveMigration()` (perte de données).
 */
object AppMigrations {

    /** v6 → v7 : colonne `category` sur le catalogue d'aliments (source = API Open Food Facts). */
    private val MIGRATION_6_7 = Migration(6, 7) { db ->
        db.execSQL("ALTER TABLE food_items ADD COLUMN category TEXT NOT NULL DEFAULT ''")
    }

    /** v7 → v8 : table `gym_visits` pour les statistiques de fréquentation Basic-Fit. */
    private val MIGRATION_7_8 = Migration(7, 8) { db ->
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS gym_visits (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                club TEXT NOT NULL,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                timestamp INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_gym_visits_date ON gym_visits(date)")
    }

    /** v8 → v9 : colonnes unit/minerals/barcode sur aliments + table créatine. */
    private val MIGRATION_8_9 = Migration(8, 9) { db ->
        db.execSQL("ALTER TABLE food_items ADD COLUMN unit TEXT NOT NULL DEFAULT 'g'")
        db.execSQL("ALTER TABLE food_items ADD COLUMN minerals TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE food_items ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN unit TEXT NOT NULL DEFAULT 'g'")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN minerals TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE food_entries ADD COLUMN barcode TEXT NOT NULL DEFAULT ''")
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS creatine_log (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                date TEXT NOT NULL,
                taken INTEGER NOT NULL DEFAULT 1,
                grams REAL NOT NULL DEFAULT 5.0,
                notes TEXT NOT NULL DEFAULT '',
                timestamp INTEGER NOT NULL
            )
        """)
        db.execSQL("CREATE INDEX IF NOT EXISTS index_creatine_log_date ON creatine_log(date)")
    }

    val ALL: Array<Migration> = arrayOf(
        MIGRATION_6_7,
        MIGRATION_7_8,
        MIGRATION_8_9
    )
}
