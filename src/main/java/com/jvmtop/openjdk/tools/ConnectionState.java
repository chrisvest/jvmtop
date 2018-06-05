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

/**
 * Values for the <i>ConnectionState</i> bound property.
 */
public enum ConnectionState {
  /**
   * The connection has been successfully established.
   */
  CONNECTED,
  /**
   * No connection present.
   */
  DISCONNECTED,
  /**
   * The connection is being attempted.
   */
  CONNECTING
}
