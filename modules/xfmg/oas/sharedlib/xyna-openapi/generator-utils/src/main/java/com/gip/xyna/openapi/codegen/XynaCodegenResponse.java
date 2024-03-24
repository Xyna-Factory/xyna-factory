package com.gip.xyna.openapi.codegen;

import com.gip.xyna.openapi.codegen.utils.Camelizer.Case;
import static com.gip.xyna.openapi.codegen.utils.Camelizer.camelize;

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
  
  final int index;
  final String code;
  final String message;
  
  final XynaCodegenProperty body;
  final List<XynaCodegenProperty> responseHeaders;
  
  public XynaCodegenResponse(CodegenResponse response, DefaultCodegen gen, XynaCodegenOperation operation, int index) {

    code = response.code;
    message = message(response);
    respLabel = operation.responseLabel + " " + getCodeWithMessage();
    respRefName = camelize(operation.responseRefName + "_" + getCodeWithMessage(), Case.PASCAL);
    respRefPath = operation.responseRefPath;
    respDescription = buildRespDescription(response);
    this.index = index;
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
  
  public String getCodeWithMessage() {
    if (message.isBlank()) {
      return code;
    }
    return code + " " + message;
  }
  
  private String message(CodegenResponse response) {
    if (response.message != null && !response.message.isBlank()) {
      return response.message;
    }
    if (StatusCodeLambda.httpStatusCodes.containsKey(response.code)) {
      return StatusCodeLambda.httpStatusCodes.get(code);
    }
    return "";
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
        index == that.index &&
        Objects.equals(code, that.code) &&
        Objects.equals(message, that.message) &&
        Objects.equals(body, that.body) &&
        Objects.equals(responseHeaders, that.responseHeaders);

  }
  
  @Override
  public int hashCode() {
      return Objects.hash(respLabel, respRefName, respRefPath, respDescription,
                          index, code, message, body, responseHeaders);
  }
  
  @Override
  public String toString() {
      final StringBuilder sb = new StringBuilder("XynaCodegenResponse{");
      sb.append("\n    ").append("respLabel='").append(respLabel).append('\'');
      sb.append(",\n    ").append("respRefName='").append(respRefName).append('\'');
      sb.append(",\n    ").append("respRefPath='").append(respRefPath).append('\'');
      sb.append(",\n    ").append("respDescription='").append(String.valueOf(respDescription).replace("\n", "\\n")).append('\'');
      sb.append(",\n    ").append("index='").append(index).append('\'');
      sb.append(",\n    ").append("code='").append(code).append('\'');
      sb.append(",\n    ").append("message='").append(message).append('\'');
      sb.append(",\n    ").append("body='").append(String.valueOf(body).replace("\n", "\n    ")).append('\'');
      sb.append(",\n    ").append("responseHeaders='").append(String.valueOf(responseHeaders).replace("\n", "\n    ")).append('\'');
      sb.append("\n}");
      return sb.toString();
  }
}
