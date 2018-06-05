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

/**
 *
 * Defines a console view.
 *
 * @author paru
 *
 */
public interface ConsoleView
{
  /**
   * Prints the view to STDOUT.
   *
   * @throws Exception
   */
  public void printView() throws Exception;

  /**
   * Notifies that this view encountered issues
   * and should be called again (e.g. due to exceptions)
   *
   * TODO: remove this method and use proper exception instead.
   *
   * @return
   */
  public boolean shouldExit();

  /**
   * Requests the view to sleep (defined as "not outputting anything").
   * However, the view is allowed to do some work / telemtry retrieval during sleep.
   *
   */
  public void sleep(long millis) throws Exception;
}
