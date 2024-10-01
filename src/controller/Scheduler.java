package controller;

import constants.Constants;
import model.AbstractTask;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class Scheduler<T extends AbstractTask> {
    private final AtomicReference<CompletableFuture<Void>> waker = new AtomicReference<>(new CompletableFuture<Void>());
    private final PriorityBlockingQueue<T> queue = new PriorityBlockingQueue<>(Constants.CAPACITY);
    private final Executor pool = Executors.newFixedThreadPool(Constants.THREADS);
    private volatile boolean running = true;

    private void wake() {
        waker.getAndSet(new CompletableFuture<>()).complete(null);
    }

    public void add(T entity) {
        this.queue.add(entity);
        wake();
    }

    public void start() {
        new Thread(() -> {
            while (running) {
                executeDueTasks();
            }
        }).start();
    }

    public void stop() {
        this.running = false;
        wake();
    }

    private void executeDueTasks() {
         if (queue.isEmpty()) {
            waker.get().join();
        } else {
            long nextFireTime = queue.peek().executionTime - System.currentTimeMillis();
            if(nextFireTime > 0) {
                CompletableFuture delayedWaker = CompletableFuture.runAsync(() -> {
                    try {
                        Thread.sleep(nextFireTime);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                });
                CompletableFuture.anyOf(waker.get(), delayedWaker).join();
            }
            else {
                exetuteTasks();
            }
        }
    }

    private void exetuteTasks() {
        long currentTime = System.currentTimeMillis();
        while (!queue.isEmpty()) {
            if (!queue.peek().isPaused && currentTime >= queue.peek().executionTime) {
                T entity = queue.poll();
                pool.execute(entity);
                if (entity.isRepeatable) {
                    entity.executionTime += entity.delay;
                    queue.add(entity);
                } else {
                    entity.isPaused = true;
                }
            } else {
                break;
            }
        }
    }
}
