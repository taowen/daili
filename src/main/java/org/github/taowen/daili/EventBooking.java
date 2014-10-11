package org.github.taowen.daili;

import kilim.PauseReason;

class EventBooking implements PauseReason, Comparable<EventBooking> {
    private final String eventName;
    private final Runnable task;
    private final long deadline;
    public Object eventData;

    EventBooking(String eventName, Runnable task, long deadline) {
        this.eventName = eventName;
        this.task = task;
        this.deadline = deadline;
    }

    public String eventName() {
        return eventName;
    }

    public Runnable task() {
        return task;
    }

    public long deadline() {
        return deadline;
    }

    @Override
    public int compareTo(EventBooking that) {
        long delta = that.deadline - this.deadline;
        if (delta > 0) {
            return -1;
        } else if (delta < 0) {
            return 1;
        }
        return 0;
    }
}
