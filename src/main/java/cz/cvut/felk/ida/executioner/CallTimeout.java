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
 */
public class CallTimeout<T> extends AbstractTimeout {

    private final Callable<? extends T> child;
    
    public CallTimeout(long timeOut,
            Callable<? extends T> child) {
        
        super(timeOut);
        this.child = child;
    }
    
    /**
     * Encapsulates a {@link Callable} into a {@link Runnable}.
     * 
     * <p>The {@link #run()} calls the {@link Callable#call()} and saves
     * the result. That's the main purpose of this class.</p>
     * 
     * <p>A potential exception is cleverly saved into
     * {@link SaveThrowable#thrown}, so that it will be
     * picked up by {@link AbstractTimeout#run(SaveThrowable, Runnable)}.</p>
     */
    private class ResultSaver extends SaveThrowable implements Runnable {
        
        /**
         * Result from calling {@link #child}.
         */
        private T outcome;
        
        /**
         * Result from calling {@link #child}.
         */
        public T outcome() {
            return outcome;
        }
        
        @Override
        public void run() {
            try {
                outcome = child.call();
            } catch (Exception ex) {
                thrown = ex;
            }
        }
    }
    
    /**
     * Run a child process, terminate if timeout is exceeded and return result.
     * 
     * <p>The child process is notified about the timeout using
     * {@link Thread#interrupt()}. It can be picked up either by catching
     * {@link InterruptedException} or {@link Thread#interrupted()}.</p>
     * 
     * <p>This method will not take much longer than the supplied timeout.</p>
     * 
     * @throws TimeoutException Time elapsed before the child ended.
     * @throws InterruptedException Interrupted while waiting for the child.
     * @throws Throwable Something went wrong in the child.
     */
    public T call() throws InterruptedException, TimeoutException, Throwable {
        
        ResultSaver resultSaver = new ResultSaver();
        run(resultSaver, resultSaver);
        return resultSaver.outcome;
    }
}
