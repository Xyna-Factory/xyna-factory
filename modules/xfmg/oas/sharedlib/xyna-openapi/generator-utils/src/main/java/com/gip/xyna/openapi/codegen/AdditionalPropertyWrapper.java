package com.gip.xyna.openapi.codegen;

import java.util.Objects;

import org.openapitools.codegen.DefaultCodegen;

import com.gip.xyna.openapi.codegen.utils.Camelizer;
import com.gip.xyna.openapi.codegen.utils.Camelizer.Case;
import com.gip.xyna.openapi.codegen.utils.Sanitizer;

public class AdditionalPropertyWrapper {

  public final String typePath;
  public final String typeName;
  public final String typeLabel;
  public final XynaCodegenProperty prop;
  
  public AdditionalPropertyWrapper(XynaCodegenProperty prop, DefaultCodegen gen) {

    typePath = Sanitizer.sanitize(gen.apiPackage() + ".wrapper");
    typeLabel = (prop.isPrimitive? prop.dataType: prop.propRefType) + " Wrapper";
    typeName = Camelizer.camelize(typeLabel, Case.PASCAL);
    this.prop = prop;
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AdditionalPropertyWrapper)) return false;
    AdditionalPropertyWrapper that = (AdditionalPropertyWrapper) o;
    return typeLabel == that.typeLabel &&
        typeName == that.typeName &&
        typePath == that.typePath &&
        Objects.equals(prop, that.prop);
  }

  @Override
  public int hashCode() {
      return Objects.hash(typeLabel, typeName, typePath, prop);
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder("XynaCodegenModel{");
      sb.append("\n    ").append("label='").append(typeLabel).append('\'');
      sb.append(",\n    ").append("typeName='").append(typeName).append('\'');
      sb.append(",\n    ").append("typePath='").append(typePath).append('\'');
      sb.append(",\n    ").append("vars=").append(String.valueOf(prop).replace("\n", "\n    "));
      sb.append("\n}");
      return sb.toString();
  }
}
