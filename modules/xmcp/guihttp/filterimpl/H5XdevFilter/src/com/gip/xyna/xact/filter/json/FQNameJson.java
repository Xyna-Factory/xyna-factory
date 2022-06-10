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
package com.gip.xyna.xact.filter.json;

import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser.JsonParserUtils;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.utils.misc.JsonSerializable;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;

public class FQNameJson implements JsonSerializable {


  private String typePath;
  private String typeName;
  private String operation;
  
  public static FQNameJson ofPathAndName(String fqn) {
    int lastDotIndex = fqn.lastIndexOf('.');
    String path = fqn.substring(0, lastDotIndex);
    String simpleName = fqn.substring(lastDotIndex + 1);
    
    return new FQNameJson(path, simpleName);
  }

  // TODO: PMOD-1193
//  public static FQNameJson ofAVariable(AVariable var) {
//    if (var.isJavaBaseType()) {
//      return new FQNameJson(null, var.getJavaTypeEnum().getJavaTypeName());
//    } else {
//      return ofPathAndName(var.getFQClassName());
//    }
//  }

  public FQNameJson(String typePath, String typeName) {
    this.typePath = typePath;
    this.typeName = typeName;
  }
  
  public FQNameJson(String typePath, String typeName, String operation) {
    this.typePath = typePath;
    this.typeName = typeName;
    this.operation = operation;
  }
  
  public FQNameJson(FQNameJson fqName) {
    this.typePath = fqName.typePath;
    this.typeName = fqName.typeName;
    this.operation = fqName.operation;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((operation == null) ? 0 : operation.hashCode());
    result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
    result = prime * result + ((typePath == null) ? 0 : typePath.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    FQNameJson other = (FQNameJson) obj;
    if (operation == null) {
      if (other.operation != null)
        return false;
    } else if (!operation.equals(other.operation))
      return false;
    if (typeName == null) {
      if (other.typeName != null)
        return false;
    } else if (!typeName.equals(other.typeName))
      return false;
    if (typePath == null) {
      if (other.typePath != null)
        return false;
    } else if (!typePath.equals(other.typePath))
      return false;
    return true;
  }

  @Override
  public String toString() {
    return (typePath != null && typeName != null) ? typePath + "." + typeName: null;
  }

  public void setOperation(String operation) {
    this.operation = operation;
  }
  
  public String getTypePath() {
    return typePath;
  }
  public String getTypeName() {
    return typeName;
  }
  public String getOperation() {
    return operation;
  }
  
  
  @Override
  public void toJson(JsonBuilder jb) {
    jb.addStringAttribute(Tags.FQN, toString());
  }

  public static FQNameJson parseAttribute(FQNameJson fqName, String label, String value) throws UnexpectedJSONContentException {
    if (label.equals(Tags.FQN)) {
      FQNameJson filledFqName = fqName;
      
      int posLastColon = value.lastIndexOf('.');
      String typePath = posLastColon > 0 ? value.substring(0, posLastColon) : "";
      filledFqName = fillPath(filledFqName, typePath);
      
      String typeName = value.length() > posLastColon+1 ? value.substring(posLastColon+1, value.length()) : "";
      filledFqName = fillName(filledFqName, typeName);
      
      return filledFqName;
    } else if (label.equals(Tags.OPERATION) ) {
      JsonParserUtils.checkNotNull(label,value);
      return fillOperation( fqName, value);
    }
    
    return fqName;
  }
  
  private static FQNameJson fillPath(FQNameJson fqName, String value) {
    if( fqName == null ) {
      return new FQNameJson(value,null);
    } else {
      fqName.typePath = value;
      return fqName;
    }
  }
  
  private static FQNameJson fillName(FQNameJson fqName, String value) {
    if( fqName == null ) {
      return new FQNameJson(null,value);
    } else {
      fqName.typeName = value;
      return fqName;
    }
  }

  private static FQNameJson fillOperation(FQNameJson fqName, String value) {
    if( fqName == null ) {
      return new FQNameJson(null, null, value);
    } else {
      fqName.operation = value;
      return fqName;
    }
  }

  public static boolean useLabel(String label) {
    return (Tags.FQN.equals(label) || Tags.OPERATION.equals(label));
  }

}