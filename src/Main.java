import controller.Scheduler;
import model.Task;

import java.util.concurrent.TimeUnit;

public class Main {

    public static void main(String[] args) {

        Scheduler<Task> scheduler = new Scheduler<Task>();



        scheduler.start();


        test1(scheduler);

        try {
            while(true){

            }
        } catch (Exception e) {

        } finally {
            scheduler.stop();
        }
    }

    public static void test2(Scheduler<Task> scheduler) {
        Task t1 = new Task(10, "t1", false, false);
        Task t2 = new Task(20, "t2", false, false);
        Task t3 = new Task(1, "t3", false, false);
        Task t4 = new Task(40, "t4", false, false);

        scheduler.add(t1);
        scheduler.add(t2);
        scheduler.add(t3);
        scheduler.add(t4);

    }

    public static void test1 (Scheduler<Task> scheduler) {

        for (int i = 0; i < 10; i++) {
            Thread t = new Thread(() -> {
               for(int j = 0; j < 10; j++) {
                   Task task = new Task(j, "name =" + j + " thread name =" + Thread.currentThread(), false,false);
                   scheduler.add(task);
               }
            });

            t.start();
        }

        try {
            TimeUnit.SECONDS.sleep(10);
            Task task = new Task(1, "immediate", false,false);
            scheduler.add(task);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
