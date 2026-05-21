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
import org.openapitools.codegen.CodegenDiscriminator.MappedModel;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.utils.ModelUtils;

import com.gip.xyna.openapi.codegen.factory.XynaCodegenFactory;
import com.gip.xyna.openapi.codegen.utils.Camelizer;
import com.gip.xyna.openapi.codegen.utils.GeneratorProperty;
import com.gip.xyna.openapi.codegen.utils.Sanitizer;
import com.gip.xyna.openapi.codegen.utils.XynaModelUtils;
import com.gip.xyna.openapi.codegen.utils.Camelizer.Case;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Schema;

import java.util.*;
import java.util.Map.Entry;
import java.io.File;

public class XmomDataModelGenerator extends DefaultCodegen {

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

  public String getDeciderPath() {
    return GeneratorProperty.getModelPath(this) + ".decider";
  }

  public String getFilteName() {
    return GeneratorProperty.getFilterName(this);
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
        modelPackage = Sanitizer.sanitize(xModelPath.replace('-', '_').replace(' ', '_').toLowerCase());
        GeneratorProperty.setModelPath(this, modelPackage);
      }
      String xClientPath = (String)vendorExtentions.get("x-client-path");
      if (xClientPath != null && !xClientPath.trim().isEmpty()) {
        GeneratorProperty.setClientPath(this, Sanitizer.sanitize(xClientPath.replace('-', '_').replace(' ', '_').toLowerCase()));
      }
      String xProviderPath = (String)vendorExtentions.get("x-provider-path");
      if (xProviderPath != null && !xProviderPath.trim().isEmpty()) {
        GeneratorProperty.setProviderPath(this, Sanitizer.sanitize(xProviderPath.replace('-', '_').replace(' ', '_').toLowerCase()));
      }
    }
    
    // determine name of Filter
    String xFilterName = vendorExtentions != null ? (String) vendorExtentions.get("x-filter-name") : info.getTitle();
    xFilterName = xFilterName != null && !xFilterName.trim().isEmpty() ? xFilterName : info.getTitle();
    xFilterName = Camelizer.camelize(Sanitizer.sanitize(xFilterName.replace('-', ' ').replace('_', ' ')), Case.PASCAL);
    GeneratorProperty.setFilterName(this, xFilterName);
    
    /**
     * Supporting Files.  You can write single files for the generator with the
     * entire object tree available.  If the input file has a suffix of `.mustache
     * it will be processed by the template engine.  Otherwise, it will be copied
     */
    supportingFiles.add(new SupportingFile("application.mustache", // the input template or file
                                           "", // the destination folder, relative `outputFolder`
                                           "application.xml") // the output file
    );
    supportingFiles.add(new SupportingFile("additionalPropertyWrapper.mustache",
                                           "XMOM/" + GeneratorProperty.getModelPath(this).replace('.', '/') + "/wrapper",
                                           "additionalPropertyWrapper_toSplit.xml"));
    supportingFiles
        .add(new SupportingFile("listwrapperprovider.mustache", "XMOM/" + modelPackage.replace(".", "/"), "ListWrapperProvider.xml"));
    supportingFiles.add(new SupportingFile("OASDecider.mustache", "XMOM/" + modelPackage.replace('.', '/') + "/decider", "OASDecider.xml"));

  }


  @Override
  public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
    objs = super.postProcessAllModels(objs);
    Map<String, ModelMap> modelMap = XynaModelUtils.getModelsFromAllModels(objs);
    setInheritance(modelMap);
    setListWrapper(modelMap);
    for (Entry<String, ModelMap> model: modelMap.entrySet()) {
      XynaCodegenModel xModel = codegenFactory.getOrCreateXynaCodegenModel(model.getValue().getModel());
      objs.get(model.getKey()).put("xynaModel", xModel);
      if(xModel.isListWrapper) {
        objs.get(model.getKey()).put("xynaListWrapper", xModel.getModelFQN());
      }
      if (GeneratorProperty.getDebugXO(this)) {
        System.out.println(xModel);
      }
    }
    return objs;
  }

  @Override
  public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
    objs = super.postProcessSupportingFileData(objs);
    Map<String, ModelMap> modelMap = XynaModelUtils.getModelsFromSupportingFileData(objs);
    setInheritance(modelMap);
    updateDiscriminatorMapping(modelMap);
    setListWrapper(modelMap);

    List<String> listWrapper = new ArrayList<String>();
    List<XynaCodegenModel> xModels = new ArrayList<XynaCodegenModel>();
    Set<AdditionalPropertyWrapper> addPropWappers = new HashSet<AdditionalPropertyWrapper>();
    for(ModelMap model: modelMap.values()) {
      XynaCodegenModel mo = codegenFactory.getOrCreateXynaCodegenModel(model.getModel());
      model.put("xynaModel", mo);
      xModels.add(mo);
      if(mo.isListWrapper) {
        listWrapper.add(mo.getModelFQN());
      }
      if (model.getModel().isAdditionalPropertiesTrue) {
        refineAdditionalProperty(model.getModel().getAdditionalProperties());
        String fqn = XynaCodegenModel.getFQN(model.getModel(), this);
        AdditionalPropertyWrapper addPropWrapper = codegenFactory.getOrCreateAdditionalPropertyWrapper(model.getModel().getAdditionalProperties(), fqn);
        addPropWappers.add(addPropWrapper);
      }
    }
    List<OperationMap> operationMaps = XynaModelUtils.getOperationsFromSupportingFileData(objs);
    for (OperationMap operationMap: operationMaps) {
      for (CodegenOperation operation: operationMap.getOperation()) {
        for (CodegenResponse response: operation.responses) {
          if (response.getAdditionalProperties() != null) {
            refineAdditionalProperty(response.getAdditionalProperties());
            String clientfqn = XynaCodegenResponse.getClientFQN(operation, this, operationMap.getPathPrefix(), response);
            AdditionalPropertyWrapper addPropWrapper = codegenFactory.getOrCreateAdditionalPropertyWrapper(response.getAdditionalProperties(), clientfqn);
            addPropWappers.add(addPropWrapper);
            String providerfqn = XynaCodegenResponse.getProviderFQN(operation, this, operationMap.getPathPrefix(), response);
            addPropWrapper = codegenFactory.getOrCreateAdditionalPropertyWrapper(response.getAdditionalProperties(), providerfqn);
            addPropWappers.add(addPropWrapper);
          }
        }
      }
    }
    ListWrapperData listWrapperData = new ListWrapperData();
    listWrapperData.setPath(GeneratorProperty.getModelPath(this));
    listWrapperData.setListWrapper(listWrapper);
    objs.put("ListWrapperData", listWrapperData);
    objs.put("xynaModels", xModels);
    objs.put("addPropWrapper", addPropWappers);
    objs.put("deciderPath", getDeciderPath());
    objs.put("filterName", getFilteName());
    return objs;
  }

  private void setListWrapper(Map<String, ModelMap> modelMap) {
    for (ModelMap model: modelMap.values()) {
      CodegenModel mo = model.getModel();
      if (XynaCodegenModel.isListWrapper(mo, this)) {
        CodegenProperty item = mo.getItems();
        CodegenProperty inner = item.mostInnerItems == null ? item.clone() : item.mostInnerItems;
        item.isContainer = true;
        mo.getAllVars().add(item);
        item.mostInnerItems = inner;
      }
    }
  }

  private void setInheritance(Map<String, ModelMap> modelMap) {
    for (Entry<String, ModelMap> model: modelMap.entrySet()) {
      if (model.getValue().getModel().getName().equals(model.getValue().getModel().parent)) {
        model.getValue().getModel().parent = null;
      }
      CodegenModel parent = model.getValue().getModel().parentModel;
      if (parent != null) {
        for(CodegenProperty var: model.getValue().getModel().getAllVars()) {
          for(CodegenProperty parentVar: parent.getAllVars()) {
            if(parentVar.getName().equals(var.getName())) {
              var.isInherited = true;
            }
          }
        }
      }
    }
  }

  private void updateDiscriminatorMapping(Map<String, ModelMap> modelMap) {
    for (Entry<String, ModelMap> model: modelMap.entrySet()) {
      if (model.getValue().getModel().getHasDiscriminatorWithNonEmptyMapping() && model.getValue().getModel().oneOf.isEmpty()) {
        List<MappedModel> toRemove = new ArrayList<>();
        for (MappedModel mapping: model.getValue().getModel().getDiscriminator().getMappedModels()) {
          CodegenModel mo = mapping.getModel();
          while (mo != null && mo.parentModel != null && mo.name != model.getValue().getModel().name) {
            mo = modelMap.get(mo.parentModel.name).getModel();
          }
          if (mo == null || mo.name != model.getValue().getModel().name) {
            toRemove.add(mapping);
          }
        }
        model.getValue().getModel().getDiscriminator().getMappedModels().removeAll(toRemove);
      }
    }
  }

  private void refineAdditionalProperty(CodegenProperty property) {
    property.baseName = "Value";
    property.name = "value";
  }


  @SuppressWarnings("rawtypes")
  protected void addParentFromContainer(CodegenModel model, Schema schema) {
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
    GeneratorProperty.setModelPath(this, modelPackage);
    GeneratorProperty.setClientPath(this, "xmcp.oas.client");
    GeneratorProperty.setProviderPath(this, "xmcp.oas.provider");

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
    supportsInheritance = true;
    supportsMultipleInheritance = false;
    supportsAdditionalPropertiesWithComposedSchema = true;

    /**
     * Language Specific Primitives.  These types will not trigger imports by
     * the client generator
     */
    languageSpecificPrimitives = new HashSet<String>(
      Arrays.asList("boolean", "integer", "long", "double", "float", "number", "string", "DateTime", "date", "password", "byte", "binary", "URI")
    );

    typeMapping.clear();
    XynaModelUtils.getTypeMapping().forEach(typeMapping::putIfAbsent);
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
    return outputFolder + "/" + sourceFolder + "/" + GeneratorProperty.getModelPath(this).replace('.', File.separatorChar);
  }

  @Override
  public void postProcess() {
    System.out.println("generation of data-model finished");
  }


  @SuppressWarnings("rawtypes")
  public Schema unaliasSchema(Schema schema) {
    if(schema == null) {
      return super.unaliasSchema(schema);
    }
    if ("array".equalsIgnoreCase(schema.getType())) {
      Schema item = schema.getItems();
      if (item != null && item.getName() == null) {
        item.setName(ModelUtils.getSimpleRef(item.get$ref()));
      }
    }
    Schema ret = super.unaliasSchema(schema);
    if (ret.getName() == null && schema.getName() != null) {
      ret.setName(schema.getName());
    }
    return ret;
}

  @SuppressWarnings("rawtypes")
  public CodegenProperty fromProperty(String name, Schema p, boolean required, boolean schemaIsFromAdditionalProperties) {
    CodegenProperty property = super.fromProperty(name, p, required, schemaIsFromAdditionalProperties);
    if (typeAliases != null && typeAliases.containsKey(p.getName())) {
      property.name = p.getName();
      property.baseName = p.getName();
    }

    return property;
  }
}
