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

import org.openapitools.codegen.*;
import org.openapitools.codegen.model.*;

import com.gip.xyna.openapi.codegen.factory.XynaCodegenFactory;
import com.gip.xyna.openapi.codegen.templating.mustache.IndexLambda;
import com.gip.xyna.openapi.codegen.templating.mustache.StatusCodeLambda;
import com.google.common.collect.ImmutableMap;
import com.samskivert.mustache.Mustache.Lambda;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import java.util.*;
import java.io.File;

public class XmomServerGenerator extends DefaultCodegen {

  // source folder where to write the files
  protected String sourceFolder = "XMOM";
  protected String xynaFactoryVersion = "CURRENT_VERSION";
  private XynaCodegenFactory codegenFactory= new XynaCodegenFactory(this);
  
  public static final String XYNA_FACTORY_VERSION = "xynaFactoryVersion";

  /**
   * Configures the type of generator.
   *
   * @return  the CodegenType for this generator
   * @see     org.openapitools.codegen.CodegenType
   */
  public CodegenType getTag() {
    return CodegenType.SERVER;
  }

  /**
   * Configures a friendly name for the generator.  This will be used by the generator
   * to select the library with the -g flag.
   *
   * @return the friendly name for the generator
   */
  public String getName() {
    return "xmom-server";
  }

  /**
   * any special handling of the entire OpenAPI spec document 
   */
  @Override
  public void preprocessOpenAPI(OpenAPI openAPI) {
    super.preprocessOpenAPI(openAPI);

    Info info = openAPI.getInfo();

    // replace spaces, "-", "." with underscores in info.title
    info.setTitle(sanitizeName(info.getTitle()));

    Map<String, Object> vendorExtentions = info.getExtensions();    

    // change the path of the generated XMOMs
    if (vendorExtentions != null) {
      String xModelPath = (String)vendorExtentions.get("x-model-path");
      if (xModelPath != null && !xModelPath.trim().isEmpty()) {
        modelPackage = xModelPath.replace('-', '_').replace(' ', '_').toLowerCase();
      }

      String xProviderPath = (String)vendorExtentions.get("x-provider-path");
      if (xProviderPath != null && !xProviderPath.trim().isEmpty()) {
        apiPackage = xProviderPath.replace('-', '_').replace(' ', '_').toLowerCase();
      }
    }
    
    /**
     * Supporting Files.  You can write single files for the generator with the
     * entire object tree available.  If the input file has a suffix of `.mustache
     * it will be processed by the template engine.  Otherwise, it will be copied
     */
    supportingFiles.add(new SupportingFile("OASFilter.mustache",   // the input template or file
      "filter/OASFilter",                                                // the destination folder, relative `outputFolder`
      "OASFilter.java")                                     // the output file
    );
    supportingFiles.add(new SupportingFile("OASDecider.mustache", "XMOM/" + apiPackage.replace('.', '/') + "/decider", "OASDecider.xml"));
    supportingFiles.add(new SupportingFile("application.mustache", "", "application.xml"));
  }

  /**
   * Provides an opportunity to inspect and modify operation data before the code is generated.
   */
  @Override
  public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
    OperationsMap results = super.postProcessOperationsWithModels(objs, allModels);

    OperationMap ops = results.getOperations();
    List<CodegenOperation> opList = ops.getOperation();
    if (!opList.isEmpty()) {
      String tag = opList.get(0).baseName;
      ops.put("apiLabel", tag + " Api");
      ops.put("apiRefName", tag + "Api");
      ops.put("apiRefPath", apiPackage);
    }
    
    List<XynaCodegenOperation> xoperationList = new ArrayList<XynaCodegenOperation>(opList.size());

    int index = 0;
    for(CodegenOperation co : opList){
      XynaCodegenOperation xOperation = codegenFactory.getOrCreateXynaCodegenProviderOperation(co, (String) ops.get("pathPrefix"), 2*index);
      xoperationList.add(xOperation);
      if (Boolean.TRUE.equals(additionalProperties.get("debugXO"))) {
        System.out.println(xOperation);
      }
      index++;
    }

