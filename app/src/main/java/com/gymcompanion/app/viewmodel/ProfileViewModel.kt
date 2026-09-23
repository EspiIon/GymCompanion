package com.gymcompanion.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gymcompanion.app.data.model.UserProfile
import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repo: GymRepository
) : ViewModel() {

    val profile: StateFlow<UserProfile?> =
        repo.getUserProfile()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    /**
     * Sauvegarde du profil. L'objectif calorique est recalculé à la volée (TDEE)
     * par Dashboard/Nutrition — aucune écriture redondante ici.
     */
    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch { repo.saveUserProfile(profile) }
    }
}
