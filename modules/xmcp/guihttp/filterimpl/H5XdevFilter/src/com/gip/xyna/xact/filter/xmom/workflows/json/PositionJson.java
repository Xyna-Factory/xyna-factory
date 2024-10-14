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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;

public abstract class PositionJson extends XMOMGuiJson {

  public enum RelativePosition {
    inside, left, right, top, bottom;
  }  
  
  private String relativeTo;
  private RelativePosition relativePosition = RelativePosition.inside;
  private Integer insideIndex;
  
  private Integer requestInsideIndex;
  private Integer nextInsideIndex;
 
  protected PositionJson() {
  }
  
  public PositionJson(String relativeTo, RelativePosition relativePosition) {
    this.relativeTo = relativeTo;
    this.relativePosition = relativePosition;
  }
  
  public PositionJson(String relativeTo, int insideIndex) {
    this.relativeTo = relativeTo;
    this.relativePosition = RelativePosition.inside;
    this.insideIndex = insideIndex;
  }
  
  public void setInsideIndex(int insideIndex) {
    this.insideIndex = insideIndex;
  }
  public void setRelativePosition(RelativePosition relativePosition) {
    this.relativePosition = relativePosition;
  }
  public void setRelativeTo(String relativeTo) {
    this.relativeTo = relativeTo;
  }
  
  public String getRelativeTo() {
    return relativeTo;
  }
  
  public RelativePosition getRelativePosition() {
    return relativePosition;
  }
  
  public Integer getInsideIndex() {
    return insideIndex;
  }
  
  protected static abstract class PositionJsonVisitor<T extends PositionJson> extends EmptyJsonVisitor<T> {
    protected T pj;

    protected PositionJsonVisitor() {
      pj = create();
    }
    
    @Override
    public T get() {
      return pj;
    }
    
    @Override
    public T getAndReset() {
      T ret = get();
      pj = create();
      return ret;
    }

    protected abstract T create();

    @Override
    public void attribute(String label, String value, Type type) {
      if( label.equals("targetId") ) {
        pj.setRelativeTo( value );
        return;
      }
      if( label.equals("relativePosition") ) {
        pj.setRelativePosition( RelativePosition.valueOf(value) );
        return;
      }
      if( label.equals("index") ) {
        pj.setInsideIndex( Integer.valueOf(value) );
        pj.setRequestInsideIndex(pj.getInsideIndex());
        return;
      }
    }
  }

  
  public Integer getRequestInsideIndex() {
    return requestInsideIndex;
  }

  
  public void setRequestInsideIndex(Integer oldInsideIndex) {
    this.requestInsideIndex = oldInsideIndex;
  }

  
  public Integer getNextInsideIndex() {
    return nextInsideIndex;
  }

  
  public void setNextInsideIndex(Integer lastInsideIndex) {
    this.nextInsideIndex = lastInsideIndex;
  }

}
