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
package com.gip.xyna.xfmg.xfctrl.datamodel.xsd.types;


/**
 */
public enum MemberType {
  
  Element(false,true), 
  Attribute(true,false), 
  Text(false,true),
  Choice(false,true),
  Any(false,true);
  
  
  //defaultwerte
  private boolean optional;
  private boolean qualified;

  private MemberType(boolean optional, boolean qualified) {
    this.optional = optional;
    this.qualified = qualified;
  }

  public boolean isOptional() {
    return optional;
  }
  public boolean isQualified() {
    return qualified;
  }

  public String form(boolean qualified) {
    if( this.qualified == qualified ) {
      return null;
    }
    return qualified ? "qualified" : "unqualified";
  }

  public String usage(boolean optional) {
    if( this.optional == optional ) {
      return null;
    }
    return optional ? "optional" : "required";
  }

  public boolean isOptional(String attribute) {
    if( "optional".equals(attribute) ) {
      return true;
    } else if( "required".equals(attribute) ) {
      return false;
    } else {
      return optional;
    }
  }

  public boolean isQualified(String attribute) {
    if( "qualified".equals(attribute) ) {
      return true;
    } else if( "unqualified".equals(attribute) ) {
      return false;
    } else {
      return qualified;
    }
  }

}
