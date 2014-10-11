package org.github.taowen.daili;

import kilim.Task;

public abstract class DailiTask extends Task {

    private final static Worker THREADLESS_WORKER = new ThreadlessWorker();
    private final Scheduler scheduler;
    private final Worker worker;

    public DailiTask(Scheduler scheduler) {
        this(scheduler, THREADLESS_WORKER);
    }

    public DailiTask(Scheduler scheduler, Worker worker) {
        this.scheduler = scheduler;
        this.worker = worker;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    @Override
    public void run() {
        worker.submit(this);
    }
}
