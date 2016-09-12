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

import cz.cvut.felk.ida.executioner.Future.Status;

/**
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 * @param <T>
 * @param <E>
 */
public class Futuroid<T, E extends Exception> implements Future<T, E> {
    
    public final Call<T,E> task;

    private final Object notified;
    
    private final Class<E> catchable;
    
    public Futuroid(Call<T,E> task, Class<E> catchable) {
        this.task = task;
        this.notified = this;
        this.catchable = catchable;
    }
    
    public Futuroid(Call<T,E> task,
            Class<E> catchable, Object notifier) {
        
        this.task = task;
        this.notified = notifier;
        this.catchable = catchable;
    }
    
    private T result;
    
    Throwable thrown;
    
    private Thread worker;
    
    private long timing;
    
    private Status status = Status.QUEUED;

    @Override
    public Status status() {
        synchronized (notified) {
            return status;
        }
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
    
    void started(Thread worker) {
        synchronized (notified) {
            this.status = Status.RUNNING;
            this.worker = worker;
            notified.notifyAll();
            this.timing = System.currentTimeMillis();
        }
    }
    
    private void success(T result) {
        synchronized (notified) {
            this.timing = System.currentTimeMillis() - this.timing;
            this.status = Status.DONE;
            this.result = result;
            this.worker = null;
            notified.notifyAll();
        }
    }
    
    private void failed(Throwable thrown) {
        synchronized (notified) {
            this.timing = System.currentTimeMillis() - this.timing;
            this.status = Status.DONE;
            this.thrown = thrown;
            this.worker = null;
            notified.notifyAll();
        }
    }
    
    @Override
    public long cpuTime() {
        synchronized (notified) {
            switch (status) {
                case DONE:
                    return timing;

                case RUNNING:
                    return System.currentTimeMillis() - this.timing;

                default:
                    throw new NotStartedYet();
            }
        }
    }
    
    @Override
    public void cancel() {
        synchronized (notified) {
            if (worker != null) {
                worker.interrupt();
                worker = null;
            }
            status = Status.DONE;
            notified.notifyAll();
        }
    }
    
    @Override
    public T get() throws InterruptedException, E {
        synchronized (notified) {
            
            while (status != Status.DONE) {
                notified.wait();
            }

            if (thrown != null) {
                if (thrown instanceof RuntimeException) {
                    throw (RuntimeException) thrown;
                }

                if (catchable.isAssignableFrom(thrown.getClass())) {
                    throw (E) thrown;
                }

                if (thrown instanceof Error) {
                    throw (Error) thrown;
                }

                throw new IllegalArgumentException(
                        "Exception not of the declared class", thrown);
            }

            return result;
        }
    }
    
    @Override
    public T get(long timeOut)
            throws InterruptedException,
                TimeoutException, E {
        
        synchronized (notified) {
            long remains = timeOut;
            long started = System.currentTimeMillis();

            while (status != Status.DONE && remains > 0) {
                notified.wait(remains);

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
}
