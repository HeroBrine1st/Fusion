package ru.herobrine1st.fusion

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

object Pools {
    val SCHEDULED_POOL: ScheduledExecutorService = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors())
}