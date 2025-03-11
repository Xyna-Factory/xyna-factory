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
package com.gip.xyna.openapi.codegen;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.openapitools.codegen.CodegenDiscriminator.MappedModel;

import com.gip.xyna.openapi.codegen.factory.XynaCodegenFactory;
import com.gip.xyna.openapi.codegen.utils.GeneratorProperty;
import com.gip.xyna.openapi.codegen.utils.Sanitizer;

import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenProperty;
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
  final List<EnumData> allowableValues;
  
  //discriminator
  final boolean hasDiscriminator;
  final String discriminatorKey;
  final String discriminatorProp;
  final List<DiscriminatorMap> discriminatorMap;

  final boolean isListWrapper;
  final boolean isOrWrapper;
  
  public XynaCodegenModel(XynaCodegenFactory factory, CodegenModel model, DefaultCodegen gen) {
    label = model.name;
    isListWrapper = isListWrapper(model, gen);
    isOrWrapper = isOrWrapper(model);
    typeName = buildTypeName(model);
    typePath = buildTypePath(gen);
    description = buildDescription(model);
    isEnum = model.isEnum;

    allowableValues = EnumData.buildFromMap(model.allowableValues);
    
    if (model.parent != null && model.parentModel != null) {
      // maybe we should find the correct model, then building a new one.
      parent = factory.getOrCreateXynaCodegenModel(model.parentModel);
    } else {
      parent = OASBASE;
    }

    String discProp = null;
    
    if (isEnum) {
      vars = List.of(factory.getOrCreateXynaCodegenEnumProperty(model.allowableValues, typeName));
    } else if (isOrWrapper) {
      vars = model.getComposedSchemas().getOneOf().stream().
          map(prop -> factory.getOrCreateXynaCodegenPropertyUsingComplexType(prop, typeName)).
          collect(Collectors.toList());
      CodegenProperty discriminator = model.getAllVars().stream().
          filter(var -> model.discriminator.getPropertyBaseName().equals(var.baseName)).
          findAny().get();
      XynaCodegenProperty xynaProp = factory.getOrCreateXynaCodegenProperty(discriminator, typeName);
      vars.add(xynaProp);
      discProp = xynaProp.propVarName;
    } else {
      vars = model.getAllVars().stream().
          map(prop -> factory.getOrCreateXynaCodegenProperty(prop, typeName)).
          collect(Collectors.toList());
    }
    if (model.isAdditionalPropertiesTrue) {
      vars.add(factory.getPropertyToAddionalPropertyWrapper(model.getAdditionalProperties(), typeName));
    }
    
    hasDiscriminator = model.getHasDiscriminatorWithNonEmptyMapping();
    if (hasDiscriminator) {
      discriminatorKey = model.discriminator.getPropertyBaseName();
      discriminatorMap = new ArrayList<DiscriminatorMap>();
      for (MappedModel mappedModel: model.discriminator.getMappedModels()) {
        String fqn = getFQN(mappedModel.getModel(), gen);
        if (isOrWrapper) {
          XynaCodegenProperty prop = vars.stream().
              filter(var -> var.getPropFQN().equals(fqn)).
              findAny().
              // if it fails, then we will fail to generate the method continueReadWithObject in OASDiceder.
              get();
          discriminatorMap.add(new DiscriminatorMap(mappedModel.getMappingName(), fqn, prop.propVarName));
        } else {
          discriminatorMap.add(new DiscriminatorMap(mappedModel.getMappingName(), fqn));
        }
      }
    } else {
      discriminatorKey = null;
      discriminatorMap = null;
    }
    discriminatorProp = discProp;
  }
  
  public static String buildTypeName(CodegenModel model) {
    return Sanitizer.sanitize(model.classname);
  }
  
  public static String buildTypePath(DefaultCodegen gen) {
    return Sanitizer.sanitize(GeneratorProperty.getModelPath(gen));
  }
  
  public String getModelFQN() {
    return getFQN(typePath, typeName);
  }
  
  public static String getFQN(CodegenModel model, DefaultCodegen gen) {
    return getFQN(buildTypePath(gen), buildTypeName(model));
  }
  
  private static String getFQN(String path, String name) {
    return path + '.' + name;
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
      List<String> originals = allowableValues.stream().map(
                                     enumData -> enumData.enumLabel
                               ).collect(Collectors.toList());
      sb.append(String.join(", ", originals)).append('\n');
    }
    if(isListWrapper) {
      sb.append("This is a Listwrapper!\n");
    }
    if(isOrWrapper) {
      sb.append("This is an Orwrapper!\n");
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
    discriminatorProp = null;
    discriminatorMap = null;
    isListWrapper = false;
    isOrWrapper = false;
    allowableValues = new ArrayList<>();
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
    String varName;
    
    DiscriminatorMap(String keyValue, String fqn) {
      this(keyValue, fqn, "");
    }
    
    DiscriminatorMap(String keyValue, String fqn, String varName) {
      this.keyValue = keyValue;
      this.fqn = fqn;
      this.varName = varName;
    }
  }
  
  public static boolean isListWrapper(CodegenModel model, DefaultCodegen gen) {
    return model.isArray && GeneratorProperty.getCreateListWrappers(gen);
  }
  
  public static boolean isOrWrapper(CodegenModel model) {
    return !model.oneOf.isEmpty() && model.getHasDiscriminatorWithNonEmptyMapping();
  }
}

class EnumData {
  final String original;
  final String enumLabel;
  final String javaEscaped;
  final String methodname;
  
  static List<EnumData> buildFromMap(Map<String, Object> allowableValuesMap) {
    if (allowableValuesMap != null) {
      @SuppressWarnings("unchecked")
      List<String> enumValues = (List<String>) allowableValuesMap.getOrDefault(("values"), List.of());
      return enumValues.stream().map(
         value -> new EnumData(value)
      ).collect(Collectors.toList());
    } else {
      return new ArrayList<>();
    }    
  }
  
  EnumData(String original) {
    this.original = original;
    enumLabel = original.toUpperCase();
    Pattern exp = Pattern.compile("(\\\"|\\'|\\\\)");
    Matcher matcher = exp.matcher(original);
    javaEscaped = matcher.replaceAll((result) -> "\\\\" + result.group());
    methodname = original.replaceAll("[^a-zA-Z0-9_]", "").toUpperCase();
  }
}
