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

import com.gip.xyna.openapi.codegen.utils.GeneratorProperty;
import com.gip.xyna.openapi.codegen.utils.Camelizer.Case;

import static com.gip.xyna.openapi.codegen.utils.Camelizer.camelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.DefaultCodegen;

import com.gip.xyna.openapi.codegen.factory.XynaCodegenFactory;
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
  final XynaCodegenProperty additionalProperty;
  final List<XynaCodegenProperty> responseHeaders;
  
  public XynaCodegenResponse(XynaCodegenFactory factory, CodegenResponse response, DefaultCodegen gen, XynaCodegenOperation operation, int index) {

    code = response.code;
    message = message(response);
    respLabel = operation.responseLabel + " " + getCodeWithMessage();
    respRefName = getName(operation.responseRefName, response);
    respRefPath = operation.responseRefPath;
    respDescription = buildRespDescription(response);
    this.index = index;
    if (response.returnProperty != null) {
      body = factory.getOrCreateXynaCodegenProperty(response.returnProperty, respRefName);
    } else {
      body = null;
    }
    if (response.getAdditionalProperties() != null) {
      additionalProperty = factory.getPropertyToAddionalPropertyWrapper(response.getAdditionalProperties(), respRefName);
    } else {
      additionalProperty = null;
    }
    if (response.headers != null) {
      responseHeaders = response.headers.stream()
          .map(prop -> factory.getOrCreateXynaCodegenProperty(prop, respRefName))
          .collect(Collectors.toList());
    } else {
      responseHeaders = new ArrayList<>();
    }
  }
  
  public String getCodeWithMessage() {
    return getCodeWithMessage(code, message);
  }
  
  public static String getCodeWithMessage(CodegenResponse response) {
    return getCodeWithMessage(response.code, message(response));
  }
  
  private static String getCodeWithMessage(String code, String message) {
    if (message.isBlank()) {
      return code;
    }
    return code + " " + message;
  }
  
  private static String message(CodegenResponse response) {
    if (StatusCodeLambda.httpStatusCodes.containsKey(response.code)) {
      return StatusCodeLambda.httpStatusCodes.get(response.code);
    }
    return "";
  }
  
  private String buildRespDescription(CodegenResponse response) {
    if (response.message != null) {
      return "Response message: " + response.message;
    }
    return "";
  }
  
  public String getRespFQN() {
    return respRefPath + "." + respRefName;
  }
  
  public static String getName(String baseName, CodegenResponse response) {
    return camelize(baseName + "_" + getCodeWithMessage(response), Case.PASCAL);
  }
  
  public static String getClientFQN(CodegenOperation operation, DefaultCodegen gen, String pathPrefix, CodegenResponse response) {
    return getFQN(operation, gen, GeneratorProperty.getClientPath(gen), pathPrefix, response);
  }
  
  public static String getProviderFQN(CodegenOperation operation, DefaultCodegen gen, String pathPrefix, CodegenResponse response) {
    return getFQN(operation, gen, GeneratorProperty.getProviderPath(gen), pathPrefix, response);
  }
  
  private static String getFQN(CodegenOperation operation, DefaultCodegen gen, String path, String pathPrefix, CodegenResponse response) {
    return XynaCodegenOperation.buildResponseRefPath(operation, gen, path, pathPrefix) + '.' +
        getName(XynaCodegenOperation.buildResponseRefName(operation, gen), response);
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
