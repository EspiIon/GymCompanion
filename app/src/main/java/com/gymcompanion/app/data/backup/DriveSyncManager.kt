package com.gymcompanion.app.data.backup

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import com.google.api.services.drive.model.File as DriveFile

/**
 * Synchronisation optionnelle vers le dossier privé Google Drive de l'app (`appDataFolder`).
 *
 * ⚠️ Prérequis (hors code) : OAuth Client ID Android dans Google Cloud Console, API Drive
 * activée, SHA-1 du certificat déclaré, écran de consentement avec le scope `drive.appdata`.
 * Sans cette configuration, la connexion échoue silencieusement (les autres sauvegardes
 * locales continuent de fonctionner).
 */
@Singleton
class DriveSyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    private val backupName = "gymcompanion_backup.zip"
    private val appDataSpace = "appDataFolder"

    private val signInOptions: GoogleSignInOptions =
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(Scope(DriveScopes.DRIVE_APPDATA))
            .build()

    fun signInIntent(): Intent =
        GoogleSignIn.getClient(context, signInOptions).signInIntent

    /** Email du compte connecté, ou null si non connecté / non autorisé. */
    fun connectedEmail(): String? {
        val acc = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        return if (GoogleSignIn.hasPermissions(acc, Scope(DriveScopes.DRIVE_APPDATA))) acc.email else null
    }

    fun isConnected(): Boolean = connectedEmail() != null

    fun signOut() {
        runCatching { GoogleSignIn.getClient(context, signInOptions).signOut() }
    }

    private fun driveOrNull(): Drive? {
        val acc = GoogleSignIn.getLastSignedInAccount(context) ?: return null
        if (!GoogleSignIn.hasPermissions(acc, Scope(DriveScopes.DRIVE_APPDATA))) return null
        val credential = GoogleAccountCredential.usingOAuth2(
            context, listOf(DriveScopes.DRIVE_APPDATA)
        ).apply { selectedAccount = acc.account }
        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        ).setApplicationName("GymCompanion").build()
    }

    /** Upload (création ou mise à jour) du zip de sauvegarde dans appDataFolder. */
    suspend fun uploadBackup(localZip: File): Boolean = withContext(Dispatchers.IO) {
        val drive = driveOrNull() ?: return@withContext false
        try {
            val content = FileContent("application/zip", localZip)
            val existing = drive.files().list()
                .setSpaces(appDataSpace)
                .setFields("files(id,name)")
                .execute().files.orEmpty()
            val match = existing.firstOrNull { it.name == backupName }
            if (match != null) {
                drive.files().update(match.id, DriveFile().apply { name = backupName }, content).execute()
            } else {
                val meta = DriveFile().apply {
                    name = backupName
                    parents = listOf(appDataSpace)
                }
                drive.files().create(meta, content).setFields("id").execute()
            }
            true
        } catch (_: Exception) {
            false
        }
    }

    /** Télécharge la sauvegarde la plus récente depuis appDataFolder, ou null. */
    suspend fun downloadLatestBackup(): File? = withContext(Dispatchers.IO) {
        val drive = driveOrNull() ?: return@withContext null
        try {
            val files = drive.files().list()
                .setSpaces(appDataSpace)
                .setOrderBy("modifiedTime desc")
                .setFields("files(id,name,modifiedTime)")
                .execute().files.orEmpty()
            val remote = files.firstOrNull() ?: return@withContext null
            val out = File(context.cacheDir, "drive_restore.zip")
            out.outputStream().use { drive.files().get(remote.id).executeMediaAndDownloadTo(it) }
            out
        } catch (_: Exception) {
            null
        }
    }
}
