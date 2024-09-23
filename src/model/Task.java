package model;

public class Task extends AbstractTask {


    public Task(long delay, String name, boolean isRepeatable, boolean isPaused) {
        super(delay, name, isRepeatable, isPaused);
    }

    @Override
    public void run() {
        super.run();
    }


    @Override
    public int compareTo(Object obj) {
        Task other = (Task) obj;

        if (this.isPaused) {
            return -1;
        }

        return Long.compare(this.delay, other.delay);
    }
}