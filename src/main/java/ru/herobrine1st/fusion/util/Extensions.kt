package ru.herobrine1st.fusion.util

import java.time.Duration
import java.time.Instant

operator fun Instant.minus(other: Instant): Duration {
    return Duration.between(other, this) // start, end: in arithmetics larger (end) minus smaller (start) returns positive value
}