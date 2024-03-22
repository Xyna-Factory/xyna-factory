package com.gip.xyna.openapi.codegen;

import java.util.List;
import java.util.Objects;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.DefaultCodegen;

public class XynaCodegenProviderOperation extends XynaCodegenOperation {
  
  final String implLabel;
  final String implVarName;
  final String implDescription;

  final int parameterId;
  final String parameterLabel;
  final String parameterVarName;
  final String parameterRefName;
  final String parameterRefPath;

  final int responseId;
  
  final String endpointWorkflowLabel;
  final String endpointWorkflowTypeName;
  final String endpointWorkflowPath;
  
  final String filterRegexPath;
  
  XynaCodegenProviderOperation(CodegenOperation operation, DefaultCodegen gen, String pathPrefix, int id) {
    super(operation, gen, pathPrefix);
    
    implLabel = baseLabel;
    implVarName = baseVarName;
    implDescription = operation.summary + "\n" + operation.notes;
    
    parameterId = id;
    parameterLabel = baseLabel + " Parameter";
    parameterVarName = baseVarName + "Parameter";
    parameterRefName = baseRefName + "Parameter";
    parameterRefPath = basePath + ".request";
    
    responseId = id + 1;
    
    endpointWorkflowLabel = baseLabel + " Endpoint";
    endpointWorkflowTypeName = baseRefName + "Endpoint";
    endpointWorkflowPath = basePath + ".wf";
    
    filterRegexPath = buildFilterRegexp(operation, pathParams);
  }
  
  @Override
  protected String getPropertyClassName() {
    return parameterRefName;
  }
  
  public String buildFilterRegexp(CodegenOperation operation, List<XynaCodegenParameter> pathParams) {
    String regexPath = operation.path;
    for(XynaCodegenParameter param : pathParams) {
      if (param.isPrimitive) {
        if (param.javaType == "Integer" || param.javaType == "Long" || param.javaType == "Double" || param.javaType == "Float") {
          regexPath = regexPath.replaceAll("\\{" + param.propLabel + "\\}", "(?<" + param.propLabel + ">[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)");
        } else if (param.dataType == "String") {
          regexPath = regexPath.replaceAll("\\{" + param.propLabel + "\\}", "(?<" + param.propLabel + ">[^/?]*)");
        } else if (param.dataType == "Boolean") {
          regexPath = regexPath.replaceAll("\\{" + param.propLabel + "\\}", "(?<" + param.propLabel + ">([fF][aA][lL][sS][eE])|([tT][rR][uU][eE]))");
        }
      }
    }
    return regexPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!super.equals(o)) return false;
    if (!(o instanceof XynaCodegenProviderOperation)) return false;
    XynaCodegenProviderOperation that = (XynaCodegenProviderOperation) o;
    return
        Objects.equals(implLabel, that.implLabel) &&
        Objects.equals(implVarName, that.implVarName) &&
        Objects.equals(implDescription, that.implDescription) &&

        parameterId == that.parameterId &&
        Objects.equals(parameterLabel, that.parameterLabel) &&
        Objects.equals(parameterVarName, that.parameterVarName) &&
        Objects.equals(parameterRefName, that.parameterRefName) &&
        Objects.equals(parameterRefPath, that.parameterRefPath) &&

        responseId == that.responseId &&
        
        Objects.equals(endpointWorkflowLabel, that.endpointWorkflowLabel) &&
        Objects.equals(endpointWorkflowTypeName, that.endpointWorkflowTypeName) &&
        Objects.equals(endpointWorkflowPath, that.endpointWorkflowPath) &&

        Objects.equals(filterRegexPath, that.filterRegexPath);
  }

  @Override
  public int hashCode() {
    int hash = super.hashCode();
    hash = 89 * Objects.hash(implLabel, implVarName, implDescription,
                            parameterId, parameterLabel, parameterVarName, parameterRefName, parameterRefPath,
                            responseId,
                            endpointWorkflowLabel, endpointWorkflowTypeName, endpointWorkflowPath,
                            filterRegexPath);
    return hash;
  }
  
  @Override
  protected void toString(StringBuilder sb) {
    if (sb == null) {
      return;
    }
    super.toString(sb);
    sb.append(",\n    ").append("implLabel='").append(implLabel).append('\'');
    sb.append(",\n    ").append("implVarName='").append(implVarName).append('\'');
    sb.append(",\n    ").append("implDescription='").append(String.valueOf(implDescription).replace("\n", "\\n")).append('\'');

    sb.append(",\n    ").append("parameterId='").append(parameterId).append('\'');
    sb.append(",\n    ").append("parameterLabel='").append(parameterLabel).append('\'');
    sb.append(",\n    ").append("parameterVarName='").append(parameterVarName).append('\'');
    sb.append(",\n    ").append("parameterRefName='").append(parameterRefName).append('\'');
    sb.append(",\n    ").append("parameterRefPath='").append(parameterRefPath).append('\'');

    sb.append(",\n    ").append("responseId='").append(responseId).append('\'');
    
    sb.append(",\n    ").append("endpointWorkflowLabel='").append(endpointWorkflowLabel).append('\'');
    sb.append(",\n    ").append("endpointWorkflowTypeName='").append(endpointWorkflowTypeName).append('\'');
    sb.append(",\n    ").append("endpointWorkflowPath='").append(endpointWorkflowPath).append('\'');
    
    sb.append(",\n    ").append("filterRegexPath='").append(filterRegexPath).append('\'');
  }
}
