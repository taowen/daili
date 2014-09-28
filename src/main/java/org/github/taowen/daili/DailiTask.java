package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;

public abstract class DailiTask extends Task {

    protected final DefaultScheduler scheduler;

    public DailiTask(DefaultScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Override
    public abstract void execute() throws Pausable, Exception;
}
