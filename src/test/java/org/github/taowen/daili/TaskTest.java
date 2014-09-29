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
        task.resume();
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
        task.resume();
        assertEquals("1", task.exitResult);
        task.resume();
        assertEquals("2", task.exitResult);
    }
}
