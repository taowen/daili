package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class Scheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private Selector selector;
    private Queue<Task> readyTasks = new LinkedList<Task>();

    {
        try {
            selector = Selector.open();
        } catch (IOException e) {
            LOGGER.error("failed to open selector", e);
        }
    }

    public SocketChannel accept(ServerSocketChannel serverSocketChannel) throws IOException, Pausable {
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (null != socketChannel) {
            return socketChannel;
        }
        SelectionKey selectionKey = serverSocketChannel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new WaitingSelectorIO());
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_ACCEPT);
        }
        WaitingSelectorIO waitingSelectorIO = (WaitingSelectorIO) selectionKey.attachment();
        waitingSelectorIO.acceptBlockedAt = System.currentTimeMillis();
        waitingSelectorIO.acceptTask = (Task) Task.getCurrentTask();
        selectionKey.attach(waitingSelectorIO);
        Task.pause(waitingSelectorIO);
        return serverSocketChannel.accept();
    }

    public void callSoon(Task task) {
        if (!readyTasks.offer(task)) {
            throw new RuntimeException("failed to add ready task");
        }
    }

    public void loop() throws IOException {
        while (true) {
            executeReadyTasks();
            selector.select();
            Set<SelectionKey> selectionKeys = selector.selectedKeys();
            for (SelectionKey selectionKey : selectionKeys) {
                WaitingSelectorIO waitingSelectorIO = (WaitingSelectorIO) selectionKey.attachment();
                if (selectionKey.isAcceptable()) {
                    Task taskToCall = waitingSelectorIO.acceptTask;
                    waitingSelectorIO.acceptBlockedAt = 0;
                    waitingSelectorIO.acceptTask = null;
                    callSoon(taskToCall);
                }
            }
        }
    }

    private void executeReadyTasks() {
        Task task;
        while((task = readyTasks.poll()) != null) {
            executeTask(task);
        }
    }

    private void executeTask(Task task) {
        try {
            task.resume();
        } catch (Exception e) {
            LOGGER.error("failed to execute task: " + task, e);
        }
    }
}
