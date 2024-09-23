package controller;

import Constants.Constants;
import model.AbstractTask;

import java.util.PriorityQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Scheduler<T extends AbstractTask> {

    private final Object lock = new Object();

    private long nextFireTime = Long.MAX_VALUE;

    private final PriorityQueue<T> queue = new PriorityQueue<>(Constants.CAPACITY);

    private final Executor pool =  Executors.newFixedThreadPool(Constants.THREADS);

    private boolean running = true;

    public void add(T entity) {
        synchronized (lock) {
            queue.add(entity);
            nextFireTime = Math.min(nextFireTime, System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(entity.delay));
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
            while (queue.isEmpty()) {
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }

            long currentTime = System.currentTimeMillis();
            while(!queue.isEmpty()) {
                if(queue.peek().isPaused != true && currentTime >= queue.peek().executionTime) {
                    T entity = queue.poll();
                    pool.execute(entity);
                    if(entity.isRepeatable) {
                        entity.executionTime = currentTime + entity.delay;
                        queue.add(entity);
                    }
                    else {
                        entity.isPaused = true;
                    }
                }
            }

            if(!queue.isEmpty()) {
                nextFireTime = Long.MAX_VALUE;
            }
            else {
                nextFireTime = queue.peek().delay;
            }


            try {
                lock.wait(nextFireTime);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
            }
        }

    }

}
