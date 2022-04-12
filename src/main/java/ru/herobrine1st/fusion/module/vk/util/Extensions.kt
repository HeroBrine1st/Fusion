package ru.herobrine1st.fusion.module.vk.util

import ru.herobrine1st.fusion.module.vk.model.Photo

fun Photo.getLargestSize(): Photo.Size {
    return sizes.maxByOrNull { it.width }!!
}