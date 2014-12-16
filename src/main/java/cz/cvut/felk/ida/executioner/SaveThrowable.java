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

import java.lang.Thread.UncaughtExceptionHandler;

/**
 * Saves the last thrown exception in a field.
 * 
 * <p>Every exception provided through {@link UncaughtExceptionHandler}
 * is saved in a local field. It can be retrieved using {@link #thrown()}.</p>
 */
class SaveThrowable implements UncaughtExceptionHandler {

    /**
     * Last exception passed to {@link #uncaughtException(Thread, Throwable)}.
     */
    protected Throwable thrown;

    /**
     * Save the exception in {@link #thrown}.
     */
    @Override
    public void uncaughtException(Thread t, Throwable thrown) {
        this.thrown = thrown;
    }

    /**
     * Last exception passed to {@link #uncaughtException(Thread, Throwable)}.
     */
    public Throwable thrown() {
        return thrown;
    }
}
