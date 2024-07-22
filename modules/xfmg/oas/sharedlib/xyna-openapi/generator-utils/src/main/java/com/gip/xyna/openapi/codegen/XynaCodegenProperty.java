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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.openapitools.codegen.DefaultCodegen;

import com.gip.xyna.openapi.codegen.factory.CodegenPropertyInfo;
import com.gip.xyna.openapi.codegen.utils.Sanitizer;
import com.gip.xyna.openapi.codegen.utils.Camelizer.Case;
import static com.gip.xyna.openapi.codegen.utils.Camelizer.camelize;

public class XynaCodegenProperty {
  
  private static final Map<String, DatatypeInfos> DatatypeMap = buildDatatypeMap();
  private static Map<String, DatatypeInfos> buildDatatypeMap() {
    Map<String, DatatypeInfos> map = new HashMap<>();
    map.put("Boolean", new DatatypeInfos("Boolean", "BooleanTypeValidator", DatatypeInfos::emptyConstraints));
    map.put("Integer", new DatatypeInfos("Integer", "NumberTypeValidator<Integer>", DatatypeInfos::numberConstraints));
    map.put("Long", new DatatypeInfos("Long", "NumberTypeValidator<Long>", DatatypeInfos::numberConstraints));
    map.put("Float", new DatatypeInfos("Float", "NumberTypeValidator<Float>", DatatypeInfos::numberConstraints));
    map.put("Double", new DatatypeInfos("Double", "NumberTypeValidator<Double>", DatatypeInfos::numberConstraints));
    map.put("Enum", new DatatypeInfos("String", "EnumTypeValidator", DatatypeInfos::enumConstraints));
    map.put("String", new DatatypeInfos("String", "StringTypeValidator", DatatypeInfos::stringConstraints));
    map.put("DateType", new DatatypeInfos("String", "StringTypeValidator", DatatypeInfos::stringConstraints));
    map.put("DateTimeType", new DatatypeInfos("String", "StringTypeValidator", DatatypeInfos::stringConstraints));
    map.put("Default", new DatatypeInfos("String", "StringTypeValidator", DatatypeInfos::emptyConstraints));
    return map;
  }

  // Used for validation exception message, to clarify context 
  final String propClassName;

  //general properties
  final String propLabel;
  final String propVarName;
  final String getPropVarName;
  final String setPropVarName;
  final boolean isInherited;
  final boolean isList;
  final boolean isPrimitive;
  
  final String propDescription;
   
  // for primitive
  final String dataType;
  final String javaType;
  final String validatorClassConstructor;
  final List<String> validatorConfig;
  
  // for not primitive
  final boolean isRequired;
  final String propRefType;
  final String propRefPath;
  // for complex lists
  final Integer minItems;
  final Integer maxItems;
  
  public XynaCodegenProperty(CodegenPropertyInfo propertyInfo, DefaultCodegen gen, String className) {
    propClassName = className;
    propLabel = propertyInfo.getBaseName();
    propVarName = Sanitizer.sanitize(camelize(propertyInfo.getName(), Case.CAMEL));
    getPropVarName = "get" + camelize(propVarName, Case.PASCAL) + "()";
    setPropVarName = "set" + camelize(propVarName, Case.PASCAL);
    isList = isList(propertyInfo);
    isInherited = propertyInfo.getIsInherited();
    isPrimitive = isPrimitive(propertyInfo);
    isRequired = propertyInfo.getRequired();
    dataType = buildDatatype(propertyInfo);
    javaType = isPrimitive ? DatatypeMap.getOrDefault(dataType, DatatypeMap.get("Default")).javaType : null;

    if (isPrimitive) {
      propRefType = null;
      propRefPath = null;
    } else {
      if(isGenericJsonObject(propertyInfo)) {
        propRefType = isList ? "JSONValue": "JSONObject";
        propRefPath = "xfmg.xfctrl.datamodel.json";
      } else {
        propRefType = getType(propertyInfo);
        propRefPath = getPath(propertyInfo, gen);
      }
    }

    if (isList) {
        minItems = propertyInfo.getMinItems();
        maxItems = propertyInfo.getMaxItems();
    } else {
        minItems = null;
        maxItems = null;
    }

    validatorClassConstructor = buildValidatorClassConstructor();
    validatorConfig = buildValidatorConfig(propertyInfo);
    propDescription = buildDescription(propertyInfo);
  }


