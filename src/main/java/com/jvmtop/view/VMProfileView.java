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
import com.jvmtop.profiler.CPUSampler;
import com.jvmtop.profiler.MethodStats;

/**
 * CPU sampling-based profiler view which shows methods with top CPU usage.
 *
 * @author paru
 */
public class VMProfileView extends AbstractConsoleView {

  private CPUSampler cpuSampler;

  private VMInfo vmInfo;

  public VMProfileView(int vmid, Integer width) throws Exception {
    super(width);
    LocalVirtualMachine localVirtualMachine =
        LocalVirtualMachine.getLocalVirtualMachine(vmid);
    vmInfo = VMInfo.processNewVM(localVirtualMachine, vmid);
    cpuSampler = new CPUSampler(vmInfo);
  }

  @Override
  public void sleep(long millis) throws Exception {
    long cur = System.currentTimeMillis();
    cpuSampler.update();
    while (cur + millis > System.currentTimeMillis()) {
      cpuSampler.update();
      super.sleep(100);
    }
  }

  @Override
  public void printView() throws Exception {
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

    int w = width - 40;
    System.out.printf(" Profiling PID %d: %40s %n%n",
        vmInfo.getId(), leftStr(vmInfo.getDisplayName(), w));

    for (MethodStats stats : cpuSampler.getTop(20)) {
      double wallRatio = (double) stats.getHits().get() / cpuSampler.getTotal() * 100;
      if (!Double.isNaN(wallRatio)) {
        double sampleTime = wallRatio / 100d * cpuSampler.getUpdateCount() * 0.1d;
        String qualifiedMethodName = stats.getClassName() + "." + stats.getMethodName();
        System.out.printf(" %6.2f%% (%9.2fs) %s()%n",
            wallRatio, sampleTime, qualifiedMethodName);
      }
    }
  }
}
