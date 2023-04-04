/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.db;

import java.util.EnumMap;

import com.gip.xyna.demon.DemonPersistency;
import com.gip.xyna.demon.persistency.PersistableCounter;

import dhcpAdapterDemon.types.DhcpAction;
import dhcpAdapterDemon.types.State;

/**
 *
 */
public abstract class AbstractDBFillerStatistics implements DBFillerStatistics {

  protected static class StateCounter {
    
    private EnumMap<State,PersistableCounter> counters = new EnumMap<State,PersistableCounter>(State.class);
    private String requestName;
    
    public StateCounter(String dbName, String requestName) {
      this.requestName = requestName;
      DemonPersistency dp = DemonPersistency.getInstance(); 
      for( State s : State.values() ) {
        PersistableCounter pc = new PersistableCounter(dbName + "."+requestName+"."+s.toString());
        counters.put( s, pc );
        dp.registerPersistable(pc);
      }
    }
    
    public void count( State state) {
      counters.get(state).increment();
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(requestName).append("(");
      sb.append("requests=").append(counters.get(State.REQUESTED).getCounter());
      sb.append(",succeeded=").append(counters.get(State.SUCCEEDED).getCounter());
      sb.append(",unexpected=").append(counters.get(State.UNEXPECTED).getCounter());
      sb.append(",failed=").append(counters.get(State.FAILED).getCounter());
      sb.append(",rejected=").append(counters.get(State.REJECTED).getCounter());
      sb.append(",rollbacked=").append(counters.get(State.ROLLBACKED).getCounter());
      sb.append(",ignored=").append(counters.get(State.IGNORED).getCounter());
      sb.append(")");
      return sb.toString();
    }

    public int get( State state) {
      return counters.get(state).getCounter();
    }
  }
  protected EnumMap<DhcpAction, StateCounter> counters = new EnumMap< DhcpAction, StateCounter>(DhcpAction.class);
   
  /**
   * @param dbFiller
   */
  public AbstractDBFillerStatistics(String name) {
    for( DhcpAction a : DhcpAction.values() ) {
      counters.put( a, new StateCounter( name, a.toString() ) );
    }
  }

  public int getCounter(DhcpAction dhcpAction, State state) {
    return counters.get(dhcpAction).get(state);
  }
  
  public String getCountersAsString(DhcpAction dhcpAction) {
    return counters.get(dhcpAction).toString();
  }
  
  public void state(DhcpAction action,State state) {
    counters.get(DhcpAction.ALL).count(state);
    counters.get(action).count(state);
  }  

}
