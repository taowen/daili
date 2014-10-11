package org.github.taowen.daili;

import kilim.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class PinnedWorker implements Worker, Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private final BlockingQueue<Task> readyTasks = new ArrayBlockingQueue<Task>(1024);

    @Override
    public void loop() {
        try {
            while (true) {
                Task task = readyTasks.take();
                try {
                    task._runExecute();
                } catch (Throwable e) {
                    LOGGER.error("failed to execute task", e);
                }
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void submit(Task task) {
        try {
            readyTasks.put(task);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void run() {
        loop();
    }
}
