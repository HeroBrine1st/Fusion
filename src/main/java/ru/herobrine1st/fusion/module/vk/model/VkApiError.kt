package ru.herobrine1st.fusion.module.vk.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class VkApiError(
    val errorCode: Int,
    val errorMsg: String
)