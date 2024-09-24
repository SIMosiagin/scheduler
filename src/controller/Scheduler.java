package controller;

import constants.Constants;
import model.AbstractTask;

import java.util.concurrent.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Scheduler<T extends AbstractTask> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    private final BlockingQueue<T> queue = new PriorityBlockingQueue<>(Constants.CAPACITY);

    private final Executor pool =  Executors.newFixedThreadPool(Constants.THREADS);

    private Thread subThread ;

    private boolean running = true;

    public void add(T entity) {
        subThread.interrupt();
        lock.writeLock().lock();
        try {
            this.queue.add(entity);
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void start() {
        subThread = new Thread(() -> {
            while (running) {
                executeDueTasks();
            }
        });
        subThread.start();
    }

    public void stop() {
        subThread.interrupt();
        lock.writeLock().lock();
        try {
            running = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void executeDueTasks() {
        synchronized (lock.readLock()) {
            try {
                if (queue.isEmpty()) {
                    lock.readLock().wait(); // Ждем, пока не появится задача
                } else {
                    lock.writeLock().lock();
                    try {
                        exetuteTasks();
                    } finally {
                        lock.writeLock().unlock();
                    }
                    if (!queue.isEmpty()) {
                        lock.readLock().wait(queue.peek().delay);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void exetuteTasks() {
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
    }
}