  private boolean isGenericJsonObject(CodegenPropertyInfo propertyInfo) { 
    if("object".equalsIgnoreCase(propertyInfo.getComplexType())) {
      return true;
    }
    
    if("object".equalsIgnoreCase(propertyInfo.getDataType())) {
      return true;
    }        
    if ("array".equalsIgnoreCase(propertyInfo.getDataType()) && "object".equalsIgnoreCase(propertyInfo.getComplexType())) {
      return true;
    }
    return false;
  }

  /**
   * derives the dataType from the codegenProperty.
   * Returns null if no primitive member should be created.
   * Otherwise returns a String matching a key in DatatypeMap
   */
  private String buildDatatype(CodegenPropertyInfo propertyInfo) {
    if (!isPrimitive(propertyInfo)) {
      return null;
    }
    if (propertyInfo.getIsEnumOrRef()) {
      return "Enum";
    }
    if (propertyInfo.getIsPrimitiveType()) {
      if (isList(propertyInfo)) {
        return buildDatatype(propertyInfo.getMostInnerItems());
      } else {
        return propertyInfo.getDataType();
      }
    } else {
      return capitalize(propertyInfo.getOpenApiType());
    }
  }


  private boolean isList(CodegenPropertyInfo property) {
    return property.getIsContainer(); //TODO: isArray?
  }


  private String capitalize(String in) {
    return in.substring(0, 1).toUpperCase() + in.substring(1);
  }

  private boolean isPrimitiveList(CodegenPropertyInfo property) {
    return isList(property) && isPrimitive(property.getMostInnerItems());
  }
  
  public String getPropFQN() {
    if (isPrimitive) {
      return javaType;
    }
    return propRefPath + "." + propRefType;
  }
  
  public static String getPath(CodegenPropertyInfo propertyInfo, DefaultCodegen gen) {
    return Sanitizer.sanitize(gen.modelPackage() + propertyInfo.getAddionalPath());
  }
  
  public static String getType(CodegenPropertyInfo propertyInfo) {
    return camelize(propertyInfo.getComplexType(), Case.PASCAL);
  }

  private boolean isString(CodegenPropertyInfo property) {
    return property.getIsString() 
      || "string".equalsIgnoreCase(property.getOpenApiType()) 
      || property.getIsEnumOrRef();
  }


  /**
   * determines whether the property should result in a primitive member or not.
   * In most cases, property.isPrimitiveType returns the desired value, however
   * there are several special cases, where testing for this property yields the
   * wrong result: Enums, primitive types with format information or collections
   * of primitive types
   */
  private boolean isPrimitive(CodegenPropertyInfo property) {
    if (isGenericJsonObject(property)) {
      return false;
    }

    return property.getIsPrimitiveType()
      || property.getComplexType() == null 
      || isString(property)
      || property.getIsNumber() 
      || property.getIsInteger() 
      || isPrimitiveList(property);
    }


  private String buildValidatorClassConstructor() {
    if (!isPrimitive) {
      return null;
    }
    String validatorPath = "com.gip.xyna.openapi.";
    StringBuilder validatorClassConstructor = new StringBuilder("new ").append(validatorPath);
    DatatypeInfos typeInfo = DatatypeMap.getOrDefault(dataType, DatatypeMap.get("Default"));
    if (isList) {
      validatorClassConstructor.append("PrimitiveListTypeValidator<")
          .append(typeInfo.javaType).append(",").append(validatorPath).append(typeInfo.validatorClassName)
          .append(">(")
          .append(validatorPath).append(typeInfo.validatorClassName).append("::new")
          .append(")");
    } else {
      validatorClassConstructor.append(typeInfo.validatorClassName).append("()");
    }
    return validatorClassConstructor.toString();
  }

