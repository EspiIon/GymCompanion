package com.gymcompanion.app.ui.screens.progress

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.gymcompanion.app.data.model.ProgressPhoto
import com.gymcompanion.app.ui.components.*
import com.gymcompanion.app.ui.theme.*
import com.gymcompanion.app.viewmodel.ProgressPhotoViewModel
import java.io.File
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val PAD = 20.dp

// ── Entry point ───────────────────────────────────────────────────────────────

@Composable
fun ProgressPhotoScreen(viewModel: ProgressPhotoViewModel = hiltViewModel()) {
    val allPhotos    by viewModel.allPhotos.collectAsStateWithLifecycle()
    val photosByDate by viewModel.photosByDate.collectAsStateWithLifecycle()

    var selectedTab by remember { mutableIntStateOf(0) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        viewModel.onCameraResult(success)
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) viewModel.addPhotoFromGallery(uri)
    }

    // Fullscreen viewer state
    var fullscreenPhoto by remember { mutableStateOf<ProgressPhoto?>(null) }

    Box(Modifier.fillMaxSize().background(NothingBlack)) {

        Column(Modifier.fillMaxSize()) {
            // ── Header ────────────────────────────────────────────────────────
            Column(Modifier.fillMaxWidth().padding(start = PAD, end = PAD, top = 26.dp, bottom = 16.dp)) {
                NLabel("SUIVI VISUEL")
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Progression", fontFamily = LocalNumericFont.current,
                        fontWeight = FontWeight.SemiBold, fontSize = 28.sp,
                        letterSpacing = 0.5.sp, color = NothingWhite)
                    if (allPhotos.isNotEmpty()) {
                        Column(horizontalAlignment = Alignment.End) {
                            NumText("${allPhotos.size}", fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            NLabel("PHOTOS", size = 7.sp, color = NothingGrey2)
                        }
                    }
                }
            }

            // ── Tab row ───────────────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(horizontal = PAD).padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("CHRONOLOGIE", "COMPARER").forEachIndexed { i, label ->
                    val active = selectedTab == i
                    Box(
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (active) NothingDark2 else Color.Transparent)
                            .border(1.dp, if (active) NothingBorderMid else NothingBorder, RoundedCornerShape(8.dp))
                            .clickable { selectedTab = i }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        NLabel(label, color = if (active) NothingWhite else NothingGrey3, size = 9.sp)
                    }
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            when (selectedTab) {
                0 -> ChronologieTab(
                    photosByDate = photosByDate,
                    onPhotoClick = { fullscreenPhoto = it }
                )
                1 -> ComparerTab(allPhotos = allPhotos)
            }
        }

        // ── FABs (only on chronologie tab) ────────────────────────────────────
        if (selectedTab == 0) {
            Column(
                Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 110.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Gallery
                SmallFAB(
                    onClick = { galleryLauncher.launch("image/*") },
                    icon = Icons.Rounded.PhotoLibrary,
                    contentDescription = "Ouvrir la galerie"
                )

                // Camera
                StandardFAB(
                    onClick = {
                        val uri = viewModel.createCameraUri()
                        cameraLauncher.launch(uri)
                    },
                    icon = Icons.Rounded.PhotoCamera,
                    contentDescription = "Prendre une photo"
                )
            }
        }
    }

    // ── Fullscreen photo viewer ───────────────────────────────────────────────
    fullscreenPhoto?.let { photo ->
        FullscreenViewer(
            photo = photo,
            onDismiss = { fullscreenPhoto = null },
            onDelete = {
                viewModel.deletePhoto(photo)
                fullscreenPhoto = null
            }
        )
    }
}

// ── Chronologie tab ───────────────────────────────────────────────────────────

@Composable
private fun ChronologieTab(
    photosByDate: List<Pair<String, List<ProgressPhoto>>>,
    onPhotoClick: (ProgressPhoto) -> Unit
) {
    if (photosByDate.isEmpty()) {
        Box(Modifier.fillMaxSize().padding(bottom = 110.dp), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.PhotoCameraBack, null, tint = NothingGrey3, modifier = Modifier.size(52.dp))
                Spacer(Modifier.height(20.dp))
                NLabel("AUCUNE PHOTO", color = NothingGrey2)
                Spacer(Modifier.height(10.dp))
                Text("Appuyez sur      pour prendre votre\npremière photo de progression",
                    color = NothingGrey3, fontSize = 12.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 40.dp))
            }
        }
        return
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(start = PAD, end = PAD, bottom = 120.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        photosByDate.forEach { (date, photos) ->
            // Date header spanning full width
            item(span = { GridItemSpan(2) }) {
                DateHeader(date)
            }
            // Photos in 2-column grid
            items(photos, key = { it.id }) { photo ->
                PhotoThumbnail(photo = photo, onClick = { onPhotoClick(photo) })
            }
        }
    }
}

@Composable
private fun DateHeader(dateStr: String) {
    val formatted = remember(dateStr) {
        try {
            val d = LocalDate.parse(dateStr)
            d.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.FRENCH))
                .replaceFirstChar { it.uppercase() }
        } catch (e: Exception) { dateStr }
    }
    Box(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp)) {
        NLabel(formatted.uppercase(), color = NothingGrey2, size = 9.sp)
    }
}

