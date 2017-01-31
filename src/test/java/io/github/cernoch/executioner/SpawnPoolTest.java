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

import static io.github.cernoch.executioner.Future.Status.DONE;
import static io.github.cernoch.executioner.Future.Status.RUNNING;
import io.github.cernoch.executioner.ThreadPoolTest.MyException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Tests for the {@link SpawnPool} class.
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class SpawnPoolTest {
    
    @Test(timeout = 800L) // 500L + overhead
    public void oneOfReturnsEarly() throws Exception {
        
        SpawnPool pool = new SpawnPool(2, true);
        
        Future<Integer,InterruptedException> first = pool.oneof(
                InterruptedException.class, 0,
                new WaitAndReturn(900L, 1),
                new WaitAndReturn(500L, 2),
                new WaitAndReturn(200L, 3),
                new WaitAndReturn(100L, 4));
        
        assertEquals(2, first.get().intValue());
    }
    
    @Test(timeout = 800L) // 500L + 100L + overhead
    public void firstWaitsForBest() throws Exception {
        
        SpawnPool pool = new SpawnPool(2, true);
        
        Future<Integer,InterruptedException> fut = pool.first(
                InterruptedException.class, 0,
                new WaitAndReturn(900L, 1),
                new WaitAndReturn(500L, 2),
                new WaitAndReturn(200L, 3),
                new WaitAndReturn(100L, 4));
        
        Thread.sleep(10L);
        
        assertEquals(RUNNING, fut.status());
        assertEquals(4, fut.get().intValue());
        assertEquals(DONE, fut.status());
        assertTrue(fut.cpuTime() < 150L);
        
        pool.shutdown();
    }

    @Test(timeout = 2000L, expected = AllTasksFailed.class)
    public void firstTimeOutCausesAllTasksException() throws Exception {
        SpawnPool pool = new SpawnPool(2, true);
        try {
            Future<Integer,InterruptedException> fut = pool.first(
                    InterruptedException.class, 50L,
                    new WaitAndReturn(9000L, 1),
                    new WaitAndReturn(5000L, 2));
            fut.get();
        } finally {
            pool.shutdown();
        }
    }

    @Test(timeout = 2000L, expected = AllTasksFailed.class)
    public void oneofTimeOutCausesAllTasksException() throws Exception {
        SpawnPool pool = new SpawnPool(2, true);
        try {
            Future<Integer,InterruptedException> fut = pool.oneof(
                    InterruptedException.class, 50L,
                    new WaitAndReturn(9000L, 1),
                    new WaitAndReturn(5000L, 2));
            fut.get();
        } finally {
            pool.shutdown();
        }
    }
    
    public static class ReturnNull implements Call<Void,MyException> {
        
        @Override
        public Void call() throws RuntimeException {
            return null;
        }
    }
    
    @Test(timeout = 2000L)
    public void firstIgnoresExceptionsIfOtherThreadIsFine() throws Exception {
        SpawnPool pool = new SpawnPool(2, true);
        try {
            Future<Void,MyException> fut = pool.first(
                    MyException.class, 50L,
                    new ThreadPoolTest.ThrowMyException(),
                    new ReturnNull());
            
            assertNull(fut.get());
            
        } finally {
            pool.shutdown();
        }
    }
}
