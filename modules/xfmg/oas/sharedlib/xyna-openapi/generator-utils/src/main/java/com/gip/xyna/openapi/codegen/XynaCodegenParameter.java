package com.gip.xyna.openapi.codegen;

import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.CamelizeOption.UPPERCASE_FIRST_CHAR;
import static org.openapitools.codegen.utils.StringUtils.camelize;

import java.util.Objects;

import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.DefaultCodegen;

public class XynaCodegenParameter {

  final String paramLabel;
  final String paramVarName;
  final String getParamVarName;
  final String setParmaVarName;
  final String description;

  final boolean isList;

  final boolean isPrimitive;
  // primitive
  final String datatype;

  // not primitive
  final String paramRefName;
  final String paramRefPath;

  final boolean isPathParam;
  final boolean isQueryParam;
  final boolean isHeaderParam;
  final boolean isBodyParam;

  XynaCodegenParameter(CodegenParameter parameter, DefaultCodegen gen) {
    paramLabel = parameter.baseName;
    paramVarName = camelize(parameter.paramName.replace(" ", "_"), LOWERCASE_FIRST_LETTER);
    getParamVarName = "get" + camelize(paramVarName.replace(" ", "_"), UPPERCASE_FIRST_CHAR) + "()";
    setParmaVarName = "set" + camelize(paramVarName.replace(" ", "_"), UPPERCASE_FIRST_CHAR);
    
    isList = parameter.isContainer;
    
    isPrimitive = parameter.isPrimitiveType || parameter.isEnumRef;
    
    datatype = buildDatatype(parameter);
    
    if (!isPrimitive) {
      paramRefName = camelize(parameter.getComplexType().replace(" ", "_"), UPPERCASE_FIRST_CHAR);
      paramRefPath = gen.modelPackage();
    } else {
      paramRefName = null;
      paramRefPath = null;
    }
    isPathParam = parameter.isPathParam;
    isQueryParam = parameter.isQueryParam;
    isHeaderParam = parameter.isHeaderParam;
    isBodyParam = parameter.isBodyParam;
    
    description = buildDescription(parameter);
  }
  
  public String buildDatatype(CodegenParameter parameter) {
    if (isPrimitive) {
      if (parameter.isEnumRef) {  
        return "String";
      } else if (isList) {
        return parameter.items.dataType;
      } else {
        return parameter.dataType;
      }
    } else {
      return null;
    }
  }
  
  private String buildDescription(CodegenParameter parameter) {
    StringBuilder sb = new StringBuilder();
    if (parameter.description != null) {
      sb.append(parameter.description).append('\n');
    }
    if (parameter.isEnumRef) {
      sb.append("Enum of Type: ");
      sb.append(paramRefPath).append('.').append(paramRefName);
      sb.append('\n');
      sb.append("values: ");
      sb.append(String.join(", ", parameter.allowableValues.keySet()));
      sb.append('\n');
    }
    if (parameter.getFormat() != null) {
      sb.append("format: ");
      sb.append(parameter.getFormat()).append('\n');
    }
    if (parameter.required) {
      sb.append("required").append('\n');
    }
    sb.append("        ");
    return sb.toString();
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof XynaCodegenParameter)) return false;
    XynaCodegenParameter that = (XynaCodegenParameter) o;
    return
        Objects.equals(paramLabel, that.paramLabel) &&
        Objects.equals(paramVarName, that.paramVarName) &&
        Objects.equals(getParamVarName, that.getParamVarName) &&
        Objects.equals(setParmaVarName, that.setParmaVarName) &&
        Objects.equals(description, that.description) &&

        isList == that.isList &&
        Objects.equals(datatype, that.datatype) &&
        isPrimitive == that.isPrimitive &&
        Objects.equals(paramRefName, that.paramRefName) &&
        Objects.equals(paramRefPath, that.paramRefPath) &&

        isPathParam == that.isPathParam &&
        isQueryParam == that.isQueryParam &&
        isHeaderParam == that.isHeaderParam &&
        isBodyParam == that.isBodyParam;
  }
  
  @Override
  public int hashCode() {
      return Objects.hash(paramLabel, paramVarName, getParamVarName, setParmaVarName, description, 
                          isList, datatype, isPrimitive, paramRefName, paramRefPath,
                          isPathParam, isQueryParam, isHeaderParam, isBodyParam);
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder("XynaCodegenParameter{");
      sb.append("\n    ").append("paramLabel='").append(paramLabel).append('\'');
      sb.append(",\n    ").append("paramVarName='").append(paramVarName).append('\'');
      sb.append(",\n    ").append("getParamVarName='").append(getParamVarName).append('\'');
      sb.append(",\n    ").append("setParmaVarName='").append(setParmaVarName).append('\'');
      sb.append(",\n    ").append("description='").append(String.valueOf(description).replace("\n", "\\n")).append('\'');
      sb.append(",\n    ").append("isList='").append(isList).append('\'');
      sb.append(",\n    ").append("datatype='").append(datatype).append('\'');
      sb.append(",\n    ").append("isPrimitive='").append(isPrimitive).append('\'');
      sb.append(",\n    ").append("paramRefName='").append(paramRefName).append('\'');
      sb.append(",\n    ").append("paramRefPath='").append(paramRefPath).append('\'');
      sb.append(",\n    ").append("isPathParam='").append(isPathParam).append('\'');
      sb.append(",\n    ").append("isQueryParam='").append(isQueryParam).append('\'');
      sb.append(",\n    ").append("isHeaderParam='").append(isHeaderParam).append('\'');
      sb.append(",\n    ").append("isBodyParam='").append(isBodyParam).append('\'');
      sb.append("\n}");
      return sb.toString();
  }
}
