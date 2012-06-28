package org.fusesource.hawtdispatch.internal;

import org.fusesource.hawtdispatch.DispatchQueue;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

/**
 * @author: ceposta
 */
public class AbstractDispatchObjectTest {

    @Test
    public void testSetTargetQueue(){
        ClassUnderTest test = new ClassUnderTest();
        HawtDispatchQueue queue = mock(HawtDispatchQueue.class);
        test.setTargetQueue(queue);

        assertEquals(queue, test.getTargetQueue());

        // verify that the queue that's currently set is still the same
        // when we try to pass the same queue again
        test.setTargetQueue(queue);
        assertEquals(queue, test.getTargetQueue());

        HawtDispatchQueue queue2 = mock(HawtDispatchQueue.class);

        // verify when a new queue is passed, it should indeed be
        // reflected
        test.setTargetQueue(queue2);
        assertFalse(queue == test.getTargetQueue());


    }

    class ClassUnderTest extends AbstractDispatchObject {

    }
}
