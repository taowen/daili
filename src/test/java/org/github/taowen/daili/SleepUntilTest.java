package org.github.taowen.daili;

import kilim.Pausable;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class SleepUntilTest extends UsingFixture {
    public void test() {
        final TestScheduler scheduler = new TestScheduler(this);
        scheduler.fixedCurrentTimeMillis = System.currentTimeMillis();
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                scheduler().sleepUntil(scheduler.fixedCurrentTimeMillis + 1000);
                exit("sleep done");
            }
        };
        task.run();
        assertThat(task.exitResult, not((Object) "sleep done"));
        scheduler.fixedCurrentTimeMillis += 3000;
        scheduler.loopOnce();
        assertThat(task.exitResult, is((Object)"sleep done"));
    }
}
