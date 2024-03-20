package com.gip.xyna.openapi.codegen;

import java.util.Objects;

import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.DefaultCodegen;

public class XynaCodegenParameter extends XynaCodegenProperty {

  final boolean isPathParam;
  final boolean isQueryParam;
  final boolean isHeaderParam;
  final boolean isBodyParam;

  XynaCodegenParameter(CodegenParameter parameter, DefaultCodegen gen, String className) {
    super(new CodegenParameterHolder(parameter), gen, className);
    
    isPathParam = parameter.isPathParam;
    isQueryParam = parameter.isQueryParam;
    isHeaderParam = parameter.isHeaderParam;
    isBodyParam = parameter.isBodyParam;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!super.equals(o)) return false;
    if (!(o instanceof XynaCodegenParameter)) return false;
    XynaCodegenParameter that = (XynaCodegenParameter) o;
    return
        isPathParam == that.isPathParam &&
        isQueryParam == that.isQueryParam &&
        isHeaderParam == that.isHeaderParam &&
        isBodyParam == that.isBodyParam;
  }
  
  @Override
  public int hashCode() {
        int hash = super.hashCode();
        hash = 89 * Objects.hash(isPathParam, isQueryParam, isHeaderParam, isBodyParam); 
        return hash;   

  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder(super.toString().replace("XynaCodegenProperty", "XynaCodegenParameter"));
      sb.delete(sb.length() - 2, sb.length());

      sb.append(",\n    ").append("isPathParam='").append(isPathParam).append('\'');
      sb.append(",\n    ").append("isQueryParam='").append(isQueryParam).append('\'');
      sb.append(",\n    ").append("isHeaderParam='").append(isHeaderParam).append('\'');
      sb.append(",\n    ").append("isBodyParam='").append(isBodyParam).append('\'');
      sb.append("\n}");
      return sb.toString();
  }
}
