package com.gip.xyna.openapi.codegen;

import java.util.Objects;

import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.DefaultCodegen;

public class XynaCodegenParameter extends XynaCodegenProperty {

  final int index;

  XynaCodegenParameter(CodegenParameter parameter, DefaultCodegen gen, String className, int index) {
    super(new CodegenParameterHolder(parameter), gen, className);
    
    this.index = index;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!super.equals(o)) return false;
    if (!(o instanceof XynaCodegenParameter)) return false;
    XynaCodegenParameter that = (XynaCodegenParameter) o;
    return
        index == that.index;
  }
  
  @Override
  public int hashCode() {
        int hash = super.hashCode();
        hash = 89 * Objects.hash(index); 
        return hash;   

  }
  
  @Override
  protected void toString(StringBuilder sb) {
    if (sb == null) {
      return;
    }
    super.toString(sb);
    sb.append(",\n    ").append("index='").append(index).append('\'');
  }
}
