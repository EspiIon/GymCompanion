package com.gymcompanion.app.data.sync

import com.gymcompanion.app.data.repository.GymRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@EntryPoint
@InstallIn(SingletonComponent::class)
interface StepSyncDeps {
    fun gymRepository(): GymRepository
}
