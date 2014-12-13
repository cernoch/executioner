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
import java.util.logging.Logger;

/**
 * Calls a given method, 
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class Timeout<T> {
    
    /**
     * “Core”, user-supplied call that will be executed.
     */
    private final Callable<? extends T> child;
    
    /**
     * Time-out in milliseconds.
     */
    private final long timeOut;

    /**
     * Value returned from the {@link #child}.
     */
    private T outcome;
    
    /**
     * Exception thrown by the {@link #child}.
     */
    private Throwable thrown;
    
    /**
     * Default logger.
     */
    private static final Logger LOG = Logger
        .getLogger(Timeout.class.getName());
    
    /**
     * Prepare for executing a child process with a given timeout.
     * 
     * @param child Method that will be executed
     * @param timeOut Timeout in milliseconds
     */
    public Timeout(Callable<? extends T> child, long timeOut) {
        this.child = child;
        this.timeOut = timeOut;
    }
    
    /**
     * Prepare for executing a child process with a given timeout.
     * 
     * @param child Method that will be executed
     * @param timeOut Timeout in milliseconds
     */
    public Timeout(Runnable child, long timeOut) {
        this.child = new MaskAsCallable(child);
        this.timeOut = timeOut;
    }
    
    /**
     * Calls the {@link #child}, remember the outcome and remember exceptions.
     */
    private class Catcher implements Runnable {
        
        @Override
        public void run() {
            try {
                outcome = child.call();
                
            } catch (Throwable t) {
                thrown = t;
            }
        }
    }

    /**
     * Translates a {@link Runnable} into a
     * {@link Callable}, which returns {@code null}.
     */
    private class MaskAsCallable implements Callable<T> {
        
        private final Runnable actualCall;

        MaskAsCallable(Runnable actualCall) {
            this.actualCall = actualCall;
        }

        @Override
        public T call() throws Exception {
            actualCall.run();
            return null;
        }
    }
    
    /**
     * Start the computation and remember results.
     * 
     * <p>Results can be obtained using
     * {@link #outcome()} and {@link #thrown()}.</p>
     * 
     * <p>This method will not take much longer than the supplied timeout.</p>
     * 
     * @throws TimeoutException If the time elapsed before the value was computed
     */
    public void execute() throws InterruptedException, TimeoutException {

        thrown = null;
        outcome = null;
        
        long started = System.currentTimeMillis();
        Thread thread = new Thread(new Catcher(), threadName()); 
        StackTraceElement[] snapshot;
        long elapsed;
        
        try {
            thread.start();

            do {
                elapsed = System.currentTimeMillis() - started;
                if (elapsed < timeOut) {
                    thread.join(timeOut - elapsed);
                }
            } while (thread.isAlive() && elapsed < timeOut);

            // Remember, where we were a when the interrupt signal was sent...
            snapshot = thread.getStackTrace();
            
        } finally {
            thread.interrupt(); // and do the interrupt.
        }

        if (elapsed >= timeOut) { // while ended not because !thread.isAlive().
            TimeoutException ex = new TimeoutException(timeOut);
            ex.addSuppressed(new HereWeWere(snapshot));
            throw ex;
        }
    }
    
    private String threadName() {
        String unit = "ms";
        long time = timeOut;
        
        if (time > 1000) {
            time /= 1000;
            unit = "s";
        }
        
        if (time > 60) {
            time /= 60;
            unit = "min";
        }
        
        if (time > 60) {
            time /= 60;
            unit = "h";
        }
        
        return "Time" + time + unit;
    }
    
    /**
     * Value successfully returned by the call.
     * 
     * <p>If both this method and {@link #thrown()} returns
     * a non-{@code null}, then the supplied call can't have
     * reacted to {@link Thread#interrupt()} signal.</p>
     */
    public T outcome() {
        return outcome;
    }
    
    /**
     * Return the exception thrown by the child process.
     */
    public Throwable thrown() {
        return thrown;
    }
    
    /**
     * Give a finite amount of time for a callable to return a value.
     * 
     * <p>This is a convenience call that encapsulates all methods.</p>
     * 
     * @param <T> Type of the returned value
     * @param child Long-lasting computation
     * @param timeOut Maximum time for computation in milliseconds
     * @return Value computed by the callable
     * @throws TimeoutException If the time elapsed before the value was computed
     * @throws InterruptedException The current thread (not the worker thread)
     *      was interrupted using {@link Thread#interrupt()}
     * @throws Exception If anything goes wrong inside the call
     */
    public static <T> T exec(Callable<? extends T> child, long timeOut)
            throws TimeoutException, InterruptedException, Throwable {
        
        Timeout<T> timeouted = new Timeout<>(child, timeOut);
        
        timeouted.execute();
        
        if (timeouted.thrown() != null) {
            throw timeouted.thrown();
        }
        
        return timeouted.outcome();
    }
    
    /**
     * Give a finite amount of time for a runnable and kill it afterwards.
     * 
     * <p>This is a convenience call that encapsulates all methods.</p>
     * 
     * @param child Long-lasting computation
     * @param timeOut Maximum time for computation in milliseconds
     * @return Value computed by the callable
     * @throws TimeoutException If the time elapsed before the value was computed
     * @throws InterruptedException The current thread (not the worker thread)
     *      was interrupted using {@link Thread#interrupt()}
     * @throws RuntimeException If anything goes wrong inside the call
     */
    public static void exec(Runnable child, long timeOut)
            throws TimeoutException, InterruptedException, Throwable {
        
        Timeout<?> timeouted = new Timeout<>(child, timeOut);
        
        timeouted.execute();
        
        if (timeouted.thrown() != null) {
            throw timeouted.thrown();
        }
    }
}
