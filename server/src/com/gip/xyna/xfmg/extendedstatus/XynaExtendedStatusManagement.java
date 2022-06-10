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

package com.gip.xyna.xfmg.extendedstatus;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.gip.xyna.Section;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;


public class XynaExtendedStatusManagement extends Section implements XynaExtendedStatusManagementInterface {
  
  
  private Map<String, ExtendedStatusInformation> extendedStatusSteps;
  
  private static ConcurrentMap<String,List<String>> furtherInformationAtStartup = new ConcurrentHashMap<String,List<String>>();

  public XynaExtendedStatusManagement() throws XynaException {
    super();
    extendedStatusSteps = new HashMap<String, ExtendedStatusInformation>();
  }

  @Override
  public String getDefaultName() {
    return "Xyna Extended Status Management";
  }


  @Override
  protected void init() throws XynaException {
    // nothing to do
  }


  public static void addFurtherInformationAtStartup(String type, String info) {
    List<String> infoList = furtherInformationAtStartup.get(type);
    if (infoList == null) {
      infoList = new LinkedList<String>();
      List<String> previous = furtherInformationAtStartup.putIfAbsent(type, infoList);
      if (previous != null) {
        infoList = previous;
      }
    }
    synchronized (infoList) {
      if (infoList.size() >= XynaProperty.STARTUP_FURTHER_INFO_MAX.get()) {
        ((LinkedList<String>) infoList).removeFirst();
      }
      infoList.add(info);
    }
  }
  
  public static boolean containsKey(String key) {
    return furtherInformationAtStartup.containsKey(key);
  }
  
  public void registerStep(StepStatus step, String componentName, String additionalInformation) {
    extendedStatusSteps.put(componentName, new ExtendedStatusInformation(step, componentName, additionalInformation));
  }


  public void updateStep(StepStatus step, String componentName, String additionalInformation) {
    ExtendedStatusInformation info = extendedStatusSteps.get(componentName);
    if(info == null) {
      info = new ExtendedStatusInformation(step, componentName, additionalInformation);
      extendedStatusSteps.put(componentName, new ExtendedStatusInformation(step, componentName, additionalInformation));
    }
    info.setAdditionalInformation(additionalInformation);
    info.setStep(step);
    info.setComponentName(componentName);
  }


  public void deregisterStep(String componentName) {
    extendedStatusSteps.remove(componentName);
  }
  

  public Collection<ExtendedStatusInformation> listExtendedStatusInformation() {
    return extendedStatusSteps.values();
  }
  
  
  public Map<String, List<String>> getFurtherInformationFromStartup() {
    return furtherInformationAtStartup;
  }


}
