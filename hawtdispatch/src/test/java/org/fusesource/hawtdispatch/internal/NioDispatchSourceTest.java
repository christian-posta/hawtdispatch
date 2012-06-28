package org.fusesource.hawtdispatch.internal;

import com.sun.org.apache.bcel.internal.generic.NEW;
import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Before;
import org.junit.Test;

import java.nio.channels.SelectableChannel;

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

    @Test
    public void testPickThreadQueue() {
        fail("write test for NioDispatchSource#pickThreadQueue()");
    }

    @Test
    public void foo() {
        HawtDispatcher dispatcher = new DispatcherConfig().createDispatcher();
        SelectableChannel channel = mock(SelectableChannel.class);
        DispatchQueue queue = dispatcher.createQueue("test");

        // cannot be zero
        int interestOps = 1;
        new NioDispatchSource(dispatcher, channel, interestOps, queue);
    }
}
