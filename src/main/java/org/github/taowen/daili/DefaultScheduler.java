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

public class DefaultScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    private Selector selector;
    private Queue<DailiTask> readyTasks = new LinkedList<DailiTask>();

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
            selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, new WaitingOnIO());
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_ACCEPT);
        }
        WaitingOnIO waitingOnIO = (WaitingOnIO) selectionKey.attachment();
        waitingOnIO.acceptBlockedAt = System.currentTimeMillis();
        waitingOnIO.acceptTask = (DailiTask) Task.getCurrentTask();
        selectionKey.attach(waitingOnIO);
        Task.pause(waitingOnIO);
        return serverSocketChannel.accept();
    }

    public void callSoon(DailiTask task) {
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
                WaitingOnIO waitingOnIO = (WaitingOnIO) selectionKey.attachment();
                if (selectionKey.isAcceptable()) {
                    DailiTask taskToCall = waitingOnIO.acceptTask;
                    waitingOnIO.acceptBlockedAt = 0;
                    waitingOnIO.acceptTask = null;
                    callSoon(taskToCall);
                }
            }
        }
    }

    private void executeReadyTasks() {
        DailiTask task;
        while((task = readyTasks.poll()) != null) {
            executeTask(task);
        }
    }

    private void executeTask(DailiTask task) {
        try {
            task.resume();
        } catch (Exception e) {
            LOGGER.error("failed to execute task: " + task, e);
        }
    }
}
