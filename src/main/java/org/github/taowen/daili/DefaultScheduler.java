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
    private List<Runnable> readyTasks = new ArrayList<Runnable>();
    // externalTasks is the only entrance to the scheduler thread
    private Queue<Task> externalTasks = new ConcurrentLinkedQueue<Task>();

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

    private SelectorBooking addSelectorBooking(SelectionKey selectionKey) {
        return new SelectorBooking(sortedBookings, selectionKey);
    }

    @Override
    public void loop() {
        while (loopOnce()) {
        }
    }

    boolean loopOnce() {
        try {
            executeExternalTasks();
            doSelect();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            ioUnblocked(iterator);
            boolean hasCancelled = false;
            while (hasDeadSelectorBooking()) {
                Booking booking = sortedBookings.poll();
                hasCancelled |= booking.cancelDeadTasks(currentTimeMillis());
            }
            if (hasCancelled) {
                selector.selectNow(); // make the cancel happen
            }
            return true;
        } catch (Exception e) {
            LOGGER.error("loop died", e);
            return false;
        }
    }

    private void executeExternalTasks() {
        Task task;
        while ((task = externalTasks.poll()) != null) {
            try {
                task._runExecute();
            } catch (Exception e) {
                LOGGER.error("failed to execute external task: " + task, e);
            }
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

    private void ioUnblocked(Iterator<SelectionKey> iterator) {
        List<Runnable> readyTasks = new ArrayList<Runnable>(this.readyTasks);
        this.readyTasks.clear();
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

    private void switchToMyThread() throws Pausable {
        externalTasks.add(Task.getCurrentTask());
        selector.wakeup();
        // =========== END WORKER THREAD =========

        Task.yield();

        // =========== BEGIN SCHEDULER THREAD =========
        // ...
    }

    @Override
    public SocketChannel accept(ServerSocketChannel serverSocketChannel, int timeout) throws IOException, Pausable, TimeoutException {
        SocketChannel socketChannel = serverSocketChannel.accept();
        if (null != socketChannel) {
            return socketChannel;
        }
        switchToMyThread();
        SelectionKey selectionKey = serverSocketChannel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = serverSocketChannel.register(selector, 0);
            selectionKey.interestOps(SelectionKey.OP_ACCEPT);
            SelectorBooking booking = addSelectorBooking(selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_ACCEPT);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        booking.acceptBlocked(currentTimeMillis() + timeout);
        return serverSocketChannel.accept();
    }

    @Override
    public int read(SocketChannel socketChannel, ByteBuffer byteBuffer, int timeout) throws IOException, Pausable, TimeoutException {
        int bytesCount = socketChannel.read(byteBuffer);
        if (bytesCount > 0) {
            return bytesCount;
        }
        readBlocked(socketChannel, timeout);
        return socketChannel.read(byteBuffer);
    }

    private void readBlocked(SelectableChannel channel, int timeout) throws ClosedChannelException, Pausable, TimeoutException {
        switchToMyThread();
        SelectionKey selectionKey = channel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = channel.register(selector, SelectionKey.OP_READ);
            SelectorBooking booking = addSelectorBooking(selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_READ);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        booking.readBlocked(currentTimeMillis() + timeout);
    }

    @Override
    public int write(SocketChannel socketChannel, ByteBuffer byteBuffer, int timeout) throws IOException, Pausable, TimeoutException {
        int bytesCount = socketChannel.write(byteBuffer);
        if (bytesCount > 0) {
            return bytesCount;
        }
        writeBlocked(socketChannel, timeout);
        return socketChannel.write(byteBuffer);
    }

    private void writeBlocked(SelectableChannel channel, int timeout) throws ClosedChannelException, Pausable, TimeoutException {
        // ...
        // =========== END WORKER THREAD =========

        switchToMyThread();

        // =========== BEGIN SCHEDULER THREAD =========
        SelectionKey selectionKey = channel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = channel.register(selector, SelectionKey.OP_WRITE);
            SelectorBooking booking = addSelectorBooking(selectionKey);
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

    public void close() throws IOException {
        selector.close();
    }

    @Override
    public void connect(SocketChannel socketChannel, SocketAddress remote, int timeout) throws IOException, TimeoutException, Pausable {
        if (socketChannel.connect(remote)) {
            return;
        }
        switchToMyThread();
        SelectionKey selectionKey = socketChannel.keyFor(selector);
        if (null == selectionKey) {
            selectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT);
            SelectorBooking booking = addSelectorBooking(selectionKey);
            selectionKey.attach(booking);
        } else {
            selectionKey.interestOps(selectionKey.interestOps() | SelectionKey.OP_CONNECT);
        }
        SelectorBooking booking = (SelectorBooking) selectionKey.attachment();
        booking.connectBlocked(currentTimeMillis() + timeout);
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
        readBlocked(datagramChannel, timeout);
        return datagramChannel.receive(byteBuffer);
    }

    @Override
    public int send(DatagramChannel channel, ByteBuffer byteBuffer, InetSocketAddress target, int timeout) throws IOException, TimeoutException, Pausable {
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

        switchToMyThread();

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
        selector.wakeup();
        // =========== END SCHEDULER THREAD =========

        return booking.blocked();

        // =========== BEGIN WORKER THREAD =========
        // ...
    }

    @Override
    public void trigger(final String eventName, final Object eventData) throws Pausable {
        // =========== END WORKER THREAD =========

        switchToMyThread();

        // =========== BEGIN SCHEDULER THREAD =========
        List<EventBooking> bookings = eventMap.remove(eventName);
        for (EventBooking booking : bookings) {
            sortedBookings.remove(booking);
            booking.trigger(eventData);
        }
        readyTasks.add(Task.getCurrentTask());
        // =========== END SCHEDULER THREAD =========

        Task.yield();

        // =========== BEGIN WORKER THREAD =========
        // ...
    }
}
