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
package com.jvmtop.view;

import com.jvmtop.monitor.VMInfo;
import com.jvmtop.monitor.VMInfoState;
import com.jvmtop.openjdk.tools.LocalVirtualMachine;

import java.lang.management.ThreadInfo;
import java.util.*;

/**
 * "detail" view, printing detail metrics of a specific jvm.
 * Also printing the top threads (based on the current CPU usage)
 *
 * @author paru
 */
public class VMDetailView extends AbstractConsoleView {

  private VMInfo vmInfo;
  private boolean sortByTotalCPU = false;
  private int numberOfDisplayedThreads = 10;
  private int threadNameDisplayWidth = 30;
  private boolean displayedThreadLimit = true;

  //TODO: refactor
  private Map<Long, Long> previousThreadCPUMillis = new HashMap<>();

  public VMDetailView(int vmid, Integer width) throws Exception {
    super(width);
    LocalVirtualMachine localVirtualMachine = LocalVirtualMachine
        .getLocalVirtualMachine(vmid);
    vmInfo = VMInfo.processNewVM(localVirtualMachine, vmid);
  }

  public boolean isSortByTotalCPU() {
    return sortByTotalCPU;
  }

  public void setSortByTotalCPU(boolean sortByTotalCPU) {
    this.sortByTotalCPU = sortByTotalCPU;
  }

  @Override
  public void printView() throws Exception {
    vmInfo.update();

    if (vmInfo.getState() == VMInfoState.ATTACHED_UPDATE_ERROR) {
      System.out.println("ERROR: Could not fetch telemetries - Process terminated?");
      exit();
      return;
    }
    if (vmInfo.getState() != VMInfoState.ATTACHED) {
      System.out.println("ERROR: Could not attach to process.");
      exit();
      return;
    }

    Map<String, String> properties = vmInfo.getSystemProperties();

    String command = properties.get("sun.java.command");
    if (command != null) {
      String[] commandArray = command.split(" ");

      List<String> commandList = Arrays.asList(commandArray);
      commandList = commandList.subList(1, commandList.size());

      System.out.printf(" PID %d: %s %n", vmInfo.getId(), commandArray[0]);

      String argJoin = join(commandList, " ");
      if (argJoin.length() > 67) {
        System.out.printf(" ARGS: %s[...]%n", leftStr(argJoin, 67));
      } else {
        System.out.printf(" ARGS: %s%n", argJoin);
      }
    } else {
      System.out.printf(" PID %d: %n", vmInfo.getId());
      System.out.printf(" ARGS: [UNKNOWN] %n");
    }

    String join = join(vmInfo.getRuntimeMXBean().getInputArguments(), " ");
    if (join.length() > 65) {
      System.out.printf(" VMARGS: %s[...]%n", leftStr(join, 65));
    } else {
      System.out.printf(" VMARGS: %s%n", join);
    }

    System.out.printf(" VM: %s %s %s%n", properties.get("java.vendor"),
        properties.get("java.vm.name"), properties.get("java.version"));
    System.out.printf(
        " UP: %-7s #THR: %-4d #THRPEAK: %-4d #THRCREATED: %-4d USER: %-12s%n",
        toHHMM(vmInfo.getRuntimeMXBean().getUptime()), vmInfo
            .getThreadCount(), vmInfo.getThreadMXBean().getPeakThreadCount(),
        vmInfo.getThreadMXBean().getTotalStartedThreadCount(), vmInfo
            .getOSUser());

    System.out.printf(
        " GC-Time: %-7s  #GC-Runs: %-8d  #TotalLoadedClasses: %-8d%n",
        toHHMM(vmInfo.getGcTime()), vmInfo.getGcCount(),
        vmInfo.getTotalLoadedClassCount());

    System.out.printf(
        " CPU: %5.2f%% GC: %5.2f%% HEAP:%5s /%5s NONHEAP:%5s /%5s%n",
        vmInfo.getCpuLoad() * 100, vmInfo.getGcLoad() * 100,
        toMB(vmInfo.getHeapUsed()), toMB(vmInfo.getHeapMax()),
        toMB(vmInfo.getNonHeapUsed()), toMB(vmInfo.getNonHeapMax()));

    System.out.println();

    printTopThreads();
  }

