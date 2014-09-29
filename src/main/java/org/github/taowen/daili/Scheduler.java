package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Set;

public class Scheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    protected Selector selector;
    private Queue<Task> readyTasks = new LinkedList<Task>();
    private Queue<SelectorBooking> selectorBookings = new PriorityQueue<SelectorBooking>();

    {
        try {
            selector = Selector.open();
            LOGGER.info("selector opened");
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
            SelectorBooking booking = addSelectorBooking();
            selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT, booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_ACCEPT);
        }
        ((SelectorBooking) selectionKey.attachment()).acceptBlocked();
        return serverSocketChannel.accept();
    }

    private SelectorBooking addSelectorBooking() {
        return new SelectorBooking(selectorBookings);
    }

    public void callSoon(Task task) {
        if (!readyTasks.offer(task)) {
            throw new RuntimeException("failed to add ready task");
        }
    }

    public void loop() throws IOException {
        while (true) {
            loopOnce();
        }
    }

    void loopOnce() throws IOException {
        executeReadyTasks();
        doSelect();
        Set<SelectionKey> selectionKeys = selector.selectedKeys();
        for (SelectionKey selectionKey : selectionKeys) {
            SelectorBooking selectorBooking = (SelectorBooking) selectionKey.attachment();
            if (selectionKey.isAcceptable()) {
                Task taskUnblocked = selectorBooking.acceptUnblocked();
                if (null == taskUnblocked) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_ACCEPT);
                } else {
                    callSoon(taskUnblocked);
                }
            }
            if (selectionKey.isReadable()) {
                Task taskUnblocked = selectorBooking.readUnblocked();
                if (null == taskUnblocked) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_READ);
                } else {
                    callSoon(taskUnblocked);
                }
            }
            if (selectionKey.isWritable()) {
                Task taskUnblocked = selectorBooking.writeUnblocked();
                if (null == taskUnblocked) {
                    selectionKey.interestOps(selectionKey.interestOps() & ~SelectionKey.OP_WRITE);
                } else {
                    callSoon(taskUnblocked);
                }
            }
        }
    }

    protected int doSelect() throws IOException {
        return selector.select();
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

    public int read(SocketChannel socketChannel, ByteBuffer byteBuffer) throws IOException, Pausable {
        int bytesCount = socketChannel.read(byteBuffer);
        if (bytesCount > 0) {
            return bytesCount;
        }
        SelectionKey selectionKey = socketChannel.keyFor(selector);
        if (null == selectionKey) {
            SelectorBooking booking = addSelectorBooking();
            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ, booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
        }
        ((SelectorBooking) selectionKey.attachment()).readBlocked();
        return socketChannel.read(byteBuffer);
    }

    public int write(SocketChannel socketChannel, ByteBuffer byteBuffer) throws IOException, Pausable {
        int bytesCount = socketChannel.write(byteBuffer);
        if (bytesCount > 0) {
            return bytesCount;
        }
        SelectionKey selectionKey = socketChannel.keyFor(selector);
        if (null == selectionKey) {
            SelectorBooking booking = addSelectorBooking();
            selectionKey = socketChannel.register(selector, SelectionKey.OP_WRITE, booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        }
        ((SelectorBooking) selectionKey.attachment()).writeBlocked();
        return socketChannel.write(byteBuffer);
    }

    public void close() throws IOException {
        for (SelectionKey key : selector.keys()) {
            key.channel().close();
        }
        selector.close();
    }
}
