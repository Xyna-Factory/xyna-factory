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
package com.gip.xyna.xact.trigger;

import java.io.Serializable;


/**
 *
 */
public class TriggerInstanceIdentification implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private String name;
  private Long revision;
  private String instanceName;

  public TriggerInstanceIdentification(String name, Long revision, String instanceName) {
    this.name = name;
    this.revision = revision;
    this.instanceName = instanceName;
  }

  public String getName() {
    return name;
  }
  
  public String getInstanceName() {
    return instanceName;
  }
  
  public Long getRevision() {
    return revision;
  }
  
  @Override
  public String toString() {
    return "TriggerInstanceIdentification("+name+","+revision+","+instanceName+")";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((instanceName == null) ? 0 : instanceName.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    result = prime * result + ((revision == null) ? 0 : revision.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    TriggerInstanceIdentification other = (TriggerInstanceIdentification) obj;
    if (instanceName == null) {
      if (other.instanceName != null)
        return false;
    } else if (!instanceName.equals(other.instanceName))
      return false;
    if (name == null) {
      if (other.name != null)
        return false;
    } else if (!name.equals(other.name))
      return false;
    if (revision == null) {
      if (other.revision != null)
        return false;
    } else if (!revision.equals(other.revision))
      return false;
    return true;
  }
  
  
}
