package com.gip.xyna.openapi.codegen;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

import org.openapitools.codegen.DefaultCodegen;

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

  final String propLabel;
  final String propVarName;
  final String getPropVarName;
  final String setPropVarName;
  final boolean isInherited;
  final boolean isList;

  final String propDescription;
   
  final boolean isPrimitive;
  // primitive
  final String dataType;
  final String javaType;
  final String validatorClassConstructor;
  final List<String> validatorConfig;
  //not primitive
  final String propRefType;
  final String propRefPath;
  
  XynaCodegenProperty(CodegenPropertyInfo propertyInfo, DefaultCodegen gen, String className) {
    propClassName = className;
    propLabel = propertyInfo.getBaseName();
    propVarName = Sanitizer.sanitize(camelize(propertyInfo.getName(), Case.CAMEL));
    getPropVarName = "get" + camelize(propVarName, Case.PASCAL) + "()";
    setPropVarName = "set" + camelize(propVarName, Case.PASCAL);
    isList = isList(propertyInfo);
    isInherited = propertyInfo.getIsInherited();
    isPrimitive = isPrimitive(propertyInfo);
    dataType = buildDatatype(propertyInfo);
    javaType = isPrimitive ? DatatypeMap.getOrDefault(dataType, DatatypeMap.get("Default")).javaType : null;
    validatorClassConstructor = buildValidatorClassConstructor();
    validatorConfig = buildValidatorConfig(propertyInfo);

    if (isPrimitive) {
      propRefType = null;
      propRefPath = null;
    } else {
      propRefType = camelize(propertyInfo.getComplexType(), Case.PASCAL);
      propRefPath = Sanitizer.sanitize(gen.modelPackage());
    }
    propDescription = buildDescription(propertyInfo);
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


  /**
   * determines whether the property should result in a primitive member or not.
   * In most cases, property.isPrimitiveType returns the desired value, however
   * there are several special cases, where testing for this property yields the
   * wrong result: Enums, primitive types with format information or collections
   * of primitive types
   */
  private boolean isPrimitive(CodegenPropertyInfo property) {
    return property.getIsPrimitiveType() || property.getIsEnumOrRef() || property.getComplexType() == null
        || property.getIsString() || property.getIsNumber() || property.getIsInteger()
        || (isList(property) && isPrimitive(property.getMostInnerItems()));
  }


  private String buildValidatorClassConstructor() {
    if (!isPrimitive) {
      return null;
    }
    String validatorPath = "com.gip.xyna.openapi.";
    StringBuilder validatorClassConstructor = new StringBuilder("new ").append(validatorPath);
    String javaClassName = DatatypeMap.getOrDefault(dataType, DatatypeMap.get("Default")).className;
    if (isList) {
      validatorClassConstructor.append("PrimitiveListTypeValidator<").append(javaClassName).append(">(").append(validatorPath)
          .append(javaClassName).append("::new").append(")");
    } else {
      validatorClassConstructor.append(javaClassName).append("()");
    }
    return validatorClassConstructor.toString();
  }

  private List<String> buildValidatorConfig(CodegenPropertyInfo propertyInfo) {
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
        
    ValuesToValidate valuesToValidate = new ValuesToValidate(propertyInfo);

    config.add("setName(\"" + propLabel + "\")");
    config.add(setValue);

    config.addAll(DatatypeMap.getOrDefault(dataType, DatatypeMap.get("Default")).setterListBuilder.apply(valuesToValidate, fix));

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
  
  public String toJson() {
    
    String result = getPropVarName + " == null ? null : ";
    if(javaType.equals("String")) {
      result += "\"\\\"\" + xfmg.xfctrl.datamodel.json.JSONDatamodelServices.encodeValue("+ 
          "new xact.templates.Document.Builder().text(" + getPropVarName + ").instance()).getText() + \"\\\"\"";
    } else {
      result += getPropVarName + ".toString()";
    }
    return result;
  }
  
  public String fromJson() {
    
    String result = "document == null ? null : ";
    if(javaType.equals("String")) {
      result += "xfmg.xfctrl.datamodel.json.JSONDatamodelServices.decodeValue(document).getText()";
    } else {
      result += javaType + ".valueOf(document.getText())";
    }
    return result;
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
        isPrimitive == that.isPrimitive &&
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
    sb.append(",\n    ").append("isPrimitive='").append(isPrimitive).append('\'');
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

    ValuesToValidate(CodegenPropertyInfo propertyInfo) {
      CodegenPropertyInfo mostInnerItems = propertyInfo.getMostInnerItems() != null ? propertyInfo.getMostInnerItems() : propertyInfo;

      minimum = mostInnerItems.getMinimum();
      maximum = mostInnerItems.getMaximum();
      excludeMin = mostInnerItems.getExclusiveMinimum();
      excludeMax = mostInnerItems.getExclusiveMaximum();
      multipleOf = mostInnerItems.getMultipleOf();
      dataFormat = mostInnerItems.getDataFormat();
      minLength = mostInnerItems.getMinLength();
      maxLength = mostInnerItems.getMaxLength();
      required = mostInnerItems.getRequired();
      nullable = mostInnerItems.getIsNullable();
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
