package controller;

import constants.Constants;
import model.AbstractTask;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Scheduler<T extends AbstractTask> {

    private CompletableFuture<Void> addEvent = new CompletableFuture<Void>();
    private CompletableFuture<Void> fireTimeEvent = new CompletableFuture<Void>();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private final BlockingQueue<T> queue = new PriorityBlockingQueue<>(Constants.CAPACITY);

    private final Executor pool =  Executors.newFixedThreadPool(Constants.THREADS);


    private boolean running = true;

    public void add(T entity) {
        lock.writeLock().lock();
        try {
            addEvent.thenRun(() -> {
                    this.queue.add(entity);
            }).join();
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void start() {
        while (running) {
            CompletableFuture.anyOf(addEvent, fireTimeEvent)
                    .thenRun(this::exetuteTasks)
                    .thenRun(this::executeDueTasks);
        }
    }

    public void stop() {
        lock.writeLock().lock();
        try {
            running = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void executeDueTasks() {
        lock.writeLock().lock();
        try {
            if(queue.isEmpty()) {
                fireTimeEvent.wait();
            } else {
                if(!queue.isEmpty()) {
                    fireTimeEvent.wait(queue.peek().delay);
                }
            }
            lock.writeLock().unlock();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private void exetuteTasks() {
        lock.writeLock().lock();
        try {
            long currentTime = System.currentTimeMillis();
            while(!queue.isEmpty()) {
                if(!queue.peek().isPaused && currentTime >= queue.peek().executionTime) {
                    T entity = queue.poll();
                    pool.execute(entity);
                    if(entity.isRepeatable) {
                        entity.executionTime += entity.delay;
                        queue.add(entity);
                    }
                    else {
                        entity.isPaused = true;
                    }
                }
                else {
                    break;
                }
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
}
