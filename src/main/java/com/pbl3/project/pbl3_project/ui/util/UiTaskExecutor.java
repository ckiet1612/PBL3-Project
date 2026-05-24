package com.pbl3.project.pbl3_project.ui.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import javafx.concurrent.Task;

public final class UiTaskExecutor {
    private static final AtomicInteger THREAD_ID = new AtomicInteger();
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(
        Math.max(4, Math.min(12, Runtime.getRuntime().availableProcessors())),
        new ThreadFactory() {
            @Override
            public Thread newThread(Runnable runnable) {
                Thread thread = new Thread(runnable, "ui-bg-" + THREAD_ID.incrementAndGet());
                thread.setDaemon(true);
                return thread;
            }
        }
    );

    private UiTaskExecutor() {
    }

    public static void execute(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        EXECUTOR.execute(runnable);
    }

    public static void execute(Task<?> task, String taskName) {
        if (task == null) {
            return;
        }
        EXECUTOR.execute(() -> {
            Thread current = Thread.currentThread();
            String previousName = current.getName();
            if (taskName != null && !taskName.isBlank()) {
                current.setName(taskName);
            }
            try {
                task.run();
            } finally {
                current.setName(previousName);
            }
        });
    }
}
