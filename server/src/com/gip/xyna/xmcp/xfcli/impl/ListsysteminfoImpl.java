/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 */
package com.gip.xyna.xmcp.xfcli.impl;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryUsage;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfmon.systeminfo.CPUInfo;
import com.gip.xyna.xfmg.xfmon.systeminfo.MEMInfo;
import com.gip.xyna.xfmg.xfmon.systeminfo.StaticSystemInformation;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Listsysteminfo;



public class ListsysteminfoImpl extends XynaCommandImplementation<Listsysteminfo> {

  public void execute(OutputStream statusOutputStream, Listsysteminfo payload) throws XynaException {
    String date = Constants.defaultUTCSimpleDateFormat().format(new Date(System.currentTimeMillis()));
    writeLineToCommandLine(statusOutputStream, "System time: " + date);

    String osName = System.getProperty("os.name");
    String osArchitecture = System.getProperty("os.arch");
    String osVersion = System.getProperty("os.version");
    String javaVersion = System.getProperty("java.version");
    String javaVendor = System.getProperty("java.vendor");
    String javaHome = System.getProperty("java.home");

    String machineInfo =
        "Operating System: " + osName + " (Version " + osVersion + ", architecture " + osArchitecture + ")\n";
    machineInfo += "Java Vendor: " + javaVendor + " (Version " + javaVersion + ", java home " + javaHome + ")\n";

    StaticSystemInformation uptodateSystemInformation = StaticSystemInformation.getInstance(true);
    ArrayList<CPUInfo> cpuInfo = uptodateSystemInformation.getCpuInfo();
    MEMInfo memInfo = uptodateSystemInformation.getMemoryInfo();

    if (cpuInfo != null && cpuInfo.size() > 0) {
      machineInfo += "\nCPU Info: \n";
      for (CPUInfo info : cpuInfo) {
        machineInfo += " * " + info.getVendorName() + " " + info.getModelname() + " @ " + info.getCpuMhz() + "\n";
      }
    } else {
      machineInfo += "\nNo parsed CPU information is available for OS '" + osName + "'\n";
    }

    boolean printedHeader = false;

    NumberFormat format = NumberFormat.getNumberInstance();
    if (memInfo != null) {
      if (payload.getReadable()) {
        try {
          long bytesMemoryFree = getNoOfBytesFromKbString(memInfo.getFreeMem());
          long bytesMemoryMax = getNoOfBytesFromKbString(memInfo.getMaxMem());
          long bytesSwapFree = getNoOfBytesFromKbString(memInfo.getFreeSwap());
          long byteSwapMax = getNoOfBytesFromKbString(memInfo.getMaxSwap());

          machineInfo += "\n" + getHeaderLine() + "\n";
          printedHeader = true;
          machineInfo += getLineMemoryOrSwap(format, bytesMemoryFree, bytesMemoryMax, "System Memory") + "\n";
          machineInfo += getLineMemoryOrSwap(format, bytesSwapFree, byteSwapMax, "System Swap") + "\n";
        } catch (RuntimeException e) {
          machineInfo += "\nSystem Memory Info: \n";
          machineInfo += memInfo.getFreeMem() + " free memory / " + memInfo.getMaxMem() + " total memory\n";
          machineInfo += memInfo.getFreeSwap() + " free swap / " + memInfo.getMaxSwap() + " total swap\n";
        }
      } else {
        machineInfo += "\nSystem Memory Info: \n";
        machineInfo += memInfo.getFreeMem() + " free memory / " + memInfo.getMaxMem() + " total memory\n";
        machineInfo += memInfo.getFreeSwap() + " free swap / " + memInfo.getMaxSwap() + " total swap\n";
      }
    } else {
      machineInfo += "\nNo parsed memory information is available for OS '" + osName + "'\n";
    }

    if (!payload.getReadable() || !printedHeader) {
      machineInfo += "\n";
      machineInfo += "JVM Memory Info:\n";
    }

    MemoryMXBean bean = ManagementFactory.getMemoryMXBean();

    if (payload.getReadable() && !printedHeader) {
      machineInfo += getHeaderLine() + "\n";
    }

    machineInfo += printMemInfo(bean.getHeapMemoryUsage(), "Heap", null, payload.getReadable(), format);
    machineInfo += printMemInfo(bean.getNonHeapMemoryUsage(), "Non-Heap", null, payload.getReadable(), format);

    if (!payload.getReadable()) {
      machineInfo += "\n   All known memory pools:";
    }
    List<MemoryPoolMXBean> poolBeans = ManagementFactory.getMemoryPoolMXBeans();
    for (MemoryPoolMXBean poolBean : poolBeans) {
      machineInfo +=
          printMemInfo(poolBean.getUsage(), poolBean.getName(), poolBean.getType().toString(), payload.getReadable(),
                       format);
      /*
       * http://docs.oracle.com/javase/6/docs/technotes/guides/management/jconsole.html
       * The Details area shows several current memory metrics:

      Used: the amount of memory currently used, including the memory occupied by all objects, both reachable and unreachable.

      Committed: the amount of memory guaranteed to be available for use by the Java VM.
      The amount of committed memory may change over time. The Java virtual machine may release
      memory to the system and the amount of committed memory could be less than the amount 
      of memory initially allocated at start up. The amount of committed memory will always be greater than or equal to the amount of used memory.

      Max: the maximum amount of memory that can be used for memory management.
      Its value may change or be undefined. A memory allocation may fail if the Java VM attempts 
      to increase the used memory to be greater than committed memory, even if the amount used is less
      than or equal to max (for example, when the system is low on virtual memory).

      GC time: the cumulative time spent on garbage collection and the total number of invocations.
      It may have multiple rows, each of which represents one garbage collector algorithm used in the Java VM.

       */
    }
    machineInfo += "\n   Number of pending object finalizations: " + bean.getObjectPendingFinalizationCount();

    machineInfo += "\n\n";
    machineInfo += printStackSize();
    machineInfo += getThreadCount() + " active threads\n";
    machineInfo += "\n";

    if (!payload.getReadable()) {
      // for the readable version, this information is already present from the above table
      long freeMem = Runtime.getRuntime().freeMemory();
      long totalMem = Runtime.getRuntime().totalMemory();
      machineInfo += format.format(freeMem / 1024) + " kB free within current heap\n";
      machineInfo += format.format((totalMem - freeMem) / 1024) + " kB actively occupied by objects within heap\n";
      machineInfo += format.format(totalMem / 1024) + " kB current total heap size\n";
      machineInfo += format.format(Runtime.getRuntime().maxMemory() / 1024) + " kB max heap size\n\n";
    }

    machineInfo += printFileDescriptors();
    
    writeLineToCommandLine(statusOutputStream, machineInfo);
  }
  
