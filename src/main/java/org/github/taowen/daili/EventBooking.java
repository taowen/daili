package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.Selector;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.TimeoutException;

class EventBooking extends Booking {

    private static final Logger LOGGER = LoggerFactory.getLogger(EventBooking.class);

    private final List<EventBooking> eventBookings;
    private final String eventName;
    private final Runnable task;
    private long deadline;
    private Object eventData;

    public EventBooking(List<EventBooking> eventBookings, String eventName, long deadline, Task task) {
        this.eventBookings = eventBookings;
        if (!this.eventBookings.add(this)) {
            throw new RuntimeException("failed to add event booking: " + this);
        }
        this.eventName = eventName;
        this.deadline = deadline;
        this.task = task;
    }

    @Override
    public boolean cancelDeadTasks(long currentTimeMillis) {
        eventBookings.remove(this);
        deadline = -1;
        try {
            task.run();
        } catch (Exception e) {
            LOGGER.error("failed to run task: " + task, e);
        }
        return false; // do not need to run extra selector.selectNow()
    }

    @Override
    public long deadline() {
        return deadline;
    }

    public Object blocked() throws TimeoutException, Pausable {
        // ...
        // =========== END SCHEDULER THREAD =========

        Task.pause(this);

        // =========== BEGIN WORKER THREAD =========
        if (-1 == this.deadline) {
            this.deadline = Long.MAX_VALUE;
            throw new TimeoutException();
        }
        return eventData;
    }

    public void trigger(Object eventData) {
        this.eventData = eventData;
        try {
            task.run();
        } catch (Exception e) {
            LOGGER.error("failed to run task: " + task, e);
        }
    }
}
