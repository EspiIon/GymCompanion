package com.gymcompanion.app.common

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Flow de la date du jour (format ISO yyyy-MM-dd).
 * Réémet automatiquement au passage de minuit — évite les ViewModels
 * figés sur la date de leur création quand l'app reste ouverte.
 */
fun todayFlow(): Flow<String> = flow {
    while (currentCoroutineContext().isActive) {
        val now = LocalDateTime.now()
        emit(now.toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE))
        val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay()
        val millis = Duration.between(now, nextMidnight).toMillis().coerceAtLeast(0)
        delay(millis + 1000)
    }
}.distinctUntilChanged()

/** Date du jour au format ISO, calculée à l'appel. */
fun todayStr(): String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
