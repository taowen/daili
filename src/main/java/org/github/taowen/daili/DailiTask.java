package org.github.taowen.daili;

import kilim.Task;

// convenient task to reference corresponding scheduler
public abstract class DailiTask extends Task {

    protected final Scheduler scheduler;

    public DailiTask(Scheduler scheduler) {
        this.scheduler = scheduler;
    }
}
