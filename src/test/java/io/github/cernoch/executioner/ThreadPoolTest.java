/* 
 * The MIT License
 *
 * Copyright 2017 Radomír Černoch (radomir.cernoch at gmail.com).
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
package io.github.cernoch.executioner;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests for the {@link ThreadPool} class.
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class ThreadPoolTest {
    
    @Test(timeout = 1000L)
    public void normalOperation() throws Throwable {
        ThreadPool pool = new ThreadPool(2, true);
        assertTrue(pool.working());
        
        try {
            Futuroid<Integer,InterruptedException> f1 = pool.submit(
                    InterruptedException.class, new WaitAndReturn(200L, 0));
            Futuroid<Integer,InterruptedException> f2 = pool.submit(
                    InterruptedException.class, new WaitAndReturn(200L, 1));
            Futuroid<Integer,InterruptedException> f3 = pool.submit(
                    InterruptedException.class, new WaitAndReturn(200L, 2));
            Futuroid<Integer,InterruptedException> f4 = pool.submit(
                    InterruptedException.class, new WaitAndReturn(200L, 3));

            Thread.sleep(100L);
            
            assertEquals(Futuroid.Status.RUNNING, f1.status());
            assertEquals(Futuroid.Status.RUNNING, f2.status());
            assertEquals(Futuroid.Status.QUEUED,  f3.status());
            assertEquals(Futuroid.Status.QUEUED,  f4.status());

            assertEquals((Integer) 0, f1.get());
            assertEquals((Integer) 1, f2.get());
            assertEquals((Integer) 2, f3.get());
            assertEquals((Integer) 3, f4.get());

            assertEquals(Futuroid.Status.DONE, f1.status());
            assertEquals(Futuroid.Status.DONE, f2.status());
            assertEquals(Futuroid.Status.DONE, f3.status());
            assertEquals(Futuroid.Status.DONE, f4.status());
            
        } finally {
            assertTrue(pool.working());

            pool.shutdown();
            
            assertFalse(pool.working());
        }
    }
    
    @Test(timeout = 1000L, expected = TimeoutException.class)
    public void timeOutOccurs() throws InterruptedException, TimeoutException {
        
        ThreadPool pool = new ThreadPool(1, true);
        try {
            pool.submit(InterruptedException.class,
                    new WaitAndReturn(500L, 0)).get(100L);

        } finally {
            pool.shutdown();
        }
    }
    
    @Test(timeout = 1000L, expected = InterruptedException.class)
    public void cancelInterruptsThread() throws InterruptedException {
        
        ThreadPool pool = new ThreadPool(1, true);
        try {
            WaitAndReturn war = new WaitAndReturn(200L, 1);
            Futuroid<Integer,InterruptedException> fut =
                    pool.submit(InterruptedException.class, war);
            
            Thread.sleep(100L);
            fut.cancel();
            
            Thread.sleep(200L);
            assertFalse(war.returned);
            
            fut.get();
        } finally {
            pool.shutdown();
        }
    }

    public static class MyError extends Error {}
    public static class MyException extends Exception {}

    public static class ThrowMyError implements Call<Void,RuntimeException> {
        @Override public Void call() {
            throw new MyError();
        }
    }
    
    public static class ThrowMyException implements Call<Void,MyException> {
        @Override public Void call() throws MyException {
            throw new MyException();
        }
    }
    
    @Test(expected = MyError.class)
    public void errorsPassedDirectly() throws Throwable {
        ThreadPool pool = new ThreadPool(1, true);
        try {
            pool.submit(RuntimeException.class, new ThrowMyError()).get();
        } finally {
            pool.shutdown();
        }
    }
    
    @Test(expected = MyException.class)
    public void exceptionsPassedDirectly() throws Exception {
        ThreadPool pool = new ThreadPool(1, true);
        try {
            pool.submit(MyException.class, new ThrowMyException()).get();
        } finally {
            pool.shutdown();
        }
    }
    
    @Test
    public void interruptWaitsForGraceful() throws InterruptedException {
        
        Call<Integer,RuntimeException> elapsedSeconds
                = new Call<Integer, RuntimeException>() {
                    
            @Override
            public Integer call() throws RuntimeException {
                int i = 0;
                while (true) {
                    try {
                        Thread.sleep(1000L);
                        i++;
                    } catch (InterruptedException ex) {
                        return i;
                    }
                }
            }
        };
        
        ThreadPool pool = new ThreadPool(1, true);
        try {
            Futuroid<Integer, RuntimeException> fut = pool.submit(
                    RuntimeException.class, elapsedSeconds);
            try {
                fut.get(1500L);
            } catch (TimeoutException ex) {
                fut.interrupt();
            }
            
            assertEquals(Integer.valueOf(1), fut.get());
            
        } finally {
            pool.shutdown();
        }
    }

    @Test(timeout = 5000L)
    public void numberOfWaitingThreadsInFixedPool() throws InterruptedException {
        ThreadPool pool = new ThreadPool(4, true);

        // Let the workers be initialized
        Thread.sleep(500L);
        assertEquals(4, pool.waiting());

        pool.submit(() -> {
            Thread.sleep(500L);
            return null;
        });
        Thread.sleep(100L);
        assertEquals(3, pool.waiting());

        Thread.sleep(700L);
        assertEquals(4, pool.waiting());
    }


    @Test(timeout = 5000L)
    public void numberOfWaitingThreadsInCachedPool() throws InterruptedException {
        ThreadPool pool = new ThreadPool(0, false);
        assertEquals(0, pool.waiting());

        pool.submit(() -> {
            Thread.sleep(500L);
            return null;
        });
        Thread.sleep(100L);
        // 1 worker should be working
        assertEquals(0, pool.waiting());

        Thread.sleep(700L);
        // Thread should be cached
        assertEquals(1, pool.waiting());
    }
}
