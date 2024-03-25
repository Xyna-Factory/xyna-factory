package com.gip.xyna.openapi.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openapitools.codegen.CodegenDiscriminator.MappedModel;

import com.gip.xyna.openapi.codegen.utils.Sanitizer;

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
  
  //discriminator
  final boolean hasDiscriminator;
  final String discriminatorKey;
  final List<DiscriminatorMap> discriminatorMap;
  
  
  XynaCodegenModel(CodegenModel model, DefaultCodegen gen) {
    label = model.name;
    typeName = buildTypeName(model);
    typePath = buildTypePath(gen);
    description = buildDescription(model);
    
    isEnum = model.isEnum;
    if (isEnum) {
      vars = List.of(new XynaCodegenProperty(new CodegenEnum(model.allowableValues), gen, typeName));
    } else {
      vars = model.vars.stream().map(prop -> new XynaCodegenProperty(new CodegenPropertyHolder(prop), gen, typeName)).collect(Collectors.toList());
    }
    if (model.allowableValues != null) {
      @SuppressWarnings("unchecked")
      List<String> enumValues = (List<String>) model.allowableValues.getOrDefault(("values"), List.of());
      allowableValues.addAll(enumValues);
    }
    if (model.parent != null) {
      // maybe we should find the correct model, then building a new one.
      parent = new XynaCodegenModel(model.parentModel, gen);
    } else {
      parent = OASBASE;
    }
    
    hasDiscriminator = model.getHasDiscriminatorWithNonEmptyMapping();
    if (hasDiscriminator) {
      discriminatorKey = model.discriminator.getPropertyBaseName();
      discriminatorMap = new ArrayList<DiscriminatorMap>();
      for (MappedModel mappedModel: model.discriminator.getMappedModels()) {
        String fqn = buildTypePath(gen) + "." + buildTypeName(mappedModel.getModel());
        discriminatorMap.add(new DiscriminatorMap(mappedModel.getMappingName(), fqn));
      }
    } else {
      discriminatorKey = null;
      discriminatorMap = null;
    }
  }
  
  private String buildTypeName(CodegenModel model) {
    return Sanitizer.sanitize(model.classname);
  }
  
  private String buildTypePath(DefaultCodegen gen) {
    return Sanitizer.sanitize(gen.modelPackage());
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
    hasDiscriminator = false;
    discriminatorKey = null;
    discriminatorMap = null;
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
        hasDiscriminator == that.hasDiscriminator &&
        Objects.equals(discriminatorKey, that.discriminatorKey) &&
        Objects.equals(discriminatorMap, that.discriminatorMap);
  }

  @Override
  public int hashCode() {
      return Objects.hash(label, typeName, typePath, description,
                          parent, vars, isEnum, allowableValues,
                          hasDiscriminator, discriminatorKey, discriminatorMap);
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
      sb.append(",\n    ").append("hasDiscriminator='").append(hasDiscriminator).append('\'');
      if (hasDiscriminator) {
        sb.append(",\n    ").append("discriminatorKey='").append(discriminatorKey).append('\'');
        sb.append(",\n    ").append("discriminatorMap='").append(discriminatorMap).append('\'');
      }
      sb.append("\n}");
      return sb.toString();
  }
  
  static class DiscriminatorMap {
    String keyValue;
    String fqn;
    
    DiscriminatorMap(String keyValue, String fqn) {
      this.keyValue = keyValue;
      this.fqn = fqn;
    }
  }
}
