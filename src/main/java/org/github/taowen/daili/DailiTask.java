package org.github.taowen.daili;

import kilim.Task;

public abstract class DailiTask extends Task {

    private final Scheduler scheduler;
    private final Worker worker;

    public DailiTask(Scheduler scheduler) {
        this.scheduler = scheduler;
        this.worker = new ThreadlessWorker();
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void run() {
        worker.submit(this);
    }
}
