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
package com.gip.xyna.xmcp.xfcli;


/**
 *
 */
public enum ReturnCode {

  //TODO freie Codes: 3,4,7,8,13-252
  
  SUCCESS(0,               "Success", "command executed sucessfully"),
  SILENT(0,                "Success", "command executed sucessfully, but 'ok' should not be printed"),
  
  SUCCESS_BUT_NO_CHANGE( 9,  "Success without change", "command executed sucessfully, but nothing was changed"), 
  SUCCESS_WITH_PROBLEM( 12,  "Success with problem", "command executed sucessfully, but some problems occurred"), 
  
  /*
   * Aus XynaFactrory.getStatusCodeSLESLike hierher
   * statuscode a la runlevel
   * 0 - Not Running
   * 1 - Starting
   * 2 - future use
   * 3 - future use
   * 4 - future use
   * 5 - Up and running
   * 6 - Stopping
   */
  STATUS_UP_AND_RUNNING(0,      "Up and running", "status"),
  STATUS_STARTING(1,            "Starting",       "status"),
  STATUS_SERVICE_NOT_RUNNING(5, "Not running",    "status"),
  STATUS_STOPPING(6,            "Stopping",       "status"),
  STATUS_ALREADY_RUNNING(2,     "Up and running", "the factory should not be running" ),
  
    
  UNKNOWN_COMMAND(10, "Unknown command", "unknown command" ),
  REJECTED(11,        "Rejected", "command was rejected" ),
  
  XYNA_EXCEPTION(253, "XynaException"),
  COMMUNICATION_FAILED(254, "communication failed" ),
  GENERAL_ERROR(255, "general exception");
  
  
  private int code;
  private String message;
  private String description;

  private ReturnCode(int code, String message, String description) {
    this.code = code;
    this.message = message;
    this.description = description;
  }
  private ReturnCode(int code, String messageDescription) {
    this.code = code;
    this.message = messageDescription;
    this.description = messageDescription;
  }

  public int getCode() {
    return code;
  }
  
  public String getMessage() {
    return message;
  }
  
  public String getDescription() {
    return description;
  }
}
