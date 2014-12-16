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
 * Executes a {@link Runnable} with a timeout. 
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class RunTimeout extends AbstractTimeout {
    
    /**
     * Default constructor with a given timeout.
     */
    public RunTimeout(long timeOut) {
        super(timeOut);
    }
    
    /**
     * Run a child process and terminate if timeout is exceeded.
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
    public void run(Runnable run) throws
            TimeoutException, InterruptedException, Throwable {

        run(run, new SaveThrowable());
    }
}
