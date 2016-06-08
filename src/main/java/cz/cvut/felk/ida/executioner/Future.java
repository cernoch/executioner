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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class Future<T> {
    
    public final Callable<T> task;

    public Future(Callable<T> task) {
        this.task = task;
    }
    
    private T result;
    
    private Throwable thrown;
    
    private Thread worker;
    
    private long timing;
    
    private Status status = Status.QUEUED;

    public static enum Status {
        QUEUED, RUNNING, DONE
    }

    public Status status() {
        return status;
    }
    
    void execute() {
        assert status != Status.RUNNING;
        if (status == Status.DONE) {
            return;
        }
        
        try {
            started(Thread.currentThread());
            success(task.call());
            
        } catch (Throwable ex) {
            failed(ex);
        }
    }
    
    synchronized void started(Thread worker) {
        this.status = Status.RUNNING;
        this.worker = worker;
        notifyAll();

        this.timing = System.currentTimeMillis();
    }
    
    private synchronized void success(T result) {
        this.timing = System.currentTimeMillis() - this.timing;
        this.status = Status.DONE;
        this.result = result;
        this.worker = null;
        notifyAll();
    }
    
    private synchronized void failed(Throwable thrown) {
        this.timing = System.currentTimeMillis() - this.timing;
        this.status = Status.DONE;
        this.thrown = thrown;
        this.worker = null;
        notifyAll();
    }
    
    public long cpuTime() {
        if (status != Status.DONE) {
            throw new IllegalStateException("Not done yet.");
        }
        
        return timing;
    }
    
    public synchronized void cancel() {
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        status = Status.DONE;
        notifyAll();
    }
    
    public synchronized T get()
            throws InterruptedException, Exception {
        
        while (status != Status.DONE) {
            wait();
        }
        
        if (thrown != null) {
            if (thrown instanceof Exception) {
                throw (Exception) thrown;
            }
            
            if (thrown instanceof Error) {
                throw (Error) thrown;
            }
            
            throw new ExecutionException(thrown);
        }

        return result;
    }
    
    public synchronized T get(long timeOut)
            throws InterruptedException,
                TimeoutException, Exception {
        
        long remains = timeOut;
        long started = System.currentTimeMillis();
        
        while (status != Status.DONE && remains > 0) {
            wait(remains);
            
            remains = timeOut + started
                    - System.currentTimeMillis();
        }
        
        if (status != Status.DONE) {
            if (worker != null) {
                throw new TimeoutException(timeOut,
                        worker.getStackTrace(), 3, 1);
            } else {
                throw new TimeoutException(timeOut);
            }
        }
        
        return get();
    }
}
