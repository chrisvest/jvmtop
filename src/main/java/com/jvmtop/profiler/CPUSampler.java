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
package com.jvmtop.profiler;

import com.jvmtop.monitor.VMInfo;

import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.lang.reflect.UndeclaredThrowableException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import static java.lang.Thread.State.RUNNABLE;

/**
 * Experimental and very basic sampling-based CPU-Profiler.
 *
 * It uses package excludes to filter common 3rd party libraries which often
 * distort application problems.
 *
 * @author paru
 */
public class CPUSampler {
  //TODO: these exception list should be expanded to the most common 3rd-party library packages
  private List<String> filter = Arrays.asList(
      "org.eclipse.", "org.apache.", "java.", "sun.", "com.sun.", "javax.",
      "oracle.", "com.trilead.", "org.junit.", "org.mockito.",
      "org.hibernate.", "com.ibm.", "com.caucho.");

  private ThreadMXBean threadMxBean;
  private ConcurrentMap<String, MethodStats> data = new ConcurrentHashMap<>();
  private ConcurrentMap<Long, Long> threadCPUTime = new ConcurrentHashMap<>();
  private AtomicLong totalThreadCPUTime = new AtomicLong();
  private AtomicLong updateCount = new AtomicLong();

  public CPUSampler(VMInfo vmInfo) throws Exception {
    super();
    threadMxBean = vmInfo.getThreadMXBean();
  }

  public List<MethodStats> getTop(int limit) {
    ArrayList<MethodStats> statList = new ArrayList<>(data.values());
    Collections.sort(statList);
    return statList.subList(0, Math.min(limit, statList.size()));
  }

  public long getTotal() {
    return totalThreadCPUTime.get();
  }

  public void update() throws Exception {
    boolean samplesAcquired = false;
    ThreadInfo[] threadInfos = new ThreadInfo[0];
    try {
      threadInfos = threadMxBean.dumpAllThreads(false, false);
    } catch (UndeclaredThrowableException e) {
      Throwable cause = e.getCause();
      if (cause instanceof ConnectException
          || cause instanceof java.rmi.ConnectException) {
        System.out.println("ERROR: Attach connection failure - process terminated?");
        System.exit(1);
      }
    }
    for (ThreadInfo ti : threadInfos) {
      long cpuTime = threadMxBean.getThreadCpuTime(ti.getThreadId());
      Long tCPUTime = threadCPUTime.get(ti.getThreadId());
      if (tCPUTime != null) {
        Long deltaCpuTime = (cpuTime - tCPUTime);

        if (ti.getStackTrace().length > 0 && ti.getThreadState() == RUNNABLE) {
          for (StackTraceElement frame : ti.getStackTrace()) {
            if (isReallySleeping(frame)) {
              break;
            }
            if (isFiltered(frame)) {
              continue;
            }
            String key = frame.getClassName() + "." + frame.getMethodName();
            data.putIfAbsent(key, new MethodStats(frame));
            data.get(key).getHits().addAndGet(deltaCpuTime);
            totalThreadCPUTime.addAndGet(deltaCpuTime);
            samplesAcquired = true;
            break;
          }
        }
      }
      threadCPUTime.put(ti.getThreadId(), cpuTime);
    }
    if (samplesAcquired) {
      updateCount.incrementAndGet();
    }
  }

  public Long getUpdateCount() {
    return updateCount.get();
  }

  private boolean isReallySleeping(StackTraceElement se) {
    return se.getClassName().equals("sun.nio.ch.EPollArrayWrapper") &&
        se.getMethodName().equals("epollWait");
  }

  public boolean isFiltered(StackTraceElement se) {
    for (String filteredPackage : filter) {
      if (se.getClassName().startsWith(filteredPackage)) {
        return true;
      }
    }
    return false;
  }
}

