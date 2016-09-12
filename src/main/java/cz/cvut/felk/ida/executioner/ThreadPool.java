/*
 * The MIT License
 *
 * Copyright 2016 Radomír Černoch (radomir.cernoch at gmail.com).
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

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class ThreadPool {
    
    private final List<Thread> pool = new LinkedList<>();

    private final boolean fixed;
    
    private final ThreadFactory factory;
    
    public ThreadPool(int threads, boolean fixed) {
        this(threads, fixed, Executors.defaultThreadFactory());
    }
    
    public ThreadPool(int threads, boolean fixed,
            ThreadFactory factory) {

        this.fixed = fixed;
        this.factory = factory;
        startThreads(threads, true);
    }
    
    private void startThreads(int count, boolean zombie) {
        
        for (int i = 0; i < count; i++) {
            Thread thread = factory.newThread(new Worker(zombie));
            pool.add(thread);
            thread.start();
        }
    }
    
    private final Queue<Futuroid<?,?>> queue = new LinkedList<>();
    
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
        
    public <T,E extends Exception> Futuroid<T,E>
            submit(Class<E> catchable, Call<T,E> task) {
        Futuroid<T,E> fut = new Futuroid<>(task, catchable);
        submit(fut);
        return fut;
    }

    public synchronized <T> Futurex<T> submit(Callable<T> task) {
        Futurex<T> fut = new Futurex<>(new LegacyCall<>(task));
        submit(fut);
        return fut;
    }
            
    public synchronized Futurun<Void> submit(Runnable task) {
        Futurun<Void> fut = new Futurun<>(new LegacyRun(task));
        submit(fut);
        return fut;
    }
            
    private boolean exitting = false;
    
    public boolean exitting() {
        return exitting;
    }
    
    public void shutdown() {
        exitting = true;

        while (working()) {
            for (Thread thread : pool) {
                thread.interrupt();
            }
        }
    }
    
    public boolean working() {
        for (Thread thread : pool) {
            if (thread.isAlive()) {
                return true;
            }
        }
        return false;
    }
    
    private int waiting = 0;
    
    private class Worker implements Runnable {

        private final boolean zombie;

        public Worker(boolean zombie) {
            this.zombie = zombie;
        }
        
        @Override
        public void run() {
            while (!exitting) {

                Futuroid<?, ?> task;

                synchronized (ThreadPool.this) {
                    try {
                        waiting++;
                        task = dequeue(zombie ? 0 : 3000L);
                    } finally {
                        waiting--;
                    }
                }

                if (task != null) {
                    task.execute();
                } else {
                    return;
                }
            }
        }
    }
}
