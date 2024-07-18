/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package com.gip.xyna.openapi.codegen.factory;

import java.util.HashMap;
import java.util.Map;

import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;

import com.gip.xyna.openapi.codegen.AdditionalPropertyWrapper;

public interface CodegenPropertyInfo {
  
  public String getBaseName();
  public String getName();
  public boolean getIsInherited();
  public boolean getIsContainer();
  public boolean getIsPrimitiveType();
  public boolean getIsEnumOrRef();
  public boolean getIsString();
  public boolean getIsNumber();
  public boolean getIsInteger();
  public String getComplexType();
  public String getOpenApiType();
  public CodegenPropertyInfo getItems();
  public CodegenPropertyInfo getMostInnerItems();
  public String getDataType();
  public String getDescription();
  public String getFormat();
  public String getMinimum();
  public String getMaximum();
  public boolean getExclusiveMinimum();
  public boolean getExclusiveMaximum();
  public Number getMultipleOf();
  public String getDataFormat();
  public String getPattern();
  public Integer getMinLength();
  public Integer getMaxLength();
  public Integer getMinItems();
  public Integer getMaxItems();
  public boolean getRequired();
  public boolean getIsNullable();
  public Map<String, Object> getAllowableValues();
}

class CodegenPropertyHolder implements CodegenPropertyInfo{
  
  private CodegenProperty property;
  private CodegenPropertyInfo items;
  private CodegenPropertyInfo mostInnerItems;

  CodegenPropertyHolder(CodegenProperty property) {
    this.property = property;
  }

  public String getBaseName() {
    return property.baseName;
  }
  
  public String getName() {
    return property.name;
  }
  
  public boolean getIsInherited() {
    return property.isInherited;
  }
  
  public boolean getIsContainer() {
    return property.isContainer;
  }
  
  public boolean getIsPrimitiveType() {
    return property.isPrimitiveType;
  }
  
  public boolean getIsEnumOrRef() {
    return property.getIsEnumOrRef();
  }
  
  public String getComplexType() {
    return property.getComplexType();
  }
  
  public CodegenPropertyInfo getItems() {
    if (items == null && property.items != null) {
      items = new CodegenPropertyHolder(property.items);
    }
    return items;
  }
  
  public CodegenPropertyInfo getMostInnerItems() {
    if (mostInnerItems == null && property.mostInnerItems != null) {
      mostInnerItems = new CodegenPropertyHolder(property.mostInnerItems);
    }
    return mostInnerItems;
  }
  
  public String getDataType() {
    return property.dataType;
  }
  
  public String getDescription() {
    return property.description;
  }
  
  public String getFormat() {
    return property.getFormat();
  }
  
  public String getMinimum() {
    return property.minimum;
  }
  
  public String getMaximum() {
    return property.maximum;
  }
  
  public boolean getExclusiveMinimum() {
    return property.exclusiveMinimum;
  }
  
  public boolean getExclusiveMaximum() {
    return property.exclusiveMaximum;
  }
  
  public Number getMultipleOf() {
    return property.multipleOf;
  }
  
  public String getDataFormat() {
    return property.dataFormat;
  }

  public String getPattern() {
      return property.pattern;
  }

  public Integer getMinLength() {
    return property.minLength;
  }

  public Integer getMaxLength() {
    return property.maxLength;
  }
  
  public Integer getMinItems() {
    return property.minItems;
  }
  
  public Integer getMaxItems() {
    return property.maxItems;
  }
  
  public boolean getRequired() {
    return property.required;
  }

  public boolean getIsNullable() {
    return property.isNullable;
  }

  public Map<String, Object> getAllowableValues() {
    return property.allowableValues;
  }

  public boolean getIsString() {
    return property.getIsString();
  }

  public boolean getIsNumber() {
    return property.getIsNumber();
  }

  public boolean getIsInteger() {
    return property.getIsInteger();
  }

  public String getOpenApiType() {
    return property.getOpenApiType();
  }
}

class CodegenParameterHolder implements CodegenPropertyInfo{
  
  private CodegenParameter parameter;
  private CodegenPropertyInfo items;
  private CodegenPropertyInfo mostInnerItems;
 
  CodegenParameterHolder(CodegenParameter parameter) {
    this.parameter = parameter;
  }

  public String getBaseName() {
    return parameter.baseName;
  }
  
  public String getName() {
    return parameter.paramName;
  }
  
  public boolean getIsInherited() {
    return false;
  }
  
  public boolean getIsContainer() {
    return parameter.isContainer;
  }
  
  public boolean getIsPrimitiveType() {
    return parameter.isPrimitiveType;
  }
  
  public boolean getIsEnumOrRef() {
    return parameter.getIsEnumOrRef();
  }
  
  public String getComplexType() {
    return parameter.getComplexType();
  }
  
  public CodegenPropertyInfo getItems() {
    if (items == null && parameter.items != null) {
      items = new CodegenPropertyHolder(parameter.items);
    }
    return items;
  }
  
  public CodegenPropertyInfo getMostInnerItems() {
    if (mostInnerItems == null && parameter.mostInnerItems != null) {
      mostInnerItems = new CodegenPropertyHolder(parameter.mostInnerItems);
    }
    return mostInnerItems;
  }
  
  public String getDataType() {
    return parameter.dataType;
  }
  
  public String getDescription() {
    return parameter.description;
  }
  
  public String getFormat() {
    return parameter.getFormat();
  }
  
  public String getMinimum() {
    return parameter.minimum;
  }
  
  public String getMaximum() {
    return parameter.maximum;
  }
  
  public boolean getExclusiveMinimum() {
    return parameter.exclusiveMinimum;
  }
  
