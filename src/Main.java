import controller.Scheduler;
import model.Task;

public class Main {

    public static void main(String[] args) {

        Scheduler<Task> scheduler = new Scheduler<Task>();

        Task t1 = new Task(10, "t1", false, false);
        Task t2 = new Task(20, "t2", false, false);
        Task t3 = new Task(1, "t3", false, false);
        Task t4 = new Task(40, "t4", false, false);

        scheduler.start();
        scheduler.add(t1);
        scheduler.add(t2);
        scheduler.add(t3);
        scheduler.add(t4);

        try {
            while(true){

            }
        } catch (Exception e) {

        } finally {
            scheduler.stop();
        }






    }
}
