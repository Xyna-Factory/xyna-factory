package com.gip.xyna.openapi.codegen;

import static org.openapitools.codegen.utils.CamelizeOption.UPPERCASE_FIRST_CHAR;
import static org.openapitools.codegen.utils.StringUtils.camelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.DefaultCodegen;

import com.gip.xyna.openapi.codegen.templating.mustache.StatusCodeLambda;

public class XynaCodegenResponse {

  final String respLabel;
  final String respRefName;
  final String respRefPath;
  final String respDescription;
  final String codeWithMessage;
  
  final XynaCodegenProperty body;
  final List<XynaCodegenProperty> responseHeaders;
  
  public XynaCodegenResponse(CodegenResponse response, DefaultCodegen gen, XynaCodegenOperation operation) {

    codeWithMessage = codeWithMessage(response.code);
    respLabel = operation.responseLabel + " " + codeWithMessage;
    respRefName = camelize((operation.responseRefName + "_" + codeWithMessage).replace(" ", "_"), UPPERCASE_FIRST_CHAR);
    respRefPath = operation.responseRefPath;
    respDescription = buildRespDescription(response);
    if (response.returnProperty != null) {
      body = new XynaCodegenProperty(new CodegenPropertyHolder(response.returnProperty), gen, respRefName);
    } else {
      body = null;
    }
    if (response.headers != null) {
      responseHeaders = response.headers.stream()
          .map(prop -> new XynaCodegenProperty(new CodegenPropertyHolder(prop), gen, respRefName))
          .collect(Collectors.toList());
    } else {
      responseHeaders = new ArrayList<>();
    }
  }
  
  private String codeWithMessage(String code) {
    if (StatusCodeLambda.httpStatusCodes.containsKey(code)) {
      return code + " " + StatusCodeLambda.httpStatusCodes.get(code);
    } else {
      return code;
    }
  }
  
  private String buildRespDescription(CodegenResponse response) {
    if (response.message != null) {
      return "Response message: " + response.message;
    }
    return "";
  }
  
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof XynaCodegenResponse)) return false;
    XynaCodegenResponse that = (XynaCodegenResponse) o;
    return Objects.equals(respLabel, that.respLabel) &&
        Objects.equals(respRefName, that.respRefName) &&
        Objects.equals(respRefPath, that.respRefPath) &&
        Objects.equals(respDescription, that.respDescription) &&
        Objects.equals(codeWithMessage, that.codeWithMessage) &&
        Objects.equals(body, that.body) &&
        Objects.equals(responseHeaders, that.responseHeaders);

  }
  
  @Override
  public int hashCode() {
      return Objects.hash(respLabel, respRefName, respRefPath, respDescription,
                          codeWithMessage, body, responseHeaders);
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder("XynaCodegenResponse{");
      sb.append("\n    ").append("respLabel='").append(respLabel).append('\'');
      sb.append(",\n    ").append("respRefName='").append(respRefName).append('\'');
      sb.append(",\n    ").append("respRefPath='").append(respRefPath).append('\'');
      sb.append(",\n    ").append("respDescription='").append(String.valueOf(respDescription).replace("\n", "\\n")).append('\'');
      sb.append(",\n    ").append("codeWithMessage='").append(codeWithMessage).append('\'');
      sb.append(",\n    ").append("body='").append(String.valueOf(body).replace("\n", "\n    ")).append('\'');
      sb.append(",\n    ").append("responseHeaders='").append(String.valueOf(responseHeaders).replace("\n", "\n    ")).append('\'');
      sb.append("\n}");
      return sb.toString();
  }
}
