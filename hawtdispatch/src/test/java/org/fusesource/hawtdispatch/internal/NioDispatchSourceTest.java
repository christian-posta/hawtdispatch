package org.fusesource.hawtdispatch.internal;

import com.sun.org.apache.bcel.internal.generic.NEW;
import org.fusesource.hawtdispatch.DispatchPriority;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.fusesource.hawtdispatch.Task;
import org.fusesource.hawtdispatch.internal.util.RunnableCountDownLatch;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.nio.channels.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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

    private ServerSocketChannel createSocketChannel(int port) throws IOException {
        ServerSocketChannel channel = ServerSocketChannel.open();
        channel.configureBlocking(false);
        channel.socket().bind(address("0.0.0.0", port));
        return channel;
    }

    @Test
    public void testWorkerThreadHasSelectorKeyOnResume() throws IOException, InterruptedException {
        HawtDispatcher dispatcher = new DispatcherConfig().createDispatcher();

        ServerSocketChannel channel = createSocketChannel(4444);
        DispatchQueue queue = dispatcher.createQueue("test");

        try {
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
        finally {
            channel.close();
        }
    }

    @Test
    public void testFire() throws IOException, InterruptedException {


        HawtDispatcher dispatcher = new DispatcherConfig().createDispatcher();

        ServerSocketChannel channel = createSocketChannel(4444);
        DispatchQueue queue = dispatcher.createQueue("test");
        RunnableCountDownLatch ran = acceptor(channel);

        try {
            final NioDispatchSource source = new NioDispatchSource(dispatcher, channel, SelectionKey.OP_ACCEPT, queue);
            source.setEventHandler(ran);

            source.resume();

            Thread.sleep(1000);

            // wait a sec for the channel to become registered on resume()...
            dispatcher.DEFAULT_QUEUE.workers.getThreads()[0].getDispatchQueue().execute(new Task() {
                @Override
                public void run() {
                    source.fire(SelectionKey.OP_ACCEPT);

                }
            });

        }finally {
            channel.close();
        }

        assertTrue(ran.await(1, TimeUnit.SECONDS));
    }


    static public InetSocketAddress address(String host, int port) throws UnknownHostException {
        return new InetSocketAddress(ip(host), port);
    }

    static public InetAddress ip(String host) throws UnknownHostException {
        return InetAddress.getByName(host);
    }

    private RunnableCountDownLatch acceptor(final ServerSocketChannel channel) {
        return new RunnableCountDownLatch(1) {
            @Override
            public void run() {
                try {
                    SocketChannel socket = channel.accept();
                    socket.close();
                    System.out.println("Ran acceptor");
                } catch (IOException e) {
                    // it will fail because of a closed socket because there isn't really
                    // a selection event... ignore it..
                }
                super.run();
            }
        };
    }


}
