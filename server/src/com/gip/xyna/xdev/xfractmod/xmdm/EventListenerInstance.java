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
package com.gip.xyna.xdev.xfractmod.xmdm;

import java.io.Serializable;
import java.util.List;


/**
 * FIXME Wieso ist das serializable? Da StartParameter und Trigger nicht serializable sind, macht das überhaupt Sinn?
 * TODO besserer Name wäre TriggerInstanceData
 * 
 */
public class EventListenerInstance<J extends StartParameter, I extends EventListener<?,J>> implements Serializable {
  

  private static final long serialVersionUID = 1L;

  private final String name;
  private final transient J startParameter;
  private final String[] paras;
  private final transient I el;
  private final String description;
  private final long revisionOfTriggerInstance; //kann andere revision sein als die vom trigger, aber die klassen haben die gleiche revision

  public EventListenerInstance(String instanceName, List<String> startParameters, I el,
                               J startParameter, String description, long revisionOfTriggerInstance) {
    this.name = instanceName;
    this.startParameter = startParameter;
    this.el = el;
    this.paras = startParameters.toArray(new String[startParameters.size()]);
    this.description = description;
    this.revisionOfTriggerInstance = revisionOfTriggerInstance;
  }

  public EventListenerInstance(EventListenerInstance<J,I> eli) {
    this.name = eli.name;
    this.startParameter = eli.startParameter;
    this.el = eli.el;
    this.paras = eli.paras.clone();
    this.description = eli.description;
    this.revisionOfTriggerInstance = eli.revisionOfTriggerInstance;
  }

  @Override
  public String toString() {
    return "EventListenerInstance("+name+")@"+System.identityHashCode(this);
  }
  
  public String getInstanceName() {
    return name;
  }


  public J getStartParameter() {
    return startParameter;
  }


  public I getEL() {
    return el;
  }


  public String[] getStartParameterAsStringArray() {
    return paras;
  }


  public String getDescription() {
    return description;
  }

  public long getRevision() {
    return revisionOfTriggerInstance;
  }


}
