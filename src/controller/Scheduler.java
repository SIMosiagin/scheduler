package controller;

import Constants.Constants;
import model.AbstractTask;

import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class Scheduler<T extends AbstractTask> {

    private final Object lock = new Object();

    private final PriorityQueue<T> queue = new PriorityQueue<>(Constants.CAPACITY);

    private final Executor pool =  Executors.newFixedThreadPool(Constants.THREADS);

    private boolean running = true;

    public void add(T entity) {
        synchronized (lock) {
            queue.add(entity);
            lock.notify();
        }
    }

    public void start() {
        Thread subThread = new Thread(() -> {
            while (running) {
                executeDueTasks();
            }
        });
        subThread.start();
    }

    public void stop() {
        running = false;
        synchronized (lock) {
            lock.notify();
        }
    }

    private void executeDueTasks() {
        synchronized (lock) {
            try {
                if(queue.isEmpty()) {
                    lock.wait();
                }
                else {
                    lock.wait(queue.peek().delay);
                }
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }

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
}
