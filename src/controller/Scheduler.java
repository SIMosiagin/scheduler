package controller;

import constants.Constants;
import model.AbstractTask;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Scheduler<T extends AbstractTask> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Condition wCond = lock.writeLock().newCondition();

    private final BlockingQueue<T> queue = new PriorityBlockingQueue<>(Constants.CAPACITY);

    private final Executor pool =  Executors.newFixedThreadPool(Constants.THREADS);

    private Thread subThread ;

    private boolean running = true;

    public void add(T entity) {
        lock.writeLock().lock();
        try {
            this.queue.add(entity);
            wCond.signalAll();

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
        lock.writeLock().lock();
        try {
            running = false;
            wCond.signalAll();
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void executeDueTasks() {

        lock.writeLock().lock();

        try {
            if(queue.isEmpty()) {
                wCond.await();
            } else {
                exetuteTasks();
                if(!queue.isEmpty()) {
                    wCond.await(queue.peek().delay, TimeUnit.MILLISECONDS);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } finally {
            lock.writeLock().unlock();
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
