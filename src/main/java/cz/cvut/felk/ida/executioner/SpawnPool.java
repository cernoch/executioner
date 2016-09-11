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
            ThreadFactory workerFactory, ThreadFactory checkerFactory) {
        
        workers = new ThreadPool(threads, fixed, workerFactory);
        checkers = new ThreadPool(0, false, checkerFactory);
    }

    public <T, E extends Exception> SpawnFuture<T, E> first(
            Class<E> catchable, long timeOut,
            Collection<? extends Call<T,E>> tasks) {
        
        // List of futures for all sub-tasks
        List<SimpleFuture<T,E>> sub = new ArrayList<>();
        
        // Future that returns the first successful sub-task
        SpawnFuture<T,E> ff = new SpawnFuture<>(sub, timeOut);
        
        // Submit the subtasks and register their futures
        for (Call<T,E> task : tasks) {
            sub.add(workers.submit(new SimpleFuture<>(task, catchable, ff)));
        }
        
        // Start monitoring the subtasks and return the first one
        checkers.submit(InterruptedException.class, ff.new PreferFirst());

        return ff;
    }
            
    public <T, E extends Exception> SpawnFuture<T, E> first(
            Class<E> catchable, long timeOut, Call<T,E>... tasks) {
        return first(catchable, timeOut, Arrays.asList(tasks));
    }
    
    public <T> SpawnFuture<T,Exception> firstCall(long timeOut,
            Iterable<? extends Callable<T>> tasks) {
        return first(Exception.class, timeOut, LegacyCall.convert(tasks));
    }
    
    public <T> SpawnFuture<T, Exception> firstCall(long timeOut,
            Callable<T>... tasks) {
        return firstCall(timeOut, Arrays.asList(tasks));
    }
    
    public SpawnFuture<Void,RuntimeException> firstRun(
            long timeOut, Collection<? extends Runnable> tasks) {
        return first(RuntimeException.class, timeOut, LegacyRun.convert(tasks));
    }
    
    public SpawnFuture<Void,RuntimeException> firstRun(
            long timeOut, Runnable... tasks) {
        return firstRun(timeOut, Arrays.asList(tasks));
    }
    
    public <T, E extends Exception> SpawnFuture<T, E> oneof(
            Class<E> catchable, long timeOut,
            Collection<? extends Call<T,E>> tasks) {
        
        // List of futures for all sub-tasks
        List<SimpleFuture<T,E>> sub = new ArrayList<>();
        
        // Future that returns the first successful sub-task
        SpawnFuture<T,E> ff = new SpawnFuture<>(sub, timeOut);
        
        // Submit the subtasks and register their futures
        for (Call<T,E> task : tasks) {
            sub.add(workers.submit(new SimpleFuture<>(task, catchable, ff)));
        }
        
        // Start monitoring the subtasks and return the first one
        checkers.submit(InterruptedException.class, ff.new PreferFastest());

        return ff;
    }

    public <T, E extends Exception> SpawnFuture<T, E> oneof(
            Class<E> catchable, long timeOut, Call<T,E>... tasks) {
        return oneof(catchable, timeOut, Arrays.asList(tasks));
    }
    
    public void shutdown() {
        checkers.shutdown();
        workers.shutdown();
    }
}
