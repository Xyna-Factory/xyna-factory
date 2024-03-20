package com.gip.xyna.openapi.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.DefaultCodegen;

public class XynaCodegenModel {
  
  final static XynaCodegenModel OASBASE = new XynaCodegenModel();

  final String label;
  final String typeName;
  final String typePath;
  final String description;
  
  final XynaCodegenModel parent;
  final List<XynaCodegenProperty> vars;
  
  final boolean isEnum;
  // enum
  final List<String> allowableValues = new ArrayList<String>();
  
  XynaCodegenModel(CodegenModel model, DefaultCodegen gen) {
    label = model.name;
    typeName = model.classname;
    description = buildDescription(model);
    vars = model.vars.stream().map(prop -> new XynaCodegenProperty(new CodegenPropertyHolder(prop), gen, typeName)).collect(Collectors.toList());
    isEnum = model.isEnum;
    if (model.allowableValues != null) {
      @SuppressWarnings("unchecked")
      List<Map<String, Object>> enumVars = (List<Map<String, Object>>) model.allowableValues.getOrDefault(("enumVars"), List.of());
      allowableValues.addAll(enumVars.stream().map(enumVar -> enumVar.get("name").toString()).collect(Collectors.toList()));
    }
    if (model.parent != null) {
      // maybe we should find the correct model, then building a new one.
      parent = new XynaCodegenModel(model.parentModel, gen);
    } else {
      parent = OASBASE;
    }
    
    typePath = gen.modelPackage();
  }
  
  private String buildDescription(CodegenModel model) {
    StringBuilder sb = new StringBuilder();
    if (model.description != null) {
      sb.append(model.description).append('\n');
    }
    if (model.allOf.size() > 0) {
      sb.append("This data type is \"allOf\": ");
      sb.append(String.join(", ", model.allOf)).append('\n');
    }
    if (model.oneOf.size() > 0) {
      sb.append("This data type is \"oneOf\": ");
      sb.append(String.join(", ", model.oneOf)).append('\n');
    }
    if (model.anyOf.size() > 0) {
      sb.append("This data type is \"anyOf\": ");
      sb.append(String.join(", ", model.anyOf)).append('\n');
    }
    if (isEnum) {
      sb.append("values: ");
      sb.append(String.join(", ", allowableValues)).append('\n');
    }
    sb.append("        ");
    return sb.toString();
  }
  
  // Construct OAS Base
  private XynaCodegenModel() {
    typeName = "OASBaseType";
    typePath = "xmcp.oas.datatype";
    isEnum = false;
    label = "OAS Base Type";
    parent = null;
    vars = new ArrayList<XynaCodegenProperty>();
    description = "";
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof XynaCodegenModel)) return false;
    XynaCodegenModel that = (XynaCodegenModel) o;
    return label == that.label &&
        typeName == that.typeName &&
        typePath == that.typePath &&
        description == that.description &&
        Objects.equals(parent, that.parent) &&
        Objects.equals(vars, that.vars) &&
        isEnum == that.isEnum &&
        Objects.equals(allowableValues, that.allowableValues);
  }
  
  @Override
  public int hashCode() {
      return Objects.hash(label, typeName, typePath, description, parent, vars, isEnum, allowableValues);
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder("XynaCodegenModel{");
      sb.append("\n    ").append("label='").append(label).append('\'');
      sb.append(",\n    ").append("typeName='").append(typeName).append('\'');
      sb.append(",\n    ").append("typePath='").append(typePath).append('\'');
      sb.append(",\n    ").append("description='").append(String.valueOf(description).replace("\n", "\\n")).append('\'');
      sb.append(",\n    ").append("parent=").append(String.valueOf(parent).replace("\n", "\n    "));
      sb.append(",\n    ").append("vars=").append(String.valueOf(vars).replace("\n", "\n    "));
      sb.append(",\n    ").append("isEnum='").append(isEnum).append('\'');
      sb.append(",\n    ").append("allowableValues='").append(allowableValues).append('\'');
      sb.append("\n}");
      return sb.toString();
  }
}
