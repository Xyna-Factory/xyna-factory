/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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

package com.gip.xyna.xdev.benchmarking;



import java.util.ArrayList;
import java.util.Date;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfmon.systeminfo.CPUInfo;
import com.gip.xyna.xfmg.xfmon.systeminfo.MEMInfo;
import com.gip.xyna.xfmg.xfmon.systeminfo.StaticSystemInformation;
import com.gip.xyna.xprc.XynaOrderCreationParameter;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTask.FREQUENCY_CONTROLLED_TASK_STATUS;
import com.gip.xyna.xprc.xfqctrl.FrequencyControlledTaskStatisticsParameter;
import com.gip.xyna.xprc.xfqctrl.XynaFrequencyControl;
import com.gip.xyna.xprc.xfqctrl.ordercreation.LoadControlledOrderCreationTaskCreationParameter;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class BenchmarkRunner {

  private static final Logger logger = CentralFactoryLogging.getLogger(BenchmarkRunner.class);
  private final String orderType;
  

  public BenchmarkRunner(String orderType) {
    this.orderType = orderType;
  }

  
  public BenchmarkResult runBenchmark(int numberOfCalls, int numberOfRuns, int maxParallel) throws XynaException {
    try {
      ArrayList<Long> frequencies = new ArrayList<Long>();

      String date = Constants.defaultUTCSimpleDateFormat().format(new Date(System.currentTimeMillis()));

      String osName = System.getProperty("os.name");
      String osArchitecture = System.getProperty("os.arch");
      String osVersion = System.getProperty("os.version");
      String javaVersion = System.getProperty("java.version");
      String javaVendor = System.getProperty("java.vendor");
      String javaHome = System.getProperty("java.home");

      String machineInfo = "Operating System: " + osName + " (Version " + osVersion + ", architecture " + osArchitecture + ")\n";
      machineInfo += "Java Vendor: " + javaVendor + " (Version " + javaVersion + ", java home " + javaHome + ")\n";

      ArrayList<CPUInfo> cpuInfo = StaticSystemInformation.getInstance().getCpuInfo();
      MEMInfo memInfo = StaticSystemInformation.getInstance().getMemoryInfo();

      String rawInfo = "!! THE FOLLOWING INFORMATION IS ONLY UPDATED AFTER THE FIRST CALL !!\n\n" + StaticSystemInformation
                      .getInstance().getRawSystemInfo() + "\n\n";

      if (cpuInfo != null && cpuInfo.size() > 0) {
        machineInfo += "\nCPU Info: \n";
        for (CPUInfo info : cpuInfo) {
          machineInfo += " * " + info.getVendorName() + " " + info.getModelname() + " @ " + info.getCpuMhz() + "\n";
        }
      }
      else {
        machineInfo += "\nNo parsed CPU information is available for OS '" + osName + "'\n";
      }

      if (memInfo != null) {
        machineInfo += "\nMemory Info: \n";
        machineInfo += "\n" + memInfo.getFreeMem() + " free memory / " + memInfo.getMaxMem() + " total memory\n";
        machineInfo += "\n" + memInfo.getFreeSwap() + " free swap / " + memInfo.getMaxMem() + " total swap\n";
      }
      else {
        machineInfo += "\nNo parsed memory information is available for OS '" + osName + "'\n";
      }

      String warnString = null;

      long before = System.currentTimeMillis();

      int i = 0;
      for (; i < numberOfRuns; i++) {
        BenchmarkResult currentResult = runBenchmark(orderType, numberOfCalls, maxParallel);
        if (currentResult == null) {
          warnString = "An error occurred while running benchmarks, results may be incomplete and/or inconsistent";
          break;
        }
        if (currentResult.getInfoMessage() != null) {
          String nextMessage = "Info for run " + i + ": " + currentResult.getInfoMessage() + "\n";
          if (warnString == null) {
            warnString = nextMessage;
          }
          else {
            warnString += nextMessage;
          }
        }
        frequencies.add(currentResult.getIntermediateFrequency());
      }

      long after = System.currentTimeMillis();

      long diff = after - before;
      long timeInSeconds = Math.round(diff / 1000.0);
      long freq = Math.round(((numberOfCalls * i) / (double) diff) * 1000);

      return new BenchmarkResult(timeInSeconds, machineInfo, orderType, date, frequencies, freq, warnString)
                      .setRawMachineInformation(rawInfo);

    }
    catch (Throwable t) {
      logger.error("Runtime exception while running benchmark for order type (" + orderType + ")", t);
      return null;
    }
  }
  
  
  private static void addOrder(ArrayList<XynaOrderCreationParameter> list, String ot, XynaObject input) {
    XynaOrderCreationParameter xocp = new XynaOrderCreationParameter(new DestinationKey(ot), input);
    xocp.setPriority(5);
    list.add(xocp);
  }
    
  
  private BenchmarkResult runBenchmark(final String orderType, int numberOfCalls, int maxParallel ) throws XynaException {
    ArrayList<XynaOrderCreationParameter> list = new ArrayList<XynaOrderCreationParameter>();
    addOrder(list, this.orderType, new Container());
    LoadControlledOrderCreationTaskCreationParameter creationParas =
        new LoadControlledOrderCreationTaskCreationParameter(list.get(0).getOrderType() + "-"
            + System.currentTimeMillis(), numberOfCalls, maxParallel, list);
    creationParas
        .setFrequencyControlledTaskStatisticsParameters(new FrequencyControlledTaskStatisticsParameter(300, 1l)); //maximal datenpunkte

    XynaFrequencyControl xfc = XynaFactory.getInstance().getProcessing().getFrequencyControl();
    
    long taskId = xfc.startFrequencyControlledTask(creationParas);
    FrequencyControlledTask fct = xfc.getActiveFrequencyControlledTask(taskId);
    
    while ( fct.getStatus() == FREQUENCY_CONTROLLED_TASK_STATUS.Running ) {
      try {
        Thread.sleep( 200 );
      } catch (InterruptedException e) {
        logger.error("Got interrupted while waiting for benchmark.", e);
        return null;
      }
    }

    long diff = fct.getTaskStopTime() - fct.getTaskStartTime();
    long timeInSeconds = Math.round(diff / 1000.0);
    long freq = Math.round(((numberOfCalls) / (double) diff) * 1000);

    String infoMsg = null;
    if ( fct.getFailedEventCount() > 0) {
      infoMsg = "Warning: " + fct.getFailedEventCount() + " orders failed.";
    }
    return new BenchmarkResult(timeInSeconds, "hwInfo", orderType, "date", null, freq, infoMsg);

  }
}