  private static long getNoOfBytesFromKbString(String kbString) {
    String kbStringWithoutSuffix = kbString.split(" ")[0].trim();
    return Long.valueOf(kbStringWithoutSuffix);
  }


  private static String getLineMemoryOrSwap(NumberFormat format, long freeBytes, long maxBytes, String name) {
    String usedMemFormatted = format.format(maxBytes - freeBytes);
    String maxMemFormatted = format.format(maxBytes);
    String usedMemPercentage =
        String.valueOf(Math.round(((double) (maxBytes - freeBytes) / ((double) maxBytes) * 100d)));
    return String.format(getLineFormat(), name, usedMemFormatted, "-", maxMemFormatted, usedMemPercentage);
  }

  private int getThreadCount() {
    ThreadGroup tg = Thread.currentThread().getThreadGroup();
    while (tg.getParent() != null) {
      if (logger.isDebugEnabled()) {
        logger.debug(tg.activeCount() + " active threads in thread group " + tg + ".");
      }
      tg = tg.getParent();
    }
    if (logger.isDebugEnabled()) {
      logger.debug(tg.activeCount() + " active threads in root thread group (" + tg + ")");
    }
    return tg.activeCount();
  }


  private String printFileDescriptors() {
    int[] openFiles = getOpenFiles();
    
    if (openFiles[0] < 0) {
      return "unknown open file descriptors\n";
    }
    return openFiles[0] + " / " + openFiles[1] + " open file descriptors\n";
  }
  
  private static int[] getOpenFiles() {
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    String openFiles;
    String maxOpenFiles;
    try {
      openFiles = String.valueOf(server.getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "OpenFileDescriptorCount"));
      maxOpenFiles = String.valueOf(server.getAttribute(new ObjectName("java.lang:type=OperatingSystem"), "MaxFileDescriptorCount"));
    } catch (AttributeNotFoundException e) {
      openFiles = "-1";
      maxOpenFiles = "-1";
    } catch (InstanceNotFoundException e) {
      throw new RuntimeException(e);
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException(e);
    } catch (MBeanException e) {
      throw new RuntimeException(e);
    } catch (ReflectionException e) {
      throw new RuntimeException(e);
    }

