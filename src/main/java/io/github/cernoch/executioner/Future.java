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

/**
 * Holds results of asynchronous computation, produced in future.
 * 
 * <p>This closely resembles a {@link java.util.concurrent.Future},
 * but instead of throwing {@link java.util.concurrent.ExecutionException},
 * we only allow throwing a checked exception {@code E}.</p>
 *
 * @param <T> type of the returned value
 * @param <E> type of allowed checked exception
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 * @see java.util.concurrent.Future
 * @see Call
 */
public interface Future<T, E extends Exception> {

    /**
     * Wait for the value to be computed (if needed) and then return it.
     * 
     * @return value returned by asynchronous computation
     * @throws E computation threw this checked exception
     * @throws InterruptedException if someone calls
     * {@link Thread#interrupt()} in the current thread
     * or if someone calls {@link #cancel()}
     */
    T get() throws InterruptedException, E;

    /**
     * Wait for a limited time (at most) and return the computed value.
     * 
     * @param timeOut number of milliseconds to wait
     * @return value returned by asynchronous computation
     * @throws E computation threw this checked exception
     * @throws TimeoutException the timeout runs out
     * @throws InterruptedException if someone calls
     * {@link Thread#interrupt()} in the current thread
     * or if someone calls {@link #cancel()}
     */
    T get(long timeOut) throws InterruptedException, TimeoutException, E;

    /**
     * Cancel the computation currently in progress.
     * 
     * <p>If the computation has already finished, this has no effect.</p>
     * 
     * <p>If not, then:</p><ol>
     * <li>Then the thread, which performs the computation
     * will receive the {@link Thread#interrupt()} signal.</li>
     * <li>All current and future calls for {@link #get()} or
     * {@link #get(long)} will result in {@link InterruptedException}.</li>
     * <li>The status becomes {@link Status#DONE}.</li></ol>
     */
    void cancel();

    /**
     * Time that the calculation needed (so far).
     * 
     * <p>For {@link Status#RUNNING} calculations this gives the real time
     * since the calculation started. For {@link Status#DONE} tasks, this
     * gives the amount of time the calculation too.
     * 
     * @return a non-negative positive long in milliseconds
     * @throws NotStartedYet if the {@link #status()} is {@link Status#QUEUED}
     */
    long cpuTime();

    /**
     * Current status of the calculation.
     * 
     * @return a non-{@code null} value
     * @see Status
     */
    Status status();
    
    /**
     * All possible states of the calculation.
     */
    public static enum Status {
        
        /**
         * Calculation has been placed in the queue, but not picked up yet.
         */
        QUEUED,
        
        /**
         * Calculation is currently in progress.
         */
        RUNNING,
        
        /**
         * Calculation has finished (not saying if successfully).
         */
        DONE
    }
}
