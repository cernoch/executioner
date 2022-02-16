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

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.logging.Level.*;

/**
 * Thread pool holds a number of threads for asynchronous calculation.
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class ThreadPool {
    
    /**
     * List of threads currently waiting for a task.
     */
    private final List<Thread> pool = new LinkedList<>();

    /**
     * Fixed pools do not create new threads on demand.
     */
    private final boolean fixed;
    
    /**
     * Factory for creating new threads.
     */
    private final ThreadFactory factory;
    
    /**
     * Create a new thread pool with a default thread factory.
     * 
     * <p>Pools that are not {@code fixed} always ensure that
     * {@linkplain #submit(Class, Call) submitted} calls
     * will start the computation very quickly.</p>
     * 
     * @param threads number of cached threads to be always available
     * @param fixed {@code true} forbids creating new threads on demand
     */
    public ThreadPool(int threads, boolean fixed) {
        this(threads, fixed, Executors.defaultThreadFactory());
    }
    
    /**
     * Create a new thread pool with a user-supplied thread factory.
     * 
     * <p>Pools that are not {@code fixed} always ensure that
     * {@linkplain #submit(Class, Call) submitted} calls
     * will start the computation very quickly.</p>
     * 
     * <p>A custom thread factory may be used to define custom thread names.</p>
     * 
     * @param threads number of cached threads to be always available
     * @param fixed {@code true} forbids creating new threads on demand
     * @param factory non-{@code null} factory for new threads
     */
    public ThreadPool(int threads, boolean fixed,
            ThreadFactory factory) {

        this.fixed = fixed;
        this.factory = factory;
        startThreads(threads, true);
    }
    
    /**
     * Start new threads.
     * 
     * @param count number of threads to be started
     * @param zombie {@code true} new threads will wait for new tasks until
     * someone {@linkplain Thread#interrupt() interrupts} them
     * (e.g. using {@link #shutdown()}).
     */
    private void startThreads(int count, boolean zombie) {
        
        for (int i = 0; i < count; i++) {
            Thread thread = factory.newThread(new Worker(zombie));
            pool.add(thread);
            thread.start();
        }
    }
    
    /**
     * Queue of tasks waiting to be executed.
     * 
     * <p>The linked list is not synchronized, use a
     * {@code synchronized(this) { ... }} block to access it.</p>
     */
    private final Queue<Futuroid<?,?>> queue = new LinkedList<>();
    
    /**
     * Pick an item from the {@link #queue}.
     * 
     * @param timeout number of milliseconds to wait
     * @return a new call or {@code null} if the worker thread should terminate
     */
    private synchronized Futuroid<?, ?> dequeue(long timeout) {
        try {
            while (queue.isEmpty()) {
                wait(timeout);
                
                if (timeout <= 0 && queue.isEmpty()) {
                    // non-zombie thread revived
                    return null;
                }
            }
        } catch (InterruptedException ex) {
            return null;
        }

        return queue.poll();
    }

    /**
     * Low-level submitting method for a fresh future.
     * 
     * @param <T> type of the returned value
     * @param <E> type of allowed checked exception
     * @param future instance that will NEVER be submitted to another pool
     */
    synchronized <T,E extends Exception> void submit(Futuroid<T,E> future) {

        queue.add(future);
        
        if (waiting == 0) {
            if (!fixed) {
                startThreads(1, false);
            }
        } else {
            notify();
        }
    }
        
    /**
     * Submit a new computation and with a checked exception.
     * 
     * <p>Because of type erasure, please supply a class of the checked
     * exception. Opening a non-blocking writer may look:</p>
     * 
     * <pre>
int threads = Runtime.getRuntime().availableProcessors();
ThreadPool pool = new ThreadPool(threads, true);
Future&lt;FileWriter,IOException&gt; fut = pool.submit(
    IOException.class, () -&gt; new FileWriter("file.txt"));

try {
    fut.get(1000L).write(...);
} catch(IOException ex) {
    ex.printStackTrace(System.err);
} catch(TimeoutException | InterruptedException ex) {
    ex.printStackTrace(System.err);
}</pre>
     * 
     * @param <T> type of the returned value
     * @param <E> type of allowed checked exception
     * @param catchable class which contains E
     * @param task computation to be performed
     * @return a future that holds the computation result
     */
    public <T,E extends Exception> Futuroid<T,E>
            submit(Class<E> catchable, Call<T,E> task) {
        Futuroid<T,E> fut = new Futuroid<>(task, catchable);
        submit(fut);
        return fut;
    }

    /**
     * Submit a legacy {@link Callable}.
     * 
     * @param <T> type of the returned value
     * @param task computation to be performed
     * @return a future that holds the computation result
     * and throws the exception thrown by the task
     */
    public synchronized <T> Futurex<T> submit(Callable<T> task) {
        Futurex<T> fut = new Futurex<>(new LegacyCall<>(task));
        submit(fut);
        return fut;
    }
            
    /**
     * Submit a legacy {@link Runnable}.
     * 
     * @param task computation to be performed
     * @return a future that holds the computation result,
     * returns {@linkplain Future#get() null} and
     * and throws the exception thrown by the task
     */
    public synchronized Futurun<Void> submit(Runnable task) {
        Futurun<Void> fut = new Futurun<>(new LegacyRun(task));
        submit(fut);
        return fut;
    }
            
    /**
     * The pool is currently in exitting state.
     */
    private boolean exitting = false;
    
    /**
     * Determines if the thread pool is able to handle new tasks.
     * 
     * @return {@code false} until someone calls {@link #shutdown()}
     */
    public boolean exitting() {
        return exitting;
    }
    
    /**
     * Interrupt all {@link Future.Status#RUNNING} tasks and stop worker threads.
     */
    public void shutdown() {
        exitting = true;

        while (working()) {
            for (Thread thread : pool) {
                thread.interrupt();
            }
        }
    }
    
    /**
     * Determines if at least 1 worker thread is working.
     * 
     * <p>Please note that a worker thread may be looking for a new task,
     * doing some maintenance even if no actual computation is going on.</p>
     * 
     * @return {@code false} if all worker threads are sleeping
     */
    public boolean working() {
        for (Thread thread : pool) {
            if (thread.isAlive()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Number of waiting worker threads.
     */
    private int waiting = 0;
    
    /**
     * Worker thread is cached and performs the actual computation.
     */
    private class Worker implements Runnable {

        /**
         * Zombie thread waits for the {@link #queue} indefinitely.
         */
        private final boolean zombie;

        /**
         * Default constructor initializes the fields.
         * @param zombie thread waits for the {@link #queue} indefinitely
         */
        public Worker(boolean zombie) {
            this.zombie = zombie;
        }
        
        @Override
        public void run() {
            while (!exitting) {

                Futuroid<?, ?> task;

                synchronized (ThreadPool.this) {
                    try {
                        L.log(FINEST, "Worker #" + hashCode()
                                + " is about to wait for a task.");
                        waiting++;
                        L.log(FINE, "Worker #" + hashCode()
                                + " sees " + waiting + " workers.");
                        task = dequeue(zombie ? 0 : 3000L);
                        L.log(FINER, "Worker #" + hashCode()
                                + " got task: " + task);
                    } finally {
                        waiting--;
                        L.log(FINEST, "Worker #" + hashCode()
                                + " stopped waiting.");
                    }
                }

                if (task != null) {
                    L.log(FINE, "Worker #" + hashCode()
                            + " starts executing: " + task);
                    task.execute();
                } else {
                    L.log(FINE, "Worker #" + hashCode()
                            + " is exiting.");
                    return;
                }
            }
        }
    }

    private static final Logger L = Logger.getLogger(
            ThreadPool.class.getName());
}
