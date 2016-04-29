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
    
    private final List<Thread> pool;

    public ThreadPool(int threads) {

        this.pool = Arrays.asList(new Thread[threads]);
        for (int thread = 0; thread < pool.size(); thread++) {
            pool.set(thread, new Thread(new Worker()));
        }
        
        this.queue = new LinkedBlockingQueue<>();
        
        for (Thread thread : pool) {
            thread.start();
        }
    }
    
    public final BlockingQueue<Future<?>> queue;
    
    public <T> Future<T> submit(Callable<T> task) {
        Future<T> future = new Future<>(task);
        queue.add(future);
        return future;
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
    
    private class Worker implements Runnable {

        @Override
        public void run() {
            try {
                while (!exitting) {
                    queue.take().execute();
                }
            } catch (InterruptedException ex) {
                // Thrown in the take(). That's fine.
            }
        }
    }
}