  public boolean getExclusiveMaximum() {
    return parameter.exclusiveMaximum;
  }
  
  public Number getMultipleOf() {
    return parameter.multipleOf;
  }
  
  public String getDataFormat() {
    return parameter.dataFormat;
  }

  public String getPattern() {
    return parameter.pattern;
  }

  public Integer getMinLength() {
    return parameter.minLength;
  }

  public Integer getMaxLength() {
    return parameter.maxLength;
  }

  public Integer getMinItems() {
  return parameter.minItems;
  }

  public Integer getMaxItems() {
    return parameter.maxItems;
  }
  
  public boolean getRequired() {
    return parameter.required;
  }

  public boolean getIsNullable() {
    return parameter.isNullable;
  }

  public Map<String, Object> getAllowableValues() {
    return parameter.allowableValues;
  }

  public boolean getIsString() {
    return parameter.isString;
  }

  public boolean getIsNumber() {
    return parameter.isNumber;
  }

  public boolean getIsInteger() {
    return parameter.isInteger;
  }

  public String getOpenApiType() {
    return parameter.getBaseType();
  }
  
}

class CodegenEnum implements CodegenPropertyInfo{
   
  private Map<String, Object> allowableValues;
  
  CodegenEnum(Map<String, Object> allowableValues) {
    this.allowableValues = allowableValues;
  }
  
  public String getBaseName() {
    return "Value";
  }
  
  public String getName() {
    return "value";
  }
  
  public boolean getIsInherited() {
    return false;
  }
  
  public boolean getIsContainer() {
    return false;
  }
  
  public boolean getIsPrimitiveType() {
    return true;
  }
  
  public boolean getIsEnumOrRef() {
    return true;
  }
  
  public String getComplexType() {
    return null;
  }
  
  public CodegenPropertyInfo getItems() {
    return null;
  }
  
  public CodegenPropertyInfo getMostInnerItems() {
    return null;
  }
  
  public String getDataType() {
    return "Enum";
  }
  
  public String getDescription() {
    return "";
  }
  
  public String getFormat() {
    return null;
  }
  
  public String getMinimum() {
    return null;
  }
  
  public String getMaximum() {
    return null;
  }
  
  public boolean getExclusiveMinimum() {
    return false;
  }
  
  public boolean getExclusiveMaximum() {
    return false;
  }
  
  public Number getMultipleOf() {
    return null;
  }
  
  public String getDataFormat() {
    return null;
  }

  public String getPattern() {
    return null;
  }

  public Integer getMinLength() {
    return null;
  }

  public Integer getMaxLength() {
    return null;
  }

  public Integer getMinItems() {
    return null;
  }

  public Integer getMaxItems() {
    return null;
  }

  public boolean getRequired() {
    return false;
  }

  public boolean getIsNullable() {
    return false;
  }

  public Map<String, Object> getAllowableValues() {
    return allowableValues;
  }

  public boolean getIsString() {
    return true;
  }

  public boolean getIsNumber() {
    return false;
  }

  public boolean getIsInteger() {
    return false;
  }

  public String getOpenApiType() {
    return "Enum";
  }
}

class AdditionalProperty implements CodegenPropertyInfo{
  
  private AdditionalPropertyWrapper wrapper;
  private boolean isList;
  private CodegenPropertyInfo item;
  
  AdditionalProperty(AdditionalPropertyWrapper wrapper) {
    this(wrapper, true);
  }
  
  private AdditionalProperty(AdditionalPropertyWrapper wrapper, boolean isList) {
    this.wrapper = wrapper;
    this.isList = isList;
  }
  
  public String getBaseName() {
    return "Addional Properties";
  }
  
  public String getName() {
    return "addionalProperties";
  }
  
  public boolean getIsInherited() {
    return false;
  }
  
  public boolean getIsContainer() {
    return isList;
  }
  
  public boolean getIsPrimitiveType() {
    return false;
  }
  
  public boolean getIsEnumOrRef() {
    return false;
  }
  
  public String getComplexType() {
    return wrapper.typeName;
  }
  
  public CodegenPropertyInfo getItems() {
    if (isList && item == null) {
      item = new AdditionalProperty(wrapper, false);
    }
    return item;
  }
  
  public CodegenPropertyInfo getMostInnerItems() {
    if (isList && item == null) {
      item = new AdditionalProperty(wrapper, false);
    }
    return item;
  }
  
  public String getDataType() {
    return wrapper.typeName;
  }
  
  public String getDescription() {
    return "Reference to additional property wrapper.";
  }
  
  public String getFormat() {
    return null;
  }
  
  public String getMinimum() {
    return null;
  }
  
  public String getMaximum() {
    return null;
  }
  
  public boolean getExclusiveMinimum() {
    return false;
  }
  
  public boolean getExclusiveMaximum() {
    return false;
  }
  
  public Number getMultipleOf() {
    return null;
  }
  
  public String getDataFormat() {
    return null;
  }

  public String getPattern() {
    return null;
  }

  public Integer getMinLength() {
    return null;
  }

  public Integer getMaxLength() {
    return null;
  }

  public Integer getMinItems() {
    return null;
  }

  public Integer getMaxItems() {
    return null;
  }

  public boolean getRequired() {
    return false;
  }

  public boolean getIsNullable() {
    return false;
  }

  public Map<String, Object> getAllowableValues() {
    return new HashMap<>();
  }

  public boolean getIsString() {
    return false;
  }

  public boolean getIsNumber() {
    return false;
  }

  public boolean getIsInteger() {
    return false;
  }

  public String getOpenApiType() {
    return wrapper.typeName;
  }
}