    return new int[]{Integer.valueOf(openFiles), Integer.valueOf(maxOpenFiles)};
  }


  /**
   * wieviel open files sind noch frei = maxfiledescriptorcount - openfiledescriptorcount
   * -1, falls unbekannt
   */
  public static int getNumberOfPossibleOpenFiles() {
    try {
      int[] openFiles = getOpenFiles();
      if (openFiles[0] < 0) {
        return -1;
      }
      return openFiles[1] - openFiles[0];
    } catch (RuntimeException e) {
      logger.warn(null, e);
      return -1;
    }
  }


  private static final Logger logger = CentralFactoryLogging.getLogger(ListsysteminfoImpl.class);


  private static String printStackSize() {
    //achtung: stacksize kann pro thread anders konfiguriert sein/werden    
    MBeanServer server = ManagementFactory.getPlatformMBeanServer();
    CompositeData result;
    try {
      result =
          (CompositeData) server.invoke(new ObjectName("com.sun.management:type=HotSpotDiagnostic"), "getVMOption",
                                        new Object[] {"ThreadStackSize"}, new String[] {String.class.getName()});
      return "StackSize: " + result.get("value") + " kB\n";
    } catch (InstanceNotFoundException e) {
      //in java5 kann die stacksize so nicht ermittelt werden.
      logger.debug("stacksize can not be found");
      if (logger.isTraceEnabled()) {
        logger.trace(null, e);
      }
      return "StackSize: unknown\n";
    } catch (MalformedObjectNameException e) {
      throw new RuntimeException(e);
    } catch (ReflectionException e) {
      throw new RuntimeException(e);
    } catch (MBeanException e) {
      throw new RuntimeException(e);
    }
/*    try {
      Set<ObjectName> queryNames = server.queryNames(new ObjectName("com.sun.management:*"), null);
      for (ObjectName on : queryNames) {
        MBeanInfo mBeanInfo = server.getMBeanInfo(on);
        MBeanOperationInfo[] operations = mBeanInfo.getOperations();
        for (MBeanOperationInfo moi : operations) {
          if (moi.getName().equals("getVMOption")) {            //moi.getSignature()[0].getType()
            Logger.getLogger(getClass()).debug(on);
         
          }
        }
      }*/
  }


  private static String printMemInfo(MemoryUsage usage, String name, String type, boolean asPrettyTable,
                                     NumberFormat numberFormat) {
    //Beachte: summe über used/committed der pools ergibt used/committed gesamt
    //         für max/init ist dies nicht so!
    if (asPrettyTable) {
      String usageKb = numberFormat.format(Long.valueOf(usage.getUsed() / 1024));
      String committedKb = numberFormat.format(Long.valueOf(usage.getCommitted() / 1024));
      String maxKb = numberFormat.format(Long.valueOf(usage.getMax() / 1024));
      String usagePercent = String.valueOf(Math.round(((double) usage.getUsed()) / ((double) usage.getMax()) * 100d));
      return String.format(getLineFormat(), name + (type != null ? (" (" + type + ")") : ""), usageKb, committedKb, maxKb,
                           usagePercent) + "\n";
    } else {
      return String.format("\n%20s kB used, max=%10d kB for %s", usage.getUsed() / 1024 + "/" + usage.getCommitted()
          / 1024, usage.getMax() / 1024, name + " type=" + type);
    }
  }


  public static long getRemainingPermGenSpace() {
    List<MemoryPoolMXBean> poolBeans = ManagementFactory.getMemoryPoolMXBeans();
    for (MemoryPoolMXBean m : poolBeans) {
      if (m.getName().contains("Perm Gen")) {
        return m.getUsage().getMax() - m.getUsage().getUsed();
      }
    }
    return 0;
  }
  
  
  public static long getRemainingClassMetaDataSpace() {
    List<MemoryPoolMXBean> poolBeans = ManagementFactory.getMemoryPoolMXBeans();
    for (MemoryPoolMXBean m : poolBeans) {
      if (m.getName().contains("Metaspace")) {
        if (m.getUsage().getMax() < 0) {
          StaticSystemInformation uptodateSystemInformation = StaticSystemInformation.getInstance(true);
          MEMInfo memInfo = uptodateSystemInformation.getMemoryInfo();
          long bytesMemoryFree = getNoOfBytesFromKbString(memInfo.getFreeMem()) * 1024;
          return bytesMemoryFree + (m.getUsage().getCommitted() - m.getUsage().getUsed());
        } else {
          return m.getUsage().getMax() - m.getUsage().getUsed();
        }
      }
      if (m.getName().contains("Perm Gen")) {
        return m.getUsage().getMax() - m.getUsage().getUsed();
      }
    }
    return 0;
  }
  
  
  
  public static int getJavaVersion() {
    String javaVersion = System.getProperty("java.version");
    String[] versionParts = javaVersion.split("\\.");
    if (versionParts.length > 1) {
      int first = Integer.parseInt(versionParts[0]);
      if (first == 1) {
        return Integer.parseInt(versionParts[1]); 
      }
      else return first;
    } else {
      return Integer.parseInt(versionParts[0]);
    }
  }
  
  
  private static String getHeaderFormat() {
    return "  %-35s  %20s %20s %15s %27s";
  }


  private static String getLineFormat() {
    return "  %-35s %20s %20s %15s %27s";
  }


  private static String getHeaderLine() {
    return String.format(getHeaderFormat(), "Name", "Current usage (kB)", "Current limit (kB)", "Maximum (kB)",
                         "Current Usage (% of Max.)");
  }
  
  public static void dumpCurrentToLog() {
    try {
      System.gc();
      Thread.sleep(100);
      System.gc();
      Listsysteminfo payload = new Listsysteminfo();
      ByteArrayOutputStream os = new ByteArrayOutputStream();
      new ListsysteminfoImpl().execute(os, payload);
      String s = new String(os.toByteArray());
      logger.debug(s);
    } catch (Exception e) {
      logger.debug(null, e);
    }
  }

}
