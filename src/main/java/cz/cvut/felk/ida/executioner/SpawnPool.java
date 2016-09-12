/*
 * The MIT License
 *
 * Copyright 2016 Radek.
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
package cz.cvut.felk.ida.executioner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

/**
 *
 * @author Radek
 */
public class SpawnPool {

    private final ThreadPool workers;
    
    private final ThreadPool checkers;
    
    public SpawnPool(int threads, boolean fixed) {
        workers = new ThreadPool(threads, fixed);
        checkers = new ThreadPool(0, false);
    }        
    
    public SpawnPool(int threads, boolean fixed,
            ThreadFactory workerFactory,
            ThreadFactory checkerFactory) {
        
        workers = new ThreadPool(threads, fixed, workerFactory);
        checkers = new ThreadPool(0, false, checkerFactory);
    }

    private <T, E extends Exception> Futuroidy<T, E> submit(
            List<Futuroid<T,E>> sink, Futuroidy<T,E> fee, Class<E> catchable,
            Call<Void, InterruptedException> checker,
            Collection<? extends Call<T,E>> tasks) {
        
        // Submit the subtasks and register their futures
        for (Call<T,E> task : tasks) {
            Futuroid<T,E> foid = new Futuroid<>(task, catchable, fee);
            workers.submit(foid);
            sink.add(foid);
        }
        
        // Start monitoring the subtasks and return the first one
        checkers.submit(InterruptedException.class, checker);

        return fee;
    }
        
    public <T, E extends Exception> Futuroidy<T, E> first(
            Class<E> catchable, long timeOut,
            Collection<? extends Call<T,E>> tasks) {
        
        List<Futuroid<T,E>> sub = new ArrayList<>();
        Futuroidy<T,E> fee = new Futuroidy<>(sub, timeOut);
        submit(sub, fee, catchable, fee.new PreferFirst(), tasks);
        return fee;
    }
            
    public <T, E extends Exception> Futuroidy<T, E> first(
            Class<E> catchable, long timeOut, Call<T,E>... tasks) {
        return first(catchable, timeOut, Arrays.asList(tasks));
    }
    
    public <T> Futurexy<T> firstCall(long timeOut,
            Iterable<? extends Callable<T>> tasks) {
        
        List<Futuroid<T,Exception>> sub = new ArrayList<>();
        Futurexy<T> fee = new Futurexy<>(sub, timeOut);
        
        submit(sub, fee, Exception.class,
                fee.new PreferFirst(),
                LegacyCall.convert(tasks));
        return fee;
    }
    
    public <T> Futurexy<T> firstCall(long timeOut, Callable<T>... tasks) {
        return firstCall(timeOut, Arrays.asList(tasks));
    }
    
    public Futuruny firstRun(long timeOut,
            Collection<? extends Runnable> tasks) {

        List<Futuroid<Void,RuntimeException>> sub = new ArrayList<>();
        Futuruny fee = new Futuruny(sub, timeOut);
        
        submit(sub, fee, RuntimeException.class,
                fee.new PreferFirst(),
                LegacyRun.convert(tasks));
        return fee;
    }
    
    public Futuruny firstRun(long timeOut, Runnable... tasks) {
        return firstRun(timeOut, Arrays.asList(tasks));
    }
    
    public <T, E extends Exception> Futuroidy<T, E> oneof(
            Class<E> catchable, long timeOut,
            Collection<? extends Call<T,E>> tasks) {
        
        List<Futuroid<T,E>> sub = new ArrayList<>();
        Futuroidy<T,E> fee = new Futuroidy<>(sub, timeOut);
        submit(sub, fee, catchable, fee.new PreferFastest(), tasks);
        return fee;
    }

    public <T, E extends Exception> Futuroidy<T, E> oneof(
            Class<E> catchable, long timeOut, Call<T,E>... tasks) {
        return oneof(catchable, timeOut, Arrays.asList(tasks));
    }
    
    
    public <T> Futurexy<T> oneofCall(long timeOut,
            Iterable<? extends Callable<T>> tasks) {
        
        List<Futuroid<T,Exception>> sub = new ArrayList<>();
        Futurexy<T> fee = new Futurexy<>(sub, timeOut);
        
        submit(sub, fee, Exception.class,
                fee.new PreferFastest(),
                LegacyCall.convert(tasks));
        return fee;
    }
    
    public <T> Futurexy<T> oneofCall(long timeOut, Callable<T>... tasks) {
        return firstCall(timeOut, Arrays.asList(tasks));
    }
    
    public Futuruny oneofRun(long timeOut,
            Collection<? extends Runnable> tasks) {

        List<Futuroid<Void,RuntimeException>> sub = new ArrayList<>();
        Futuruny fee = new Futuruny(sub, timeOut);
        
        submit(sub, fee, RuntimeException.class,
                fee.new PreferFastest(),
                LegacyRun.convert(tasks));
        return fee;
    }
    
    public Futuruny oneofRun(long timeOut, Runnable... tasks) {
        return firstRun(timeOut, Arrays.asList(tasks));
    }

    public void shutdown() {
        checkers.shutdown();
        workers.shutdown();
    }
}