  private List<String> buildValidatorConfig(CodegenPropertyInfo propertyInfo) {
    List<String> config = new ArrayList<String>();
    if (!isPrimitive) {
      return config;
    }
    
    //prepare valuesToValidate
    ValuesToValidate valuesToValidate = new ValuesToValidate(propertyInfo, javaType);
    
    String setValue = "setValue(" + getPropVarName + ")";
    String setRequired = "setRequired()";
    String setNullable = "setNullable()";
    String setMinItems = "setMinItems("+valuesToValidate.minItems+")";
    String setMaxItems = "setMaxItems("+valuesToValidate.maxItems+")";
    
    //for primitive lists only
    PraefixPostfix fix = new PraefixPostfix();
    if (isList) {
      fix.praefix = "getValidatorsNonNull().forEach(val -> val.";
      fix.postfix = ")";
    }
    
    //build config
    config.add("setName(\"" + propLabel + "\")");
    config.add(setValue);

    config.addAll(DatatypeMap.getOrDefault(dataType, DatatypeMap.get("Default")).setterListBuilder.apply(valuesToValidate, fix));

    if (isList && valuesToValidate.minItems!=null) {
        config.add(setMinItems);
    }
    if (isList && valuesToValidate.maxItems!=null) {
        config.add(setMaxItems);
    }
    if (valuesToValidate.required) {
      config.add(setRequired);
    }
    if (valuesToValidate.nullable) {
      config.add(setNullable);
    }

    return config;
  }

  @SuppressWarnings("unchecked")
  private String buildDescription(CodegenPropertyInfo propertyInfo) {
    StringBuilder sb = new StringBuilder();
    if (propertyInfo.getDescription() != null) {
      sb.append(propertyInfo.getDescription()).append('\n');
    }
    if (propertyInfo.getIsEnumOrRef()) {
      sb.append("values: ");
      sb.append(String.join(", ", (List<String>)propertyInfo.getAllowableValues().getOrDefault("values", List.of())));
      sb.append('\n');
    }
    if (propertyInfo.getFormat() != null) {
      sb.append("format: ");
      sb.append(propertyInfo.getFormat()).append('\n');
    }
    if (propertyInfo.getRequired()) {
      sb.append("required").append('\n');
    }
    sb.append("        ");
    return sb.toString();
  }


  public boolean isString() {
    return "String".equals(javaType);
  }


  public boolean isNumber() {
    return "Integer".equals(javaType) || "Long".equals(javaType) || "Float".equals(javaType) || "Double".equals(javaType);
  }


  public boolean isBoolean() {
    return "Boolean".equals(javaType);
  }
  
