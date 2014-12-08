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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * 
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public final class Futures {

    private Futures() {}

    private static Exception tryToExtractCause(Exception ex) {
        
        Throwable cause = ex.getCause();
        if (cause == null) {
            return ex;
        }
        
        if (cause instanceof Exception) {
            cause.addSuppressed(ex);
            return (Exception) cause;
        } else {
            return ex;
        }
    }
    
    public static <T> T get(Future<T> future) throws Exception {
        try {
            return future.get();
            
        } catch (ExecutionException ex) {
            throw tryToExtractCause(ex);
        }
    }
    
    public static <T> T get(Future<T> future,
            long timeout, TimeUnit unit)
            throws Exception {
        
        try {
            return future.get(timeout, unit);
            
        } catch (ExecutionException ex) {
            throw tryToExtractCause(ex);
        }
    }
}
