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
package com.gip.xyna.xprc.xfractwfe.formula;



public abstract class Operator {

  private final int lastIdx;
  private boolean needsEquals = false;


  protected Operator(int lastIdx) {
    this.lastIdx = lastIdx;
  }


  public String toJavaCode() {
    return getOperatorAsString();
  }
  
  public abstract String getOperatorAsString();


  public void setNeedsEquals(boolean b) {
    needsEquals = b;
  }


  public boolean needsEquals() {
    return needsEquals;
  }


  public int getLastIdx() {
    return lastIdx;
  }


  public boolean needsClosingBrace() {
    return false;
  }


  public String getPrefix() {
    return "";
  }

}
