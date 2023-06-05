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



public class BenchmarkResult {

  private long duration;
  private String machineInformation;
  private String rawMachineInformation;
  private String orderType;
  private String date;
  private ArrayList<Long> frequencies;
  private long intermediateFrequency;
  private String infoMessage;


  public BenchmarkResult(long duration, String machineInformation, String orderType, String date,
                         ArrayList<Long> freqs, Long intermediateFreq, String infoMessage) {

    this.setDuration(duration);
    this.setMachineInformation(machineInformation);
    this.setOrderType(orderType);
    this.setDate(date);
    this.setFrequencies(freqs);
    this.setIntermediateFrequency(intermediateFreq);
    this.infoMessage = infoMessage;

  }


  public BenchmarkResult setDuration(long duration) {
    this.duration = duration;
    return this;
  }


  public long getDuration() {
    return duration;
  }


  public BenchmarkResult setIntermediateFrequency(Long intermediateFrequency) {
    this.intermediateFrequency = intermediateFrequency;
    return this;
  }


  public Long getIntermediateFrequency() {
    return intermediateFrequency;
  }


  public BenchmarkResult setFrequencies(ArrayList<Long> frequencies) {
    this.frequencies = frequencies;
    return this;
  }


  public ArrayList<Long> getFrequencies() {
    return frequencies;
  }


  public BenchmarkResult setDate(String date) {
    this.date = date;
    return this;
  }


  public String getDate() {
    return date;
  }


  public BenchmarkResult setOrderType(String orderType) {
    this.orderType = orderType;
    return this;
  }


  public String getOrderType() {
    return orderType;
  }


  public BenchmarkResult setMachineInformation(String hardwareInformation) {
    this.machineInformation = hardwareInformation;
    return this;
  }


  public String getMachineInformation() {
    return machineInformation;
  }


  public BenchmarkResult setInfoMessage(String infoMessage) {
    this.infoMessage = infoMessage;
    return this;
  }


  public String getInfoMessage() {
    return infoMessage;
  }


  public BenchmarkResult setRawMachineInformation(String s) {
    this.rawMachineInformation = s;
    return this;
  }


  public String getRawMachineInformation() {
    return rawMachineInformation;
  }

}
