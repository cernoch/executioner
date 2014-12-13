/*
 * Copyright (c) 2014 Czech Technical University in Prague 
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 */

package cz.cvut.felk.ida.executioner;

import cz.cvut.felk.ida.executioner.Timeout;
import cz.cvut.felk.ida.executioner.TimeoutException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * 
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class TimeoutTest {

    @Test(expected = TimeoutException.class)
    public void timeOutCutsEarly() throws Throwable {
        Timeout.exec(new Callable<Object>() {
            @Override public Object call() throws Exception {
                ahoj();
                throw new RuntimeException();
            }
            
            public void ahoj() throws InterruptedException {
                Thread.sleep(1000);
            }
        }, 100);
    }

    @Test
    public void timeOutGivesEnoughTime() throws Throwable {
        String out = Timeout.exec(new Callable<String>() {
            @Override public String call() throws Exception {
                Thread.sleep(1000);
                return "hello";
            }
        }, 2000);
        Assert.assertEquals("hello", out);
    }

    @Test
    public void exceptionHasCorrectTime() throws Throwable {
        try {
            Timeout.exec(new Callable<Object>() {
                @Override public Object call() throws Exception {
                    Thread.sleep(1000);
                    throw new RuntimeException();
                }
            }, 100);
            
        } catch (TimeoutException ex) {
            assertTrue(ex.timeOut() >= 100);
        }
    }
    
    @Test
    public void interruptKillsTheChildThread() throws Throwable {

            final List<String> unexpected = new LinkedList<>();
            final List<String> expected = new LinkedList<>();
            
            Thread waitingThread = new Thread() {

                @Override
                public void run() {
                    
                    try {
                        Timeout.exec(new Callable<Object>() {
                            @Override public Object call() throws Exception {
                                Thread.sleep(5000);
                                unexpected.add("Here!");
                                return null;
                            }
                            
                        }, 3000);
                        
                    } catch (InterruptedException ex) {
                        expected.add(ex.toString());
                        
                    } catch (Throwable ex) {
                        
                    }
                }
            };
            
            waitingThread.start(); // Start the called and...
            waitingThread.join(1000); // ...before the timeout...
            waitingThread.interrupt(); // ...kill it!
            
            // Wait until the worker finishes...
            Thread.sleep(5000);
            // ...worker should have been interrupted...
            assertTrue(unexpected.isEmpty()); 
            // ...and the caller should meet InterruptedEx.
            assertEquals(1, expected.size()); 
    }
}
