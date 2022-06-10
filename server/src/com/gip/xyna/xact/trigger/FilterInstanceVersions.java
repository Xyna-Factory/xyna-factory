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
package com.gip.xyna.xact.trigger;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.collections.Triple;


public class FilterInstanceVersions {
  
  private String applicationName;
  private String versionName; //Version dieser Filterinstanz
  private FilterInstanceStorable filterInstance;
  private List<String> outdatedVersions = new ArrayList<String>(); //Versionen der outdated Filterinstanzen
  
  public FilterInstanceVersions(String applicationName, String versionName, FilterInstanceStorable filterInstance) {
    this.applicationName = applicationName;
    this.versionName = versionName;
    this.filterInstance = filterInstance;
  }
  
  public String getApplicationName() {
    return applicationName;
  }

  public String getVersionName() {
    return versionName;
  }
  
  public FilterInstanceStorable getFilterInstance() {
    return filterInstance;
  }
  
  public List<String> getOutdatedVersions() {
    return outdatedVersions;
  }

  public void addOutdatedVersion(String outdatedVersion) {
    outdatedVersions.add(outdatedVersion);
  }

  
  public static Transformation<FilterInstanceVersions, String> transformationGetApplicationName() {
    return new GetApplicationName();
  }
  
  public static class GetApplicationName implements Transformation<FilterInstanceVersions, String> {
    public String transform(FilterInstanceVersions from) {
      return from.getApplicationName();
    }
  }

  
  public static Transformation<FilterInstanceVersions, Triple<String, String, String>> transformationGetFilterInstance() {
    return new GetFilterInstance();
  }
  
  public static class GetFilterInstance implements Transformation<FilterInstanceVersions, Triple<String, String, String>> {
    public Triple<String, String, String> transform(FilterInstanceVersions from) {
      String first = from.getFilterInstance().getFilterInstanceName();
      String second = from.getFilterInstance().getFilterName();
      String third = from.getFilterInstance().getTriggerInstanceName();
      return Triple.of(first, second, third);
    }
  }
}
