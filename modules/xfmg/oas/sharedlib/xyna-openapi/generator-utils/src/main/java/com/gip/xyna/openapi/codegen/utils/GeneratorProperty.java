package com.gip.xyna.openapi.codegen.utils;

import org.openapitools.codegen.DefaultCodegen;

public class GeneratorProperty {
  
  public static void setModelPath(DefaultCodegen gen, String path) {
    gen.additionalProperties().put("x-modelPath", path);
  }
  
  public static String getModelPath(DefaultCodegen gen) {
    return (String) gen.additionalProperties().get("x-modelPath");
  }
  
  public static void setProviderPath(DefaultCodegen gen, String path) {
    gen.additionalProperties().put("x-providerPath", path);
  }
  
  public static String getProviderPath(DefaultCodegen gen) {
    return (String) gen.additionalProperties().get("x-providerPath");
  }
  
  public static void setClientPath(DefaultCodegen gen, String path) {
    gen.additionalProperties().put("x-clientPath", path);
  }
  
  public static String getClientPath(DefaultCodegen gen) {
    return (String) gen.additionalProperties().get("x-clientPath");
  }
  
  // external Properties
  
  public static void setCreateListWrappers(DefaultCodegen gen, boolean createListWrappers) {
    gen.additionalProperties().put("x-createListWrappers", createListWrappers);
  }
  
  public static boolean getCreateListWrappers(DefaultCodegen gen) {
    Object propertyEntry = gen.additionalProperties().get("x-createListWrappers");
    if (propertyEntry == null) {
      return false;
    }
    return (boolean) propertyEntry;
  }
  
  public static void setDebugXO(DefaultCodegen gen, boolean debug) {
    gen.additionalProperties().put("debugXO", debug);
  }
  
  public static boolean getDebugXO(DefaultCodegen gen) {
    Object propertyEntry = gen.additionalProperties().get("debugXO");
    if (propertyEntry == null) {
      return false;
    }
    return (boolean) propertyEntry;
  }
}
