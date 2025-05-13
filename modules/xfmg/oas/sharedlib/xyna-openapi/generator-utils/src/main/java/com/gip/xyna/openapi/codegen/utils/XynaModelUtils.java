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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openapitools.codegen.CodegenModel;
import org.openapitools.codegen.model.ApiInfoMap;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;

public class XynaModelUtils {

  private static final Map<String, String> typeMapping = Map.ofEntries(
    Map.entry("boolean", "Boolean"),
    Map.entry("integer", "Long"),
    Map.entry("long", "Long"),
    Map.entry("double", "Double"),
    Map.entry("float", "Double"),
    Map.entry("number", "Double"),
    Map.entry("string", "String"),
    Map.entry("DateTime", "DateTimeType"),
    Map.entry("date", "DateType"),
    Map.entry("password", "String"),
    Map.entry("byte", "String"),
    Map.entry("binary", "String"),
    Map.entry("URI", "String")
  );

  public static Map<String, String> getTypeMapping() {
    return typeMapping;
  }

  public static Map<String, ModelMap> getModelsFromSupportingFileData(Map<String, Object> objs) {
    @SuppressWarnings("unchecked")
    List<ModelMap> models = (List<ModelMap>) objs.get("models");
    Map<String, ModelMap> modelMap = new HashMap<>();
    for (ModelMap model: models) {
      modelMap.put(model.getModel().getName(), model);
    }
    return modelMap;
  }

  public static List<OperationMap> getOperationsFromSupportingFileData(Map<String, Object> objs) {
    ApiInfoMap apiInfo = (ApiInfoMap) objs.get("apiInfo");
    List<OperationsMap> apiList = ((List<OperationsMap>) apiInfo.getApis());
    List<OperationMap> operations = new ArrayList<>();
    for(OperationsMap api: apiList) {
      operations.add(api.getOperations());
    }
    return operations;
  }

  public static Map<String, ModelMap> getModelsFromAllModels(Map<String, ModelsMap> objs) {
    Map<String, ModelMap> modelMap = new HashMap<>();
    for (String modelname: objs.keySet()) {
      modelMap.put(modelname, getModelMapByName(modelname, objs));
    }
    return modelMap;
  }

  public static ModelMap getModelMapByName(String name, Map<String, ModelsMap> models) {
    ModelsMap data = models.get(name);
    if (data != null) {
      List<ModelMap> modelMapList = data.getModels();
      if (modelMapList != null) {
        for (ModelMap entryMap : modelMapList) {
          CodegenModel model = entryMap.getModel();
          if (model != null) {
            return entryMap;
          }
        }
      }
    }
    return null;
  }
  
  public static boolean parentModelHasAdditionalProperties(CodegenModel model) {
    while (model != null) {
      if (model.isAdditionalPropertiesTrue) {
        return true;
      }
      model = model.getParentModel();
    }
    return false;
  }
}
