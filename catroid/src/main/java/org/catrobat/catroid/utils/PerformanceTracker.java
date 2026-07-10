package org.catrobat.catroid.utils;

import android.util.Log;
import org.catrobat.catroid.content.Script;
import java.util.concurrent.atomic.AtomicLong;

public class PerformanceTracker {
    private static final AtomicLong totalPhysicsTime = new AtomicLong(0);
    private static final AtomicLong totalLogicTime = new AtomicLong(0);
    private static final AtomicLong totalRenderTime = new AtomicLong(0);
    private static final AtomicLong frames = new AtomicLong(0);

    public static final AtomicLong formulaEvaluations = new AtomicLong(0);
    public static final AtomicLong blocksExecuted = new AtomicLong(0);

    public static final AtomicLong activeThreads = new AtomicLong(0);
    public static final AtomicLong totalBlockTimeNs = new AtomicLong(0);

    private static long lastLogTime = System.currentTimeMillis();

    public static void recordFrame(long physicsNs, long logicNs, long renderNs) {
        totalPhysicsTime.addAndGet(physicsNs);
        totalLogicTime.addAndGet(logicNs);
        totalRenderTime.addAndGet(renderNs);
        frames.incrementAndGet();

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime >= 1000) {
            printStats();
            reset();
            lastLogTime = currentTime;
        }
    }

    public static void logHeavyScript(Script script, long durationNs) {
        double ms = durationNs / 1_000_000.0;
        String scriptName = (script != null) ? script.getClass().getSimpleName() : "Unknown Script";
        Log.w("CAT_PROFILER_HEAVY", "🔥 HEAVY SCRIPT DETECTED: " + scriptName + " took " + String.format("%.2f", ms) + " ms in a single frame!");
    }

    private static void printStats() {
        long f = frames.get();
        if (f == 0) return;

        double avgPhysics = (totalPhysicsTime.get() / (double) f) / 1_000_000.0;
        double avgLogic = (totalLogicTime.get() / (double) f) / 1_000_000.0;
        double avgRender = (totalRenderTime.get() / (double) f) / 1_000_000.0;
        double avgBlock = (totalBlockTimeNs.get() / (double) f) / 1_000_000.0;
        long avgThreads = activeThreads.get() / f;

        Log.i("CAT_PROFILER", String.format(
                "FPS: %d | Threads: %d | Logic: %.2fms (Blocks alone: %.2fms) | Render: %.2fms | Physics: %.2fms | Formulas: %d | Blocks: %d",
                f, avgThreads, avgLogic, avgBlock, avgRender, avgPhysics, formulaEvaluations.get(), blocksExecuted.get()
        ));
    }

    private static void reset() {
        totalPhysicsTime.set(0);
        totalLogicTime.set(0);
        totalRenderTime.set(0);
        frames.set(0);
        formulaEvaluations.set(0);
        blocksExecuted.set(0);
        activeThreads.set(0);
        totalBlockTimeNs.set(0);
    }
}
