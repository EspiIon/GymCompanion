package com.gymcompanion.app.viewmodel

import android.content.Context
import android.net.Uri
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.data.model.ProgressPhoto
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ProgressPhotoViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val repo: GymRepository
) : ViewModel() {

    val allPhotos: StateFlow<List<ProgressPhoto>> =
        repo.getAllProgressPhotos()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Grouped by date descending, for the timeline grid
    val photosByDate: StateFlow<List<Pair<String, List<ProgressPhoto>>>> =
        allPhotos.map { photos ->
            photos
                .groupBy { it.date }
                .entries
                .sortedByDescending { it.key }
                .map { (date, list) -> date to list.sortedByDescending { it.timestamp } }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // URI pre-created for TakePicture contract
    private var pendingCameraPath: String? = null

    fun createCameraUri(): Uri {
        val photoDir = File(appContext.filesDir, "progress_photos")
        photoDir.mkdirs()
        val file = File(photoDir, "cam_${System.currentTimeMillis()}.jpg")
        pendingCameraPath = file.absolutePath
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            file
        )
    }

    fun onCameraResult(success: Boolean, notes: String = "") {
        val path = pendingCameraPath ?: return
        pendingCameraPath = null
        if (!success) {
            // Camera cancelled — delete the empty file if it exists
            File(path).delete()
            return
        }
        viewModelScope.launch {
            repo.addProgressPhoto(
                ProgressPhoto(
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    imagePath = path,
                    notes = notes
                )
            )
        }
    }

    fun addPhotoFromGallery(uri: Uri, notes: String = "") = viewModelScope.launch {
        try {
            val photoDir = File(appContext.filesDir, "progress_photos")
            photoDir.mkdirs()
            val extension = appContext.contentResolver.getType(uri)
                ?.let { MimeTypeMap.getSingleton().getExtensionFromMimeType(it) }
                ?.takeIf { it.matches(Regex("[A-Za-z0-9]+")) } ?: "jpg"
            val file = File(photoDir, "gal_${UUID.randomUUID()}.$extension")
            val source = appContext.contentResolver.openInputStream(uri) ?: return@launch
            try {
                source.use { input -> file.outputStream().use { output -> input.copyTo(output) } }
            } catch (e: Exception) {
                file.delete()
                throw e
            }
            repo.addProgressPhoto(
                ProgressPhoto(
                    date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE),
                    imagePath = file.absolutePath,
                    notes = notes
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun deletePhoto(photo: ProgressPhoto) = viewModelScope.launch {
        // DB d'abord : si le delete échoue, la photo (et son fichier) restent cohérents.
        repo.deleteProgressPhoto(photo)
        File(photo.imagePath).delete()
    }
}
