package ru.herobrine1st.fusion.module.vk.exceptions

class VkApiException(val code: Int, override val message: String): Exception()