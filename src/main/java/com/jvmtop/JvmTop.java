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
package com.jvmtop;

import com.jvmtop.view.ConsoleView;
import com.jvmtop.view.VMDetailView;
import com.jvmtop.view.VMOverviewView;
import com.jvmtop.view.VMProfileView;
import joptsimple.OptionParser;
import joptsimple.OptionSet;

import java.io.BufferedOutputStream;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JvmTop entry point class.
 *
 * - parses program arguments
 * - selects console view
 * - prints header
 * - main "iteration loop"
 *
 * TODO: refactor to split these tasks
 *
 * @author paru
 */
public class JvmTop {

  public static final String VERSION = "0.8.0 alpha";
  private final static String CLEAR_TERMINAL_ANSI_CMD = new String(
      new byte[]{
          (byte) 0x1b, (byte) 0x5b, (byte) 0x32, (byte) 0x4a, (byte) 0x1b,
          (byte) 0x5b, (byte) 0x48});
  private static Logger logger;

  private Double delay = 1.0;
  private Boolean supportsSystemAverage;
  private java.lang.management.OperatingSystemMXBean localOSBean;
  private int maxIterations = -1;

  private static OptionParser createOptionParser() {
    OptionParser parser = new OptionParser();
    parser.acceptsAll(Arrays.asList("help", "?", "h"), "shows this help").forHelp();
    parser.accepts("once",
        "jvmtop will exit after first output iteration [deprecated, use -n 1 instead]");
    parser.acceptsAll(Arrays.asList("n", "iteration"),
        "jvmtop will exit after n output iterations").withRequiredArg()
        .ofType(Integer.class);
    parser.acceptsAll(Arrays.asList("d", "delay"),
        "delay between each output iteration").withRequiredArg()
        .ofType(Double.class);
    parser.accepts("profile", "start CPU profiling at the specified jvm");
    parser.accepts("sysinfo", "outputs diagnostic information");
    parser.accepts("verbose", "verbose mode");
    parser.accepts("threadlimit",
        "sets the number of displayed threads in detail mode")
        .withRequiredArg().ofType(Integer.class);
    parser.accepts("disable-threadlimit", "displays all threads in detail mode");
    parser.acceptsAll(Arrays.asList("p", "pid"),
        "PID to connect to").withRequiredArg().ofType(Integer.class);
    parser.acceptsAll(Arrays.asList("w", "width"),
        "Width in columns for the console display").withRequiredArg().ofType(Integer.class);
    parser.accepts("threadnamewidth",
        "sets displayed thread name length in detail mode (defaults to 30)")
        .withRequiredArg().ofType(Integer.class);

    return parser;
  }

  public static void main(String[] args) throws Exception {
    Locale.setDefault(Locale.US);

    logger = Logger.getLogger("jvmtop");

    OptionParser parser = createOptionParser();
    OptionSet a = parser.parse(args);

    if (a.has("help")) {
      System.out.println("jvmtop - java monitoring for the command-line");
      System.out.println("Usage: jvmtop.sh [options...] [PID]");
      System.out.println("");
      parser.printHelpOn(System.out);
      System.exit(0);
    }
    boolean sysInfoOption = a.has("sysinfo");
    Integer pid = null;
    Integer width = null;
    double delay = 1.0;
    boolean profileMode = a.has("profile");
    Integer iterations = a.has("once") ? 1 : -1;
    Integer threadlimit = null;
    boolean threadLimitEnabled = true;
    Integer threadNameWidth = null;

    if (a.hasArgument("delay")) {
      delay = (Double) (a.valueOf("delay"));
      if (delay < 0.1d) {
        throw new IllegalArgumentException("Delay cannot be set below 0.1");
      }
    }

    if (a.hasArgument("n")) {
      iterations = (Integer) a.valueOf("n");
    }

    //to support PID as non option argument
    if (a.nonOptionArguments().size() > 0) {
      pid = Integer.valueOf((String) a.nonOptionArguments().get(0));
    }

    if (a.hasArgument("pid")) {
      pid = (Integer) a.valueOf("pid");
    }

    if (a.hasArgument("width")) {
      width = (Integer) a.valueOf("width");
    }

    if (a.hasArgument("threadlimit")) {
      threadlimit = (Integer) a.valueOf("threadlimit");
    }

    if (a.has("disable-threadlimit")) {
      threadLimitEnabled = false;
    }

    if (a.has("verbose")) {
      fineLogging();
      logger.setLevel(Level.ALL);
      logger.fine("Verbosity mode.");
    }

    if (a.hasArgument("threadnamewidth")) {
      threadNameWidth = (Integer) a.valueOf("threadnamewidth");
    }

    if (sysInfoOption) {
      outputSystemProps();
    } else {
      JvmTop jvmTop = new JvmTop();
      jvmTop.setDelay(delay);
      jvmTop.setMaxIterations(iterations);
      if (pid == null) {
        jvmTop.run(new VMOverviewView(width));
      } else {
        if (profileMode) {
          jvmTop.run(new VMProfileView(pid, width));
        } else {
          VMDetailView vmDetailView = new VMDetailView(pid, width);
          vmDetailView.setDisplayedThreadLimit(threadLimitEnabled);
          if (threadlimit != null) {
            vmDetailView.setNumberOfDisplayedThreads(threadlimit);
          }
          if (threadNameWidth != null) {
            vmDetailView.setThreadNameDisplayWidth(threadNameWidth);
          }
          jvmTop.run(vmDetailView);

        }

      }
    }
  }

