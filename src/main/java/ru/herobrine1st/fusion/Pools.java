package ru.herobrine1st.fusion;

import java.util.concurrent.*;
import java.util.function.Supplier;

public class Pools {
    public final static ExecutorService CONNECTION_POOL = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    public final static ScheduledExecutorService SCHEDULED_POOL = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
}
