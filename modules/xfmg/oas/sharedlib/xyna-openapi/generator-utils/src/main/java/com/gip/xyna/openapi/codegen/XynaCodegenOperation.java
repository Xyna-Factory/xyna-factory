package com.gip.xyna.openapi.codegen;

import static org.openapitools.codegen.utils.CamelizeOption.UPPERCASE_FIRST_CHAR;
import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.StringUtils.camelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.DefaultCodegen;

public class XynaCodegenOperation {
  
  final String implLabel;
  final String implVarName;
  final String implDescription;

  final int parameterId;
  final String parameterLabel;
  final String parameterVarName;
  final String parameterRefName;
  final String parameterRefPath;

  final int responseId;
  final String responseLabel;
  final String responseVarName;
  final String responseRefName;
  final String responseRefPath;
  final String responseDescription;
  
  final String workflowLabel;
  final String workflowTypeName;
  final String workflowPath;
  
  final String filterRegexPath;
  
  final boolean hasBody;
  final List<XynaCodegenParameter> params = new ArrayList<>();
  final List<XynaCodegenResponse> responses = new ArrayList<>();
  
  final String httpMethod;

  
  XynaCodegenOperation(CodegenOperation operation, DefaultCodegen gen, String pathPrefix, int id) {
    
    implLabel = operation.httpMethod + " " + operation.path;
    implVarName = camelize(gen.sanitizeName((operation.httpMethod + "_" + operation.path)), LOWERCASE_FIRST_LETTER);
    implDescription = operation.summary + "\n" + operation.notes;
    
    parameterId = id;
    parameterLabel = implLabel + " Parameter";
    parameterVarName = implVarName + "Parameter";
    parameterRefName = camelize(implVarName.replace(" ", "_"), UPPERCASE_FIRST_CHAR) + "Parameter";
    parameterRefPath = gen.apiPackage() + "." + pathPrefix + ".request";
    
    responseId = id + 1;
    responseLabel = implLabel + " Response";
    responseVarName = implVarName + "Response";
    responseRefName = camelize(implVarName.replace(" ", "_"), UPPERCASE_FIRST_CHAR) + "Response";
    responseRefPath = gen.apiPackage() + "." + pathPrefix + ".response";
    responseDescription = buildResponseDescription(operation);
    
    workflowLabel = implLabel + " Endpoint";
    workflowTypeName = camelize(implVarName.replace(" ", "_"), UPPERCASE_FIRST_CHAR) + "Endpoint";
    workflowPath = gen.apiPackage() + "." + pathPrefix + ".wf";
    
    filterRegexPath = buildFilterRegexp(operation);
    
    hasBody = operation.getHasBodyParam();
    for (CodegenParameter para: operation.allParams) {
      params.add(new XynaCodegenParameter(para, gen, parameterRefName));
    }
    for (CodegenResponse res: operation.responses) {
      responses.add(new XynaCodegenResponse(res, gen, this));
    }
    
    httpMethod = operation.httpMethod;
  }
  
  public String buildFilterRegexp(CodegenOperation operation) {
    String regexPath = operation.path;
    if (operation.pathParams != null) {
      for(CodegenParameter param : operation.pathParams) {
        if (param.isNumeric || param.isInteger || param.isLong || param.isNumber || param.isFloat || param.isDouble) {
          regexPath = regexPath.replaceAll("\\{" + param.baseName + "\\}", "(?<" + param.baseName + ">[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)");
        }
        else if (param.isString) {
          regexPath = regexPath.replaceAll("\\{" + param.baseName + "\\}", "(?<" + param.baseName + ">[^/?]*)");
        }
      }
    }
    return regexPath;
  }
  
  private String buildResponseDescription(CodegenOperation operation) {
    final StringBuilder sb = new StringBuilder("Specified Responses:").append("\n    ");
    for (XynaCodegenResponse res: responses) {
      sb.append(res.codeWithMessage).append("\n    ");
    }
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof XynaCodegenOperation)) return false;
    XynaCodegenOperation that = (XynaCodegenOperation) o;
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
        Objects.equals(responseLabel, that.responseLabel) &&
        Objects.equals(responseVarName, that.responseVarName) &&
        Objects.equals(responseRefName, that.responseRefName) &&
        Objects.equals(responseRefPath, that.responseRefPath) &&
        Objects.equals(responseDescription, that.responseDescription) &&
        
        Objects.equals(workflowLabel, that.workflowLabel) &&
        Objects.equals(workflowTypeName, that.workflowTypeName) &&
        Objects.equals(workflowPath, that.workflowPath) &&

        Objects.equals(filterRegexPath, that.filterRegexPath) &&
        Objects.equals(httpMethod, that.httpMethod) &&
        
        hasBody == that.hasBody &&
        Objects.equals(params, that.params) &&
        Objects.equals(responses, that.responses);
  }

  @Override
  public int hashCode() {
      return Objects.hash(implLabel, implVarName, implDescription,
                          parameterId, parameterLabel, parameterVarName, parameterRefName, parameterRefPath,
                          responseId, responseLabel, responseVarName, responseRefName, responseRefPath, responseDescription,
                          workflowLabel, workflowTypeName, workflowPath,
                          filterRegexPath, httpMethod,
                          hasBody, params, responses);
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder("XynaCodegenOperation{");
      sb.append(",\n    ").append("implLabel='").append(implLabel).append('\'');
      sb.append(",\n    ").append("implVarName='").append(implVarName).append('\'');
      sb.append(",\n    ").append("implDescription='").append(String.valueOf(implDescription).replace("\n", "\\n")).append('\'');

      sb.append(",\n    ").append("parameterId='").append(parameterId).append('\'');
      sb.append(",\n    ").append("parameterLabel='").append(parameterLabel).append('\'');
      sb.append(",\n    ").append("parameterVarName='").append(parameterVarName).append('\'');
      sb.append(",\n    ").append("parameterRefName='").append(parameterRefName).append('\'');
      sb.append(",\n    ").append("parameterRefPath='").append(parameterRefPath).append('\'');

      sb.append(",\n    ").append("responseId='").append(responseId).append('\'');
      sb.append(",\n    ").append("responseLabel='").append(responseLabel).append('\'');
      sb.append(",\n    ").append("responseVarName='").append(responseVarName).append('\'');
      sb.append(",\n    ").append("responseRefName='").append(responseRefName).append('\'');
      sb.append(",\n    ").append("responseRefPath='").append(responseRefPath).append('\'');
      sb.append(",\n    ").append("responseDescription='").append(String.valueOf(responseDescription).replace("\n", "\\n")).append('\'');
      
      sb.append(",\n    ").append("workflowLabel='").append(workflowLabel).append('\'');
      sb.append(",\n    ").append("workflowTypeName='").append(workflowTypeName).append('\'');
      sb.append(",\n    ").append("workflowPath='").append(workflowPath).append('\'');
      
      sb.append(",\n    ").append("filterRegexPath='").append(filterRegexPath).append('\'');
      sb.append(",\n    ").append("httpMethod='").append(httpMethod).append('\'');

      sb.append(",\n    ").append("hasBody='").append(hasBody).append('\'');
      sb.append(",\n    ").append("params=").append(String.valueOf(params).replace("\n", "\n    "));
      sb.append(",\n    ").append("responses=").append(String.valueOf(responses).replace("\n", "\n    "));
      sb.append("\n}");
      return sb.toString();
  }
}
