package com.gymcompanion.app.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class ChatMessage(
    val role: String,         // "user" | "assistant"
    val content: String
)

data class AiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val apiKey: String = "",
    val error: String? = null,
    val showKeySetup: Boolean = false
)

@HiltViewModel
class AiViewModel @Inject constructor(
    private val repo: GymRepository,
    private val dataStore: DataStore<Preferences>,
    baseClient: OkHttpClient
) : ViewModel() {

    companion object {
        val API_KEY_PREF = stringPreferencesKey("openrouter_api_key")
        val MODEL_PREF = stringPreferencesKey("ai_model")
        const val DEFAULT_MODEL = "meta-llama/llama-3.3-70b-instruct:free"
    }

    private val _state = MutableStateFlow(AiState())
    val state: StateFlow<AiState> = _state.asStateFlow()

    /** Dérivé du client partagé — même pool de connexions, lecture plus longue pour le LLM. */
    private val client: OkHttpClient = baseClient.newBuilder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    val selectedModel: StateFlow<String> =
        dataStore.data.map { it[MODEL_PREF] ?: DEFAULT_MODEL }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_MODEL)

    init {
        viewModelScope.launch {
            dataStore.data.map { it[API_KEY_PREF] ?: "" }.collect { key ->
                _state.update { it.copy(apiKey = key, showKeySetup = key.isBlank()) }
            }
        }
    }

    fun saveApiKey(key: String) = viewModelScope.launch {
        dataStore.edit { it[API_KEY_PREF] = key.trim() }
    }

    fun toggleKeySetup() {
        _state.update { it.copy(showKeySetup = !it.showKeySetup) }
    }

    fun clearError() { _state.update { it.copy(error = null) } }

    fun sendMessage(userText: String) {
        val trimmed = userText.trim()
        if (trimmed.isBlank()) return
        val key = _state.value.apiKey
        if (key.isBlank()) {
            _state.update { it.copy(showKeySetup = true, error = "Entre ta clé API OpenRouter pour utiliser le Coach IA.") }
            return
        }

        val userMsg = ChatMessage("user", trimmed)
        _state.update { it.copy(messages = it.messages + userMsg, isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Build system prompt safely — failures fall back to minimal prompt
                val systemPrompt = try { buildSystemPrompt() } catch (_: Exception) {
                    "Tu es un coach fitness francophone expert. Sois concis et motivant."
                }
                // Read straight from dataStore — selectedModel.value can be stale
                // because nothing collects it (WhileSubscribed has no subscribers here).
                val model = dataStore.data.map { it[MODEL_PREF] ?: DEFAULT_MODEL }
                    .first()
                    .ifBlank { DEFAULT_MODEL }
                val reply = callOpenRouter(key, model, systemPrompt, _state.value.messages)
                _state.update { it.copy(
                    messages = it.messages + ChatMessage("assistant", reply),
                    isLoading = false
                )}
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message ?: "Erreur réseau") }
            }
        }
    }

    private suspend fun buildSystemPrompt(): String = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        val since30 = LocalDate.now().minusDays(30).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val profile     = repo.getUserProfile().first()
        val latestBody  = repo.getLatestBodyRecord().first()
        val bodyHistory = repo.getBodyRecordsSince(since30).first()
        val workouts    = repo.getRecentWorkouts().first().take(10)
        val nutrition   = repo.getDailySummary(today).first()

        buildString {
            appendLine("Tu es un coach fitness personnel francophone expert et bienveillant.")
            appendLine("Tu analyses les données de l'utilisateur et fournis des conseils précis, motivants et personnalisés.")
            appendLine("Sois concis (3-5 phrases max par réponse), factuel et chaleureux.")
            appendLine()

            profile?.let { p ->
                if (p.name.isNotBlank()) appendLine("Prénom: ${p.name}")
                if (p.birthYear > 0) {
                    val age = LocalDate.now().year - p.birthYear
                    appendLine("Âge: $age ans | Taille: ${p.heightCm.toInt()} cm | Sexe: ${if (p.genderCode == "M") "Homme" else "Femme"}")
                }
                val obj = when (p.objectiveCode) {
                    "cut" -> "Perte de masse grasse (déficit −500 kcal)"
                    "bulk" -> "Prise de masse musculaire (surplus +300 kcal)"
                    else -> "Maintien du poids"
                }
                appendLine("Objectif: $obj")
                appendLine()
            }

            latestBody?.let { b ->
                appendLine("=== COMPOSITION CORPORELLE — ${b.date} ===")
                b.weightKg?.let { appendLine("Poids: ${it} kg") }
                b.bodyFatPercent?.let {
                    val fatKg = b.weightKg?.let { w -> w * it / 100f }
                    appendLine("Masse grasse: $it% ${fatKg?.let { "(${String.format(java.util.Locale.US, "%.1f", it)} kg)" } ?: ""}")
                }
                b.muscleMassKg?.let { appendLine("Masse musculaire: $it kg") }
                b.bonePercent?.let {
                    val boneKg = b.weightKg?.let { w -> w * it / 100f }
                    appendLine("Masse osseuse: $it% ${boneKg?.let { "(${String.format(java.util.Locale.US, "%.1f", it)} kg)" } ?: ""}")
                }
                b.waterPercent?.let { appendLine("Eau corporelle: $it%") }
            }

            if (bodyHistory.size >= 2) {
                appendLine()
                appendLine("=== ÉVOLUTION 30 JOURS ===")
                val first = bodyHistory.first()
                val last  = bodyHistory.last()
                first.weightKg?.let { fw -> last.weightKg?.let { lw ->
                    val d = lw - fw
                    appendLine("Poids: ${fw}→${lw} kg (${if (d < 0) "" else "+"}${String.format(java.util.Locale.US, "%.1f", d)} kg)")
                }}
                first.bodyFatPercent?.let { ff -> last.bodyFatPercent?.let { lf ->
                    val d = lf - ff
                    appendLine("Graisse: ${ff}→${lf}% (${if (d < 0) "" else "+"}${String.format(java.util.Locale.US, "%.1f", d)}%)")
                }}
                first.muscleMassKg?.let { fm -> last.muscleMassKg?.let { lm ->
                    val d = lm - fm
                    appendLine("Muscle: ${fm}→${lm} kg (${if (d < 0) "" else "+"}${String.format(java.util.Locale.US, "%.1f", d)} kg)")
                }}
            }

            if (workouts.isNotEmpty()) {
                appendLine()
                appendLine("=== SÉANCES RÉCENTES ===")
                workouts.take(7).forEach { w ->
                    val muscles = if (w.muscleGroups.isNotBlank()) " [${w.muscleGroups}]" else ""
                    appendLine("${w.date}: ${w.name} (${w.category}, ${w.durationMinutes}min, ${w.caloriesBurned}kcal)$muscles")
                }
                appendLine("Total séances chargées: ${workouts.size}")
            }

            nutrition?.let { n ->
                if (n.totalCalories > 0) {
                    appendLine()
                    appendLine("=== NUTRITION AUJOURD'HUI ===")
                    appendLine("Calories: ${n.totalCalories} kcal | Protéines: ${n.totalProtein.toInt()}g | Glucides: ${n.totalCarbs.toInt()}g | Lipides: ${n.totalFat.toInt()}g")
                }
            }
        }
    }

    private suspend fun callOpenRouter(
        apiKey: String,
        model: String,
        systemPrompt: String,
        history: List<ChatMessage>
    ): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw Exception("Clé API manquante. Configure-la dans Paramètres.")
        }
        if (model.isBlank()) {
            throw Exception("Modèle non configuré. Entre un modèle OpenRouter valide.")
        }

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", systemPrompt))
            // Keep last 10 messages for context window efficiency
            history.takeLast(10).forEach { msg ->
                put(JSONObject().put("role", msg.role).put("content", msg.content))
            }
        }

        val bodyJson = JSONObject().apply {
            put("model", model)
            put("messages", messages)
            put("max_tokens", 600)
            put("temperature", 0.7)
        }.toString()

        val request = Request.Builder()
            .url("https://openrouter.ai/api/v1/chat/completions")
            .post(bodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://gymcompanion.app")
            .header("X-Title", "GymCompanion")
            .header("User-Agent", "GymCompanion/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            val responseStr = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                // OpenRouter returns {"error":{"message":...}} on failures, not {"choices":...}
                val apiMsg = try {
                    val errObj = JSONObject(responseStr)
                    errObj.optJSONObject("error")?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: errObj.optString("message").takeIf { it.isNotBlank() }
                } catch (_: Exception) { null }

                val hint = when (response.code) {
                    401, 403    -> "Clé API OpenRouter invalide ou expirée. Vérifie ta clé."
                    402         -> "Crédits insuffisants sur ton compte OpenRouter."
                    404         -> "Modèle introuvable : « $model ». Choisis-en un autre dans Paramètres."
                    429         -> "Limite de requêtes atteinte (modèle gratuit). Réessaie dans un instant."
                    in 500..599 -> "OpenRouter est momentanément indisponible. Réessaie."
                    else        -> "Erreur OpenRouter (${response.code})."
                }
                throw Exception(apiMsg?.let { "$hint\n$it" } ?: hint)
            }

            val content = try {
                val choices = JSONObject(responseStr).optJSONArray("choices")
                if (choices == null || choices.length() == 0) throw IllegalStateException("empty choices")
                choices.getJSONObject(0)
                    .optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()
            } catch (_: Exception) {
                throw Exception("Réponse inattendue du modèle. Réessaie ou change de modèle.")
            }

            if (content.isBlank()) {
                throw Exception("Le modèle a renvoyé une réponse vide. Réessaie ou change de modèle.")
            }
            content
        }
    }
}
