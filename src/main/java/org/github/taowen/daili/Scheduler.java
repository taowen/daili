package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

public class Scheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
    protected Selector selector;
    private Queue<Task> readyTasks = new LinkedList<Task>();
    private Queue<SelectorBooking> selectorBookings = new PriorityQueue<SelectorBooking>();
    public int timeout = 1000 * 60;

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
            selectionKey = serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            SelectorBooking booking = addSelectorBooking(selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_ACCEPT);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        booking.acceptBlocked(getCurrentTimeMillis() + timeout);
        return serverSocketChannel.accept();
    }

    private SelectorBooking addSelectorBooking(SelectionKey selectionKey) {
        return new SelectorBooking(selectorBookings, selectionKey);
    }

    public void callSoon(Task task) {
        if (!readyTasks.offer(task)) {
            throw new RuntimeException("failed to add ready task");
        }
    }

    public void loop() {
        while (loopOnce()) {
        }
    }

    boolean loopOnce() {
        try {
            executeReadyTasks();
            doSelect();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            ioUnblocked(iterator);
            while (hasDeadSelectorBooking()) {
                SelectorBooking booking = selectorBookings.poll();
                booking.cancelDeadTasks(getCurrentTimeMillis());
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("loop died", e);
            return false;
        }
    }

    private boolean hasDeadSelectorBooking() {
        SelectorBooking booking = selectorBookings.peek();
        if (null == booking) {
            return false;
        }
        return booking.getEarliestDeadline() < getCurrentTimeMillis();
    }

    protected long getCurrentTimeMillis() {
        return System.currentTimeMillis();
    }

    private void ioUnblocked(Iterator<SelectionKey> iterator) {
        while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            iterator.remove();
            SelectorBooking selectorBooking = (SelectorBooking) selectionKey.attachment();
            for (Task task : selectorBooking.ioUnblocked()) {
                callSoon(task);
            }
        }
    }

    protected int doSelect() throws IOException {
        SelectorBooking booking = selectorBookings.peek();
        if (null == booking) {
            return selector.select();
        } else {
            long delta = booking.getEarliestDeadline() - getCurrentTimeMillis();
            if (delta > 0) {
                return selector.select(delta);
            } else {
                return selector.selectNow();
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

    public int read(SocketChannel socketChannel, ByteBuffer byteBuffer) throws IOException, Pausable {
        int bytesCount = socketChannel.read(byteBuffer);
        if (bytesCount > 0) {
            return bytesCount;
        }
        SelectionKey selectionKey = socketChannel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = socketChannel.register(selector, SelectionKey.OP_READ);
            SelectorBooking booking = addSelectorBooking(selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        booking.readBlocked(getCurrentTimeMillis() + timeout);
        return socketChannel.read(byteBuffer);
    }

    public int write(SocketChannel socketChannel, ByteBuffer byteBuffer) throws IOException, Pausable {
        int bytesCount = socketChannel.write(byteBuffer);
        if (bytesCount > 0) {
            return bytesCount;
        }
        SelectionKey selectionKey = socketChannel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = socketChannel.register(selector, SelectionKey.OP_WRITE);
            SelectorBooking booking = addSelectorBooking(selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        booking.writeBlocked(getCurrentTimeMillis() + timeout);
        return socketChannel.write(byteBuffer);
    }

    public void close() throws IOException {
        for (SelectionKey key : selector.keys()) {
            key.channel().close();
        }
        selector.close();
    }
}
