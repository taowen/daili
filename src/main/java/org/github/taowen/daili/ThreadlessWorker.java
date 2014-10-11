package org.github.taowen.daili;

import kilim.Task;

// run task directly in scheduler thread
public class ThreadlessWorker implements Worker {
    @Override
    public void submit(Task task) {
        task._runExecute();
    }
}
