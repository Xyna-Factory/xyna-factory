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
package com.gip.xyna.xprc.xpce.ordersuspension;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xprc.XynaOrderServerExtension;


/**
 *
 */
public class ResumeTarget implements StringSerializable<ResumeTarget>, Serializable {

  private static final long serialVersionUID = 1L;
  private static final String PATTERN_STRING = "ResumeTarget\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*([^)\\s]+)\\s*\\)";
  private static final Pattern PATTERN = Pattern.compile(PATTERN_STRING);
  //"ExecutionPeriod\\((\\w+),(\\d+),?(\\d+)?\\)";
  
  
  private final Long rootId;
  private final Long orderId;
  private final String laneId; //kann null sein

  public ResumeTarget(Long rootId, Long orderId) {
    this.rootId = rootId;
    this.orderId = orderId;
    laneId = null;
  }
  
  public ResumeTarget(Long rootId, Long orderId, String laneId) {
    this.rootId = rootId;
    this.orderId = orderId;
    this.laneId = laneId;
  }
 
  public ResumeTarget(XynaOrderServerExtension xo) {
    this.rootId = xo.getRootOrder().getId();
    this.orderId = xo.getId();
    laneId = null;
  }

  public ResumeTarget(XynaOrderServerExtension xo, String laneId) {
    this.rootId = xo.getRootOrder().getId();
    this.orderId = xo.getId();
    this.laneId = laneId;
  }

  
  public Long getRootId() {
    return rootId;
  }
  
  public Long getOrderId() {
    return orderId;
  }
  
  public String getLaneId() {
    return laneId;
  }
  
  @Override
  public String toString() {
    return "ResumeTarget("+rootId+","+orderId+","+laneId+")";
  }

  public ResumeTarget deserializeFromString(String string) {
    return valueOf(string);
  }

  /**
   * @param string
   * @return
   */
  public static ResumeTarget valueOf(String string) {
    if( string == null ) {
      return null;
    }
    Matcher m = PATTERN.matcher(string);
    if( m.matches() ) {
      try {
        Long rootOrderId = Long.valueOf(m.group(1));
        Long orderId = Long.valueOf(m.group(2));
        String laneId = m.group(3);
        return new ResumeTarget(rootOrderId,orderId,laneId);
      } catch( Exception e ) {
        throw new IllegalArgumentException("Input \""+string+"\" could not be parsed successfully: "+e.getClass().getSimpleName()+" "+e.getMessage(), e );
      }
    } else {
      throw new IllegalArgumentException("Input \""+string+"\" does not match regexp \""+PATTERN_STRING+"\"");
    }
  }

  public String serializeToString() {
    return "ResumeTarget("+rootId+","+orderId+","+laneId+")";
  }

  /**
   * @return
   */
  public static Transformation<ResumeTarget, Long> transformationGetRootId() {
    return new GetRootId();
  }
  public static class GetRootId implements Transformation<ResumeTarget, Long> {
    public Long transform(ResumeTarget from) {
      return from.getRootId();
    }
  }
  
}
