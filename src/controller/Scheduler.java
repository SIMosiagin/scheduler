package controller;

import constants.Constants;
import model.AbstractTask;

import java.util.concurrent.*;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Scheduler<T extends AbstractTask> {

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);
    private final Condition notEmpty = lock.writeLock().newCondition();

    private final BlockingQueue<T> queue = new PriorityBlockingQueue<>(Constants.CAPACITY);

    private final Executor pool =  Executors.newFixedThreadPool(Constants.THREADS);

    private Thread subThread ;

    private boolean running = true;

    public void add(T entity) {
        subThread.interrupt();
        lock.writeLock().lock();
        try {
            queue.add(entity);
            notEmpty.signal(); // Уведомляем, что очередь не пустая
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
        subThread.notify();
        lock.writeLock().lock();
        try {
            running = false;
        } finally {
            lock.writeLock().unlock();
        }

    }

    private void executeDueTasks() {
        try {
            lock.readLock().lock();
             try {
                 if (queue.isEmpty()) {
                     subThread.wait(); // Ждем, пока не появится задача
                 } else {
                     lock.readLock().unlock(); // Освобождаем readLock перед переходом в writeLock
                     lock.writeLock().lock();
                     try {
                         exetuteTasks();
                     } finally {
                         lock.writeLock().unlock();
                     }
                     lock.readLock().lockInterruptibly(); // Захватываем readLock обратно после выполнения задач
                 }

            } finally {
                lock.readLock().unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // Восстанавливаем статус прерывания
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
