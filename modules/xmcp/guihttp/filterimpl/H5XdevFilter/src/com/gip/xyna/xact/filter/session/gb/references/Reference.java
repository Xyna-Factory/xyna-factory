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

package com.gip.xyna.xact.filter.session.gb.references;



import java.io.Serializable;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.FQName;



public class Reference implements Serializable {

  private static final long serialVersionUID = 1L;
  
  private final FQName fqName;
  private final String label;
  private final ReferenceType referenceType;
  private final Type objectType;


  public Reference(FQName fqName, String label, ReferenceType referenceType, Type objectType) {
    this.fqName = fqName;
    this.label = label;
    this.referenceType = referenceType;
    this.objectType = objectType;
  }


  public FQName getFqName() {
    return fqName;
  }


  public ReferenceType getReferenceType() {
    return referenceType;
  }


  public Type getObjectType() {
    return objectType;
  }


  public String getLabel() {
    return label;
  }
}
