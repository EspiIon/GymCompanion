package com.gymcompanion.app.data.importer

import android.content.Context
import android.net.Uri
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.gymcompanion.app.data.model.BodyRecord
import com.gymcompanion.app.data.model.GymVisit
import java.io.InputStreamReader

/**
 * Parses a Basic Fit JSON data export (GDPR download) and extracts body composition records.
 *
 * Supported formats:
 *
 * Format A — "measurements" array (scale data, values are PERCENTAGES except weight):
 * {
 *   "measurements": [
 *     { "date":"2026-06-02T11:36:50Z", "fat":20, "muscle":76, "bone":4, "water":54.9, "weight":87 }
 *   ]
 * }
 * Where: fat=fat%, muscle=skeletal muscle%, bone=bone%, water=water%, weight=kg
 * muscle_kg = (muscle% / 100) * weight
 *
 * Format B — "stats" array (daily aggregates, values are KILOGRAMS):
 * {
 *   "stats": [
 *     { "day":"2026-06-02T00:00:00Z", "fat":17.4, "muscle":66.2, "bone":3.4, "weight":87, "gymVisits":1 }
 *   ]
 * }
 *
 * Format C — legacy flat format:
 * { "bodyMeasurements": [...] }
 */
object BasicFitImporter {

    data class ImportResult(
        val records: List<BodyRecord>,
        val visits: List<GymVisit>,
        val errors: List<String>
    )

    fun import(context: Context, uri: Uri): ImportResult {
        return try {
            val stream = context.contentResolver.openInputStream(uri)
                ?: return ImportResult(emptyList(), emptyList(), listOf("Impossible d'ouvrir le fichier"))
            val root = stream.use { JsonParser.parseReader(InputStreamReader(it)).asJsonObject }
            parseRoot(root)
        } catch (e: Exception) {
            ImportResult(emptyList(), emptyList(), listOf("Erreur de parsing : ${e.message}"))
        }
    }

    private fun parseRoot(root: JsonObject): ImportResult {
        val errors = mutableListOf<String>()
        val records = mutableListOf<BodyRecord>()
        val visits = mutableListOf<GymVisit>()

        // Format A: Basic Fit "measurements" — percentage values from InBody/Tanita scale
        if (root.has("measurements") && root["measurements"].isJsonArray) {
            records += parseMeasurementsArray(root.getAsJsonArray("measurements"), errors)
        }

        // Format B: Basic Fit "stats" — daily aggregated kg values
        if (records.isEmpty() && root.has("stats") && root["stats"].isJsonArray) {
            records += parseStatsArray(root.getAsJsonArray("stats"), errors)
        }

          // Format C: Legacy flat array keys
        if (records.isEmpty()) {
            val legacyKeys = listOf(
                "bodyMeasurements", "body_measurements", "bodyComposition",
                "body_composition", "userData.measurements"
            )
            for (key in legacyKeys) {
                if (root.has(key) && root[key].isJsonArray) {
                    records += parseLegacyArray(root.getAsJsonArray(key), errors)
                    break
                }
            }
        }

        // Visits array (e.g. Basic Fit visits history)
        if (root.has("visits") && root["visits"].isJsonArray) {
            visits += parseVisitsArray(root.getAsJsonArray("visits"), errors)
        }

        if (records.isEmpty() && visits.isEmpty() && errors.isEmpty()) {
            errors.add("Format non reconnu. Clés : ${root.keySet().joinToString()}")
        }

        // Deduplicate by date (keep most recent per date)
        val dedupedRecords = records.groupBy { it.date }.map { (_, recs) -> recs.last() }
        val dedupedVisits = visits.groupBy { "${it.date}_${it.time}" }.map { (_, v) -> v.first() }
        return ImportResult(dedupedRecords, dedupedVisits, errors)
    }

    /**
     * Format A: measurements[] — fat/muscle/bone/water are PERCENTAGES, weight is KG
     * muscle_kg = (muscle% / 100) * weight
     */
    private fun parseMeasurementsArray(array: JsonArray, errors: MutableList<String>): List<BodyRecord> {
        val records = mutableListOf<BodyRecord>()
        for ((i, element) in array.withIndex()) {
            try {
                val obj = element.asJsonObject
                val date = extractDate(obj, "date", "timestamp", "datetime") ?: run {
                    errors.add("measurements[#$i] : date introuvable"); continue
                }
                val weight = extractFloat(obj, "weight", "weightKg") ?: run {
                    errors.add("measurements[#$i] : poids introuvable"); continue
                }
                val fatPct    = extractFloat(obj, "fat", "bodyFat", "fat_percentage")
                val musclePct = extractFloat(obj, "muscle", "skeletal_muscle_percentage", "muscle_percentage")
                val bonePct   = extractFloat(obj, "bone", "bone_percentage")
                val waterPct  = extractFloat(obj, "water", "water_percentage")

                // Convert percentages to kg where needed
                val muscleKg = musclePct?.let { (it / 100f) * weight }

                records.add(BodyRecord(
                    date           = date,
                    weightKg       = weight,
                    bodyFatPercent = fatPct,
                    muscleMassKg   = muscleKg,
                    bonePercent    = bonePct,
                    waterPercent   = waterPct
                ))
            } catch (e: Exception) {
                errors.add("measurements[#$i] ignorée : ${e.message}")
            }
        }
        return records
    }

