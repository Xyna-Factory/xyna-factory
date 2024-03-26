package com.gip.xyna.openapi.codegen.factory;

import java.util.HashMap;
import java.util.Map;

import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.CodegenOperation;
import org.openapitools.codegen.CodegenParameter;
import org.openapitools.codegen.CodegenProperty;
import org.openapitools.codegen.CodegenResponse;
import org.openapitools.codegen.DefaultCodegen;

import com.gip.xyna.openapi.codegen.XynaCodegenClientOperation;
import com.gip.xyna.openapi.codegen.XynaCodegenModel;
import com.gip.xyna.openapi.codegen.XynaCodegenOperation;
import com.gip.xyna.openapi.codegen.XynaCodegenProperty;
import com.gip.xyna.openapi.codegen.XynaCodegenProviderOperation;
import com.gip.xyna.openapi.codegen.XynaCodegenResponse;

public class XynaCodegenFactory {

  private final static Map<XynaCodegenModel, XynaCodegenModel> xynaModels = new HashMap<XynaCodegenModel, XynaCodegenModel>();
  private final static Map<XynaCodegenProviderOperation, XynaCodegenProviderOperation> xynaProviderOperation = new HashMap<XynaCodegenProviderOperation, XynaCodegenProviderOperation>();
  private final static Map<XynaCodegenClientOperation, XynaCodegenClientOperation> xynaClientOperation = new HashMap<XynaCodegenClientOperation, XynaCodegenClientOperation>();
  private final static Map<XynaCodegenResponse, XynaCodegenResponse> xynaResponses = new HashMap<XynaCodegenResponse, XynaCodegenResponse>();
  private final static Map<XynaCodegenProperty, XynaCodegenProperty> xynaProperties = new HashMap<XynaCodegenProperty, XynaCodegenProperty>();

  private DefaultCodegen gen;
  
  public XynaCodegenFactory(DefaultCodegen generator) {
    setGenerator(generator);
  }

  public void setGenerator(DefaultCodegen generator) {
    gen = generator;
  }

  public XynaCodegenModel getOrCreateXynaCodegenModel(CodegenModel model) {
    XynaCodegenModel newModel = new XynaCodegenModel(this, model, gen);
    xynaModels.putIfAbsent(newModel, newModel);
    return xynaModels.get(newModel);
  }

  public XynaCodegenProviderOperation getOrCreateXynaCodegenProviderOperation(CodegenOperation operation, String pathPrefix, int id) {
    XynaCodegenProviderOperation newOperation = new XynaCodegenProviderOperation(this, operation, gen, pathPrefix, id);
    xynaProviderOperation.putIfAbsent(newOperation, newOperation);
    return xynaProviderOperation.get(newOperation);
  }

  public XynaCodegenClientOperation getOrCreateXynaCodegenClientOperation(CodegenOperation operation, String pathPrefix, int id) {
    XynaCodegenClientOperation newOperation = new XynaCodegenClientOperation(this, operation, gen, pathPrefix, id);
    xynaClientOperation.putIfAbsent(newOperation, newOperation);
    return xynaClientOperation.get(newOperation);
  }

  public XynaCodegenResponse getOrCreateXynaCodegenResponse(CodegenResponse response, XynaCodegenOperation operation, int index) {
    XynaCodegenResponse newResponse = new XynaCodegenResponse(this, response, gen, operation, index);
    xynaResponses.putIfAbsent(newResponse, newResponse);
    return xynaResponses.get(newResponse);
  }

  public XynaCodegenProperty getOrCreateXynaCodegenProperty(CodegenProperty property, String className) {
    CodegenPropertyHolder holder = new CodegenPropertyHolder(property);
    return getOrCreateXynaCodegenProperty(holder, className);
  }

  public XynaCodegenProperty getOrCreateXynaCodegenProperty(CodegenParameter parameter, String className) {
    CodegenParameterHolder holder = new CodegenParameterHolder(parameter);
    return getOrCreateXynaCodegenProperty(holder, className);
  }

  public XynaCodegenProperty getOrCreateXynaCodegenEnumProperty(Map<String, Object> allowableValues, String className) {
    CodegenEnum holder = new CodegenEnum(allowableValues);
    return getOrCreateXynaCodegenProperty(holder, className);
  }

  public XynaCodegenProperty getOrCreateXynaCodegenProperty(CodegenPropertyInfo info, String className) {
    XynaCodegenProperty newProperty = new XynaCodegenProperty(info, gen, className);
    xynaProperties.putIfAbsent(newProperty, newProperty);
    return xynaProperties.get(newProperty);
  }
}
