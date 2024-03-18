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
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.utils.ModelUtils;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;

import java.util.*;
import java.io.File;

public class XmomDataModelGenerator extends DefaultCodegen {

  // source folder where to write the files
  protected String sourceFolder = "XMOM";
  protected String xynaFactoryVersion = "CURRENT_VERSION";
  
  public static final String XYNA_FACTORY_VERSION = "xynaFactoryVersion";

  /**
   * Configures the type of generator.
   *
   * @return  the CodegenType for this generator
   * @see     org.openapitools.codegen.CodegenType
   */
  public CodegenType getTag() {
    return CodegenType.SCHEMA;
  }

  /**
   * Configures a friendly name for the generator.  This will be used by the generator
   * to select the library with the -g flag.
   *
   * @return the friendly name for the generator
   */
  public String getName() {
    return "xmom-data-model";
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
    }
  }

  @Override
  public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
    objs = super.postProcessAllModels(objs);
    for (String modelname : objs.keySet()) {
      CodegenModel model = ModelUtils.getModelByName(modelname, objs);
      if (model.getName().equals(model.parent)) {
        model.parent = null;
      }
      CodegenModel parent = ModelUtils.getModelByName(model.parent, objs);
      if (parent != null) {
        for(CodegenProperty var: model.vars) {
          for(CodegenProperty parentVar: parent.vars) {
            if(parentVar.getName().equals(var.getName())) {
              var.isInherited = true;
            }
          }
        }
      }
      XynaCodegenModel xModel = new XynaCodegenModel(model, this);
      objs.get(modelname).put("xynaModel", xModel);
      if (Boolean.TRUE.equals(additionalProperties.get("debugXO"))) {
        System.out.println(xModel);
      }
    }
    return objs;
  }
  
  @Override
  public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
    List<ModelMap> models = (List<ModelMap>) objs.get("models");
    models.forEach((ModelMap map) -> map.put("xynaModel", new XynaCodegenModel(map.getModel(), this)));
    return objs;
  }
  
  /**
   * Returns human-friendly help for the generator.  Provide the consumer with help
   * tips, parameters here
   *
   * @return A string value for the help message
   */
  public String getHelp() {
    return "Generates a xmom-data-model client library.";
  }

  public XmomDataModelGenerator() {
    super();

    // set the output folder here
    outputFolder = "generated-code/xmom-data-model";

    /**
     * Models.  You can write model files using the modelTemplateFiles map.
     * if you want to create one template for file, you can do so here.
     * for multiple files for model, just put another entry in the `modelTemplateFiles` with
     * a different extension
     */
    modelTemplateFiles.put(
      "model.mustache", // the template to use
      ".xml");       // the extension for each file to write

    /**
     * Template Location.  This is the location which templates will be read from.  The generator
     * will use the resource stream to attempt to read the templates.
     */
    templateDir = "xmom-data-model";

    /**
     * path of the XMOM objects, 
     * can be changed via "x-model-path" in the info section of the spec file
     */
    modelPackage = "model.generated";

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
     * Supporting Files.  You can write single files for the generator with the
     * entire object tree available.  If the input file has a suffix of `.mustache
     * it will be processed by the template engine.  Otherwise, it will be copied
     */
    supportingFiles.add(new SupportingFile("application.mustache",   // the input template or file
      "",                                                       // the destination folder, relative `outputFolder`
      "application.xml")                                          // the output file
    );

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
   * Location to write model files.  You can use the modelPackage() as defined when the class is
   * instantiated
   */
  public String modelFileFolder() {
    return outputFolder + "/" + sourceFolder + "/" + modelPackage().replace('.', File.separatorChar);
  }

  @Override
  @SuppressWarnings("static-method")
  public void postProcess() {
      System.out.println("generation of data-model finished");
  }
}
