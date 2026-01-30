/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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
package com.gip.xyna.xnwh.sharedresources;



import java.util.List;



public class SharedResourceSynchronizerInstance {

  public enum Status {

    Stop("Stop"), Start("Start"), NextStart("NextStart");


    private final String value;


    Status(String value) {
      this.value = value;
    }


    public static Status fromValue(String value) {
      for (Status status : Status.values()) {
        if (status.value.equals(value)) {
          return status;
        }
      }
      throw new IllegalArgumentException("Invalid value: " + value);
    }


    public String getValue() {
      return value;
    }


    public String toString() {
      return value;
    }
  }


  private String instanceName;
  private String typeName;
  private List<String> configuration;
  private Status status = Status.Stop;


  public String getInstanceName() {
    return instanceName;
  }


  public void setInstanceName(String instanceName) {
    this.instanceName = instanceName;
  }


  public String getTypeName() {
    return typeName;
  }


  public void setTypeName(String typeName) {
    this.typeName = typeName;
  }


  public List<String> getConfiguration() {
    return configuration;
  }


  public void setConfiguration(List<String> configuration) {
    if (configuration == null) {
      throw new IllegalArgumentException("Configuration cannot be null");
    }
    this.configuration = configuration;
  }


  public Status getStatus() {
    return status;
  }


  public void setStatus(Status status) {
    if (status == null) {
      throw new IllegalArgumentException("Status cannot be null");
    }
    this.status = status;
  }
}
