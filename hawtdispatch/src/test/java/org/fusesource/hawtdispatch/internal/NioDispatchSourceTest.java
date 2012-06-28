package org.fusesource.hawtdispatch.internal;

import com.sun.org.apache.bcel.internal.generic.NEW;
import org.fusesource.hawtdispatch.DispatchPriority;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

/**
 * @author: ceposta
 */
public class NioDispatchSourceTest {


    @Test(expected = IllegalArgumentException.class)
    public void testCannotInstantiateZeroSelectionOp() {

        DispatcherConfig config = mock(DispatcherConfig.class);
        HawtDispatcher dispatcher = new HawtDispatcher(config);
        SelectableChannel channel = mock(SelectableChannel.class);
        DispatchQueue queue = mock(DispatchQueue.class);

        // cannot be zero
        int interestOps = 0;
        new NioDispatchSource(dispatcher, channel, interestOps, queue);

    }

    @Test(expected = NullPointerException.class)
    public void testNullTargetQueue() {

        HawtDispatcher dispatcher = new DispatcherConfig().createDispatcher();
        SelectableChannel channel = mock(SelectableChannel.class);
        int interestOps = 1;

        // target channel cannot be null
        new NioDispatchSource(dispatcher, channel, interestOps, null);

    }

    @Test
    public void testCreation() {
        HawtDispatcher dispatcher = new DispatcherConfig().createDispatcher();
        SelectableChannel channel = mock(SelectableChannel.class);
        DispatchQueue queue = dispatcher.createQueue("test");
        int interestOps = 1;

        NioDispatchSource source = new NioDispatchSource(dispatcher, channel, interestOps, queue);
        assertEquals(queue, source.getTargetQueue());
        assertTrue(source.isSuspended());
        assertEquals(null, source.getData());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testResumeNoEventHandler() throws IOException {
        HawtDispatcher dispatcher = new DispatcherConfig().createDispatcher();
        SelectableChannel channel = mock(SelectableChannel.class);
        DispatchQueue queue = dispatcher.createQueue("test");
        int interestOps = 1;

        NioDispatchSource source = new NioDispatchSource(dispatcher, channel, interestOps, queue);

        // should fail, there is no event handler!!
        source.resume();

    }

    private SelectableChannel createSocketChannel(int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(new InetSocketAddress(port));
        return channel;
    }

    @Test
    public void testWorkerThreadHasSelectorKeyOnResume() throws IOException, InterruptedException {
        HawtDispatcher dispatcher = new DispatcherConfig().createDispatcher();
        SelectableChannel channel = createSocketChannel(4444);
        DispatchQueue queue = dispatcher.createQueue("test");

        NioDispatchSource source = new NioDispatchSource(dispatcher, channel, SelectionKey.OP_ACCEPT, queue);
        source.setEventHandler(new Task(){

            @Override
            public void run() {
                System.out.println("hello, from the event source");
            }
        });

        source.resume();

        // wait a sec for the channel to become registered on resume()...
        Thread.sleep(1000);

        boolean oneThreadHasSelectorForThisChannel = false;
        WorkerThread[] threads = dispatcher.DEFAULT_QUEUE.workers.getThreads();
        for (WorkerThread workerThread : threads) {
            Selector selector = workerThread.getNioManager().getSelector();
            SelectionKey key = channel.keyFor(selector);
            if (key != null) {
                oneThreadHasSelectorForThisChannel = true;
            }
        }

        assertTrue(oneThreadHasSelectorForThisChannel);
    }
}
