package com.pbl3.project.pbl3_project.ui.util;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import javafx.application.Platform;

public final class AsyncPageCache<T> {

    private static final long DEFAULT_TTL_NANOS = java.time.Duration.ofMinutes(10).toNanos();

    private record CacheEntry<T>(T value, long storedAtNanos) {
    }

    private final Map<Object, CacheEntry<T>> cache;
    private final Map<Object, CompletableFuture<T>> inFlight = new ConcurrentHashMap<>();
    private final AtomicLong generation = new AtomicLong();
    private final long ttlNanos;

    public AsyncPageCache(int maxEntries) {
        this(maxEntries, DEFAULT_TTL_NANOS);
    }

    public AsyncPageCache(int maxEntries, java.time.Duration ttl) {
        this(maxEntries, ttl == null ? DEFAULT_TTL_NANOS : ttl.toNanos());
    }

    private AsyncPageCache(int maxEntries, long ttlNanos) {
        int boundedMaxEntries = Math.max(4, maxEntries);
        this.ttlNanos = Math.max(java.time.Duration.ofSeconds(1).toNanos(), ttlNanos);
        this.cache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Object, CacheEntry<T>> eldest) {
                return size() > boundedMaxEntries;
            }
        });
    }

    public T get(Object key) {
        if (key == null) {
            return null;
        }
        synchronized (cache) {
            CacheEntry<T> entry = cache.get(key);
            if (entry == null) {
                return null;
            }
            if (System.nanoTime() - entry.storedAtNanos() > ttlNanos) {
                cache.remove(key);
                return null;
            }
            return entry.value();
        }
    }

    public void put(Object key, T value) {
        if (key == null || value == null) {
            return;
        }
        synchronized (cache) {
            cache.put(key, new CacheEntry<>(value, System.nanoTime()));
        }
    }

    public void clear() {
        synchronized (cache) {
            cache.clear();
        }
        inFlight.clear();
        generation.incrementAndGet();
    }

    public CompletableFuture<T> load(Object key, Callable<T> work, String threadName) {
        if (work == null) {
            CompletableFuture<T> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalArgumentException("Page load work is required"));
            return failed;
        }
        if (key == null) {
            CompletableFuture<T> future = new CompletableFuture<>();
            startLoad(null, work, future, generation.get(), threadName);
            return future;
        }

        T cachedValue = get(key);
        if (cachedValue != null) {
            return CompletableFuture.completedFuture(cachedValue);
        }

        while (true) {
            CompletableFuture<T> existing = inFlight.get(key);
            if (existing != null) {
                return existing;
            }
            CompletableFuture<T> future = new CompletableFuture<>();
            CompletableFuture<T> raced = inFlight.putIfAbsent(key, future);
            if (raced == null) {
                startLoad(key, work, future, generation.get(), threadName);
                return future;
            }
        }
    }

    public void prefetch(Object key, Callable<T> work, Consumer<Throwable> onFailed, String threadName) {
        if (key == null || get(key) != null) {
            return;
        }
        CompletableFuture<T> future = load(key, work, threadName);
        if (onFailed == null) {
            return;
        }
        future.whenComplete((value, throwable) -> {
            if (throwable == null) {
                return;
            }
            Throwable unwrapped = unwrapCompletionException(throwable);
            Platform.runLater(() -> onFailed.accept(unwrapped));
        });
    }

    private void startLoad(
        Object key,
        Callable<T> work,
        CompletableFuture<T> future,
        long loadGeneration,
        String threadName
    ) {
        javafx.concurrent.Task<T> task = new javafx.concurrent.Task<>() {
            @Override
            protected T call() throws Exception {
                return work.call();
            }
        };
        task.setOnSucceeded(event -> {
            T value = task.getValue();
            if (key != null && loadGeneration == generation.get()) {
                put(key, value);
            }
            if (key != null) {
                inFlight.remove(key, future);
            }
            future.complete(value);
        });
        task.setOnFailed(event -> {
            if (key != null) {
                inFlight.remove(key, future);
            }
            Throwable exception = task.getException();
            future.completeExceptionally(exception != null ? exception : new IllegalStateException("Page load failed"));
        });
        UiTaskExecutor.execute(task, threadName == null || threadName.isBlank() ? "page-prefetch-loader" : threadName);
    }

    private static Throwable unwrapCompletionException(Throwable throwable) {
        Throwable current = throwable;
        while (current instanceof CompletionException && current.getCause() != null) {
            current = current.getCause();
        }
        return current;
    }
}
