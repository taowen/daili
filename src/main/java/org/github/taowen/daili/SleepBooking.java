package org.github.taowen.daili;

import kilim.Task;

public class SleepBooking extends Booking {

    private final long deadline;
    private final Task task;

    public SleepBooking(long deadline, Task task) {
        this.deadline = deadline;
        this.task = task;
    }

    @Override
    public boolean cancelDeadTasks(long currentTimeMillis) {
        task.run(); // cancel the sleep is to run it
        return false; // do not need to run extra selector.selectNow()
    }

    @Override
    public long deadline() {
        return deadline;
    }
}
