package ru.herobrine1st.fusion.module.vk.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Group(
    val id: Int,
    val name: String,
    val screenName: String,
    val isClosed: Boolean,
    val hasPhoto: Boolean,
    val photo_200: String
)
