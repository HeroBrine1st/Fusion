package ru.herobrine1st.fusion;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Pools {
    public final static ExecutorService CONNECTION_POOL = Executors.newWorkStealingPool();
}