  public int getMaxIterations() {
    return maxIterations;
  }

  public void setMaxIterations(int iterations) {
    maxIterations = iterations;
  }

  private static void fineLogging() {
    //get the top Logger:
    Logger topLogger = java.util.logging.Logger.getLogger("");

    // Handler for console (reuse it if it already exists)
    Handler consoleHandler = null;
    //see if there is already a console handler
    for (Handler handler : topLogger.getHandlers()) {
      if (handler instanceof ConsoleHandler) {
        //found the console handler
        consoleHandler = handler;
        break;
      }
    }

    if (consoleHandler == null) {
      //there was no console handler found, create a new one
      consoleHandler = new ConsoleHandler();
      topLogger.addHandler(consoleHandler);
    }
    //set the console handler to fine:
    consoleHandler.setLevel(java.util.logging.Level.FINEST);
  }

  private static void outputSystemProps() {
    for (Object key : System.getProperties().keySet()) {
      System.out.println(key + "=" + System.getProperty(key + ""));
    }
  }

  protected void run(ConsoleView view) throws Exception {
    try {
      System.setOut(new PrintStream(new BufferedOutputStream(
          new FileOutputStream(FileDescriptor.out)), false));
      int iterations = 0;
      while (!view.shouldExit()) {
        if (maxIterations > 1 || maxIterations == -1) {
          clearTerminal();
        }
        printTopBar();
        view.printView();
        System.out.flush();
        iterations++;
        if (iterations >= maxIterations && maxIterations > 0) {
          break;
        }
        view.sleep((int) (delay * 1000));
      }
    } catch (NoClassDefFoundError e) {
      e.printStackTrace(System.err);

      System.err.println("");
      System.err.println("ERROR: Some JDK classes cannot be found.");
      System.err.println("       Please check if the JAVA_HOME environment variable has been set to a JDK path.");
      System.err.println("");
    }
  }

  /**
   *
   */
  private void clearTerminal() {
    if (System.getProperty("os.name").contains("Windows")) {
      //hack
      System.out.printf("%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n%n");
    } else if (System.getProperty("jvmtop.altClear") != null) {
      System.out.print('\f');
    } else {
      System.out.print(CLEAR_TERMINAL_ANSI_CMD);
    }
  }

  public JvmTop() {
    localOSBean = ManagementFactory.getOperatingSystemMXBean();
  }

  /**
   * @throws NoSuchMethodException
   * @throws SecurityException
   */
  private void printTopBar() {
    Date now = new Date();
    String arch = localOSBean.getArch();
    int availCpus = localOSBean.getAvailableProcessors();
    String osString = localOSBean.getName() + " " + localOSBean.getVersion();
    System.out.printf(" JvmTop %s - %8tT, %6s, %2d cpus, %15.15s",
        VERSION, now, arch, availCpus, osString);

    if (supportSystemLoadAverage() && localOSBean.getSystemLoadAverage() != -1) {
      System.out.printf(", load avg %3.2f%n",
          localOSBean.getSystemLoadAverage());
    } else {
      System.out.println();
    }
    System.out.println(" https://github.com/chrisvest/jvmtop");
    System.out.println();
  }

  private boolean supportSystemLoadAverage() {
    if (supportsSystemAverage == null) {
      try {
        Method method = localOSBean.getClass().getMethod("getSystemLoadAverage");
        supportsSystemAverage = (method != null);
      } catch (Throwable e) {
        supportsSystemAverage = false;
      }
    }
    return supportsSystemAverage;
  }

  public Double getDelay() {
    return delay;
  }

  public void setDelay(Double delay) {
    this.delay = delay;
  }
}
