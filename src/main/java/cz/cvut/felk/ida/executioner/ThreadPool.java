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
import java.util.concurrent.*;

/**
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class ThreadPool {
    
    private final List<Thread> pool = new LinkedList<>();

    private final boolean fixed;
    
    public final BlockingQueue<Future<?>> queue = new LinkedBlockingQueue<>();

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
    
    public <T> Future<T> submit(Callable<T> task) {
        
        Future<T> future = new Future<>(task);
        queue.add(future);

        if (!fixed && !queue.isEmpty()) {
            
            // This is really ugly.
            try {Thread.sleep(1L);}
            catch (InterruptedException ex) {}
            // gives time for the queue to
            // update its .isEmpty() status...
            // We need a better queue!
            
            if (!queue.isEmpty()) {
                startThreads(1, false);
            }
        }
        
        return future;
    }
    
    public <T> Future<T> oneof(Callable<T>... tasks)
            throws InterruptedException {
        
        ArrayList<Future<T>> flist = new ArrayList<>(tasks.length);
        
        synchronized (flist) {
        
            for (Callable<T> task : tasks) {
                Future<T> future = new Future<>(task, flist);
                queue.add(future);
                flist.add(future);
            }

            if (!fixed && !queue.isEmpty()) {

                // This is really ugly.
                try {Thread.sleep(1L);}
                catch (InterruptedException ex) {}
                // gives time for the queue to
                // update its .isEmpty() status...
                // We need a better queue!

                if (!queue.isEmpty()) {
                    startThreads(1, false);
                }
            }

            while (true) {
                flist.wait();

                for (Future<T> future : flist) {
                    if (future.status() == Future.Status.DONE) {
                        return future;
                    }
                }
            }
        }
    }
    
    public <T> Future<T> first(Callable<T>... tasks)
            throws InterruptedException {
        
        ArrayList<Future<T>> flist = new ArrayList<>(tasks.length);
        
        synchronized (flist) {
        
            for (Callable<T> task : tasks) {
                Future<T> future = new Future<>(task, flist);
                queue.add(future);
                flist.add(future);
            }

            if (!fixed && !queue.isEmpty()) {

                // This is really ugly.
                try {Thread.sleep(1L);}
                catch (InterruptedException ex) {}
                // gives time for the queue to
                // update its .isEmpty() status...
                // We need a better queue!

                if (!queue.isEmpty()) {
                    startThreads(1, false);
                }
            }
        
            Future<T> best = null;

            while (true) {
                flist.wait();

                for (Future<T> future : flist) {
                    if (future.status() == Future.Status.DONE) {
                        if (best == null || future.cpuTime() < best.cpuTime()) {
                            best = future;
                        }
                    }
                }

                if (best != null) {
                    boolean allOver = true;

                    for (Future<T> future : flist) {
                        if (future.status() == Future.Status.QUEUED ||
                            future.cpuTime() < best.cpuTime()) {
                            allOver = false;
                            break;
                        }
                    }

                    if (allOver) {
                        return best;
                    }
                }
            }
        }
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
        
        private Future<?> pickup() throws InterruptedException {
            Future<?> out;

            //synchronized (queue) {
                try {
                    waiting++;
                    
                    out = zombie ? queue.take()
                        : queue.poll(3, TimeUnit.SECONDS);
                    
                } finally {
                    waiting--;
                }
            //}
            
            if (out == null) {
                throw new InterruptedException("Dusk of worker's life.");
            }
            
            return out;
        }
        
        @Override
        public void run() {
            try {
                while (!exitting) {
                    pickup().execute();
                }
            } catch (InterruptedException ex) {
                // Thrown in the queue.take() or not a zombie. That's fine.
            }
        }
    }
}