    /**
     * Format B: stats[] — fat/muscle/bone in KG, weight in KG, gymVisits ignored for body records
     * Only entries with weight data are imported.
     */
    private fun parseStatsArray(array: JsonArray, errors: MutableList<String>): List<BodyRecord> {
        val records = mutableListOf<BodyRecord>()
        for ((i, element) in array.withIndex()) {
            try {
                val obj = element.asJsonObject
                val weight = extractFloat(obj, "weight") ?: continue  // skip entries without weight
                val date = extractDate(obj, "day", "date") ?: run {
                    errors.add("stats[#$i] : date introuvable"); continue
                }
                val fatKg    = extractFloat(obj, "fat")
                val muscleKg = extractFloat(obj, "muscle")
                val boneKg   = extractFloat(obj, "bone")

                // Convert kg to % for storage (guard against zero weight)
                val fatPct  = fatKg?.let  { if (weight > 0f) (it / weight) * 100f else null }
                val bonePct = boneKg?.let { if (weight > 0f) (it / weight) * 100f else null }

                records.add(BodyRecord(
                    date           = date,
                    weightKg       = weight,
                    bodyFatPercent = fatPct,
                    muscleMassKg   = muscleKg,
                    bonePercent    = bonePct,
                    waterPercent   = null   // not available in stats format
                ))
            } catch (e: Exception) {
                errors.add("stats[#$i] ignorée : ${e.message}")
            }
        }
        return records
    }

    /**
     * Format C: Legacy generic format (bodyMeasurements / bodyComposition)
     */
    private fun parseLegacyArray(array: JsonArray, errors: MutableList<String>): List<BodyRecord> {
        val records = mutableListOf<BodyRecord>()
        for ((i, element) in array.withIndex()) {
            try {
                val obj = element.asJsonObject
                val date = extractDate(obj, "date", "timestamp", "datetime", "recordDate") ?: run {
                     errors.add("#$i : date introuvable"); continue
                }
                records.add(BodyRecord(
                    date           = date,
                    weightKg       = extractFloat(obj, "weight", "body_weight", "poids", "weightKg"),
                    bodyFatPercent = extractFloat(obj, "bodyFatPercentage", "body_fat", "bodyFat", "fat_percentage"),
                    muscleMassKg   = extractMuscle(obj),
                    waistCm        = extractFloat(obj, "waist", "waistCm", "waist_cm"),
                    chestCm        = extractFloat(obj, "chest", "chestCm", "chest_cm"),
                    armCm          = extractFloat(obj, "arm", "armCm", "arm_cm", "bicep")
                ))
            } catch (e: Exception) {
                errors.add("#$i ignorée : ${e.message}")
            }
        }
        return records
    }

    private fun extractDate(obj: JsonObject, vararg keys: String): String? {
        for (key in keys) {
            val v = obj[key]?.takeIf { !it.isJsonNull }?.asString ?: continue
            return v.take(10)   // truncate to yyyy-MM-dd
        }
        return null
    }

    private fun extractFloat(obj: JsonObject, vararg keys: String): Float? {
        for (key in keys) {
            val v = obj[key]?.takeIf { !it.isJsonNull } ?: continue
            return try { v.asFloat } catch (_: Exception) { null }
        }
        return null
    }

    private fun extractMuscle(obj: JsonObject): Float? {
        extractFloat(obj, "muscleMassKg", "muscle_mass", "muscleMass", "lean_mass")?.let { return it }
        val pct = extractFloat(obj, "skeletalMusclePercentage", "muscle_percentage", "muscle_pct") ?: return null
        val weight = extractFloat(obj, "weight", "body_weight", "weightKg") ?: return null
        return (pct / 100f) * weight
    }

    private fun parseVisitsArray(array: JsonArray, errors: MutableList<String>): List<GymVisit> {
        val visits = mutableListOf<GymVisit>()
        for ((i, element) in array.withIndex()) {
            try {
                val obj = element.asJsonObject
                val club = obj["club"]?.takeIf { !it.isJsonNull }?.asString ?: "Basic-Fit"
                val dateStr = obj["date"]?.takeIf { !it.isJsonNull }?.asString ?: continue
                val timeStr = obj["time"]?.takeIf { !it.isJsonNull }?.asString ?: "00:00"

                val normalizedDate = normalizeVisitDate(dateStr)

                visits.add(GymVisit(
                    club = club,
                    date = normalizedDate,
                    time = timeStr,
                    timestamp = parseVisitTimestamp(normalizedDate, timeStr)
                ))
            } catch (e: Exception) {
                errors.add("visits[#$i] ignorée : ${e.message}")
            }
        }
        return visits
    }

    private fun normalizeVisitDate(d: String): String {
        val parts = d.split("-", "/")
        if (parts.size == 3) {
            val day = parts[0].padStart(2, '0')
            val month = parts[1].padStart(2, '0')
            val year = parts[2]
            if (year.length == 4) return "$year-$month-$day"
        }
        return d.take(10)
    }

    private fun parseVisitTimestamp(date: String, time: String): Long {
        return try {
            val dt = java.time.LocalDateTime.parse("$date $time", java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
            dt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
        } catch (_: Exception) {
            System.currentTimeMillis()
        }
    }
}
