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

import io.github.cernoch.executioner.Future.Status;

/**
 * Default implementation of a {@link Future}.
 *
 * @param <T> type of the returned value
 * @param <E> type of allowed checked exception
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 * @see Future
 * @see Call
 */
public class Futuroid<T, E extends Exception> implements Future<T, E> {
    
    /**
     * Task to be calculated.
     */
    public final Call<T,E> task;

    /**
     * Synchronizer for modification of the state, usually {@code this}.
     */
    private final Object notified;
    
    /**
     * Class of the checked exception used for detection.
     */
    private final Class<E> catchable;
    
    /**
     * Default constructor, which synchronizes on {@code this}.
     * 
     * @param task task to be calculated
     * @param catchable class of the checked exception
     */
    Futuroid(Call<T,E> task, Class<E> catchable) {
        this.task = task;
        this.notified = this;
        this.catchable = catchable;
    }
    
    /**
     * Constructor which synchronizes on a supplied object.
     * 
     * @param task task to be calculated
     * @param catchable class of the checked exception
     * @param notifier monitor used for synchronization
     */
    Futuroid(Call<T,E> task,
            Class<E> catchable, Object notifier) {
        
        this.task = task;
        this.notified = notifier;
        this.catchable = catchable;
    }
    
    /**
     * Result of the computation.
     * 
     * <p>Initialized at {@code null}, may change after
     * {@link #status} becomes {@link Status#DONE}.</p>
     */
    private T result;
    
    /**
     * Exception thrown during the computation.
     * 
     * <p>Initialized at {@code null}, may change after
     * {@link #status} becomes {@link Status#DONE}.</p>
     */
    Throwable thrown;
    
    /**
     * Thread that performs the computation.
     * 
     * <p>Is {@code null} if and only if {@link #status}
     * is not {@link Status#RUNNING}.</p>
     */
    private Thread worker;
    
    /**
     * Holds information about runtime.
     * 
     * <p>For {@link Status#QUEUED}, this is {@code 0}. When the status becomes
     * {@link Status#RUNNING} we assign {@link System#currentTimeMillis()}.
     * After the transition to {@link Status#DONE}, this becomes
     * the elapsed time (in milliseconds).
     */
    private long timing;
    
    /**
     * Current status of the calculation.
     */
    private Status status = Status.QUEUED;

    @Override
    public Status status() {
        synchronized (notified) {
            return status;
        }
    }
    
    /**
     * Do the calculation.
     * 
     * <p>This method automatically sets the {@link #status}.</p>
     */
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
    
    /**
     * Transition from {@link Status#QUEUED} to {@link Status#RUNNING}.
     * 
     * @param worker thread that performs the computation
     */
    void started(Thread worker) {
        synchronized (notified) {
            this.status = Status.RUNNING;
            this.worker = worker;
            notified.notifyAll();
            this.timing = System.currentTimeMillis();
        }
    }
    
    /**
     * Transition from {@link Status#RUNNING} to {@link Status#DONE}.
     * 
     * @param result successful result of the calculation
     */
    private void success(T result) {
        synchronized (notified) {
            this.timing = System.currentTimeMillis() - this.timing;
            this.status = Status.DONE;
            this.result = result;
            this.worker = null;
            notified.notifyAll();
        }
    }
    
    /**
     * Transition from {@link Status#RUNNING} to {@link Status#DONE}.
     * 
     * @param thrown exception thrown by the calculation
     */
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
    public void interrupt() {
        synchronized (notified) {
            switch (status) {
                case QUEUED:
                    this.timing = 0;
                    this.thrown = new InterruptedException();
                    this.status = Status.DONE;
                    notified.notifyAll();
                    break;
                    
                case RUNNING:
                    worker.interrupt();
                    break;
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
            if (status != Status.DONE) {
                this.timing = System.currentTimeMillis() - this.timing;            
                this.thrown = new InterruptedException();
            }
            this.status = Status.DONE;
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
