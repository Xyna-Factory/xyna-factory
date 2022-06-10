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

package com.gip.xyna.xfmg.xfmon.systeminfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.xfmg.Constants;



public class StaticSystemInformation {

  private static final Logger logger = CentralFactoryLogging.getLogger(StaticSystemInformation.class);
  private static final StaticSystemInformation _instance = new StaticSystemInformation();


  private ArrayList<CPUInfo> cpuInfo = null;
  private MEMInfo memInfo = null;

  private String rawSystemInfo = null;

  private String osName = null;


  public static StaticSystemInformation getInstance() {
    return getInstance(false);
  }


  public static StaticSystemInformation getInstance(boolean refreshAll) {
    if (refreshAll) {
      StaticSystemInformation newInstance = new StaticSystemInformation();
      return newInstance;
    } else {
      if (_instance.memInfo != null && _instance.osName.toLowerCase().contains("linux")) {
        _instance.updateProcMemInfo();
      }
      return _instance;
    }
  }


  private StaticSystemInformation() {

    osName = System.getProperty("os.name");

    if (osName.toLowerCase().contains("windows")) {
      logger.warn("Cannot obtain hardware information on operating system '" + osName + "'");
      // TODO this might be possible using the package org.apache.excalibur.util.system
    } else if (osName.toLowerCase().contains("sunos")) {

      if (logger.isInfoEnabled()) {
        logger.info("Gathering static system information for OS '" + osName + "'...");
      }

      String tmpFileName = "sysinfoTempFile.txt";
      File tmpInfo = new File(tmpFileName);
      String command =
          Constants.fileSeparator + "usr" + Constants.fileSeparator + "sbin" + Constants.fileSeparator
              + "prtdiag -v > " + tmpInfo.getAbsolutePath();
      if (logger.isInfoEnabled()) {
        logger.info("Executing command " + command);
      }
      try {
        Runtime.getRuntime().exec(command);
      } catch (IOException e2) {
        logger.warn("An error occurred while executing script gathering system information", e2);
        if (tmpInfo.exists()) {
          tmpInfo.delete();
        }
      }

      try {
        Thread.sleep(2500);
      } catch (InterruptedException e2) {
        logger.warn("Interrupted while waiting for script to gather system information");
      }

      if (!tmpInfo.exists()) {
        logger.warn("Could not resolve hardware information on operating system '" + osName + "'");
        return;
      }

      try {
        rawSystemInfo = FileUtils.readFileAsString(tmpInfo, true, Constants.DEFAULT_ENCODING);
      } catch (Ex_FileWriteException e) {
        logger.warn("Could not resolve hardware information on operating system '" + osName + "'");
        return;
      }

      tmpInfo.delete();

      if (logger.isInfoEnabled()) {
        logger.info("No parsing schema available for OS " + osName + ", only setting raw output");
      }

    } else {

      if (logger.isInfoEnabled()) {
        logger.info("Gathering static system information for OS '" + osName + "'...");
      }

      File cpuInfoFile = new File("/proc/cpuinfo");
      if (!cpuInfoFile.exists()) {
        logger.warn("Could not resolve hardware information on operating system '" + osName + "'");
        return;
      }

      try {
        rawSystemInfo = FileUtils.readFileAsString(cpuInfoFile, true);
      } catch (Exception e) {
        logger.error(null, e);
        return;
      }

      BufferedReader reader = new BufferedReader(new StringReader(rawSystemInfo));

      logger.debug("Parsing CPU information");
      cpuInfo = parseProcCpuInfo(reader);

      updateProcMemInfo();

    }

  }


  public ArrayList<CPUInfo> getCpuInfo() {
    if (cpuInfo == null)
      return new ArrayList<CPUInfo>();
    return cpuInfo;
  }


  public MEMInfo getMemoryInfo() {
    return memInfo;
  }


  public String getRawSystemInfo() {
    return rawSystemInfo;
  }


