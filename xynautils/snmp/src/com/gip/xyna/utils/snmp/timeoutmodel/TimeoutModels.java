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
package com.gip.xyna.utils.snmp.timeoutmodel;

import java.util.HashMap;



/**
 * TimeoutModels is a helper class for creating instances of interface 
 * {@link TimeoutModel} and for checking the needed data.
 * 
 * New defined TimeoutModels should be registered:
 * TimeoutModels.registerTimeoutModel( "defined", new DefinedTimeoutModel() );
 * Then they can be used in SnmpAccessData and SnmpContext.
 */
public class TimeoutModels {

  private TimeoutModels() {/*only static usage*/}

  private static HashMap<String,TimeoutModel> timeoutModels = new HashMap<String,TimeoutModel>();
  static {
    timeoutModels.put("simple", new SimpleTimeoutModel() );
    timeoutModels.put("interval", new IntervalTimeoutModel() );
    timeoutModels.put("default", new IntervalTimeoutModel(new int[]{500,1000,2000,5000,5000}) );
  }
 
  /**
   * registers a customized timeoutModel with the given name
   * @param name
   * @param timeoutModel
   * @return
   */
  public static TimeoutModel registerTimeoutModel( String name, TimeoutModel timeoutModel) {
    if( name == null ) {
      throw new IllegalArgumentException("Name is null");
    }
    if( timeoutModel == null ) {
      throw new IllegalArgumentException("TimeoutModel is null");
    }
    return timeoutModels.put(name,timeoutModel);
  }
  
  public static boolean containsTimeoutMode(String name) {
    return timeoutModels.containsKey(name);
  }
    
  /**
   * Creates a new timeoutModel-intance
   * @param timeoutModelName
   * @param timeoutModelData
   * @return
   */
  public static TimeoutModel newTimeoutModel(String timeoutModelName, int[] timeoutModelData) {
    TimeoutModel tmCreator =  timeoutModels.get(timeoutModelName);
    if( tmCreator == null ) {
      throw new IllegalArgumentException( "Unknown TimeoutModel \""+timeoutModelName+"\"");
    }
    if( timeoutModelName.equals("default") ) {
      return tmCreator.newInstance();
    }
    if( tmCreator instanceof TimeoutModel2 ) {
      return ((TimeoutModel2) tmCreator).newInstance(timeoutModelData);
    }
    if( timeoutModelData != null && timeoutModelData.length == 2 ) {
      return tmCreator.newInstance(timeoutModelData[0],timeoutModelData[1]);
    } else {
      throw new IllegalArgumentException( "Wrong timeoutModelData");
    }
  }

  /**
   * Checks whether the timeoutModelName exists and the timeoutModelData are valid
   * @param timeoutModelName
   * @param timeoutModelData
   * @return
   */
  public static String check(String timeoutModelName, int[] timeoutModelData) {
    TimeoutModel tmCreator =  timeoutModels.get(timeoutModelName);
    if( tmCreator == null ) {
      return "Unknown TimeoutModel \""+timeoutModelName+"\"";
    }
    if( timeoutModelName.equals("default") ) {
      return null; //Default-TimeoutModel needs no parameter
    }
    if( tmCreator instanceof TimeoutModel2 ) {
      return ((TimeoutModel2) tmCreator).check(timeoutModelData);
    }
    if( timeoutModelData != null && timeoutModelData.length == 2 ) {
      return tmCreator.check(timeoutModelData[0],timeoutModelData[1]);
    } else {
      return "Wrong timeoutModelData, expected was 2 ints";
    }
  }
  
  
}
