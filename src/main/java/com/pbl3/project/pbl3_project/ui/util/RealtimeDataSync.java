package com.pbl3.project.pbl3_project.ui.util;

import com.pbl3.project.pbl3_project.service.RealtimeDataSyncService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javafx.scene.Node;
import javafx.util.Duration;

public final class RealtimeDataSync {
    private static final Duration DEFAULT_PRODUCT_POLL_INTERVAL = Duration.seconds(2);

    private RealtimeDataSync() {
    }

    public static void installProductInventoryRefresh(
        Node owner,
        RealtimeDataSyncService syncService,
        Runnable onChanged
    ) {
        installProductInventoryRefresh(owner, syncService, onChanged, DEFAULT_PRODUCT_POLL_INTERVAL);
    }

    public static void installProductInventoryRefresh(
        Node owner,
        RealtimeDataSyncService syncService,
        Runnable onChanged,
        Duration interval
    ) {
        if (owner == null || syncService == null || onChanged == null) {
            return;
        }
        Duration pollInterval = interval == null || interval.lessThanOrEqualTo(Duration.ZERO)
            ? DEFAULT_PRODUCT_POLL_INTERVAL
            : interval;
        AtomicBoolean active = new AtomicBoolean(false);
        AtomicBoolean inFlight = new AtomicBoolean(false);
        AtomicReference<String> lastToken = new AtomicReference<>();
        javafx.animation.PauseTransition timer = new javafx.animation.PauseTransition(pollInterval);

        Runnable[] scheduleNextRef = new Runnable[1];
        scheduleNextRef[0] = () -> {
            if (active.get() && owner.getScene() != null) {
                timer.playFromStart();
            }
        };

        timer.setOnFinished(event -> {
            if (!active.get() || owner.getScene() == null || !inFlight.compareAndSet(false, true)) {
                scheduleNextRef[0].run();
                return;
            }
            javafx.concurrent.Task<String> task = new javafx.concurrent.Task<>() {
                @Override
                protected String call() {
                    return syncService.productInventoryToken();
                }
            };
            task.setOnSucceeded(done -> {
                inFlight.set(false);
                if (active.get() && owner.getScene() != null) {
                    String nextToken = task.getValue();
                    String previousToken = lastToken.getAndSet(nextToken);
                    if (previousToken != null && nextToken != null && !previousToken.equals(nextToken)) {
                        onChanged.run();
                    }
                }
                scheduleNextRef[0].run();
            });
            task.setOnFailed(done -> {
                inFlight.set(false);
                scheduleNextRef[0].run();
            });
            Thread worker = new Thread(task, "realtime-product-inventory-poll");
            worker.setDaemon(true);
            worker.start();
        });

        owner.sceneProperty().addListener((obs, oldScene, newScene) -> {
            boolean attached = newScene != null;
            active.set(attached);
            if (attached) {
                scheduleNextRef[0].run();
            } else {
                timer.stop();
            }
        });
        if (owner.getScene() != null) {
            active.set(true);
            scheduleNextRef[0].run();
        }
    }
}
