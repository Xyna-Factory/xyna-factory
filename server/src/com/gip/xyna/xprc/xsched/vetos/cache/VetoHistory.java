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
package com.gip.xyna.xprc.xsched.vetos.cache;

import com.gip.xyna.utils.collections.RepeationCountingRingBuffer;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyInt;
import com.gip.xyna.xprc.xsched.vetos.VetoInformation;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;
import com.gip.xyna.xprc.xsched.vetos.cache.cluster.VetoRequest.VetoRequestEntry.Action;

public abstract class VetoHistory {

  public static final XynaPropertyInt HISTORY_SIZE = 
      new XynaPropertyInt("xprc.veto.cache_history_size", 0).
      setDefaultDocumentation(DocumentationLanguage.EN, "Size of state-history in each veto in cache: 0, 1, n").
      setDefaultDocumentation(DocumentationLanguage.DE, "L�nge der Status-Historie in jedem Veto im Cache: 0, 1, n");

  private static final VetoHistory NONE = new VetoHistory() {
    @Override
    public StringBuilder appendTo(StringBuilder sb, String sep) {
      return sb;
    }
  };

  public static VetoHistory create(State state) {
    VetoHistory vh = instantiate();
    vh.update( State.None, state);
    return vh;
  }

  private static VetoHistory instantiate() {
    int size = HISTORY_SIZE.get();
    if( size == 0 ) {
      return NONE;
    } else if( size > 1 ) {
      return new VetoHistoryBuffer(size);
    } else {
      return new VetoHistoryLast();
    }
  }

  public static VetoHistory createReplicated(State state, VetoInformation vetoInformation) {
    VetoHistory vh = instantiate();
    vh.append("Replicated:"+state);
    if( state == State.Scheduled ) {
      vh.binding(vetoInformation.getBinding());
    }
    return vh;
  }


  public void binding(int binding) {}
  public void append(String string) {}
  public void update(State last, State current) {}
  public void remoteActionStart(Action action, long requestId, int binding) {}
  public void remoteActionEnd(int binding, String message) {}

  public abstract StringBuilder appendTo(StringBuilder sb, String sep);

  @Override
  public String toString() {
    return appendTo(new StringBuilder(), "").toString();
  }


  public static class VetoHistoryLast extends VetoHistory {
    private State lastState = State.None;

    @Override
    public void update(State last, State current) {
      lastState = last;
    }

    @Override
    public String toString() {
      return "History("+lastState+")";
    }

    @Override
    public StringBuilder appendTo(StringBuilder sb, String sep) {
      sb.append(sep).append("History(").append(lastState).append(")");
      return sb;
    }

  }

  public static class VetoHistoryBuffer extends VetoHistory {
    private RepeationCountingRingBuffer<String> buffer;
    private StringBuilder remoteAction;
    private int raCounter;
    
    public VetoHistoryBuffer(int size) {
      buffer = new RepeationCountingRingBuffer<>(size);
    }

    @Override
    public void binding(int binding) {
      add("B"+binding);
    }

    @Override
    public void append(String string) {
      add(string);
    }

    @Override
    public void update(State last, State current) {
      add(current.name());
    }

    @Override
    public void remoteActionStart(Action action, long requestId, int binding) {
      synchronized ( this ) {
        if( remoteAction == null ) {
         remoteAction = new StringBuilder();
         raCounter = 0;
        }
        ++raCounter;
        remoteAction.append("["+binding+action+requestId);
      }
    }

    @Override
    public void remoteActionEnd(int binding, String message) {
      synchronized ( this ) {
        if( remoteAction == null ) {
          buffer.add("RemoteActionEnd-"+message+"-"+binding+"]");
        } else {
          remoteAction.append("-"+message+"-"+binding+"]");
          --raCounter;
          if( raCounter == 0 ) {
            buffer.add(remoteAction.toString());
            remoteAction = null;
          }
        }
      }
    }

    private void add(String string) {
      synchronized ( this ) {
        if( remoteAction != null ) {
          remoteAction.append("-").append(string);
        } else {
          buffer.add(string);
        }
      }
    }

    @Override
    public StringBuilder appendTo(StringBuilder sb, String sep) {
      sb.append(sep);
      String sepBuff = "HistoryBuffer(";
      for( RepeationCountingRingBuffer.Entry<String> e : buffer.getEntries() ) {

        sb.append(sepBuff).append(e.getValue());
        if( e.getCount() > 1 ) {
          sb.append("(*").append(e.getCount()).append(")");
        }
        sepBuff = "->";
      }
      synchronized ( this ) {
        if( remoteAction != null ) {
          sb.append(sepBuff).append(remoteAction.toString());
        }
      }
      sb.append(")");
      return sb;
    }
  }

}