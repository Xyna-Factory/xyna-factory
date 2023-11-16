package com.gip.xyna.openapi.codegen;

import org.openapitools.codegen.*;
import org.openapitools.codegen.model.*;

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
  protected String apiVersion = "1.0.0";
  protected String xynaFactoryVersion = "9.0.2.3";
  
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

  @Override
  public void preprocessOpenAPI(OpenAPI openAPI) {
    super.preprocessOpenAPI(openAPI);
  }

  /**
   * changes to the internal data for the supporting files
   */
  @Override
  public Map<String, Object> postProcessSupportingFileData(Map<String, Object> objs) {
    objs = super.postProcessSupportingFileData(objs);

    Info info = openAPI.getInfo();
    // replace spaces, "-", "." with underscores in info.title
    info.setTitle(sanitizeName(info.getTitle()));
    
    return objs;
  }

  /**
   * Provides an opportunity to inspect and modify operation data before the code is generated.
   */
  @Override
  public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
    OperationsMap results = super.postProcessOperationsWithModels(objs, allModels);

    OperationMap ops = results.getOperations();
    List<CodegenOperation> opList = ops.getOperation();

    for(CodegenOperation co : opList){
      
      // add regexPath property to each operation which is used by the filter
      String regexPath = co.path;
      if (co.pathParams.size() > 0) {
        for(CodegenParameter p : co.pathParams) {
          if (p.isNumeric || p.isInteger || p.isLong || p.isNumber || p.isFloat || p.isDouble) {
            regexPath = regexPath.replaceAll("\\{" + p.baseName + "\\}", "(?<" + p.baseName + ">[-+]?[0-9]*\\.?[0-9]+([eE][-+]?[0-9]+)?)");
          }
          else if (p.isString) {
            regexPath = regexPath.replaceAll("\\{" + p.baseName + "\\}", "(?<" + p.baseName + ">[^/?]*)");
          }
        }
      }
      co.vendorExtensions.put("regexPath", regexPath);
      
      // boolean to check if a 'request data type' for this operation is needed to auto parse parameters
      co.vendorExtensions.put("hasParseParams", co.getHasPathParams() || co.getHasQueryParams() || co.getHasHeaderParams());
    }

    return results;
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
    apiTemplateFiles.put("decodeWorkflow.mustache", "_decodeWorkflows_toSplit.xml");
    apiTemplateFiles.put("encodeWorkflow.mustache", "_encodeWorkflow_toSplit.xml");

    templateDir = "xmom-server";

    apiPackage = "xmcp.oas";
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
    additionalProperties.put("apiVersion", apiVersion);

    /**
     * Supporting Files.  You can write single files for the generator with the
     * entire object tree available.  If the input file has a suffix of `.mustache
     * it will be processed by the template engine.  Otherwise, it will be copied
     */
    supportingFiles.add(new SupportingFile("OASFilter.mustache",   // the input template or file
      "filter/OASFilter",                                                // the destination folder, relative `outputFolder`
      "OASFilter.java")                                     // the output file
    );
    supportingFiles.add(new SupportingFile("application.mustache", "", "application.xml"));

    /**
     * Language Specific Primitives.  These types will not trigger imports by
     * the client generator
     */
    languageSpecificPrimitives = new HashSet<String>(
      Arrays.asList("boolean", "integer", "long", "double", "float", "string", "DateTime", "date", "password", "byte", "binary", "URI")
    );

    typeMapping.clear();
    typeMapping.put("boolean", "Boolean");
    typeMapping.put("integer", "Integer");
    typeMapping.put("long", "Long");
    typeMapping.put("double", "Double");
    typeMapping.put("float", "Double");
    typeMapping.put("string", "String");
    typeMapping.put("DateTime", "String");
    typeMapping.put("date", "String");
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

  /**
   * override with any special text escaping logic to handle unsafe
   * characters so as to avoid code injection
   *
   * @param input String to be cleaned up
   * @return string with unsafe characters removed or escaped
   */
  @Override
  public String escapeUnsafeCharacters(String input) {
    //TODO: check that this logic is safe to escape unsafe characters to avoid code injection
    return input;
  }

  /**
   * Escape single and/or double quote to avoid code injection
   *
   * @param input String to be cleaned up
   * @return string with quotation mark removed or escaped
   */
  public String escapeQuotationMark(String input) {
    //TODO: check that this logic is safe to escape quotation mark to avoid code injection
    return input.replace("\"", "\\\"");
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
