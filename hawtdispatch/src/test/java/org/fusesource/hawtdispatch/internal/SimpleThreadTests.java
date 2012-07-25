package org.fusesource.hawtdispatch.internal;

import org.junit.Test;

/**
 * Created with IntelliJ IDEA.
 * User: cposta
 * Date: 7/2/12
 * Time: 11:49 AM
 * To change this template use File | Settings | File Templates.
 */
public class SimpleThreadTests {

    @Test
    public void testSynchTest() throws InterruptedException {
        long start = System.currentTimeMillis();
        testNonSync();

        long elapsed = System.currentTimeMillis() - start;
        System.out.println(String.format("took %dms", elapsed));

    }

    private synchronized void testSynch() {
        for (int i = 0; i < 10000000; i++) {
            if (i % 1000 == 0) {
                System.out.println("You got this far" + (i -(i%5)) );
            }
        }
    }

    private void testNonSync() {
        for (int i = 0; i < 1000; i++) {

            if (i % 100 == 0) {
                System.out.println("You got this far" + (i -(i%5)) );
            }
        }
    }

}
