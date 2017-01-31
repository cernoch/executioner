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

import java.util.concurrent.Callable;

/**
 * Executable code which returns some value and may throw a checked exception.
 * 
 * <p>If you use this interface instead of {@link Callable}, you may
 * still use all the {@code java.util.concurrent.*} classes.</p>
 * 
 * @param <T> type of the returned result
 * @param <E> type of a checked exception
 * 
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 * @see Callable
 * @see Future
 */
public interface Call<T, E extends Exception> extends Callable<T> {
    
    /**
     * Produce a result or throw an exception.
     * 
     * <p>This may throw any unchecked exception or a checked
     * exception if it is a subclass of {@code E}.</p>
     * 
     * <p>A good practice is to periodically check {@link Thread#interrupted()}
     * and if you get {@code true}, then stop the calculation
     * as quickly as possible.</p>
     * 
     * @return any value, even {@code null} is supported
     * @throws E if the value cannot be produced
     */
    @Override
    T call() throws E;
}