  private ArrayList<CPUInfo> parseProcCpuInfo(BufferedReader reader) {

    ArrayList<CPUInfo> result = new ArrayList<CPUInfo>();

    boolean finished = false;
    try {
      while (!finished) {

        Integer procId = null;
        Integer mHz = null;
        String vendorName = null;
        String modelName = null;

        int count = 0;

        while (true) {

          count++;
          if (count > 200) {
            logger.warn("An error occurred while determining CPU information, unparsable output");
            return result;
          }

          String s = reader.readLine();
          if (s == null) {
            finished = true;
            break;
          }

          int position = s.lastIndexOf(":");
          if (s.toLowerCase().startsWith("processor")) {
            if (procId != null) {
              logger.debug("Found second processor line: " + s);
              continue;
            }
            String substring = s.substring(position + 2, s.length()).trim();
            procId = Integer.valueOf(substring);
            if (logger.isDebugEnabled()) {
              logger.debug("Found CPU id " + procId);
            }
          } else if (s.toLowerCase().startsWith("vendor_id")) {
            if (vendorName != null) {
              logger.debug("Found second cpu id line: " + s);
              continue;
            }
            vendorName = s.substring(position + 2, s.length());
            if (logger.isDebugEnabled()) {
              logger.debug("Found vendor name " + vendorName);
            }
          } else if (s.toLowerCase().startsWith("model name")) {
            if (modelName != null) {
              logger.debug("Found second model name line: " + s);
              continue;
            }
            modelName = s.substring(position + 2, s.length());
            if (logger.isDebugEnabled()) {
              logger.debug("Found model name " + modelName);
            }
          } else if (s.toLowerCase().startsWith("cpu mhz")) {
            if (mHz != null) {
              logger.debug("Found second cpu mhz line: " + s);
              continue;
            }
            mHz = (int) Math.round(Double.valueOf(s.substring(position + 2, s.length())));
            if (logger.isDebugEnabled()) {
              logger.debug("Found MHz " + mHz);
            }
          }

          if (mHz != null && procId != null && vendorName != null && modelName != null) {
            result.add(new CPUInfo(procId, vendorName, modelName, mHz));
            break;
          }

        }
      }
    } catch (IOException e) {
      logger.warn("Could not resolve hardware information");
      return null;
    }

    return result;

  }


  private void updateProcMemInfo() {

    logger.debug("Parsing Memory information");
    File memInfoFile = new File("/proc/meminfo");
    if (!memInfoFile.exists()) {
      logger.warn("Could not resolve memory information on operating system '" + osName + "'");
      return;
    }

    BufferedReader reader2 = null;
    try {
      reader2 = new BufferedReader(new FileReader(memInfoFile));
    } catch (FileNotFoundException e) {
      logger.warn("Could not resolve memory information on operating system '" + osName + "'");
      return; // this should never happen
    }
    try {
      memInfo = parseProcMeminfo(reader2);
    } finally {
      try {
        reader2.close();
      } catch (IOException e) {
      }
    }

  }


  private MEMInfo parseProcMeminfo(BufferedReader reader) {

    try {

      String maxMem = null;
      String freeMem = null;
      String maxSwap = null;
      String freeSwap = null;

      int count = 0;

      while (true) {

        count++;
        if (count > 200) {
          logger.warn("An error occurred while determining MEM information, unparsable output");
          return null;
        }

        String s = reader.readLine();
        if (s == null) {
          return null;
        }

        int position = s.lastIndexOf(":");
        if (s.trim().contains("MemTotal")) {
          if (maxMem != null) {
            logger.error("An error occurred while determining MEM information");
            return null;
          }
          maxMem = s.trim().substring(position + 1, s.length()).trim();
          if (logger.isDebugEnabled()) {
            logger.debug("Found MemTotal " + maxMem);
          }
        } else if (s.trim().contains("MemFree")) {
          if (freeMem != null) {
            logger.error("An error occurred while determining MEM information");
            return null;
          }
          freeMem = s.trim().substring(position + 1, s.length()).trim();
          if (logger.isDebugEnabled()) {
            logger.debug("Found MemFree " + freeMem);
          }
        } else if (s.trim().contains("SwapTotal")) {
          if (maxSwap != null) {
            logger.error("An error occurred while determining MEM information");
            return null;
          }
          maxSwap = s.trim().substring(position + 1, s.length()).trim();
          if (logger.isDebugEnabled()) {
            logger.debug("Found SwapTotal " + maxSwap);
          }
        } else if (s.trim().contains("SwapFree")) {
          if (freeSwap != null) {
            logger.error("An error occurred while determining MEM information");
            return null;
          }
          freeSwap = s.trim().substring(position + 1, s.length()).trim();
          if (logger.isDebugEnabled()) {
            logger.debug("Found SwapFree " + freeSwap);
          }
        }

        if (freeMem != null && maxMem != null && freeSwap != null && maxSwap != null) {
          return new MEMInfo(maxMem, freeMem, maxSwap, freeSwap);
        }

      }
    } catch (IOException e) {
      logger.warn("Could not resolve hardware information");
      return null;
    }

  }

}
