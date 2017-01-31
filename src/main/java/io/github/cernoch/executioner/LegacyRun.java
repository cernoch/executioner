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

/**
 * Encapsulates a legacy {@link Runnable} into a {@link Call}.
 * 
 * <p>The type of the returned value is {@link Void}. Hence the {@link Futurun}
 * will return {@code null} if the computation finishes successfully.</p>
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class LegacyRun implements Call<Void, RuntimeException> {
    
    /** The legacy call. */
    private final Runnable call;

    /**
     * Default constructor only initializes the members.
     * 
     * @param call non-{@code null} legacy call to be called later
     */
    public LegacyRun(Runnable call) {
        this.call = call;
    }

    @Override
    public Void call() throws RuntimeException {
        call.run();
        return null;
    }
    
    /**
     * Converts a list of {@link Runnable}s into a list of {@link Call}s.
     * 
     * @param callables legacy calls
     * @return a list with all converted items
     */
    public static List<LegacyRun> convert(
            Iterable<? extends Runnable> callables) {
        
        List<LegacyRun> list;
        
        if (callables instanceof Collection) {
            Collection<Runnable> coll = (Collection<Runnable>) callables;
            list = new ArrayList<>(coll.size());
        } else {
            list = new LinkedList<>();
        }
        
        for (Runnable callable : callables) {
            list.add(new LegacyRun(callable));
        }
        
        return list;
    }
}
