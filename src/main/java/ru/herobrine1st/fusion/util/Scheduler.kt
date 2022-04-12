package ru.herobrine1st.fusion.util

import dev.minn.jda.ktx.getDefaultScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.herobrine1st.fusion.Pools
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

private val coroutineScope = getDefaultScope(
    job = SupervisorJob()
)

fun scheduleAtFixedRate(unit: TimeUnit, period: Long, initialDelay: Long = period, block: suspend () -> Unit): ScheduledFuture<*> {
    return Pools.SCHEDULED_POOL.scheduleAtFixedRate({
        coroutineScope.launch { block() }
    }, initialDelay, period, unit)
}