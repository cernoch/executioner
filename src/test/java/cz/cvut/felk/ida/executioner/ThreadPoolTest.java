/*
 * The MIT License
 *
 * Copyright 2016 Radomír Černoch (radomir.cernoch at gmail.com).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package cz.cvut.felk.ida.executioner;

import java.util.concurrent.Callable;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class ThreadPoolTest {
    
    private static class WaitAndReturn implements Callable<Integer> {

        private final long waitTime;
        
        private final Integer value;
        
        public boolean returned = false;

        public WaitAndReturn(long waitTime, Integer value) {
            this.waitTime = waitTime;
            this.value = value;
        }
        
        @Override
        public Integer call() throws Exception {
            Thread.sleep(waitTime);
            returned = true;
            return value;
        }
    }
    
    @Test(timeout = 1000L)
    public void normalOperation() throws Throwable {
        ThreadPool pool = new ThreadPool(2);
        assertTrue(pool.working());
        
        try {
            Future<Integer> f1 = pool.submit(new WaitAndReturn(200L, 0));
            Future<Integer> f2 = pool.submit(new WaitAndReturn(200L, 1));
            Future<Integer> f3 = pool.submit(new WaitAndReturn(200L, 2));
            Future<Integer> f4 = pool.submit(new WaitAndReturn(200L, 3));

            Thread.sleep(100L);
            
            assertEquals(Future.Status.RUNNING, f1.status());
            assertEquals(Future.Status.RUNNING, f2.status());
            assertEquals(Future.Status.QUEUED,  f3.status());
            assertEquals(Future.Status.QUEUED,  f4.status());

            assertEquals((Integer) 0, f1.get());
            assertEquals((Integer) 1, f2.get());
            assertEquals((Integer) 2, f3.get());
            assertEquals((Integer) 3, f4.get());

            assertEquals(Future.Status.DONE, f1.status());
            assertEquals(Future.Status.DONE, f2.status());
            assertEquals(Future.Status.DONE, f3.status());
            assertEquals(Future.Status.DONE, f4.status());
            
        } finally {
            assertTrue(pool.working());

            pool.shutdown();
            
            assertFalse(pool.working());
        }
    }
    
    @Test(timeout = 1000L, expected = TimeoutException.class)
    public void timeOutOccurs() throws Throwable {
        
        ThreadPool pool = new ThreadPool(1);
        try {
            pool.submit(new WaitAndReturn(500L, 0)).get(100L);

        } finally {
            pool.shutdown();
        }
    }
    
    @Test(timeout = 1000L, expected = InterruptedException.class)
    public void cancelInterruptsThread() throws Throwable {
        
        ThreadPool pool = new ThreadPool(1);
        try {
            WaitAndReturn war = new WaitAndReturn(200L, 1);
            Future<Integer> fut = pool.submit(war);
            
            Thread.sleep(100L);
            fut.cancel();
            
            Thread.sleep(200L);
            assertFalse(war.returned);
            
            fut.get();
        } finally {
            pool.shutdown();
        }
    }
    
    private class MyError extends Error {}
    private class MyException extends Exception {}
    
    @Test(expected = MyError.class)
    public void errorsPassedDirectly() throws Throwable {
        ThreadPool pool = new ThreadPool(1);
        try {
            pool.submit(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    throw new MyError();
                }
            }).get();
        } finally {
            pool.shutdown();
        }
    }
    
    @Test(expected = MyException.class)
    public void exceptionsPassedDirectly() throws Exception {
        ThreadPool pool = new ThreadPool(1);
        try {
            pool.submit(new Callable<Void>() {
                @Override public Void call() throws Exception {
                    throw new MyException();
                }
            }).get();
        } finally {
            pool.shutdown();
        }
    }
}
