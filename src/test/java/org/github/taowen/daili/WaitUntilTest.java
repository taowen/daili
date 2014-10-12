package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;

import java.util.concurrent.TimeoutException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class WaitUntilTest extends UsingFixture {
    public void testSuccess() {
        final DefaultScheduler scheduler = new DefaultScheduler();
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                Object val = scheduler().waitUntil("hello", System.currentTimeMillis() + 1000);
                exit(val);
            }
        };
        task.run();
        assertThat((String) task.exitResult, not("world"));
        new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                scheduler.trigger("hello", "world");
            }
        }.run();
        while (scheduler.loopOnce()) {
            if ("world".equals(task.exitResult)) {
                return;
            }
        }
    }

    public void testTimeout() {
        final TestScheduler scheduler = new TestScheduler(this);
        scheduler.fixedCurrentTimeMillis = System.currentTimeMillis();
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                try {
                    scheduler().waitUntil("hello", scheduler.fixedCurrentTimeMillis + 1000);
                } catch (TimeoutException e) {
                    exit("timeout");
                }
            }
        };
        task.run();
        scheduler.fixedCurrentTimeMillis += 3000;
        scheduler.loopOnce();
        assertThat(task.exitResult, is((Object)"timeout"));
    }
}
