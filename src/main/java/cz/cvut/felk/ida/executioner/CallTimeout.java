/*
 * Copyright (c) 2014 Radomír Černoch (radomir.cernoch at gmail.com)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a
 * copy of this software and associated documentation files (the "Software"), 
 * to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
 * OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER
 * DEALINGS IN THE SOFTWARE.
 */

package cz.cvut.felk.ida.executioner;

import java.util.concurrent.Callable;

/**
 * Executes a {@link Callable} with a timeout and returns its result.
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 * @param <T> Type of the result
 */
public class CallTimeout<T> implements Callable<T> {

    /**
     * Time-out in milliseconds.
     */
    protected final long timeOut;

    /**
     * Child process to be executed.
     */
    private final Callable<? extends T> child;

    private final ThreadPool pool;

    private static final ThreadPool DEFAULT_POOL = new ThreadPool(0, false);
    
    public CallTimeout(long timeOut,
            Callable<? extends T> child) {
        this(timeOut, child, DEFAULT_POOL);
    }
    
    public CallTimeout(long timeOut,
            Callable<? extends T> child,
            ThreadPool pool) {
        
        this.timeOut = timeOut;
        this.child = child;
        this.pool = pool;
    }
    
    /**
     * Start the computation and terminate after timeout.
     * 
     * <p>The child process is notified about the timeout using
     * {@link Thread#interrupt()}. It can be picked up either by catching
     * {@link InterruptedException} or {@link Thread#interrupted()}.</p>
     * 
     * <p>This method will not take much longer than the supplied timeout.</p>
     * 
     * @throws InterruptedException Interrupted while waiting for the child.
     * @throws TimeoutException Time elapsed before the child ended.
     */
    @Override
    public T call() throws InterruptedException, TimeoutException, Exception {
        
        Futuroid<? extends T, Exception> future = pool.submit(child);
        try {
            return future.get(timeOut);
        } finally {
            future.cancel();
        }
    }
}
