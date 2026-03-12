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
package com.gip.xyna.openapi.codegen.utils;

import org.openapitools.codegen.DefaultCodegen;

public class GeneratorProperty {
  
  public static void setModelPath(DefaultCodegen gen, String path) {
    gen.additionalProperties().put("x-model-path", path);
  }
  
  public static String getModelPath(DefaultCodegen gen) {
    return (String) gen.additionalProperties().get("x-model-path");
  }
  
  public static void setProviderPath(DefaultCodegen gen, String path) {
    gen.additionalProperties().put("x-provider-path", path);
  }
  
  public static String getProviderPath(DefaultCodegen gen) {
    return (String) gen.additionalProperties().get("x-provider-path");
  }
  
  public static void setClientPath(DefaultCodegen gen, String path) {
    gen.additionalProperties().put("x-client-path", path);
  }
  
  public static String getClientPath(DefaultCodegen gen) {
    return (String) gen.additionalProperties().get("x-client-path");
  }
  
  public static void setFilterName(DefaultCodegen gen, String name) {
    gen.additionalProperties().put("x-filter-name", name);
  }
  
  public static String getFilterName(DefaultCodegen gen) {
    return (String) gen.additionalProperties().get("x-filter-name");
  }
  
  // external Properties
  
  public static void setCreateListWrappers(DefaultCodegen gen, boolean createListWrappers) {
    gen.additionalProperties().put("x-createListWrappers", createListWrappers);
  }
  
  public static boolean getCreateListWrappers(DefaultCodegen gen) {
    return (boolean) gen.additionalProperties().getOrDefault("x-createListWrappers", false);
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
