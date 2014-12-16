/*
 * Copyright (c) 2014 Czech Technical University in Prague 
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 */

package cz.cvut.felk.ida.executioner;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests for the {@link Timeouts} class.
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class TimeoutsTest {

    @Test(expected = TimeoutException.class)
    public void timeOutCutsEarly() throws Throwable {
        
        Timeouts.call(10, new Callable<Object>() {
            
            @Override
            public Object call() throws Exception {
                Thread.sleep(1000);
                throw new RuntimeException();
            }
        });
    }

    @Test
    public void timeOutGivesEnoughTime() throws Throwable {
        
        String out = Timeouts.call(200, new Callable<String>() {
            
            @Override
            public String call() throws Exception {
                Thread.sleep(50);
                return "hello";
            }
        });
        Assert.assertEquals("hello", out);
    }

    @Test
    public void exceptionHasCorrectTime() throws Throwable {
        try {
            Timeouts.call(100, new Callable<Object>() {
                
                @Override
                public Object call() throws Exception {
                    Thread.sleep(1000);
                    throw new RuntimeException();
                }
            });
            
        } catch (TimeoutException ex) {
            assertTrue(ex.timeOut() >= 100);
        }
    }
    
    @Test(expected = OutOfMemoryError.class)
    public void exceptionsFromChildCallableThrownAsExpected() throws Throwable {

        Timeouts.call(100, new Callable<Object>() {

            @Override
            public Object call() throws Exception {
                throw new OutOfMemoryError();
            }
        });
    }
    
    @Test(expected = OutOfMemoryError.class)
    public void exceptionsFromChildRunnableThrownAsExpected() throws Throwable {
        
        Timeouts.run(100, new Runnable() {

            @Override
            public void run() {
                throw new OutOfMemoryError();
            }
        });
    }
    
    @Test
    public void interruptKillsTheChildThread() throws Throwable {

        final List<String> unexpected = new LinkedList<>();
        final List<String> expected = new LinkedList<>();

        Thread waitingThread = new Thread() {

            @Override
            public void run() {

                try {
                    Timeouts.call(300, new Callable<Object>() {
                        @Override public Object call() throws Exception {
                            Thread.sleep(500);
                            unexpected.add("Here!");
                            return null;
                        }
                    });

                } catch (InterruptedException ex) {
                    expected.add(ex.toString());

                } catch (Throwable ex) {

                }
            }
        };

        waitingThread.start(); // Start the called and...
        waitingThread.join(100); // ...before the timeout...
        waitingThread.interrupt(); // ...kill it!

        // Wait until the worker finishes...
        Thread.sleep(800);
        // ...worker should have been interrupted...
        assertTrue(unexpected.isEmpty()); 
        // ...and the caller should meet InterruptedEx.
        assertEquals(1, expected.size()); 
    }
}
