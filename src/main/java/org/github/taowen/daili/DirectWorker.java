package org.github.taowen.daili;

import kilim.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// run task directly in scheduler thread
public class DirectWorker implements Worker {
    private static final Logger LOGGER = LoggerFactory.getLogger(DirectWorker.class);

    @Override
    public void loop() {
        // does not need to run in a new thread
    }

    @Override
    public void submit(Task task) {
        try {
            task._runExecute();
        } catch (Exception e) {
            LOGGER.error("failed to execute task: " + task, e);
        }
    }
}
