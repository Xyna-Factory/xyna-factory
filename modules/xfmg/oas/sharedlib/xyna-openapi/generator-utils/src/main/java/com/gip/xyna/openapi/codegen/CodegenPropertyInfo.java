package com.gip.xyna.openapi.codegen;

import java.util.Map;

import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;

public interface CodegenPropertyInfo {
  
  public String getBaseName();
  public String getName();
  public boolean getIsInherited();
  public boolean getIsContainer();
  public boolean getIsPrimitiveType();
  public boolean getIsEnumRef();
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
  public Integer getMinLength();
  public Integer getMaxLength();
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
  
  public boolean getIsEnumRef() {
    return property.isEnumRef;
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

  public Integer getMinLength() {
    return property.minLength;
  }

  public Integer getMaxLength() {
    return property.maxLength;
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
  
  public boolean getIsEnumRef() {
    return parameter.isEnumRef;
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

  public Integer getMinLength() {
    return parameter.minLength;
  }

  public Integer getMaxLength() {
    return parameter.maxLength;
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