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
package com.gip.xyna.xprc.xsched.vetos.cache.cluster;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.xprc.xsched.vetos.cache.VetoCache.State;
import com.gip.xyna.xprc.xsched.vetos.cache.VetoCacheEntry;

public class VetoResponse implements Serializable {
  
  private static final long serialVersionUID = 1L;
  
  private long id;
  private List<VetoResponseEntry> responses= new ArrayList<>();
  private String failedMessage;
  
  public VetoResponse(long id) {
    this.id = id;
  }
  
  @Override
  public String toString() {
    if( isFailed() ) {
      return "VetoResponse("+id+",failed: "+failedMessage+")";
    } else {
      return "VetoResponse("+id+","+responses+")";
    }
  }
  
  public void add(VetoResponseEntry vetoResponse) {
    responses.add(vetoResponse);
  }

  public long getId() {
    return id;
  }
  
  public List<VetoResponseEntry> getResponses() {
    return responses;
  }
  
  public boolean isFailed() {
    return failedMessage != null;
  }
  
  public static VetoResponse failed(long id, String message) {
    VetoResponse vr = new VetoResponse(id);
    vr.failedMessage = String.valueOf(message);
    return vr;
  }
  
  public List<String> getVetoNames() {
    List<String> list = new ArrayList<>();
    for( VetoResponseEntry vre : responses ) { 
      list.add( vre.getName() );
    }
    return list;
  }

  
  public static class VetoResponseEntry implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Response {
      COMPARE, SUCCESS, FAILED, MISSING, IN_USE;
    }
    public enum Compare {
      ADMIN, LOCAL, REMOTE, UNUSED;
    }

    private final String name;
    private final Response response;
    private String state;
    
    private VetoResponseEntry(String name, Response response) {
      this.name = name;
      this.response = response;
    }
    private VetoResponseEntry(String name, Response response, String state) {
      this.name = name;
      this.response = response;
      this.state = state;
    }
    
    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append(response).append("(").append(name);
      if( state != null ) {
        sb.append(",").append(state);
      }
      sb.append(")");
      return sb.toString();
    }
    
    public Response getResponse() {
      return response;
    }
    
    public String getName() {
      return name;
    }

    public static VetoResponseEntry fail(String name, String message) {
      return new VetoResponseEntry(name, Response.FAILED, message);
    }
    
    public static VetoResponseEntry failMissing(String name) {
      return new VetoResponseEntry(name, Response.MISSING);
    }

    public static VetoResponseEntry success(String name) {
      return new VetoResponseEntry(name, Response.SUCCESS);
    }
    
    public static VetoResponseEntry failUnexpectedState(VetoCacheEntry veto) {
      return new VetoResponseEntry(veto.getName(), Response.FAILED, String.valueOf(veto.getState()) );
    }
    
    public static VetoResponseEntry inUse(String name) {
      return new VetoResponseEntry(name, Response.IN_USE);
    }
    public static VetoResponseEntry inUse(String name, State state) {
      return new VetoResponseEntry(name, Response.IN_USE, String.valueOf(state) );
    }

    public static VetoResponseEntry compare(String name, Compare compare) {
      return new VetoResponseEntry(name, Response.COMPARE, compare.toString() );
    }
    
    public Compare getCompareResult() {
      if( response == Response.COMPARE ) {
        for( Compare c : Compare.values() ) {
          if( state.startsWith( c.toString() ) ) {
            return c;
          }
        }
      }
      return null;
    }
    
  }

}
