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

import java.util.concurrent.atomic.AtomicLong;

/**
 * Stores method invocations in a thread-safe manner.
 *
 * @author paru
 */
public class MethodStats implements Comparable<MethodStats> {
  private AtomicLong hits = new AtomicLong();
  private String className;
  private String methodName;

  public MethodStats(StackTraceElement frame) {
    className = frame.getClassName();
    methodName = frame.getMethodName();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((className == null) ? 0 : className.hashCode());
    result = prime * result + ((methodName == null) ? 0 : methodName.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    MethodStats other = (MethodStats) obj;
    if (className == null) {
      if (other.className != null) {
        return false;
      }
    } else if (!className.equals(other.className)) {
      return false;
    }
    if (methodName == null) {
      return other.methodName == null;
    }
    return methodName.equals(other.methodName);
  }

  /**
   * Compares a MethodStats object by its hits
   */
  @Override
  public int compareTo(MethodStats o) {
    return Long.compare(o.hits.get(), hits.get());
  }

  public AtomicLong getHits() {
    return hits;
  }

  public String getClassName() {
    return className;
  }

  public String getMethodName() {
    return methodName;
  }
}