  /**
   * @throws Exception
   */
  private void printTopThreads() throws Exception {
    System.out.printf(" %6s %-" + threadNameDisplayWidth
            + "s  %13s %8s    %8s %5s %n", "TID", "NAME", "STATE", "CPU",
        "TOTALCPU", "BLOCKEDBY");

    if (vmInfo.getThreadMXBean().isThreadCpuTimeSupported()) {

      //TODO: move this into VMInfo?
      Map<Long, Long> newThreadCPUMillis = new HashMap<>();

      Map<Long, Long> cpuTimeMap = new TreeMap<>();

      for (Long tid : vmInfo.getThreadMXBean().getAllThreadIds()) {
        long threadCpuTime = vmInfo.getThreadMXBean().getThreadCpuTime(tid);
        long deltaThreadCpuTime = 0;
        if (previousThreadCPUMillis.containsKey(tid)) {
          deltaThreadCpuTime = threadCpuTime - previousThreadCPUMillis.get(tid);

          cpuTimeMap.put(tid, deltaThreadCpuTime);
        }
        newThreadCPUMillis.put(tid, threadCpuTime);
      }

      cpuTimeMap = sortByValue(cpuTimeMap, true);

      int displayedThreads = 0;
      for (Long tid : cpuTimeMap.keySet()) {
        ThreadInfo info = vmInfo.getThreadMXBean().getThreadInfo(tid);
        displayedThreads++;
        if (displayedThreads > numberOfDisplayedThreads
            && displayedThreadLimit) {
          break;
        }
        if (info != null) {
          System.out.printf(
              " %6d %-" + threadNameDisplayWidth
                  + "s  %13s %5.2f%%    %5.2f%% %5s %n",
              tid,
              leftStr(info.getThreadName(), threadNameDisplayWidth),
              info.getThreadState(),
              getThreadCPUUtilization(cpuTimeMap.get(tid),
                  vmInfo.getDeltaUptime()),
              getThreadCPUUtilization(vmInfo.getThreadMXBean()
                  .getThreadCpuTime(tid), vmInfo.getProxyClient()
                  .getProcessCpuTime(), 1), getBlockedThread(info));
        }
      }
      if (newThreadCPUMillis.size() >= numberOfDisplayedThreads
          && displayedThreadLimit) {

        System.out.printf(
            " Note: Only top %d threads (according cpu load) are shown!",
            numberOfDisplayedThreads);
      }
      previousThreadCPUMillis = newThreadCPUMillis;
    } else {

      System.out
          .printf("%n -Thread CPU telemetries are not available on the monitored jvm/platform-%n");
    }
  }

  private String getBlockedThread(ThreadInfo info) {
    if (info.getLockOwnerId() >= 0) {
      return "" + info.getLockOwnerId();
    } else {
      return "";
    }
  }

  public int getNumberOfDisplayedThreads() {
    return numberOfDisplayedThreads;
  }

  public void setNumberOfDisplayedThreads(int numberOfDisplayedThreads) {
    this.numberOfDisplayedThreads = numberOfDisplayedThreads;
  }

  public boolean isDisplayedThreadLimit() {
    return displayedThreadLimit;
  }

  public void setDisplayedThreadLimit(boolean displayedThreadLimit) {
    this.displayedThreadLimit = displayedThreadLimit;
  }

  public int getThreadNameDisplayWidth() {
    return threadNameDisplayWidth;
  }

  public void setThreadNameDisplayWidth(int threadNameDisplayWidth_) {
    this.threadNameDisplayWidth = threadNameDisplayWidth_;
  }

  private double getThreadCPUUtilization(long deltaThreadCpuTime, long totalTime) {
    return getThreadCPUUtilization(deltaThreadCpuTime, totalTime, 1000 * 1000);
  }

  private double getThreadCPUUtilization(long deltaThreadCpuTime,
                                         long totalTime, double factor) {
    if (totalTime == 0) {
      return 0;
    }
    return deltaThreadCpuTime / factor / totalTime * 100d;
  }
}