@Composable
private fun PhotoThumbnail(photo: ProgressPhoto, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        val fileExistsThumb = remember(photo.imagePath) { runCatching { File(photo.imagePath).exists() }.getOrDefault(false) }
        if (fileExistsThumb) {
            AsyncImage(
                model = photo.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(NothingDark), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.BrokenImage, null, tint = NothingGrey3, modifier = Modifier.size(28.dp))
            }
        }
        // Notes indicator
        if (photo.notes.isNotEmpty()) {
            Box(
                Modifier.align(Alignment.BottomEnd).padding(6.dp)
                    .size(20.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.6f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Notes, null, tint = NothingWhite, modifier = Modifier.size(12.dp))
            }
        }
    }
}

// ── Fullscreen viewer ─────────────────────────────────────────────────────────

@Composable
private fun FullscreenViewer(
    photo: ProgressPhoto,
    onDismiss: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Box(
        Modifier.fillMaxSize().background(Color.Black)
            .clickable(onClick = onDismiss)
    ) {
        val fileExistsFullscreen = remember(photo.imagePath) { runCatching { File(photo.imagePath).exists() }.getOrDefault(false) }
        if (fileExistsFullscreen) {
            AsyncImage(
                model = photo.imagePath,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        }

        // Top bar
        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
                .align(Alignment.TopStart),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Close, null, tint = NothingWhite, modifier = Modifier.size(18.dp))
            }
            Box(
                Modifier.size(36.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { showDeleteConfirm = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.DeleteOutline, null, tint = NothingRed, modifier = Modifier.size(18.dp))
            }
        }

        // Bottom info
        Column(
            Modifier.align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))))
                .padding(horizontal = 20.dp, vertical = 20.dp)
        ) {
            NLabel(photo.date, color = NothingGrey1)
            if (photo.notes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Text(photo.notes, color = NothingWhite, fontSize = 13.sp)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = NothingDark,
            title = { Text("Supprimer", color = NothingWhite) },
            text = { Text("Supprimer cette photo de progression ?", color = NothingGrey1) },
            confirmButton = {
                Button(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = NothingRed)
                ) { Text("Supprimer", color = NothingWhite) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Annuler", color = NothingGrey2)
                }
            }
        )
    }
}

// ── Comparer tab ──────────────────────────────────────────────────────────────

