package org.github.taowen.daili;

import junit.framework.TestCase;
import kilim.Pausable;
import kilim.Task;

public class TaskTest extends TestCase {
    public void testExit() {
        Task task = new Task() {
            @Override
            public void execute() throws Pausable {
                exit("hello");
            }
        };
        task.run();
        assertEquals("hello", task.exitResult);
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
        assertEquals("1", task.exitResult);
        task.run();
        assertEquals("2", task.exitResult);
    }
}
