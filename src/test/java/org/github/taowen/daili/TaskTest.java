package org.github.taowen.daili;

import junit.framework.TestCase;
import kilim.Pausable;
import kilim.Task;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class TaskTest extends TestCase {
    public void testExit() {
        Task task = new Task() {
            @Override
            public void execute() throws Pausable {
                exit("hello");
            }
        };
        task.run();
        assertThat("hello", is(task.exitResult));
    }

    public void testPause() {
        Task task = new Task() {
            @Override
            public void execute() throws Pausable {
                exitResult = "1";
                yield();
                exitResult = "2";
            }
        };
        task.run();
        assertThat("1", is(task.exitResult));
        task.run();
        assertThat("2", is(task.exitResult));
    }
}
