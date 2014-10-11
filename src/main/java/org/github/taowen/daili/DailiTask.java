package org.github.taowen.daili;

import kilim.Task;

public abstract class DailiTask extends Task {

    private final static Worker DIRECT_WORKER = new DirectWorker();
    private final Scheduler scheduler;
    private final Worker worker;

    public DailiTask(Scheduler scheduler) {
        this(scheduler, DIRECT_WORKER);
    }

    public DailiTask(Scheduler scheduler, Worker worker) {
        this.scheduler = scheduler;
        this.worker = worker;
    }

    public Scheduler scheduler() {
        return scheduler;
    }

    @Override
    public void run() {
        worker.submit(this);
    }
}
