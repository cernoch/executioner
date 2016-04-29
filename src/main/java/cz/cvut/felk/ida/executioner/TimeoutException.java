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
 * Timeout exception that carries the information about time spent waiting.
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class TimeoutException extends java.util.concurrent.TimeoutException {
    
    private static final long serialVersionUID = 643848510L;

    private final long timeOut;

    public TimeoutException(long timeOut) {
        super("Time ran out after " + timeOut + "ms.");
        this.timeOut = timeOut;
    }

    public TimeoutException(long timeOut,
            StackTraceElement[] add,
            int skipAdded, int skipMine) {
        
        this(timeOut);
        StackTraceElement[] old = getStackTrace();

        if (skipAdded > add.length) {
            skipAdded = add.length;
        }
        
        if (skipMine > old.length) {
            skipMine = old.length;
        }
        
        StackTraceElement[] neu = new StackTraceElement[
                old.length + add.length - skipAdded - skipMine];
        
        System.arraycopy(add, 0,        neu, 0, add.length - skipAdded);
        System.arraycopy(old, skipMine, neu,    add.length - skipAdded,
                         old.length - skipMine);
        
        setStackTrace(neu);
    }
    
    /**
     * Time spent by spending for the other thread to stop.
     * 
     * @return Amount of time in milliseconds.
     */
    public long timeOut() {
        return timeOut;
    }
}