  public boolean isGenericJsonObject() {
    return "xfmg.xfctrl.datamodel.json".equals(propRefPath) && "JSONObject".equals(propRefType);
  }
  public boolean isGenericJsonList() {
    return "xfmg.xfctrl.datamodel.json".equals(propRefPath) && "JSONValue".equals(propRefType);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof XynaCodegenProperty)) return false;

    XynaCodegenProperty that = (XynaCodegenProperty) o;
    return Objects.equals(propClassName, that.propClassName) &&
        Objects.equals(propLabel, that.propLabel) &&
        Objects.equals(propVarName, that.propVarName) &&
        Objects.equals(getPropVarName, that.getPropVarName) &&
        Objects.equals(setPropVarName, that.setPropVarName) &&
        Objects.equals(propDescription, that.propDescription) &&
        isInherited == that.isInherited &&
        isList == that.isList &&
        Objects.equals(minItems, that.minItems) &&
        Objects.equals(maxItems, that.maxItems) &&
        isPrimitive == that.isPrimitive &&
        isRequired == that.isRequired &&
        Objects.equals(dataType, that.dataType) &&
        Objects.equals(javaType, that.javaType) &&
        Objects.equals(validatorClassConstructor, that.validatorClassConstructor) &&
        Objects.equals(validatorConfig, that.validatorConfig) &&
        Objects.equals(propRefType, that.propRefType) &&
        Objects.equals(propRefPath, that.propRefPath);

  }

  @Override
  public int hashCode() {
    return Objects.hash(propClassName, propLabel, propVarName, getPropVarName, setPropVarName, propDescription, isInherited, isList,
                        isPrimitive, dataType, javaType, validatorClassConstructor, validatorConfig, propRefType, propRefPath);
  }

  protected void toString(StringBuilder sb) {
    if (sb == null) {
      return;
    }
    sb.append("\n    ").append("propClassName='").append(propClassName).append('\'');
    sb.append("\n    ").append("propLabel='").append(propLabel).append('\'');
    sb.append(",\n    ").append("propVarName='").append(propVarName).append('\'');
    sb.append(",\n    ").append("getPropVarName='").append(getPropVarName).append('\'');
    sb.append(",\n    ").append("setPropVarName='").append(setPropVarName).append('\'');
    sb.append(",\n    ").append("isInherited='").append(isInherited).append('\'');
    sb.append(",\n    ").append("isList='").append(isList).append('\'');
    sb.append(",\n    ").append("minItems='").append(minItems).append('\'');
    sb.append(",\n    ").append("maxItems='").append(maxItems).append('\'');
    sb.append(",\n    ").append("isPrimitive='").append(isPrimitive).append('\'');
    sb.append(",\n    ").append("isRequired='").append(isRequired).append('\'');
    sb.append(",\n    ").append("propDescription='").append(String.valueOf(propDescription).replace("\n", "\\n")).append('\'');
    sb.append(",\n    ").append("dataType='").append(dataType).append('\'');
    sb.append(",\n    ").append("javaType='").append(javaType).append('\'');
    sb.append(",\n    ").append("validatorClassConstructor='").append(validatorClassConstructor).append('\'');
    sb.append(",\n    ").append("validatorConfig='").append(String.valueOf(validatorConfig).replace("\n", "\n    ")).append('\'');
    sb.append(",\n    ").append("propRefType='").append(propRefType).append('\'');
    sb.append(",\n    ").append("propRefPath='").append(propRefPath).append('\'');
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");
      toString(sb);
      sb.append("\n}");
      return sb.toString();
  }


  static class DatatypeInfos {

    String javaType;
    String validatorClassName;
    BiFunction<ValuesToValidate, PraefixPostfix, List<String>> setterListBuilder;

    DatatypeInfos(String javaType, String className, BiFunction<ValuesToValidate, PraefixPostfix, List<String>> setterListBuilder) {
      this.javaType = javaType;
      this.validatorClassName = className;
      this.setterListBuilder = setterListBuilder;
    }

    public static List<String> emptyConstraints(ValuesToValidate values, PraefixPostfix fix) {
      return new ArrayList<String>();
    }

    public static List<String> numberConstraints(ValuesToValidate values, PraefixPostfix fix) {
      List<String> result = new ArrayList<String>();
      if (values.minimum != null) {
        result.add(fix.praefix + "setMin(" + values.minimum + ")" + fix.postfix);
      }
      if (values.maximum != null) {
        result.add(fix.praefix + "setMax(" + values.maximum + ")" + fix.postfix);
      }
      if (values.excludeMin) {
        result.add(fix.praefix + "setExcludeMax()" + fix.postfix);
      }
      if (values.excludeMax) {
        result.add(fix.praefix + "setExcludeMax()" + fix.postfix);
      }
      if (values.multipleOf != null) {
        result.add(fix.praefix + "setMultipleOf(" + values.multipleOf + ")" + fix.postfix);
      }
      return result;
    }

    public static List<String> stringConstraints(ValuesToValidate values, PraefixPostfix fix) {
      List<String> result = new ArrayList<String>();
      if (values.dataFormat != null) {
        result.add(fix.praefix + "setFormat(\"" + values.dataFormat + "\")" + fix.postfix);
      }
      if (values.minLength != null) {
        result.add(fix.praefix + "setMinLength(" + values.minLength + ")" + fix.postfix);
      }
      if (values.maxLength != null) {
        result.add(fix.praefix + "setMaxLength(" + values.maxLength + ")" + fix.postfix);
      }
      if (values.pattern != null) {
          // clean delimiters
          String cleanedPattern = values.pattern;
          if (cleanedPattern.startsWith("/") && cleanedPattern.endsWith("/")) {
              cleanedPattern = cleanedPattern.substring(1,cleanedPattern.length()-1);
          }
          result.add(fix.praefix + "setPattern(\"" + cleanedPattern + "\")" + fix.postfix);
      }
      return result;
    }

    public static List<String> enumConstraints(ValuesToValidate values, PraefixPostfix fix) {
      List<String> result = new ArrayList<String>();
      if (values.allowableValues != null && !values.allowableValues.isEmpty()) {
        StringBuilder sb = new StringBuilder();
        sb.append('\"').append(String.join("\", \"", values.allowableValues)).append('\"');
        result.add(fix.praefix + "setAllowableValues(" + sb.toString() + ")" + fix.postfix);
      }
      return result;
    }
  }

  static class ValuesToValidate {

    String minimum;
    String maximum;
    boolean excludeMin;
    boolean excludeMax;
    Number multipleOf;
    String dataFormat;
    Integer minLength;
    Integer maxLength;
    Integer minItems;
    Integer maxItems;
    boolean required;
    boolean nullable;
    String pattern;

    List<String> allowableValues = new ArrayList<String>();

    ValuesToValidate(CodegenPropertyInfo propertyInfo, String javatype) {
      CodegenPropertyInfo mostInnerItems = propertyInfo.getMostInnerItems() != null ? propertyInfo.getMostInnerItems() : propertyInfo;

      minimum = mostInnerItems.getMinimum();
      if ("Long".equals(javatype) && minimum != null) {
        minimum = minimum + "L";
      }
      if ("Float".equals(javatype) && minimum != null) {
        minimum = minimum + "F";
      }
      if ("Double".equals(javatype) && minimum != null) {
        minimum = minimum + "D";
      }
      maximum = mostInnerItems.getMaximum();
      if ("Long".equals(javatype) && maximum != null) {
        maximum = maximum + "L";
      }
      if ("Float".equals(javatype) && maximum != null) {
        maximum = maximum + "F";
      }
      if ("Double".equals(javatype) && maximum != null) {
        maximum = maximum + "D";
      }
      excludeMin = mostInnerItems.getExclusiveMinimum();
      excludeMax = mostInnerItems.getExclusiveMaximum();
      multipleOf = mostInnerItems.getMultipleOf();
      dataFormat = mostInnerItems.getDataFormat();
      pattern = mostInnerItems.getPattern();
      minLength = mostInnerItems.getMinLength();
      maxLength = mostInnerItems.getMaxLength();
      nullable = mostInnerItems.getIsNullable();
      minItems = propertyInfo.getMinItems();
      maxItems = propertyInfo.getMaxItems();
      required = mostInnerItems.getRequired();
      if (propertyInfo.getIsContainer() && !required) {
          required = propertyInfo.getRequired();
      }
      if (mostInnerItems.getAllowableValues() != null) {
        @SuppressWarnings("unchecked")
        List<String> enumValues = (List<String>) mostInnerItems.getAllowableValues().getOrDefault(("values"), List.of());
        allowableValues.addAll(enumValues);
      }
    }
  }

  static class PraefixPostfix {
    String praefix = "";
    String postfix = "";
  }
}