@Composable
private fun ComparerTab(allPhotos: List<ProgressPhoto>) {
    var beforePhoto by remember { mutableStateOf<ProgressPhoto?>(null) }
    var afterPhoto  by remember { mutableStateOf<ProgressPhoto?>(null) }
    var showPickerFor by remember { mutableStateOf<String?>(null) } // "before" | "after"

    LazyColumn(
        contentPadding = PaddingValues(start = PAD, end = PAD, top = 8.dp, bottom = 110.dp)
    ) {
        // Slider comparison
        if (beforePhoto != null && afterPhoto != null) {
            item(key = "slider") {
                Spacer(Modifier.height(8.dp))
                ComparisonSlider(before = beforePhoto!!, after = afterPhoto!!)
                Spacer(Modifier.height(20.dp))
                DottedDivider()
                Spacer(Modifier.height(20.dp))
            }
        } else {
            item(key = "hint") {
                Box(
                    Modifier.fillMaxWidth().height(200.dp)
                        .border(1.dp, NothingBorder, RoundedCornerShape(12.dp))
                        .clip(RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Rounded.CompareArrows, null, tint = NothingGrey3, modifier = Modifier.size(36.dp))
                        Spacer(Modifier.height(12.dp))
                        NLabel("SÉLECTIONNER AVANT & APRÈS", color = NothingGrey3)
                    }
                }
                Spacer(Modifier.height(20.dp))
                DottedDivider()
                Spacer(Modifier.height(20.dp))
            }
        }

        // Pickers
        item(key = "pickers") {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PhotoPickerSlot(
                    label = "AVANT",
                    photo = beforePhoto,
                    onClick = { showPickerFor = "before" },
                    modifier = Modifier.weight(1f)
                )
                PhotoPickerSlot(
                    label = "APRÈS",
                    photo = afterPhoto,
                    onClick = { showPickerFor = "after" },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "Appuyez sur une photo pour choisir",
                color = NothingGrey3, fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    // Photo picker sheet
    if (showPickerFor != null) {
        PhotoPickerSheet(
            photos = allPhotos,
            onSelect = { photo ->
                if (showPickerFor == "before") beforePhoto = photo
                else afterPhoto = photo
                showPickerFor = null
            },
            onDismiss = { showPickerFor = null }
        )
    }
}

@Composable
private fun PhotoPickerSlot(
    label: String,
    photo: ProgressPhoto?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        NLabel(label, size = 9.sp, color = NothingGrey2)
        Spacer(Modifier.height(8.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, NothingBorderMid, RoundedCornerShape(10.dp))
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            val pickerFileExists = remember(photo?.imagePath) { photo?.let { runCatching { File(it.imagePath).exists() }.getOrDefault(false) } ?: false }
            if (photo != null && pickerFileExists) {
                AsyncImage(
                    model = photo.imagePath,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Date overlay
                Box(
                    Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    NLabel(photo.date, size = 7.5.sp, color = NothingWhite)
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Add, null, tint = NothingGrey3, modifier = Modifier.size(28.dp))
                    Spacer(Modifier.height(6.dp))
                    NLabel("CHOISIR", size = 8.sp, color = NothingGrey3)
                }
            }
        }
    }
}

// ── Draggable comparison slider ───────────────────────────────────────────────

@Composable
private fun ComparisonSlider(before: ProgressPhoto, after: ProgressPhoto) {
    var sliderPos by remember { mutableStateOf(0.5f) }
    var widthPx   by remember { mutableIntStateOf(1) }

    NLabel("GLISSEZ POUR COMPARER", size = 8.sp, color = NothingGrey2,
        modifier = Modifier.fillMaxWidth().wrapContentWidth(Alignment.CenterHorizontally)
            .padding(bottom = 10.dp))

    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(3f / 4f)
            .clip(RoundedCornerShape(12.dp))
            .onSizeChanged { widthPx = it.width }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, drag ->
                    change.consume()
                    sliderPos = (sliderPos + drag / widthPx).coerceIn(0.02f, 0.98f)
                }
            }
    ) {
        // After — full width background
        val afterFileExists = remember(after.imagePath) { runCatching { File(after.imagePath).exists() }.getOrDefault(false) }
        if (afterFileExists) {
            AsyncImage(
                model = after.imagePath,
                contentDescription = "Après",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(Modifier.fillMaxSize().background(NothingDark))
        }

        // Before — clipped to left portion
        val beforeFileExists = remember(before.imagePath) { runCatching { File(before.imagePath).exists() }.getOrDefault(false) }
        if (beforeFileExists) {
            AsyncImage(
                model = before.imagePath,
                contentDescription = "Avant",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(object : Shape {
                        override fun createOutline(
                            size: Size, layoutDirection: LayoutDirection, density: Density
                        ) = Outline.Rectangle(Rect(0f, 0f, size.width * sliderPos, size.height))
                    }),
                contentScale = ContentScale.Crop
            )
        }

        // Divider line
        Box(
            Modifier
                .align(Alignment.TopStart)
                .offset(x = with(LocalDensity.current) { (widthPx * sliderPos).toDp() } - 1.dp)
                .fillMaxHeight()
                .width(2.dp)
                .background(NothingWhite)
        )

        // Handle circle on divider
        Box(
            Modifier
                .align(Alignment.Center)
                .offset(x = with(LocalDensity.current) { (widthPx * sliderPos).toDp() } - 16.dp)
                .size(32.dp)
                .clip(CircleShape)
                .background(NothingWhite),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.SwapHoriz, null, tint = NothingBlack, modifier = Modifier.size(18.dp))
        }

        // Labels
        Box(
            Modifier.align(Alignment.BottomStart).padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) { NLabel("AVANT", size = 8.sp, color = NothingWhite) }

        Box(
            Modifier.align(Alignment.BottomEnd).padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(horizontal = 6.dp, vertical = 3.dp)
        ) { NLabel("APRÈS", size = 8.sp, color = NothingWhite) }
    }

    Spacer(Modifier.height(8.dp))
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        NLabel(before.date, size = 8.sp, color = NothingGrey2)
        NLabel(after.date, size = 8.sp, color = NothingGrey2)
    }
}

// ── Photo picker bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PhotoPickerSheet(
    photos: List<ProgressPhoto>,
    onSelect: (ProgressPhoto) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = NothingDark,
        dragHandle = {
            Box(Modifier.padding(top = 12.dp, bottom = 8.dp)) {
                Box(Modifier.width(36.dp).height(4.dp).clip(RoundedCornerShape(2.dp)).background(NothingBorderMid))
            }
        }
    ) {
        NLabel("CHOISIR UNE PHOTO",
            modifier = Modifier.padding(horizontal = PAD).padding(bottom = 16.dp),
            color = NothingGrey2)

        if (photos.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                NLabel("AUCUNE PHOTO DISPONIBLE", color = NothingGrey3)
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                contentPadding = PaddingValues(start = PAD, end = PAD, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(photos, key = { it.id }) { photo ->
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onSelect(photo) }
                    ) {
        val fileExists = remember(photo.imagePath) { runCatching { File(photo.imagePath).exists() }.getOrDefault(false) }
        if (fileExists) {
                            AsyncImage(
                                model = photo.imagePath,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(Modifier.fillMaxSize().background(NothingDark2), contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.BrokenImage, null, tint = NothingGrey3, modifier = Modifier.size(20.dp))
                            }
                        }
                        // Date bottom overlay
                        Box(
                            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.55f))
                                .padding(2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(photo.date, color = NothingWhite, fontSize = 7.sp, fontFamily = MonoFamily)
                        }
                    }
                }
            }
        }
    }
}
