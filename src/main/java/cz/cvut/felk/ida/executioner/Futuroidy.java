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

import java.util.Collections;
import java.util.List;

/**
 *
 * @author Radek
 * @param <T>
 * @param <E>
 */
public class Futuroidy<T, E extends Exception> implements Future<T, E> {

    final List<Futuroid<T, E>> tasks;
    
    private final long timeOut;
    
    public final long started = System.currentTimeMillis();
    
    private Futuroid<T, E> best = null;
    
    private Status status = Status.QUEUED;

    public Futuroidy(List<Futuroid<T, E>> flist, long timeOut) {
        this.tasks = Collections.unmodifiableList(flist);
        this.timeOut = timeOut;
    }

    
    private long timeOut() {
        if (best != null) {
            return best.cpuTime();
        }
        return timeOut;
    }
    
    @Override
    public synchronized Status status() {
        return status;
    }

    @Override
    public synchronized T get() throws InterruptedException, E {
        
        while (status != Status.DONE) {
            wait();
        }
        
        if (best != null) {
            return best.get();
        }
        
        // if "best" is null & status is DONE,
        // then all subtasks must have failed.
        AllTasksFailed thrown = new AllTasksFailed();
        for (Futuroid<T,E> fut : tasks) {
            if (fut.thrown != null) {
                thrown.addSuppressed(fut.thrown);
            }
        }
        throw thrown;
    }

    @Override
    public synchronized T get(long timeOut)
            throws InterruptedException, TimeoutException, E {
        
        while (status != Status.DONE) {
            wait(timeOut);
        }
        return best.get();
    }

    @Override
    public synchronized void cancel() {
        for (Futuroid<T, E> future : tasks) {
            future.cancel();
        }
    }

    @Override
    public synchronized long cpuTime() {
        if (status == Status.DONE) {
            if (best != null) {
                return best.cpuTime();
            } else {
                return timeOut;
            }
        }
        return System.currentTimeMillis() - started;
    }

    public synchronized Timing timing(long errorValue) {
        if (status != Status.DONE) {
            throw new NotDoneYet();
        }
        
        long[] cpuTimes = new long[this.tasks.size()];
        
        int i = 0;
        for (Futuroid<T,E> fut : tasks) {

            if (fut.thrown == null) {
                cpuTimes[i++] = fut.cpuTime();
            } else {
                cpuTimes[i++] = errorValue;
            }
        }
        
        return Timing.from(cpuTimes);
    }

    class PreferFirst implements Call<Void, InterruptedException> {

        @Override
        public Void call() throws InterruptedException {
            synchronized (Futuroidy.this) {

                boolean allDone = false;
                while (!allDone) {
                    allDone = true;
                    
                    for (Futuroid<T, E> fut : tasks) {

                        if (fut.status() == Status.RUNNING) {
                            if (status != Status.RUNNING) {
                                status = Status.RUNNING;
                                Futuroidy.this.notifyAll();
                            }
                        }
                        if (fut.status() == Status.DONE && fut.thrown == null) {
                            if (best == null || fut.cpuTime() < timeOut()) {
                                best = fut;
                            }
                        }
                    }
                    
                    // time to the next expected event
                    long nextEvent = timeOut();
                    
                    for (Futuroid<T, E> future : tasks) {

                        if (future.status() == Status.RUNNING) {
                            long remains = timeOut() - future.cpuTime();
                            
                            if (remains <= 0) {
                                if (timeOut > 0 || best != null) {
                                    // value in "remains" is valid
                                    future.cancel();
                                    continue;
                                }
                            } else if (nextEvent > remains) {
                                nextEvent = remains;
                            }
                        }
                        
                        if (future.status() != Status.DONE) {
                            // the "continue" above is necessary
                            // if cuture.cancel() was called,
                            // its status will become DONE
                            allDone = false;
                        }
                    }
                    
                    if (!allDone) {
                        Futuroidy.this.wait(nextEvent);
                    }
                }
                status = Status.DONE;
                Futuroidy.this.notifyAll();
                return null;

            }
        }
    }

    class PreferFastest implements Call<Void, InterruptedException> {

        @Override
        public Void call() throws InterruptedException {
            synchronized (Futuroidy.this) {

                boolean someRuns = false;
                while (!someRuns) {
                    someRuns = false;
                    
                    for (Futuroid<T, E> fut : tasks) {

                        if (fut.status() == Status.RUNNING) {
                            if (status != Status.RUNNING) {
                                status = Status.RUNNING;
                                Futuroidy.this.notifyAll();
                            }
                        }
                        
                        if (fut.status() == Status.DONE) {
                            someRuns = true;
                            
                            if (fut.thrown == null) {
                                best = fut;
                            }
                        }
                    }
                    
                    // time to the next expected event
                    long nextEvent = timeOut;
                    
                    for (Futuroid<T, E> future : tasks) {

                        if (future.status() == Status.RUNNING) {
                            if (timeOut > 0) {
                                long remains = timeOut - future.cpuTime();
                                
                                if (remains <= 0) {
                                    future.cancel();
                                    
                                } else if (nextEvent > remains) {
                                    nextEvent = remains;
                                }
                            }
                        }
                    }
                    
                    if (!someRuns) {
                        Futuroidy.this.wait(nextEvent);
                    }
                }
                
                for (Futuroid<T, E> future : tasks) {
                    future.cancel();
                }                
                
                status = Status.DONE;
                Futuroidy.this.notifyAll();
                return null;
            }
        }
    }
}
