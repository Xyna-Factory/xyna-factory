package com.gip.xyna.openapi.codegen;

import static org.openapitools.codegen.utils.CamelizeOption.LOWERCASE_FIRST_LETTER;
import static org.openapitools.codegen.utils.CamelizeOption.UPPERCASE_FIRST_CHAR;
import static org.openapitools.codegen.utils.StringUtils.camelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.DefaultCodegen;

public abstract class XynaCodegenOperation {
  
  protected final String baseLabel;
  protected final String baseVarName;
  protected final String baseRefName;
  protected final String basePath;
  
  final String responseLabel;
  final String responseVarName;
  final String responseRefName;
  final String responseRefPath;
  final String responseDescription;
  
  final boolean hasBody;
  final List<XynaCodegenParameter> params;
  final List<XynaCodegenParameter> headerParams;
  final List<XynaCodegenParameter> pathParams;
  final List<XynaCodegenParameter> queryParams;
  final List<XynaCodegenParameter> bodyParams;
  final List<XynaCodegenResponse> responses;
  
  final String httpMethod;

  
  XynaCodegenOperation(CodegenOperation operation, DefaultCodegen gen, String pathPrefix) {
    
    baseLabel = operation.httpMethod + " " + operation.path;
    baseVarName = camelize(gen.sanitizeName((operation.httpMethod + "_" + operation.path)), LOWERCASE_FIRST_LETTER);
    baseRefName = camelize(baseVarName.replace(" ", "_"), UPPERCASE_FIRST_CHAR);
    basePath = gen.apiPackage() + "." + pathPrefix;
    
    responseLabel = baseLabel + " Response";
    responseVarName = baseVarName + "Response";
    responseRefName = baseRefName + "Response";
    responseRefPath = basePath + ".response";
    
    hasBody = operation.getHasBodyParam();
    params = buildXynaCodegenParameter(operation.allParams, gen);
    headerParams = buildXynaCodegenParameter(operation.headerParams, gen);
    pathParams = buildXynaCodegenParameter(operation.pathParams, gen);
    queryParams = buildXynaCodegenParameter(operation.queryParams, gen);
    bodyParams = buildXynaCodegenParameter(operation.bodyParams, gen);
    responses = buildXynaCodegenResponse(operation.responses, gen);
    
    responseDescription = buildResponseDescription(operation);

    httpMethod = operation.httpMethod;
  }
  
  protected abstract String getPropertyClassName();
  
  private List<XynaCodegenParameter> buildXynaCodegenParameter(List<CodegenParameter> params, DefaultCodegen gen) {
    List<XynaCodegenParameter> xynaParams = new ArrayList<>(params.size());
    int index = 0;
    for (CodegenParameter para: params) {
      xynaParams.add(new XynaCodegenParameter(para, gen, getPropertyClassName(), index));
      index++;
    }
    return  xynaParams;
  }
  
  private List<XynaCodegenResponse> buildXynaCodegenResponse(List<CodegenResponse> responses, DefaultCodegen gen) {
    List<XynaCodegenResponse> xynaResp = new ArrayList<>(responses.size());
    int index = 0;
    for (CodegenResponse resp: responses) {
      xynaResp.add(new XynaCodegenResponse(resp, gen, this, index));
      index++;
    }
    return  xynaResp;
  }
  
  private String buildResponseDescription(CodegenOperation operation) {
    final StringBuilder sb = new StringBuilder("Specified Responses:").append("\n    ");
    for (XynaCodegenResponse res: responses) {
      sb.append(res.getCodeWithMessage()).append("\n    ");
    }
    return sb.toString();
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof XynaCodegenOperation)) return false;
    XynaCodegenOperation that = (XynaCodegenOperation) o;
    return
        Objects.equals(baseLabel, that.baseLabel) &&
        Objects.equals(baseVarName, that.baseVarName) &&
        Objects.equals(baseRefName, that.baseRefName) &&
        Objects.equals(basePath, that.basePath) &&
        
        Objects.equals(responseLabel, that.responseLabel) &&
        Objects.equals(responseVarName, that.responseVarName) &&
        Objects.equals(responseRefName, that.responseRefName) &&
        Objects.equals(responseRefPath, that.responseRefPath) &&
        Objects.equals(responseDescription, that.responseDescription) &&

        Objects.equals(httpMethod, that.httpMethod) &&
        
        hasBody == that.hasBody &&
        Objects.equals(params, that.params) &&
        Objects.equals(headerParams, that.headerParams) &&
        Objects.equals(pathParams, that.pathParams) &&
        Objects.equals(queryParams, that.queryParams) &&
        Objects.equals(bodyParams, that.bodyParams) &&
        Objects.equals(responses, that.responses);
  }

  @Override
  public int hashCode() {
      return Objects.hash(baseLabel, baseVarName, baseRefName, basePath,
                          httpMethod, hasBody, responses,
                          params, headerParams, pathParams, queryParams, bodyParams);
  }

  protected void toString(StringBuilder sb) {
    if (sb == null) {
      return;
    }
    sb.append(",\n    ").append("httpMethod='").append(httpMethod).append('\'');

    sb.append(",\n    ").append("hasBody='").append(hasBody).append('\'');
    sb.append(",\n    ").append("params=").append(String.valueOf(params).replace("\n", "\n    "));
    sb.append(",\n    ").append("headerParams=").append(String.valueOf(params).replace("\n", "\n    "));
    sb.append(",\n    ").append("pathParams=").append(String.valueOf(params).replace("\n", "\n    "));
    sb.append(",\n    ").append("queryParams=").append(String.valueOf(params).replace("\n", "\n    "));
    sb.append(",\n    ").append("bodyParams=").append(String.valueOf(params).replace("\n", "\n    "));
    sb.append(",\n    ").append("responses=").append(String.valueOf(responses).replace("\n", "\n    "));
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder(getClass().getSimpleName()).append("{");
      toString(sb);
      sb.append("\n}");
      return sb.toString();
  }
}
