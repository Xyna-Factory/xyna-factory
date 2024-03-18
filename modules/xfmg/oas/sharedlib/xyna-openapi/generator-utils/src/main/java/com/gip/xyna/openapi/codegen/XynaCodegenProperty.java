package com.gip.xyna.openapi.codegen;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.DefaultCodegen;

import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.CamelizeOption.UPPERCASE_FIRST_CHAR;
import static org.openapitools.codegen.utils.StringUtils.camelize;

public class XynaCodegenProperty {
  
  private static final Map<String, DatatypeInfos> DatatypeMap = buildDatatypeMap();
  private static Map<String, DatatypeInfos> buildDatatypeMap() {
    Map<String, DatatypeInfos> map = new HashMap<>();
    map.put("Boolean", new DatatypeInfos("Boolean", "BooleanTypeValidator", DatatypeInfos::emptyConstraints));
    map.put("Integer", new DatatypeInfos("Integer", "NumberTypeValidator&lt;Integer&gt;", DatatypeInfos::numberConstraints));
    map.put("Long", new DatatypeInfos("Long", "NumberTypeValidator&lt;Long&gt;", DatatypeInfos::numberConstraints));
    map.put("Float", new DatatypeInfos("Float", "NumberTypeValidator&lt;Float&gt;", DatatypeInfos::numberConstraints));
    map.put("Double", new DatatypeInfos("Double", "NumberTypeValidator&lt;Double&gt;", DatatypeInfos::numberConstraints));
    map.put("Enum", new DatatypeInfos("String", "EnumTypeValidator", DatatypeInfos::enumConstraints));
    map.put("String", new DatatypeInfos("String", "StringTypeValidator", DatatypeInfos::stringConstraints));
    map.put("DateType", new DatatypeInfos("String", "StringTypeValidator", DatatypeInfos::stringConstraints));
    map.put("DateTimeType", new DatatypeInfos("String", "StringTypeValidator", DatatypeInfos::stringConstraints));
    map.put("Default", new DatatypeInfos("String", "StringTypeValidator;", DatatypeInfos::emptyConstraints));
    return map;
  }

  // Used for validation exception message, to clarify context 
  final String propClassName;

  final String propLabel;
  final String propVarName;
  final String getPropVarName;
  final boolean isInherited;
  final boolean isList;

  final String propDescription;
   
  final boolean isPrimitive;
  // primitive
  final String datatype;
  final String javaType;
  final String validatorClassConstructor;
  final List<String> validatorConfig;
  //not primitive
  final String propRefType;
  final String propRefPath;
  
  XynaCodegenProperty(CodegenProperty property, DefaultCodegen gen, String className) {
    propClassName = className;
    propLabel = camelize(property.baseName, UPPERCASE_FIRST_CHAR);
    propVarName = camelize(propLabel.replace(" ", "_"), LOWERCASE_FIRST_LETTER);
    getPropVarName = "get" + camelize(propVarName.replace(" ", "_"), UPPERCASE_FIRST_CHAR) + "()";
    isInherited = property.isInherited;
    isList = property.isContainer;

    isPrimitive = property.isPrimitiveType || property.isEnumRef;
    datatype = buildDatatype(property);
    if (isPrimitive) {
      javaType = DatatypeMap.getOrDefault(datatype, DatatypeMap.get("Default")).javaType;
    } else {
      javaType = null;
    }
    validatorClassConstructor = buildValidatorClassConstructor(property);
    validatorConfig = buildValidatorConfig(property);
    
    if (isPrimitive) {
      propRefType = null;
      propRefPath = null;
    } else {
      propRefType = camelize(property.getComplexType().replace(" ", "_"), UPPERCASE_FIRST_CHAR);
      propRefPath = gen.modelPackage();
    }    
    propDescription = buildDescription(property);
  }
  
  private String buildDatatype(CodegenProperty property) {
    if (isPrimitive) {
      if (property.isEnumRef) {  
        return "Enum";
      } else if (isList) {
        return property.items.dataType;
      } else {
        return property.dataType;
      }
    } else {
      return null;
    }
  }

  private String buildValidatorClassConstructor(CodegenProperty property) {
    if (!isPrimitive) {
      return null;
    }
    String validatorPath = "com.gip.xyna.openapi.";
    StringBuilder validatorClassConstructor = new StringBuilder("new ").append(validatorPath);

    if (isList) {
      validatorClassConstructor.append("PrimitiveListTypeValidator&lt;&gt;").append("(")
      .append(validatorPath).append(DatatypeMap.getOrDefault(datatype, DatatypeMap.get("Default")).className).append("::new")
      .append(")");
    } else {
      validatorClassConstructor.append(DatatypeMap.getOrDefault(datatype, DatatypeMap.get("Default")).className).append("()");
    }
    return validatorClassConstructor.toString();
  }

