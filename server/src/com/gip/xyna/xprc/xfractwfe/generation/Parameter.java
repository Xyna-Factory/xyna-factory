/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.xprc.xfractwfe.generation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;

public class Parameter {
  // instance-id
  
  private long instanceId;
  
  public void setInstanceId(long instanceId) {
    this.instanceId = instanceId;
  }
  
  public long getInstanceId() {
    return instanceId;
  }
  
  // parent-order-id
  
  private long parentOrderId;
  
  public void setParentOrderId(long parentOrderId) {
    this.parentOrderId = parentOrderId;
  }
  
  public long getParentOrderId() {
    return parentOrderId;
  }
  
  // foreach-indices
  
  private List<Integer> foreachIndices = new ArrayList<Integer>();
  
  public void setForeachIndices(List<Integer> foreachIndices) {
    this.foreachIndices = foreachIndices;
  }
  
  public List<Integer> getForeachIndices() {
    return foreachIndices;
  }
  
  public boolean foreachIndicesEqual(List<Integer> indicesToCompareWith) {
    List<Integer> foreachIndices = getForeachIndices();
    if (foreachIndices == null) {
      if (indicesToCompareWith == null) {
        return true;
      } else {
        return false;
      }
    } else {
      if (foreachIndices.size() != indicesToCompareWith.size()) {
        return false;
      } else {
        for (int indexNo = 0; indexNo < foreachIndices.size(); indexNo++) {
          if (!foreachIndices.get(indexNo).equals(indicesToCompareWith.get(indexNo))) {
            return false;
          }
        }
        
        return true;
      }
    }
  }
  
  // retry counter
  private int retryCounter = 0;
  
  public void setRetryCounter(int retryCounter) {
    this.retryCounter = retryCounter;
  }
  
  public int getRetryCounter() {
    return retryCounter;
  }
  
  // loop indices
  
  public List<Integer> getLoopIndices() {
    if ( (foreachIndices != null) ) {
      return foreachIndices;
    } else {
      List<Integer> loopIndices = new ArrayList<Integer>();
      loopIndices.add(retryCounter);
      
      return loopIndices;
    }
  }
  
  // input data
  
  private List<AVariable> inputData = new ArrayList<AVariable>();
  
  public void setInputData(List<AVariable> inputData) {
    this.inputData = inputData;
  }
  
  public List<AVariable> addInputData(List<AVariable> inputData) {
    this.inputData.addAll(inputData);
    return this.inputData;
  }
  
  public List<AVariable> getInputData() {
    return inputData;
  }
  
  // output data
  
  private List<AVariable> outputData = new ArrayList<AVariable>();
  
  public void setOutputData(List<AVariable> outputData) {
    this.outputData = outputData;
  }
  
  public List<AVariable> addOutputData(List<AVariable> outputData) {
    this.outputData.addAll(outputData);
    return this.outputData;
  }
  
  public List<AVariable> getOutputData() {
    return outputData;
  }
  
  // step error data
  
  private ErrorInfo error = null;
  
  public void setErrorInfo(ErrorInfo error) {
    this.error = error;
  }
  
  public ErrorInfo getErrorInfo() {
    return error;
  }
  
  // caught exception
  
  private ErrorInfo caughtException = null;
  
  public void setCaughtExceptionInfo(ErrorInfo error) {
    this.caughtException = error;
  }
  
  public ErrorInfo getCaughtExceptionInfo() {
    return caughtException;
  }
  
  // input time stamp
  
  private String inputTimeStamp = null;
  
  public void setInputTimeStamp(String formattedTimeStamp) {
    inputTimeStamp = formattedTimeStamp;
  }
  
  public String getInputTimeStamp() {
    return inputTimeStamp;
  }
  
  public long getInputTimeStampUnix() throws ParseException {
    return ( (inputTimeStamp == null) ? -1 : convertToUnixTime(inputTimeStamp) );
  }
  
  // output time stamp
  
  private String outputTimeStamp = null;
  
  public void setOutputTimeStamp(String formattedTimeStamp) {
    this.outputTimeStamp = formattedTimeStamp;
  }
  
  public String getOutputTimeStamp() {
    return outputTimeStamp;
  }
  
  public long getOutputTimeStampUnix() throws ParseException {
    return ( (outputTimeStamp == null) ? -1 : convertToUnixTime(outputTimeStamp) );
  }
  
  // error time stamp
  
  private String errorTimeStamp = null;
  
  public void setErrorTimeStamp(String formattedTimeStamp) {
    errorTimeStamp = formattedTimeStamp;
  }
  
  public String getErrorTimeStamp() {
    return errorTimeStamp;
  }
  
  public long getErrorTimeStampUnix() throws ParseException {
    return ( (errorTimeStamp == null) ? -1 : convertToUnixTime(errorTimeStamp) );
  }
  
  // internal helper methods
  
  private long convertToUnixTime(String formattedTime) throws ParseException {
    SimpleDateFormat sdf;
    if (XynaProperty.XYNA_PROCESS_MONITOR_SHOW_STEP_MILLISECONDS.get()) {
      sdf = Constants.defaultUTCSimpleDateFormatWithMS();
    } else {
      sdf = Constants.defaultUTCSimpleDateFormat();
    }
    
    return sdf.parse(formattedTime).getTime();
  }

  public boolean isPrefixOfForeachIndices(List<Integer> foreachIndicesPrefix) {
    List<Integer> foreachIndices = getForeachIndices();
    if (foreachIndicesPrefix.size() > foreachIndices.size()) {
      return false;
    }

    for (int level = 0; level < foreachIndicesPrefix.size(); level++) {
      if (!foreachIndices.get(level).equals(foreachIndicesPrefix.get(level))) {
        return false;
      }
    }

    return true;
  }
  
}
