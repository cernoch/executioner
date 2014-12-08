/*
 * Copyright (c) 2014 Czech Technical University in Prague 
 *
 * All rights reserved. No warranty, explicit or implicit, provided.
 */

package cz.cvut.felk.ida.executioner;

import cz.cvut.felk.ida.executioner.Timeout;
import cz.cvut.felk.ida.executioner.TimeoutException;
import java.util.concurrent.Callable;
import org.junit.Assert;
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
}
