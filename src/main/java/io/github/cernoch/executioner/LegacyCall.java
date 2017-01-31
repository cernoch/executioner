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

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Encapsulates a legacy {@link Callable} into a {@link Call}.
 *
 * @param <T> type of the returned result
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class LegacyCall<T> implements Call<T, Exception> {
    
    /** The legacy call. */
    private final Callable<T> call;

    /**
     * Default constructor only initializes the members.
     * 
     * @param call non-{@code null} legacy call to be called later
     */
    public LegacyCall(Callable<T> call) {
        this.call = call;
    }

    @Override
    public T call() throws Exception {
        return call.call();
    }
    
    /**
     * Converts a list of {@link Callable}s into a list of {@link Call}s.
     * 
     * @param <T> type of the returned result
     * @param callables legacy calls
     * @return a list with all converted items
     */
    public static <T> List<LegacyCall<T>>
            convert(Iterable<? extends Callable<T>> callables) {
        
        List<LegacyCall<T>> list;
        
        if (callables instanceof Collection) {
            Collection<Callable<T>> coll = (Collection<Callable<T>>) callables;
            list = new ArrayList<>(coll.size());
        } else {
            list = new LinkedList<>();
        }
        
        for (Callable<T> callable : callables) {
            list.add(new LegacyCall<>(callable));
        }
        
        return list;
    }
}
