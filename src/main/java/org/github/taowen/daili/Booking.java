package org.github.taowen.daili;

import kilim.PauseReason;

public abstract class Booking implements PauseReason, Comparable<Booking> {

    public abstract boolean cancelDeadTasks(long currentTimeMillis);

    public abstract long deadline();

    @Override
    public int compareTo(Booking that) {
        long delta = that.deadline() - this.deadline();
        if (delta > 0) {
            return -1;
        } else if (delta < 0) {
            return 1;
        }
        return 0;
    }
}
