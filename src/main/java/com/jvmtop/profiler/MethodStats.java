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
 *
 */
public class MethodStats implements Comparable<MethodStats>
{
  private AtomicLong hits_       = new AtomicLong(0);

  private String        className_  = null;

  private String        methodName_ = null;

  /**
   * @param className
   * @param methodName
   */
  public MethodStats(String className, String methodName)
  {
    super();
    className_ = className;
    methodName_ = methodName;
  }


  @Override
  public int hashCode()
  {
    final int prime = 31;
    int result = 1;
    result = prime * result
        + ((className_ == null) ? 0 : className_.hashCode());
    result = prime * result
        + ((methodName_ == null) ? 0 : methodName_.hashCode());
    return result;
  }



  @Override
  public boolean equals(Object obj)
  {
    if (this == obj)
    {
      return true;
    }
    if (obj == null)
    {
      return false;
    }
    if (getClass() != obj.getClass())
    {
      return false;
    }
    MethodStats other = (MethodStats) obj;
    if (className_ == null)
    {
      if (other.className_ != null)
      {
        return false;
      }
    }
    else if (!className_.equals(other.className_))
    {
      return false;
    }
    if (methodName_ == null)
    {
      if (other.methodName_ != null)
      {
        return false;
      }
    }
    else if (!methodName_.equals(other.methodName_))
    {
      return false;
    }
    return true;
  }



  @Override
  /**
   * Compares a MethodStats object by its hits
   */
  public int compareTo(MethodStats o)
  {
    return Long.valueOf(o.hits_.get()).compareTo(hits_.get());
  }

  public AtomicLong getHits()
  {
    return hits_;
  }

  public String getClassName()
  {
    return className_;
  }

  public String getMethodName()
  {
    return methodName_;
  }



}