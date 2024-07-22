package com.gip.xyna.openapi.codegen;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.openapitools.codegen.DefaultCodegen;

import com.gip.xyna.openapi.codegen.utils.Camelizer;
import com.gip.xyna.openapi.codegen.utils.Camelizer.Case;
import com.gip.xyna.openapi.codegen.utils.GeneratorProperty;
import com.gip.xyna.openapi.codegen.utils.Sanitizer;

public class AdditionalPropertyWrapper {

  public final String wrapperPath;
  public final String wrapperName;
  public final String wrapperLabel;
  public final XynaCodegenProperty prop;
  public final Set<String> usersFQN;
  
  public AdditionalPropertyWrapper(XynaCodegenProperty prop, DefaultCodegen gen) {

    wrapperPath = Sanitizer.sanitize(GeneratorProperty.getModelPath(gen) + ".wrapper");
    wrapperLabel = (prop.isPrimitive? prop.dataType: prop.propRefType) + " Wrapper";
    wrapperName = Camelizer.camelize(wrapperLabel, Case.PASCAL);
    this.prop = prop;
    usersFQN = new HashSet<>();
  }
  
  public void addUserFQN(String fqn) {
    usersFQN.add(fqn);
  }
  
  public String getWrapperFQN() {
    return wrapperPath + '.' + wrapperName;
  }
  
  // don't compare usersFQN, because every user should use the same wrapper and therefore wrapper with different users are considered equal.
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof AdditionalPropertyWrapper)) return false;
    AdditionalPropertyWrapper that = (AdditionalPropertyWrapper) o;
    return wrapperLabel == that.wrapperLabel &&
        wrapperName == that.wrapperName &&
        wrapperPath == that.wrapperPath &&
        Objects.equals(prop, that.prop);
  }

  @Override
  public int hashCode() {
      return Objects.hash(wrapperLabel, wrapperName, wrapperPath, prop);
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder("XynaCodegenModel{");
      sb.append("\n    ").append("label='").append(wrapperLabel).append('\'');
      sb.append(",\n    ").append("typeName='").append(wrapperName).append('\'');
      sb.append(",\n    ").append("typePath='").append(wrapperPath).append('\'');
      sb.append(",\n    ").append("vars=").append(String.valueOf(prop).replace("\n", "\n    "));
      sb.append(",\n    ").append("usersFQN=").append(String.valueOf(usersFQN).replace("\n", "\n    "));
      sb.append("\n}");
      return sb.toString();
  }
}
