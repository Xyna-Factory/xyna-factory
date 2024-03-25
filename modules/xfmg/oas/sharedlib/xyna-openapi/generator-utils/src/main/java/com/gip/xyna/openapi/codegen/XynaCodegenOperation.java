package com.gip.xyna.openapi.codegen;

import com.gip.xyna.openapi.codegen.utils.Sanitizer;
import com.gip.xyna.openapi.codegen.utils.Camelizer.Case;
import static com.gip.xyna.openapi.codegen.utils.Camelizer.camelize;

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
  final List<XynaCodegenProperty> params;
  final List<XynaCodegenProperty> headerParams;
  final List<XynaCodegenProperty> pathParams;
  final List<XynaCodegenProperty> queryParams;
  final List<XynaCodegenProperty> bodyParams;
  final List<XynaCodegenResponse> responses;
  
  final String httpMethod;

  
  XynaCodegenOperation(CodegenOperation operation, DefaultCodegen gen, String pathPrefix) {
    
    baseLabel = operation.httpMethod + " " + operation.path;
    baseVarName = camelize(gen.sanitizeName(operation.httpMethod + "_" + operation.path), Case.CAMEL);
    baseRefName = camelize(baseVarName, Case.PASCAL);
    basePath = Sanitizer.sanitize(gen.apiPackage() + "." + pathPrefix);
    
    responseLabel = baseLabel + " Response";
    responseVarName = baseVarName + "Response";
    responseRefName = baseRefName + "Response";
    responseRefPath = basePath + ".response";
    
    hasBody = operation.getHasBodyParam();
    params = buildXynaCodegenProperty(operation.allParams, gen);
    headerParams = buildXynaCodegenProperty(operation.headerParams, gen);
    pathParams = buildXynaCodegenProperty(operation.pathParams, gen);
    queryParams = buildXynaCodegenProperty(operation.queryParams, gen);
    bodyParams = buildXynaCodegenProperty(operation.bodyParams, gen);
    responses = buildXynaCodegenResponse(operation.responses, gen);
    
    responseDescription = buildResponseDescription(operation);

    httpMethod = operation.httpMethod;
  }
  
  protected abstract String getPropertyClassName();
  
  private List<XynaCodegenProperty> buildXynaCodegenProperty(List<CodegenParameter> params, DefaultCodegen gen) {
    List<XynaCodegenProperty> xynaProp = new ArrayList<>(params.size());
    for (CodegenParameter para: params) {
      xynaProp.add(new XynaCodegenProperty(new CodegenParameterHolder(para), gen, getPropertyClassName()));
    }
    return  xynaProp;
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
    sb.append(",\n    ").append("headerParams=").append(String.valueOf(headerParams).replace("\n", "\n    "));
    sb.append(",\n    ").append("pathParams=").append(String.valueOf(pathParams).replace("\n", "\n    "));
    sb.append(",\n    ").append("queryParams=").append(String.valueOf(queryParams).replace("\n", "\n    "));
    sb.append(",\n    ").append("bodyParams=").append(String.valueOf(bodyParams).replace("\n", "\n    "));
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