  private List<String> buildValidatorConfig(CodegenProperty property) {
    List<String> config = new ArrayList<String>();
    if (!isPrimitive) {
      return config;
    }
    
    String setValue;
    if (isList) {
      setValue = "addValues(" + getPropVarName + ")";
    } else {
      setValue = "setValue(" + getPropVarName + ")";
    }
    String setRequired = "setRequired()";
    String setNullable = "setNullable()";

    
    PraefixPostfix fix = new PraefixPostfix();
    if (isList) {
      fix.praefix = "getValidators().forEach(val -> val.";
      fix.postfix = ")";
    }
        
    ValuesToValidate valuesToValidate = new ValuesToValidate(property);

    config.add("setName(" + propLabel + ")");
    config.add(setValue);

    config.addAll(DatatypeMap.getOrDefault(datatype, DatatypeMap.get("Default")).setterListBuilder.apply(valuesToValidate, fix));
    
    if (valuesToValidate.required) {
      config.add(setRequired);
    }
    if (valuesToValidate.nullable) {
      config.add(setNullable);
    }
    
    return config;
  }
  
  private String buildDescription(CodegenProperty property) {
    StringBuilder sb = new StringBuilder();
    if (property.description != null) {
      sb.append(property.description).append('\n');
    }
    if (property.isEnumRef) {
      sb.append("Enum of Type: ");
      sb.append(propRefType).append('.').append(propRefType);
      sb.append('\n');
      sb.append("values: ");
      sb.append(String.join(", ", property.allowableValues.keySet()));
      sb.append('\n');
    }
    if (property.getFormat() != null) {
      sb.append("format: ");
      sb.append(property.getFormat()).append('\n');
    }
    if (property.required) {
      sb.append("required").append('\n');
    }
    sb.append("        ");
    return sb.toString();
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
        Objects.equals(propDescription, that.propDescription) &&
        isInherited == that.isInherited &&
        isList == that.isList &&
        isPrimitive == that.isPrimitive &&
        Objects.equals(datatype, that.datatype) &&
        Objects.equals(javaType, that.javaType) &&
        Objects.equals(validatorClassConstructor, that.validatorClassConstructor) &&
        Objects.equals(validatorConfig, that.validatorConfig) &&
        Objects.equals(propRefType, that.propRefType) &&
        Objects.equals(propRefPath, that.propRefPath);

  }
  
  @Override
  public int hashCode() {
      return Objects.hash(propClassName, propLabel, propVarName, getPropVarName, propDescription, isInherited, 
                          isList, isPrimitive, datatype, javaType, validatorClassConstructor, validatorConfig,
                          propRefType, propRefPath);
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder("XynaCodegenProperty{");
      sb.append("\n    ").append("propClassName='").append(propClassName).append('\'');
      sb.append("\n    ").append("propLabel='").append(propLabel).append('\'');
      sb.append(",\n    ").append("propVarName='").append(propVarName).append('\'');
      sb.append(",\n    ").append("getPropVarName='").append(getPropVarName).append('\'');
      sb.append(",\n    ").append("isInherited='").append(isInherited).append('\'');
      sb.append(",\n    ").append("isList='").append(isList).append('\'');
      sb.append(",\n    ").append("isPrimitive='").append(isPrimitive).append('\'');
      sb.append(",\n    ").append("propDescription='").append(String.valueOf(propDescription).replace("\n", "\\n")).append('\'');
      sb.append(",\n    ").append("datatype='").append(datatype).append('\'');
      sb.append(",\n    ").append("javaType='").append(javaType).append('\'');
      sb.append(",\n    ").append("validatorClassConstructor='").append(validatorClassConstructor).append('\'');
      sb.append(",\n    ").append("validatorConfig='").append(String.valueOf(validatorConfig).replace("\n", "\n    ")).append('\'');
      sb.append(",\n    ").append("propRefType='").append(propRefType).append('\'');
      sb.append(",\n    ").append("propRefPath='").append(propRefPath).append('\'');
      sb.append("\n}");
      return sb.toString();
  }

  static class DatatypeInfos{
    String javaType;
    String className;
    BiFunction<ValuesToValidate, PraefixPostfix, List<String>> setterListBuilder;
    
    DatatypeInfos(String javaType, String className, BiFunction<ValuesToValidate, PraefixPostfix, List<String>> setterListBuilder) {
      this.javaType = javaType;
      this.className = className;
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
    boolean required;
    boolean nullable;
    List<String> allowableValues = new ArrayList<String>();
    
    ValuesToValidate(CodegenProperty property) {
      CodegenProperty mostInnerItems = property.mostInnerItems != null ? property.mostInnerItems : property;
      
      minimum = mostInnerItems.minimum;
      maximum = mostInnerItems.maximum;
      excludeMin = mostInnerItems.exclusiveMinimum;
      excludeMax = mostInnerItems.exclusiveMaximum;
      multipleOf = mostInnerItems.multipleOf;
      dataFormat = mostInnerItems.dataFormat;
      minLength = mostInnerItems.minLength;
      maxLength = mostInnerItems.maxLength;
      required = mostInnerItems.required;
      nullable = mostInnerItems.isNullable;
      if (mostInnerItems.allowableValues != null) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> enumVars = (List<Map<String, Object>>) mostInnerItems.allowableValues.getOrDefault(("enumVars"), List.of());
        allowableValues.addAll(enumVars.stream().map(enumVar -> enumVar.get("name").toString()).collect(Collectors.toList()));
      }
    }
  }
  
  static class PraefixPostfix {
    String praefix = "";
    String postfix = "";
  }
}
