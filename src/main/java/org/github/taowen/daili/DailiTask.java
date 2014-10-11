package org.github.taowen.daili;

import kilim.Task;

public abstract class DailiTask extends Task {

    private final Scheduler scheduler;

    public DailiTask(Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }
}
