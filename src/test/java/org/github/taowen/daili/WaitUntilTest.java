package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;

public class WaitUntilTest extends UsingFixture {
    public void testSuccess() {
        final DefaultScheduler scheduler = new DefaultScheduler();
        DailiTask task = new DailiTask(scheduler) {
            @Override
            public void execute() throws Pausable, Exception {
                Object val = getScheduler().waitUntil("hello", System.currentTimeMillis() + 1000);
                exit(val);
            }
        };
        task.run();
        assertThat((String) task.exitResult, not("world"));
        new Task() {
            @Override
            public void execute() throws Pausable, Exception {
                scheduler.trigger("hello", "world");
            }
        }.run();
        while(scheduler.loopOnce()) {
            if ("world".equals(task.exitResult)) {
                return;
            }
        }
    }
}
