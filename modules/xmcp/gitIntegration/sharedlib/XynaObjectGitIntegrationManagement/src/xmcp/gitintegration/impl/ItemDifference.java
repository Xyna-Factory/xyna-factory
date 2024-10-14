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
package xmcp.gitintegration.impl;

public class ItemDifference<T> {

  private XynaContentDifferenceType type;
  private T from;
  private T to;


  public ItemDifference() {

  }


  public ItemDifference(XynaContentDifferenceType type, T from, T to) {
    this.type = type;
    this.from = from;
    this.to = to;
  }


  public XynaContentDifferenceType getType() {
    return type;
  }


  public void setType(XynaContentDifferenceType type) {
    this.type = type;
  }


  public T getFrom() {
    return from;
  }


  public void setFrom(T from) {
    this.from = from;
  }


  public T getTo() {
    return to;
  }


  public void setTo(T to) {
    this.to = to;
  }
}
