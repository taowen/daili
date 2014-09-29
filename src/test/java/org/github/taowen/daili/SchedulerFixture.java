package org.github.taowen.daili;

import kilim.Pausable;

import java.io.IOException;

public class SchedulerFixture extends UsingFixture.Fixture {
    public Scheduler scheduler;

    public SchedulerFixture(UsingFixture testCase) {
        super(testCase);
    }

    @Override
    public void execute() throws Pausable, Exception {
        scheduler = new Scheduler() {
            @Override
            protected int doSelect() throws IOException {
                return selector.selectNow();
            }
        };
        yield();
        scheduler.close();
    }
}
