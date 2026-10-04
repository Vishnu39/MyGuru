package com.vish.myguru.features.vacabulary.model

import kotlinx.serialization.Serializable

@Serializable
data class Word(
    val id: String,
    val germanTerm: String,
    val englishTranslation: String,
    val article: String,
    val isMastered: Boolean = false,
    val boxLevel: Int = 1,
    val nextReviewEpoch: Long = 0L
)