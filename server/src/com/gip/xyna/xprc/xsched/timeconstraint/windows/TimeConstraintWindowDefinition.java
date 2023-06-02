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
package com.gip.xyna.xprc.xsched.timeconstraint.windows;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xsched.timeconstraint.windows.MultiTimeWindow.MultiTimeWindowDefinition;


/**
 * Definition eines {@link TimeConstraintWindow}s
 * Klasse ist immutable.
 */
public class TimeConstraintWindowDefinition implements Serializable {
  private static final long serialVersionUID = 1L;
  
  private String name;
  private String description;
  private TimeWindowDefinition timeWindowDefinition;
  private boolean persistent;
  
  public TimeConstraintWindowDefinition(String name, String description, TimeWindowDefinition timeWindowDefinition) {
    this.name = name;
    this.description = description;
    this.timeWindowDefinition = timeWindowDefinition;
    this.persistent = true;
  }
  
  public TimeConstraintWindowDefinition(String name, String description, TimeWindowDefinition timeWindowDefinition, boolean persistent) {
    this.name = name;
    this.description = description;
    this.timeWindowDefinition = timeWindowDefinition;
    this.persistent = persistent;
  }

  public String getName() {
    return name;
  }
  
  public String getDescription() {
    return description;
  }
    
  public boolean isPersistent() {
    return persistent;
  }
    
  public TimeWindowDefinition getTimeWindowDefinition() {
    return timeWindowDefinition;
  }

  
  
  public static Builder construct(String name) {
    return new Builder(name); 
  }
  
  public static class Builder {

    private String name;
    private String description;
    private List<TimeWindowDefinition> timeWindowDefinitions;
    private boolean persistent = true;

    public Builder(String name) {
      this.name = name;
    }
    
    public Builder setName(String name) {
      this.name = name;
      return this;
    }
    
    public Builder setDescription(String description) {
      this.description = description;
      return this;
    }
    
    public Builder setPersistent(boolean persistent) {
      this.persistent = persistent;
      return this;
    }
    
    public Builder setTimeWindowDefinition(TimeWindowDefinition timeWindowDefinition) {
      this.timeWindowDefinitions = new ArrayList<TimeWindowDefinition>();
      timeWindowDefinitions.add(timeWindowDefinition);
      return this;
    }
    
    public Builder setTimeWindowDefinitions(List<TimeWindowDefinition> timeWindowDefinitions) {
      this.timeWindowDefinitions = new ArrayList<TimeWindowDefinition>(timeWindowDefinitions);
      return this;
    }

    public Builder addTimeWindowDefinition(TimeWindowDefinition timeWindowDefinition) {
      if( timeWindowDefinitions == null ) {
        timeWindowDefinitions = new ArrayList<TimeWindowDefinition>();
      }
      timeWindowDefinitions.add(timeWindowDefinition);
      return this;
    }

    public TimeConstraintWindowDefinition construct() {
      if( timeWindowDefinitions == null || timeWindowDefinitions.isEmpty() ) {
        throw new IllegalStateException("No timeWindowDefinitions set");
      } else if( timeWindowDefinitions.size() == 1 ) {
        return new TimeConstraintWindowDefinition(name, description, timeWindowDefinitions.get(0), persistent);
      } else {
        return new TimeConstraintWindowDefinition(name, description, MultiTimeWindowDefinition.construct(timeWindowDefinitions), persistent);
      }
    }

  }
  
  
  
  
  
  
  public List<TimeConstraintWindowStorable> toStorables() {
    List<TimeConstraintWindowStorable> list = new ArrayList<TimeConstraintWindowStorable>();
    int size = 0;
    if( timeWindowDefinition instanceof MultiTimeWindowDefinition ) {
      ((MultiTimeWindowDefinition)timeWindowDefinition).size();
    } else {
      size = 1;
    }
    if( timeWindowDefinition instanceof MultiTimeWindowDefinition ) {
      MultiTimeWindowDefinition multTWD = (MultiTimeWindowDefinition)timeWindowDefinition;
      size = multTWD.size();
      for( int subId = 0; subId<size; ++subId ) {
        TimeConstraintWindowStorable tcws = TimeConstraintWindowStorable.create();
        list.add(tcws);
        tcws.setName(name);
        tcws.setSubId(subId);
        tcws.setDefinition(multTWD.getDefinition(subId));
      }
    } else {
      size = 1;
      TimeConstraintWindowStorable tcws = TimeConstraintWindowStorable.create();
      list.add(tcws);
      tcws.setName(name);
      tcws.setSubId(0);
      tcws.setDefinition(timeWindowDefinition);
    }    
    TimeConstraintWindowStorable master = list.get(0);
    master.setDescription(description);
    master.setSubWindowCount( size );
    
    return list;
  }
  
  public static TimeConstraintWindowDefinition fromStorables(String name, List<TimeConstraintWindowStorable> storables) {
    //Master suchen und storables prüfen, dass sie zum selben Zeitfenster gehören
    TimeConstraintWindowStorable master = null;
    int size = storables.size();
    List<TimeWindowDefinition> definitions = new ArrayList<TimeWindowDefinition>(size);
    for( int i=0; i<storables.size(); ++i ) {
      definitions.add(null); //Plätze vorbelegen
    }
    for( TimeConstraintWindowStorable tcws : storables ) {
      if( ! name.equals(tcws.getName()) ) {
        throw new IllegalArgumentException("No consistent set of storables: different names");
      }
      int index = tcws.getSubId();
      if( index < 0 || index >= size ) {
        throw new IllegalArgumentException("No consistent set of storables: invalid subId "+index);
      }
      TimeWindowDefinition old = definitions.set(index, tcws.getDefinition() );
      if( old != null ) {
        throw new IllegalArgumentException("No consistent set of storables: duplicate subId "+index);
      }
      
      if( index == 0 ) {
        master = tcws; 
      }
    }
    if( master == null ) {
      throw new IllegalArgumentException("No consistent set of storables: no master");
    }
    
    TimeWindowDefinition timeWindowDefinition = null;
    if( definitions.size() == 1 ) {
      timeWindowDefinition = definitions.get(0);
    } else {
      timeWindowDefinition = MultiTimeWindowDefinition.construct( definitions);
    }
    return new TimeConstraintWindowDefinition(name, master.getDescription(), timeWindowDefinition, true);
  }



  
}
