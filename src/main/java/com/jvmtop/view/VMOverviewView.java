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

import java.util.*;
import java.util.Map.Entry;

/**
 * "overview" view, providing the most-important metrics of all accessible jvms in a top-like manner.
 *
 * @author paru
 */
public class VMOverviewView extends AbstractConsoleView {

  private List<VMInfo> vmInfoList = new ArrayList<>();
  private Map<Integer, LocalVirtualMachine> vmMap = new HashMap<>();

  public VMOverviewView(Integer width) {
    super(width);
  }

  public void printView() throws Exception {
    printHeader();

    //to reduce cpu effort, scan only every 5 iterations for new vms
    scanForNewVMs();

    updateVMs(vmInfoList);

    Collections.sort(vmInfoList, VMInfo.CPU_LOAD_COMPARATOR);

    for (VMInfo vmInfo : vmInfoList) {
      if (vmInfo.getState() == VMInfoState.ATTACHED) {
        printVM(vmInfo);
      } else if (vmInfo.getState() == VMInfoState.ATTACHED_UPDATE_ERROR) {
        System.out.printf(
            "%5d %-15.15s [ERROR: Could not fetch telemetries (Process DEAD?)] %n",
            vmInfo.getId(), getEntryPointClass(vmInfo.getDisplayName()));
      } else if (vmInfo.getState() == VMInfoState.ERROR_DURING_ATTACH) {
        System.out.printf("%5d %-15.15s [ERROR: Could not attach to VM] %n",
            vmInfo.getId(), getEntryPointClass(vmInfo.getDisplayName()));
      } else if (vmInfo.getState() == VMInfoState.CONNECTION_REFUSED) {
        System.out.printf(
            "%5d %-15.15s [ERROR: Connection refused/access denied] %n",
            vmInfo.getId(), getEntryPointClass(vmInfo.getDisplayName()));
      }
    }
  }

  /**
   * @param name
   * @return
   */
  private String getEntryPointClass(String name) {
    if (name.indexOf(' ') > 0) {
      name = name.substring(0, name.indexOf(' '));
    }
    return rightStr(name, 15);
  }

  /**
   * @param vmInfo
   * @return
   */
  private void printVM(VMInfo vmInfo) {
    String deadlockState = "";
    if (vmInfo.hasDeadlockThreads()) {
      deadlockState = "!D";
    }

    System.out.printf(
        "%5d %-15.15s %5s %5s %5s %5s %5.2f%% %5.2f%% %-5.5s %8.8s %4d %2.2s%n",
        vmInfo.getId(), getEntryPointClass(vmInfo.getDisplayName()),
        toMB(vmInfo.getHeapUsed()), toMB(vmInfo.getHeapMax()),
        toMB(vmInfo.getNonHeapUsed()), toMB(vmInfo.getNonHeapMax()),
        vmInfo.getCpuLoad() * 100, vmInfo.getGcLoad() * 100,
        vmInfo.getVMVersion(), vmInfo.getOSUser(), vmInfo.getThreadCount(),
        deadlockState);

  }

  /**
   * @param vmList
   */
  private void updateVMs(List<VMInfo> vmList) {
    for (VMInfo vmInfo : vmList) {
      vmInfo.update();
    }
  }

  private void scanForNewVMs() {
    Map<Integer, LocalVirtualMachine> machines = LocalVirtualMachine
        .getNewVirtualMachines(vmMap);
    Set<Entry<Integer, LocalVirtualMachine>> set = machines.entrySet();

    for (Entry<Integer, LocalVirtualMachine> entry : set) {
      LocalVirtualMachine localvm = entry.getValue();
      int vmid = localvm.vmid();

      if (!vmMap.containsKey(vmid)) {
        VMInfo vmInfo = VMInfo.processNewVM(localvm, vmid);
        vmInfoList.add(vmInfo);
      }
    }
    vmMap = machines;
  }

  private void printHeader() {
    System.out.printf("%5s %-15.15s %5s %5s %5s %5s %6s %6s %5s %8s %4s %2s%n",
        "PID", "MAIN-CLASS", "HPCUR", "HPMAX", "NHCUR", "NHMAX", "CPU", "GC",
        "VM", "USERNAME", "#T", "DL");
  }
}
