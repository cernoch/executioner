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

/**
 * Calls a {@link Runnable} and stores a result in a {@link SaveThrowable}.
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
abstract class AbstractTimeout {
    
    /**
     * Time-out in milliseconds.
     */
    protected final long timeOut;

    /**
     * Prepare for executing a child process with a given timeout.
     * 
     * @param timeOut Timeout in milliseconds
     */
    AbstractTimeout(long timeOut) {
        this.timeOut = timeOut;
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
     * @param child Child runnable to be executed.
     * @param thrownSink Stores any thrown exception.
     * 
     * @throws InterruptedException Interrupted while waiting for the child.
     * @throws TimeoutException Time elapsed before the child ended.
     * @throws Throwable Exception from the child.
     */
    protected void run(Runnable child, SaveThrowable thrownSink)
            throws InterruptedException, TimeoutException, Throwable {

        Thread thread = new Thread(child, threadName()); 
        thread.setUncaughtExceptionHandler(thrownSink);
        
        try {
            thread.start();
            thread.join(timeOut);
            
            // Remember stack before the interrupt.
            StackTraceElement[] snapshot = thread.getStackTrace();
            // Local reference for thread safety.
            Throwable thrownInChild = thrownSink.thrown();

            // TimeoutRunnable has passed
            if (thread.isAlive()) {
                TimeoutException ex = new TimeoutException(timeOut);

                if (snapshot != null) {
                    ex.addSuppressed(new HereWeWere(snapshot));
                }

                if (thrownInChild != null) {
                    ex.addSuppressed(thrownInChild);
                }
                throw ex;
            }

            // An exception has been thrown
            if (thrownInChild != null) {
                thrownInChild.addSuppressed(new HereWeWere(
                        new Exception().getStackTrace()));
                throw thrownInChild;
            }
            
        } finally {
            thread.interrupt(); // and do the interrupt.
        }
    }
    
    /**
     * Number of threads created so far.
     */
    private static int threadsCreatedSoFar = 0;
    
    /**
     * Gives a unique thread name, based on the
     * {@link #timeOut} and {@link #threadsCreatedSoFar}.
     */
    private String threadName() {
        String unit = "ms";
        double time = timeOut;
        
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
        
        return String.format("TimeOut%02.1f%s#%d",
                time, unit, ++threadsCreatedSoFar);
    }
}
