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

package com.gip.xyna.xnwh.persistence.javaserialization;

import java.io.Serializable;
import java.lang.reflect.Type;


public class ColumnObject implements Serializable {

  /**
   * 
   */
  private static final long serialVersionUID = -5778908972579337808L;
  
  
  private long size;
  private Type type;
  private String name;
  
  public ColumnObject() {
  }
  
  public ColumnObject(String name, Type type, long size) {
    this.name = name;
    this.type = type;
    this.size = size;
  }

  
  public long getSize() {
    return size;
  }

  
  public void setSize(long size) {
    this.size = size;
  }

  
  public Type getType() {
    return type;
  }

  
  public void setType(Type type) {
    this.type = type;
  }

  
  public String getName() {
    return name;
  }

  
  public void setName(String name) {
    this.name = name;
  }
  
  
  @Override
  public String toString() {
    return "Name: " + name + " | Type: " + type.toString() + " | Size: " + size;
  }
  
}