    ops.put("xynaOperation" , xoperationList);
    return results;
  }
  
  @Override
  public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
    objs = super.postProcessSupportingFileData(objs);
    @SuppressWarnings("unchecked")
    List<ModelMap> models = (ArrayList<ModelMap>) objs.get("models");
    List<XynaCodegenModel> xModels = new ArrayList<XynaCodegenModel>();
    for (ModelMap modelMap : models) {
      CodegenModel model = modelMap.getModel();
      if (model.getName().equals(model.parent)) {
        model.parent = null;
      }
      CodegenModel parent = models.stream()
          .map(mo -> mo.getModel())
          .filter(mo -> mo.getName().equals(model.parent))
          .findFirst().orElse(null);
      if (parent != null) {
        for(CodegenProperty var: model.vars) {
          for(CodegenProperty parentVar: parent.vars) {
            if(parentVar.getName().equals(var.getName())) {
              var.isInherited = true;
            }
          }
        }
      }
      XynaCodegenModel xModel = codegenFactory.getOrCreateXynaCodegenModel(model);
      xModels.add(xModel);
      if (Boolean.TRUE.equals(additionalProperties.get("debugXO"))) {
        System.out.println(xModel);
      }
    }
    objs.put("xynaModels", xModels);
    return objs;
  }

  /**
   * Returns human-friendly help for the generator.  Provide the consumer with help
   * tips, parameters here
   *
   * @return A string value for the help message
   */
  public String getHelp() {
    return "Generates a xyna application for the implementation of a server";
  }

  public XmomServerGenerator() {
    super();

    // set the output folder here
    outputFolder = "generated-code/xmom-server";

    /**
     * Api classes.  You can write classes for each Api file with the apiTemplateFiles map.
     * as with models, add multiple entries with different extensions for multiple files per
     * class
     */
    apiTemplateFiles.put("apiDataType.mustache", ".xml");
    apiTemplateFiles.put("requestDataType.mustache", "_requestDataTypes_toSplit.xml");
    apiTemplateFiles.put("responseDataType.mustache", "_responseDataTypes_toSplit.xml");
    apiTemplateFiles.put("endpointWorkflow.mustache", "_endpointWorkflows_toSplit.xml");

    templateDir = "xmom-server";
    
    /**
     * path of the XMOM objects, 
     * can be changed via "x-model-path" and "x-provider-path" in the info section of the spec file
     */
    modelPackage = "model.generated";
    apiPackage = "xmcp.oas.provider";

    /**
     * Reserved words.  Override this with reserved words specific to your language
     */
    /* reservedWords = new HashSet<String> (
      Arrays.asList(
        "sample1",  // replace with static values
        "sample2")
    ); */
    reservedWords = new HashSet<String>();

    /**
     * Additional Properties.  These values can be passed to the templates and
     * are available in models, apis, and supporting files
     */
    additionalProperties.put(XYNA_FACTORY_VERSION, xynaFactoryVersion);

    /**
     * Language Specific Primitives.  These types will not trigger imports by
     * the client generator
     */
    languageSpecificPrimitives = new HashSet<String>(
      Arrays.asList("boolean", "integer", "long", "double", "float", "number", "string", "DateTime", "date", "password", "byte", "binary", "URI")
    );

    typeMapping.clear();
    typeMapping.put("boolean", "Boolean");
    typeMapping.put("integer", "Long");
    typeMapping.put("long", "Long");
    typeMapping.put("double", "Double");
    typeMapping.put("float", "Double");
    typeMapping.put("number", "Double");
    typeMapping.put("string", "String");
    typeMapping.put("DateTime", "DateTimeType");
    typeMapping.put("date", "DateType");
    typeMapping.put("password", "String");
    typeMapping.put("byte", "String");
    typeMapping.put("binary", "String");
    typeMapping.put("file", "String");
    typeMapping.put("URI", "String");
  }

  /**
   * Escapes a reserved word as defined in the `reservedWords` array. Handle escaping
   * those terms here.  This logic is only called if a variable matches the reserved words
   *
   * @return the escaped term
   */
  @Override
  public String escapeReservedWord(String name) {
    return "_" + name;  // add an underscore to the name
  }

  /**
   * Location to write api files.  You can use the apiPackage() as defined when the class is
   * instantiated
   */
  @Override
  public String apiFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + apiPackage().replace('.', File.separatorChar);
  }

  @Override
  protected ImmutableMap.Builder<String, Lambda> addMustacheLambdas() {
    ImmutableMap.Builder<String, Lambda> lambdaBuilder = super.addMustacheLambdas();
    
    lambdaBuilder.put("index", new IndexLambda(1));
    lambdaBuilder.put("statuscode", new StatusCodeLambda());
    return lambdaBuilder;
  }

  @Override
  @SuppressWarnings("static-method")
  public void postProcess() {
      System.out.println("server generator finished");
  }
}
