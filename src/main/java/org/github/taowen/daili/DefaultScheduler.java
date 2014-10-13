package org.github.taowen.daili;

import kilim.Pausable;
import kilim.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeoutException;

public class DefaultScheduler extends Scheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultScheduler.class);
    protected Selector selector;
    private Queue<Booking> sortedBookings = new PriorityQueue<Booking>();
    private Map<String, List<EventBooking>> eventMap = new HashMap<String, List<EventBooking>>();
    private List<Task> outgoingTasks = new ArrayList<Task>();
    // incomingTasks is the only entrance to the scheduler thread
    private Queue<Task> incomingTasks = new ConcurrentLinkedQueue<Task>();

    public DefaultScheduler() {
        try {
            selector = Selector.open();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("selector opened");
            }
        } catch (IOException e) {
            LOGGER.error("failed to open selector", e);
        }
    }

    @Override
    public void loop() {
        while (selector.isOpen()) {
            loopOnce();
        }
    }

    @Override
    public void exit() throws IOException {
        selector.close();
    }

    @Override
    public SocketChannel accept(ServerSocketChannel serverSocketChannel, int timeout) throws IOException, Pausable, TimeoutException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (null != socketChannel) {
            return socketChannel;
        }
        // =========== END WORKER THREAD =========

        switchToSchedulerThread();

        // =========== BEGIN SCHEDULER THREAD =========
        SelectionKey selectionKey = serverSocketChannel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = serverSocketChannel.register(selector, 0);
            selectionKey.interestOps(SelectionKey.OP_ACCEPT);
            SelectorBooking booking = new SelectorBooking(sortedBookings, selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_ACCEPT);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        // =========== END SCHEDULER THREAD =========

        booking.acceptBlocked(currentTimeMillis() + timeout);

        // =========== BEGIN WORKER THREAD =========
        return serverSocketChannel.accept();
    }

    @Override
    public int read(SocketChannel socketChannel, ByteBuffer byteBuffer, int timeout) throws IOException, Pausable, TimeoutException {
        int bytesCount = socketChannel.read(byteBuffer);
        if (bytesCount > 0) {
            return bytesCount;
        }
        // =========== END WORKER THREAD =========

        readBlocked(socketChannel, timeout); // switched to scheduler thread inside

        // =========== BEGIN WORKER THREAD =========
        return socketChannel.read(byteBuffer);
    }

    private void readBlocked(SelectableChannel channel, int timeout) throws ClosedChannelException, Pausable, TimeoutException {
        // ...
        // =========== END WORKER THREAD =========

        switchToSchedulerThread();

        // =========== BEGIN SCHEDULER THREAD =========
        SelectionKey selectionKey = channel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = channel.register(selector, SelectionKey.OP_READ);
            SelectorBooking booking = new SelectorBooking(sortedBookings, selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        // =========== END SCHEDULER THREAD =========

        booking.readBlocked(currentTimeMillis() + timeout);

        // =========== BEGIN WORKER THREAD =========
        // ...
    }

    @Override
    public int write(SocketChannel socketChannel, ByteBuffer byteBuffer, int timeout) throws IOException, Pausable, TimeoutException {
        int bytesCount = socketChannel.write(byteBuffer);
        if (bytesCount > 0) {
            return bytesCount;
        }
        // =========== END WORKER THREAD =========

        writeBlocked(socketChannel, timeout); // switched to scheduler thread inside

        // =========== BEGIN WORKER THREAD =========
        return socketChannel.write(byteBuffer);
    }

    private void writeBlocked(SelectableChannel channel, int timeout) throws ClosedChannelException, Pausable, TimeoutException {
        // ...
        // =========== END WORKER THREAD =========

        switchToSchedulerThread();

        // =========== BEGIN SCHEDULER THREAD =========
        SelectionKey selectionKey = channel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = channel.register(selector, SelectionKey.OP_WRITE);
            SelectorBooking booking = new SelectorBooking(sortedBookings, selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_WRITE);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        // =========== END SCHEDULER THREAD =========

        booking.writeBlocked(currentTimeMillis() + timeout);

        // =========== BEGIN WORKER THREAD =========
        // ...
    }

    @Override
    public void connect(SocketChannel socketChannel, SocketAddress remote, int timeout) throws IOException, TimeoutException, Pausable {
        if (socketChannel.connect(remote)) {
            return;
        }
        // =========== END WORKER THREAD =========

        switchToSchedulerThread();

        // =========== BEGIN SCHEDULER THREAD =========
        SelectionKey selectionKey = socketChannel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
            SelectorBooking booking = new SelectorBooking(sortedBookings, selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_CONNECT);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        // =========== END SCHEDULER THREAD =========

        booking.connectBlocked(currentTimeMillis() + timeout);

        // =========== BEGIN WORKER THREAD =========
        if (!socketChannel.finishConnect()) {
            throw new RuntimeException("still not connected, after connect op unblocked");
        }
    }

    @Override
    public SocketAddress receive(DatagramChannel datagramChannel, ByteBuffer byteBuffer, int timeout) throws IOException, TimeoutException, Pausable {
        SocketAddress clientAddress = datagramChannel.receive(byteBuffer);
        if (null != clientAddress) {
            return clientAddress;
        }
        // =========== END WORKER THREAD =========

        readBlocked(datagramChannel, timeout); // switched to scheduler thread inside

        // =========== BEGIN WORKER THREAD =========
        return datagramChannel.receive(byteBuffer);
    }

    @Override
    public int send(DatagramChannel channel, ByteBuffer byteBuffer, SocketAddress target, int timeout) throws IOException, TimeoutException, Pausable {
        int bytesCount = channel.send(byteBuffer, target);
        if (bytesCount > 0) {
            return bytesCount;
        }
        // =========== END WORKER THREAD =========

        writeBlocked(channel, timeout); // switched to scheduler thread inside

        // =========== BEGIN WORKER THREAD =========
        return channel.send(byteBuffer, target);
    }

    @Override
    public Object waitUntil(String eventName, long deadline) throws Pausable, TimeoutException {
        // =========== END WORKER THREAD =========

        switchToSchedulerThread();

        // =========== BEGIN SCHEDULER THREAD =========
        List<EventBooking> eventBookings = eventMap.get(eventName);
        if (null == eventBookings) {
            eventBookings = new ArrayList<EventBooking>(2);
            eventMap.put(eventName, eventBookings);
        }
        EventBooking booking = new EventBooking(eventBookings, eventName, deadline, Task.getCurrentTask());
        if (!sortedBookings.add(booking)) {
            throw new RuntimeException("failed to add booking: " + this);
        }
        // =========== END SCHEDULER THREAD =========

        return booking.blocked();

        // =========== BEGIN WORKER THREAD =========
        // ...
    }

    @Override
    public void trigger(final String eventName, final Object eventData) throws Pausable {
        // =========== END WORKER THREAD =========

        switchToSchedulerThread();

        // =========== BEGIN SCHEDULER THREAD =========
        pushEventReadyTasks(eventName, eventData);
        // =========== END SCHEDULER THREAD =========

        switchToWorkerThread();

        // =========== BEGIN WORKER THREAD =========
        // ...
    }

    @Override
    public void sleepUntil(long deadline) throws Pausable {
        // =========== END WORKER THREAD =========

        switchToSchedulerThread();

        // =========== BEGIN SCHEDULER THREAD =========
        SleepBooking booking = new SleepBooking(deadline, Task.getCurrentTask());
        if (!sortedBookings.add(booking)) {
            throw new RuntimeException("failed to add booking: " + this);
        }
        // =========== END SCHEDULER THREAD =========

        Task.yield();

        // =========== BEGIN WORKER THREAD =========
        // ...
    }

    boolean loopOnce() {
        try {
            doSelect();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            pushIoReadyTasks(iterator);
            pullIncomingTasks();
            pushOutgoingTasks();
            cancelDeadTasks();
            return true;
        } catch (Exception e) {
            LOGGER.error("loop died", e);
            return false;
        }
    }

    private void cancelDeadTasks() throws IOException {
        boolean hasCancelled = false;
        while (hasDeadSelectorBooking()) {
            Booking booking = sortedBookings.poll();
            hasCancelled |= booking.cancelDeadTasks(currentTimeMillis());
        }
        if (hasCancelled) {
            selector.selectNow(); // make the cancel happen
        }
    }

    private boolean hasDeadSelectorBooking() {
        Booking booking = sortedBookings.peek();
        if (null == booking) {
            return false;
        }
        return booking.deadline() < currentTimeMillis();
    }

    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }

    protected int doSelect() throws IOException {
        Booking booking = sortedBookings.peek();
        if (null == booking) {
            return selector.select();
        } else {
            long delta = booking.deadline() - currentTimeMillis();
            if (delta > 0) {
                return selector.select(delta);
            } else {
                return selector.selectNow();
            }
        }
    }

    private void switchToSchedulerThread() throws Pausable {
        incomingTasks.add(Task.getCurrentTask());
        selector.wakeup();
        // =========== END WORKER THREAD =========

        Task.yield();

        // =========== BEGIN SCHEDULER THREAD =========
        // ...
    }

    private void switchToWorkerThread() throws Pausable {
        outgoingTasks.add(Task.getCurrentTask()); // assume it will consumed
        // =========== END SCHEDULER THREAD =========

        Task.yield();

        // =========== BEGIN WORKER THREAD =========
        // ...
    }

    private void pullIncomingTasks() {
        Task task;
        while ((task = incomingTasks.poll()) != null) {
            try {
                task._runExecute();
            } catch (Exception e) {
                LOGGER.error("failed to execute external task: " + task, e);
            }
        }
    }

    private void pushOutgoingTasks() {
        for (Task task : outgoingTasks) {
            try {
                task.run();
            } catch (Exception e) {
                LOGGER.error("failed to run task: " + task, e);
            }
        }
        outgoingTasks.clear();
    }

    private void pushEventReadyTasks(String eventName, Object eventData) {
        List<EventBooking> bookings = eventMap.remove(eventName);
        for (EventBooking booking : bookings) {
            sortedBookings.remove(booking);
            booking.trigger(eventData);
        }
    }

    private void pushIoReadyTasks(Iterator<SelectionKey> iterator) {
        List<Runnable> readyTasks = new ArrayList<Runnable>();
        while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            iterator.remove();
            SelectorBooking selectorBooking = (SelectorBooking) selectionKey.attachment();
            for (Runnable task : selectorBooking.ioUnblocked()) {
                readyTasks.add(task);
            }
        }
        for (Runnable readyTask : readyTasks) {
            try {
                readyTask.run();
            } catch (Exception e) {
                LOGGER.error("failed to run task: " + readyTask, e);
            }
        }
    }
}
