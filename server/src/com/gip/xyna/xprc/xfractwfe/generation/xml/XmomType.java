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
package com.gip.xyna.xprc.xfractwfe.generation.xml;


/**
 *
 */
public class XmomType {
  
  private final String path;
  private final String name;
  private final String label;
  private final boolean isAbstract;


  public XmomType(String path, String name, String label) {
    this(path, name, label, false);
  }

  public XmomType(String path, String name, String label, boolean isAbstract) {
    this.path = path;
    this.name = name;
    this.label = label;
    this.isAbstract = isAbstract;
  }

  @Override
  public String toString() {
    if( name.equals(label) ) {
      return "XmomType("+path+","+name+")";
    } else {
      return "XmomType("+path+","+name+","+label+")";
    }
  }
  
  public String getName() {
    return name;
  }
  
  public String getPath() {
    return path;
  }

  public String getLabel() {
    return label;
  }

  public String getFQTypeName() {
    return path+"."+name;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public static XmomType ofFQTypeName( String fqTypeName) {
    int idx = fqTypeName.lastIndexOf('.');
    if( idx == -1 ) {
      return new XmomType("", fqTypeName, fqTypeName);
    } else {
      String path = fqTypeName.substring(0,idx);
      String name = fqTypeName.substring(idx+1);
      return new XmomType(path, name, name);
    }
  }

  public com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType toXynaObject() {
    return new com.gip.xyna.xfmg.xfctrl.datamodelmgmt.xynaobjects.XmomType(path,name,label) ;
  }

}
