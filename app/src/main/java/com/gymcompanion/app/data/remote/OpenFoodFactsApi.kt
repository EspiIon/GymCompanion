package com.gymcompanion.app.data.remote

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

data class ScannedProduct(
    val name: String,
    val brand: String,
    val calories: Int,       // per 100g
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val servingSizeG: Float? = null,  // per-serving if available
    val category: String? = null      // catégorie simplifiée (ex. "Snacks", "Boissons")
)

/**
 * Fetches nutritional data from Open Food Facts (world.openfoodfacts.org).
 * No API key required.
 *
 * - [fetchByBarcode] : lookup d'un produit par code-barres (API v2).
 * - [searchByName]   : recherche texte (API CGI legacy) → liste de produits.
 *
 * Utilise un [OkHttpClient] injecté (timeouts garantis — jamais de blocage infini).
 */
@Singleton
class OpenFoodFactsApi @Inject constructor(
    private val client: OkHttpClient
) {

    private companion object {
        const val PRODUCT = "https://world.openfoodfacts.org/api/v2/product"
        const val SEARCH = "https://world.openfoodfacts.org/cgi/search.pl"
        const val FIELDS = "product_name,brands,nutriments,serving_size,categories_tags"
    }

    private fun getBody(url: String): String? =
        client.newCall(Request.Builder().url(url).build()).execute().use { response ->
            if (!response.isSuccessful) null else response.body?.string()
        }

    suspend fun fetchByBarcode(barcode: String): ScannedProduct? = withContext(Dispatchers.IO) {
        try {
            val url = "$PRODUCT/$barcode.json?fields=$FIELDS"
            val body = getBody(url) ?: return@withContext null
            val root = JsonParser.parseString(body).asJsonObject
            if (root["status"]?.asInt != 1) return@withContext null
            val product = root.getAsJsonObject("product") ?: return@withContext null
            parseProduct(product)
        } catch (_: Exception) {
            null
        }
    }

    /** Recherche par nom — renvoie jusqu'à [pageSize] produits exploitables (avec macros). */
    suspend fun searchByName(query: String, pageSize: Int = 20): List<ScannedProduct> =
        withContext(Dispatchers.IO) {
            val q = query.trim()
            if (q.isBlank()) return@withContext emptyList()
            try {
                val encoded = java.net.URLEncoder.encode(q, "UTF-8")
                val url = "$SEARCH?search_terms=$encoded&search_simple=1&action=process" +
                    "&json=1&page_size=$pageSize&fields=$FIELDS"
                val body = getBody(url) ?: return@withContext emptyList()
                val root = JsonParser.parseString(body).asJsonObject
                val products = root.getAsJsonArray("products") ?: return@withContext emptyList()
                products.mapNotNull { el ->
                    (el as? JsonObject)?.let { parseProduct(it) }
                }.filter { it.calories > 0 || it.protein > 0f || it.carbs > 0f || it.fat > 0f }
            } catch (_: Exception) {
                emptyList()
            }
        }

    /** Convertit un objet "product" Open Food Facts en [ScannedProduct] (valeurs /100 g). */
    private fun parseProduct(p: JsonObject): ScannedProduct? {
        return try {
            val n = p.getAsJsonObject("nutriments") ?: return null

            fun num(vararg keys: String): Float {
                for (k in keys) {
                    val v = n.get(k)?.takeIf { !it.isJsonNull } ?: continue
                    return try { v.asFloat } catch (_: Exception) { continue }
                }
                return 0f
            }

            val name = p.get("product_name")?.asString?.takeIf { it.isNotBlank() } ?: return null
            val brand = p.get("brands")?.asString?.split(",")?.firstOrNull()?.trim() ?: ""
            val displayName = if (brand.isNotBlank()) "$name ($brand)" else name

            // OpenFoodFacts uses "energy-kcal_100g" or "energy_100g" (kJ)
            val kcal = when {
                n.has("energy-kcal_100g") -> num("energy-kcal_100g")
                n.has("energy_100g") -> num("energy_100g") / 4.184f  // kJ → kcal
                else -> 0f
            }

            val servingStr = p.get("serving_size")?.asString
            val servingG = servingStr?.replace(Regex("[^0-9.]"), "")?.toFloatOrNull()

            ScannedProduct(
                name = displayName,
                brand = brand,
                calories = kcal.toInt(),
                protein = num("proteins_100g"),
                carbs = num("carbohydrates_100g"),
                fat = num("fat_100g"),
                servingSizeG = servingG,
                category = parseCategory(p)
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Extrait une catégorie simplifiée et francisée depuis `categories_tags`
     * (ex. ["en:snacks","en:chips"] → "Snacks"). Retourne null si absente.
     */
    private fun parseCategory(p: JsonObject): String? {
        val tags = p.getAsJsonArray("categories_tags") ?: return null
        for (el in tags) {
            val raw = (el as? com.google.gson.JsonPrimitive)?.asString ?: continue
            val tag = raw.substringAfter(':').lowercase()
            val label = CATEGORY_LABELS.entries.firstOrNull { tag.contains(it.key) }?.value
            if (label != null) return label
        }
        return null
    }

    private val CATEGORY_LABELS = mapOf(
        "snack"          to "Snacks",
        "sweets"         to "Snacks",
        "chocolate"      to "Snacks",
        "biscuit"        to "Snacks",
        "beverage"       to "Boissons",
        "drink"          to "Boissons",
        "water"          to "Boissons",
        "juice"          to "Boissons",
        "dairy"          to "Produits laitiers",
        "yogurt"         to "Produits laitiers",
        "cheese"         to "Produits laitiers",
        "milk"           to "Produits laitiers",
        "meat"           to "Viandes & poissons",
        "poultry"        to "Viandes & poissons",
        "fish"           to "Viandes & poissons",
        "seafood"        to "Viandes & poissons",
        "charcuterie"    to "Viandes & poissons",
        "fruit"          to "Fruits",
        "vegetable"      to "Légumes",
        "cereals"        to "Féculents",
        "pasta"          to "Féculents",
        "rice"           to "Féculents",
        "bread"          to "Féculents",
        "potato"         to "Féculents",
        "legume"         to "Légumineuses",
        "bean"           to "Légumineuses",
        "lentil"         to "Légumineuses",
        "prepared-meal"  to "Plats préparés",
        "meals"          to "Plats préparés",
        "sandwich"       to "Plats préparés",
        "pizza"          to "Plats préparés",
        "soup"           to "Plats préparés",
        "sauce"          to "Condiments",
        "condiment"      to "Condiments",
        "spice"          to "Condiments"
    )
}
