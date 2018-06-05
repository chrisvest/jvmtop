/*
 * jvmtop - java monitoring for the command-line
 * Copyright © 2013 Patric Rufflar
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
/*
 * This file has been modified by jvmtop project authors
 */
package com.jvmtop.openjdk.tools;

import java.lang.management.MemoryUsage;

public class MemoryPoolStat {
    private String      poolName;
    private long        usageThreshold;
    private MemoryUsage usage;
    private long        lastGcId;
    private long        lastGcStartTime;
    private long        lastGcEndTime;
    private long        collectThreshold;
    private MemoryUsage beforeGcUsage;
    private MemoryUsage afterGcUsage;

    MemoryPoolStat(String name,
                   long usageThreshold,
                   MemoryUsage usage,
                   long lastGcId,
                   long lastGcStartTime,
                   long lastGcEndTime,
                   long collectThreshold,
                   MemoryUsage beforeGcUsage,
                   MemoryUsage afterGcUsage) {
        this.poolName = name;
        this.usageThreshold = usageThreshold;
        this.usage = usage;
        this.lastGcId = lastGcId;
        this.lastGcStartTime = lastGcStartTime;
        this.lastGcEndTime = lastGcEndTime;
        this.collectThreshold = collectThreshold;
        this.beforeGcUsage = beforeGcUsage;
        this.afterGcUsage = afterGcUsage;
    }

    /**
     * Returns the memory pool name.
     */
    public String getPoolName() {
        return poolName;
    }

    /**
     * Returns the current memory usage.
     */
    public MemoryUsage getUsage() {
        return usage;
    }

    /**
     * Returns the current usage threshold.
     * -1 if not supported.
     */
    public long getUsageThreshold() {
        return usageThreshold;
    }

    /**
     * Returns the current collection usage threshold.
     * -1 if not supported.
     */
    public long getCollectionUsageThreshold() {
        return collectThreshold;
    }

    /**
     * Returns the Id of GC.
     */
    public long getLastGcId() {
        return lastGcId;
    }


    /**
     * Returns the start time of the most recent GC on
     * the memory pool for this statistics in milliseconds.
     *
     * Return 0 if no GC occurs.
     */
    public long getLastGcStartTime() {
        return lastGcStartTime;
    }

    /**
     * Returns the end time of the most recent GC on
     * the memory pool for this statistics in milliseconds.
     *
     * Return 0 if no GC occurs.
     */
    public long getLastGcEndTime() {
        return lastGcEndTime;
    }

    /**
     * Returns the memory usage before the most recent GC started.
     * null if no GC occurs.
     */
    public MemoryUsage getBeforeGcUsage() {
        return beforeGcUsage;
    }

    /**
     * Returns the memory usage after the most recent GC finished.
     * null if no GC occurs.
     */
    public MemoryUsage getAfterGcUsage() {
        return beforeGcUsage;
    }
}
