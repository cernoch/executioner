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

import java.util.Arrays;

/**
 * Statistical properties of sub-tasks in a spawned call.
 *
 * @author Radomír Černoch (radomir.cernoch at gmail.com)
 */
public class Timing {

    /**
     * Shortest time a sub-call achieved.
     */
    public final long min;

    /**
     * Longest time a sub-call achieved.
     */
    public final long max;

    /**
     * Median of sub-calls' run-times.
     */
    public final long med;

    /**
     * Median of absolute deviations of sub-calls' run-times.
     */
    public final long mad;
    
    /**
     * Mean value of sub-calls' run-times.
     */
    public final double avg;

    /**
     * Standards deviation of sub-calls' run-times.
     */
    public final double dev;

    /**
     * Default constructor initializes all values.
     * 
     * @param min shortest time a sub-call achieved
     * @param max longest time a sub-call achieved
     * @param med median of sub-calls' run-times
     * @param mad median of absolute deviations of sub-calls' run-times
     * @param avg mean value of sub-calls' run-times
     * @param dev standards deviation of sub-calls' run-times
     */
    public Timing(long min, long max,
            long med, long mad,
            double avg, double dev) {
        this.min = min;
        this.max = max;
        this.med = med;
        this.mad = mad;
        this.avg = avg;
        this.dev = dev;
    }

    @Override
    public String toString() {
        return String.format("med=%d+-%d, avg=%.1f+-%.1f", med, mad, avg, dev);
    }

    /**
     * Create from an array of measurements.
     * 
     * @param ments non-empty, non-{@code null} array of measurements
     * that WILL BE MODIFIED (for speed) during the call
     * @return a non-{@code null} instance
     */
    public static Timing from(long[] ments) {
        assert ments.length >= 1;
        
        // Arithmetic mean
        double avg = 0.0;
        for (long value : ments) {
            avg += value;
        }
        avg /= ments.length;
        
        // Standard deviation
        double dev = 0.0;
        for (long value : ments) {
            double delta = value - avg;
            dev += delta * delta;
        }
        dev = Math.sqrt(dev) / ments.length;
        
        // Median (sorts the array)
        Arrays.sort(ments);
        long min = ments[0];
        long max = ments[ments.length - 1];
        long med = ments[ments.length / 2];
        
        // Median of absolute deviations (destructs the array)
        for (int i = 0; i < ments.length; i++) {
            ments[i] -= med;
            if (ments[i] < 0) {
                ments[i] = -ments[i];
            }
        }
        
        Arrays.sort(ments);
        long mad = ments[ments.length / 2];
        
        return new Timing(min, max, med, mad, avg, dev);
    }
}
