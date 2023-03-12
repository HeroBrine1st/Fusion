package ru.herobrine1st.fusion.module.vk.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class User(
    val id: Long,
    @Suppress("PropertyName") val photo_200: String?,
    val firstName: String,
    val lastName: String,
    val canAccessClosed: Boolean,
    val isClosed: Boolean
)