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
package com.gip.xyna.xdev.xfractmod.xmdm;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.utils.misc.StringParameter.StringParameterParsingException;
import com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException;
import com.gip.xyna.xact.trigger.FilterInstanceStorable;


public class ConnectionFilterInstance<I extends ConnectionFilter<?>> implements Serializable {

  private static final long serialVersionUID = 1L;

  private I cf;
  private final String filterName;
  private final String instanceName;
  private final String[] sharedLibs;
  private final String description;
  private final String triggerInstanceName;
  private final FilterConfigurationParameter configuration;
  private final long revision;


  public ConnectionFilterInstance(I cf, FilterInstanceStorable filterInstance,
                                  String filterName, String triggerInstanceName,
                                  String[] sharedLibs) throws XACT_InvalidFilterConfigurationParameterValueException {
    this.cf = cf;
    this.filterName = filterName;
    this.instanceName = filterInstance.getFilterInstanceName();
    this.sharedLibs = sharedLibs;
    this.description = filterInstance.getDescription();
    this.triggerInstanceName = triggerInstanceName;
    this.configuration = parseConfiguration(cf.createFilterConfigurationTemplate(), filterInstance.getConfiguration() );
    this.revision = filterInstance.getRevision();
  }
  
  public ConnectionFilterInstance(I cf, String filterName, String triggerInstanceName, String instanceName,
                                  String[] sharedLibs, String description, long revision) throws XACT_InvalidFilterConfigurationParameterValueException, StringParameterParsingException {
    this.cf = cf;
    this.filterName = filterName;
    this.instanceName = instanceName;
    this.sharedLibs = sharedLibs;
    this.description = description;
    this.triggerInstanceName = triggerInstanceName;
    this.configuration = parseConfiguration(cf.createFilterConfigurationTemplate(), Collections.<String>emptyList() ); //FIXME
    this.revision = revision;
  }

  private FilterConfigurationParameter parseConfiguration(FilterConfigurationParameter template, List<String> params) throws XACT_InvalidFilterConfigurationParameterValueException {
    if( template != null ) {
      try {
        return template.build( StringParameter.parse(params).with(template.getAllStringParameters()) );
      } catch (StringParameterParsingException e) {
        throw new XACT_InvalidFilterConfigurationParameterValueException(e.getParameterName(), e);
      }
    } else {
      return null;
    }
  }

  public String getFilterName() {
    return filterName;
  }


  public String getInstanceName() {
    return instanceName;
  }


  public I getCF() {
    return cf;
  }


  public String[] getSharedLibs() {
    return sharedLibs;
  }


  public String getDescription() {
    return description;
  }


  public String getTriggerInstanceName() {
    return triggerInstanceName;
  }


  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if ((obj == null) || (obj.getClass() != this.getClass())) {
      return false;
    }
    ConnectionFilterInstance<?> test = (ConnectionFilterInstance<?>) obj;
    if (filterName == null && instanceName == null) {
      return test.filterName == null && test.instanceName == null;
    }
    if (filterName == null) {
      return test.filterName == null && test.instanceName.equals(instanceName);
    }
    if (instanceName == null) {
      return test.instanceName == null && test.filterName.equals(filterName);
    }
    return filterName.equals(test.filterName) && instanceName.equals(test.instanceName);
  }


  private transient int hash;

  @Override
  public int hashCode() {
    int h = hash;
    if (h == 0) {
      h = filterName.hashCode() ^ instanceName.hashCode();
      hash = h;
    }
    return h;
  }

  public void setFilterInstance(I cf) {
    this.cf = cf;
  }


  public long getRevision() {
    return revision;
  }
  
  public FilterConfigurationParameter getConfiguration() {
    return configuration;
  }

}
