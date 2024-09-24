package model;

import java.util.concurrent.TimeUnit;

public abstract class AbstractTask implements RunnableTask {

    public long delay;
    public long executionTime;

    public String name;
    public boolean isRepeatable;
    public boolean isPaused;


    public AbstractTask(long delay, String name, boolean isRepeatable, boolean isPaused) {
        this.executionTime = System.currentTimeMillis() + delay;
        this.delay = delay;
        this.name = name;
        this.isRepeatable = isRepeatable;
        this.isPaused = isPaused;
    }

    @Override
    public void run() {
        System.out.println("hello from task =" + this.name);
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
