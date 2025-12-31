package org.catrobat.catroid.utils;

import android.util.Log;

public class PerformanceTracker {
    private static long totalPhysicsTime = 0;
    private static long totalLogicTime = 0;
    private static long totalRenderTime = 0;
    private static long frames = 0;

    public static long formulaEvaluations = 0;
    public static long blocksExecuted = 0;

    private static long lastLogTime = System.currentTimeMillis();

    public static void recordFrame(long physicsNs, long logicNs, long renderNs) {
        totalPhysicsTime += physicsNs;
        totalLogicTime += logicNs;
        totalRenderTime += renderNs;
        frames++;

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastLogTime >= 1000) {
            printStats();
            reset();
            lastLogTime = currentTime;
        }
    }

    private static void printStats() {
        if (frames == 0) return;

        double avgPhysics = (totalPhysicsTime / (double) frames) / 1_000_000.0;
        double avgLogic = (totalLogicTime / (double) frames) / 1_000_000.0;
        double avgRender = (totalRenderTime / (double) frames) / 1_000_000.0;

        Log.i("CAT_PROFILER", String.format(
                "FPS: %d | Logic: %.2f ms | Render: %.2f ms | Physics: %.2f ms | Formulas: %d | Blocks: %d",
                frames, avgLogic, avgRender, avgPhysics, formulaEvaluations, blocksExecuted
        ));
    }

    private static void reset() {
        totalPhysicsTime = 0;
        totalLogicTime = 0;
        totalRenderTime = 0;
        frames = 0;
        formulaEvaluations = 0;
        blocksExecuted = 0;
    }
}