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



import java.io.File;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.benchmarking.BenchmarkResult;
import com.gip.xyna.xdev.benchmarking.BenchmarkRunner;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;
import com.gip.xyna.xmcp.xfcli.generated.Benchmark;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public class BenchmarkImpl extends XynaCommandImplementation<Benchmark> {

  private static final Logger logger = CentralFactoryLogging.getLogger(BenchmarkImpl.class);


  public void execute(OutputStream statusOutputStream, Benchmark payload) throws XynaException {
    try {
      executeInternally(statusOutputStream, payload);
    } catch (IOException e) {
      throw new Ex_FileAccessException("(benchmark file)", e);
    }
  }


  private void executeInternally(OutputStream statusOutputStream, Benchmark payload) throws IOException, XynaException {


    final String orderType = payload.getOrderType();

    final Integer cnt;
    try {
      cnt = Integer.valueOf(payload.getNumberOfCalls());
    } catch (NumberFormatException e) {
      writeLineToCommandLine(statusOutputStream, e.getMessage());
      return;
    }

    int numberOfRuns;
    if (payload.getNumberOfRuns() != null) {
      numberOfRuns = Integer.valueOf(payload.getNumberOfRuns());
    } else {
      numberOfRuns = 1;
    }

    int maxParallel = 1;
    if (payload.getMaxParallel() != null) {
      maxParallel = Integer.valueOf(payload.getMaxParallel());
    } else {
      maxParallel = 1;
    }

    BenchmarkResult result = new BenchmarkRunner(orderType).runBenchmark(cnt, numberOfRuns, maxParallel);

    if (result == null) {
      writeLineToCommandLine(statusOutputStream,
                             "An unknown error occurred while running the benchmark\nPlease view the server log file for further information");
      return;
    }


    String benchmarkResultsDir = "benchmarkresults";
    boolean createOutputFile = true;
    if (!new File(benchmarkResultsDir).exists()) {
      new File(benchmarkResultsDir).mkdir();
    } else {
      if (!new File(benchmarkResultsDir).isDirectory()) {
        logger.warn("./" + benchmarkResultsDir
            + " is expected to be a directory but is a file, cannot create benchmark result output file");
        createOutputFile = false;
      }
    }

    String resultMsg = "Benchmark results as of " + result.getDate() + ":\n\n";


    resultMsg +=
        new String("Needed ~" + result.getDuration() + " seconds to finish " + cnt + " calls "
            + result.getFrequencies().size() + " times\n");
    resultMsg += "This is an intermediate frequency of approximately " + result.getIntermediateFrequency() + " Hz\n";

    resultMsg += "\n**************************\n";

    resultMsg += "Detailed frequency information:\n";
    int count = 0;
    for (Long l : result.getFrequencies()) {
      count++;
      resultMsg += " * Frequency in run " + count + ": " + l + " Hz\n";
    }

    resultMsg += "**************************\n";

    resultMsg += "\nOrdertype: " + orderType + " (see below for workflow XML)\n";
    resultMsg += "Number of calls per run: " + cnt + "\n";
    resultMsg += "Runs: " + numberOfRuns + "\n";
    resultMsg += "Maximal parallel execution: " + maxParallel + "\n";
    resultMsg +=
        "Loglevel: " + (logger.isDebugEnabled() ? "DEBUG" : "")
            + (!logger.isDebugEnabled() && logger.isInfoEnabled() ? "INFO" : "")
            + (!logger.isDebugEnabled() && !logger.isInfoEnabled() ? "WARN or higher" : "") + "\n";
    Integer monLvl =
        XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
            .getMonitoringLevel(new DestinationKey(orderType));
    if (monLvl == null) {
      monLvl =
          XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getMonitoringDispatcher()
              .getDefaultMonitoringLevel();
    }
    resultMsg += "Monitoring Level for " + orderType + ": " + monLvl + "\n";
    resultMsg += "Workflow pool size: " + XynaProperty.XYNA_WORKFLOW_POOL_SIZE.get() + "\n\n";
     

    if (result.getInfoMessage() != null) {
      resultMsg += "!!!!!!!!!!!!!!!!!!!!!!!!!!\n";
      resultMsg += result.getInfoMessage() + "\n";
      resultMsg += "!!!!!!!!!!!!!!!!!!!!!!!!!!\n\n";
    }

    resultMsg += "**************************\n";
    resultMsg += result.getMachineInformation();
    resultMsg += "**************************\n";

    writeToCommandLine(statusOutputStream, resultMsg);

    if (result.getRawMachineInformation() != null) {
      resultMsg += "\n\n**************************\n";
      resultMsg += "Raw machine information available:\n";
      resultMsg += "**************************\n\n";
      resultMsg += result.getRawMachineInformation();
    }

    String xmlFileName =
        GenerationBase.getFileLocationOfXmlNameForDeployment(GenerationBase.lookupXMLNameByJavaClassName(orderType, RevisionManagement.REVISION_DEFAULT_WORKSPACE, false)) + ".xml";
    File xmlFile = new File(xmlFileName);

    resultMsg += "\n\n**************************\n";
    resultMsg += "Workflow XML\n";
    resultMsg += "**************************\n\n";

    resultMsg += FileUtils.readFileAsString(xmlFile);

    String outputFileName = "result" + result.getDate() + ".txt";
    File outputFile = new File(benchmarkResultsDir, outputFileName);

    if (createOutputFile) {
      if (outputFile.exists() || outputFile.createNewFile()) {
        FileUtils.writeStringToFile(resultMsg, outputFile);
        writeLineToCommandLine(statusOutputStream, "Information written to " + outputFile.getAbsolutePath());
      }
    }


  }

}
