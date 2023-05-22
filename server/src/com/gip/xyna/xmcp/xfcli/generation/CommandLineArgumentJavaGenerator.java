/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.xmcp.xfcli.generation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.exceptions.utils.InvalidXMLException;
import com.gip.xyna.utils.exceptions.utils.XMLUtils;
import com.gip.xyna.utils.exceptions.utils.codegen.CodeBuffer;


public class CommandLineArgumentJavaGenerator {
  
  public static final String METHODNAME_GETDEPENDENCIES = "getDependencies";

/*  private static final String fileSeparator = Constants.fileSeparator;
  private static final String DEFAULT_ENCODING = Constants.DEFAULT_ENCODING;

  private static final String XMCP_SIMPLE_CLASS_NAME = "Xyna Multi-Channel Portal";
  private static final String INVALID_PARAMETERNUMBER_CLASS_NAME = XMCP_INVALID_PARAMETERNUMBER.class.getName();
  private static final String INVALID_PARAMETERNUMBER_SIMPLE_CLASS_NAME = XMCP_INVALID_PARAMETERNUMBER.class.getSimpleName();
  private static final String AXYNACOMMAND_CLASS_NAME = AXynaCommand.class.getName();
  private static final String AXYNACOMMAND_SIMPLE_CLASS_NAME = AXynaCommand.class.getSimpleName();
  private static final String XYNA_COMMAND_IMPLEMENTATION_CLASS_NAME = XynaCommandImplementation.class.getName();
  private static final String XYNA_COMMAND_IMPLEMENTATION_SIMPLE_CLASS_NAME = XynaCommandImplementation.class.getSimpleName();*/
  
  /*
   * FIXME dependencies aufr�umen: derzeit ist das problem, dass beim bauen des cliclassgenerator.jars das xynafactory.jar
   * noch nicht existiert, dabei aber klassen daraus verwendet werden (s.o.)
   */
  
  private static final String fileSeparator = "/";
  private static final String DEFAULT_ENCODING = "UTF8";

  private static final String XMCP_SIMPLE_CLASS_NAME = "Xyna Multi-Channel Portal";
  private static final String INVALID_PARAMETERNUMBER_CLASS_NAME =
      "com.gip.xyna.xmcp.exceptions.XMCP_INVALID_PARAMETERNUMBER";
  private static final String INVALID_PARAMETERNUMBER_SIMPLE_CLASS_NAME = "XMCP_INVALID_PARAMETERNUMBER";
  private static final String AXYNACOMMAND_CLASS_NAME = "com.gip.xyna.xmcp.xfcli.AXynaCommand";
  private static final String AXYNACOMMAND_SIMPLE_CLASS_NAME = "AXynaCommand";
  private static final String XYNA_COMMAND_IMPLEMENTATION_CLASS_NAME = "com.gip.xyna.xmcp.xfcli.XynaCommandImplementation";
  private static final String XYNA_COMMAND_IMPLEMENTATION_SIMPLE_CLASS_NAME = "XynaCommandImplementation";
  
  
  //xml namen
  public static final String ELEMENT_COMMAND = "XynaCommandLineCommand";
  public static final String ELEMENT_COMMAND_NAME = "CommandDefinition";
  public static final String ELEMENT_EXTENDEDDESCRIPTION = "ExtendedDescription";
  public static final String ELEMENT_ARGUMENT = "Argument";
  public static final String ELEMENT_BOOL_OPTION = "BoolOption";

  public static final String ATTRIBUTE_NAME = "Name";
  public static final String ATTRIBUTE_GROUPS = "Groups";
  public static final String ATTRIBUTE_DESCRIPTION = "Description";
  public static final String ATTRIBUTE_LONG_NAME = "LongName";
  public static final String ATTRIBUTE_ARGUMENT_NAME = "ArgumentName";
  public static final String ATTRIBUTE_OPTIONAL = "Optional";
  public static final String ATTRIBUTE_MULTIPLE_VALUES = "MultipleValues";
  public static final String ATTRIBUTE_DEPENDENCIES = "Dependencies";
  public static final String ATTRIBUTE_DEPRECATED = "Deprecated";

  private static final String CLIREGISTRY_CLASSNAME = "com.gip.xyna.xmcp.xfcli.CLIRegistry";

  public static void main(String[] args) throws Exception {

    String xmlFileBaseLocationString = args[0];
    String targetSourceFolderString = args[1];
    String targetPackage = args[2];

    // TODO allow directories and filter recursively
    FilenameFilter xmlLocationFilter = new FilenameFilter() {
      
      public boolean accept(File dir, String name) {
        if (new File(dir, name).isDirectory()) {
          return false;
        }
        if (name.endsWith(".xml")) {
          return true;
        }
        return false;
      }
    };

    String[] packageParts = targetPackage.split("\\.");
    String targetPackagePath = "";
    for (int i = 0; i<packageParts.length; i++) {
      if (i > 0) {
        targetPackagePath += fileSeparator;
      }
      targetPackagePath += packageParts[i];
    }
    String pathToXmcpXfcli =
        targetSourceFolderString + fileSeparator + targetPackagePath;

    List<GeneratedJava> resultingGeneratedJava = null;
    File xmlFileBaseLocation = new File(xmlFileBaseLocationString);
    String[] fileNames;
    if (!xmlFileBaseLocation.exists()) {
      System.out.println("No XML definitions found.");
    } else {
      fileNames = xmlFileBaseLocation.list(xmlLocationFilter);
      if (fileNames == null) {
        System.out.println("No XML definitions found.");
      } else {
        resultingGeneratedJava = generateJavaForCommands(pathToXmcpXfcli, xmlFileBaseLocationString, fileNames, targetPackage);
      }
    }

    if (resultingGeneratedJava == null) {
      return;
    }
    generateOverallInfoProvider(resultingGeneratedJava, pathToXmcpXfcli, targetPackage);

  }


  /**
   * @return all fqClassNames of the generated java files
   */
  private static List<GeneratedJava> generateJavaForCommands(String pathToCliClasses, String xmlFileBaseLocationString,
                                                             String[] fileNames, String targetPackage) throws Exception {

    List<GeneratedJava> resultingFqClassNames = new ArrayList<GeneratedJava>();

    System.out.println("Processing " + fileNames.length + " XML file(s).");
    List<Document> documents = new ArrayList<Document>();
    for (String fileName : fileNames) {
//      System.out.println("Parsing file: " + fileName);
      documents.add(parseFile(new File(xmlFileBaseLocationString, fileName).getAbsolutePath()));
    }

    File targetGeneratedFolder = new File(pathToCliClasses + fileSeparator + "generated");
    if (!targetGeneratedFolder.exists()) {
      targetGeneratedFolder.mkdirs();
    }

    File targetImplFilesFolder = new File(pathToCliClasses + fileSeparator + "impl");
    if (!targetImplFilesFolder.exists()) {
      targetImplFilesFolder.mkdirs();
    }

    for (Document nextDoc : documents) {

      // Generate information container and parser
      GeneratedJava generatedJava = generateParserJava(nextDoc, targetPackage);
      File targetParserFile = new File(targetGeneratedFolder, generatedJava.className + ".java");
      targetParserFile.createNewFile();
      writeStringToFile(generatedJava.javaCode, targetParserFile);

      resultingFqClassNames.add(generatedJava);

      // generate an implementation template if none exists
      File targetImplFile =
          new File(targetImplFilesFolder, getImplClassNameByCommandName(generatedJava.commandName) + ".java");
      if (targetImplFile.exists()) {
        continue; // dont overwrite an existing implementation
      }

      targetImplFile.createNewFile();
      GeneratedJava implTemplateJava = generateImplTemplateJava(generatedJava.commandName, targetPackage);
      writeStringToFile(implTemplateJava.javaCode, targetImplFile);
     
    }

    return resultingFqClassNames;

  }

  //copy&paste von FileUtils. aber aus classpathgr�nden hier dupliziert.
  private static void writeStringToFile(String content, File f) throws IOException {
    if (!f.exists()) {
      if (f.getParentFile() != null) {
        f.getParentFile().mkdirs();
      }
      f.createNewFile();
    }
    try (FileOutputStream out = new FileOutputStream(f);
         BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(out, "UTF-8"), 1024 * 32)) {
      bw.write(content);
      bw.flush();
    }
  }

  public static Document parseFile(String xmlFileLocation) throws InvalidXMLException {
    return XMLUtils.parse(xmlFileLocation);
  }


  private static void generateOverallInfoProvider(List<GeneratedJava> resultingFqClassNames, String pathToXmcpXfcli, String targetPackage)
      throws UnsupportedEncodingException, IOException {

    final String className = "OverallInformationProvider";

    CodeBuffer cb = new CodeBuffer(XMCP_SIMPLE_CLASS_NAME);
    cb.addLine("package ", targetPackage, ".generated").addLB();
    cb.addLine("import ", List.class.getName());
    cb.addLine("import ", ArrayList.class.getName());
    cb.addLine("import ", AXYNACOMMAND_CLASS_NAME);
    cb.addLB(2);

    cb.addLine("/*");
    cb.addLine("* THIS FILE IS GENERATED AUTOMATICALLY");
    cb.addLine("* DO NOT MODIFY OR ADD TO SVN");
    cb.addLine("*/");
    cb.addLine("public final class ", className, " {").addLB();

    cb.addLine("private ", className, "() {");
    cb.addLine("}").addLB();

    cb.addLine("public static List<Class<? extends ", AXYNACOMMAND_SIMPLE_CLASS_NAME, ">> getCommands() throws ",
               ClassNotFoundException.class.getSimpleName(), "{");
    cb.addLine(List.class.getSimpleName(),"<Class<? extends ", AXYNACOMMAND_SIMPLE_CLASS_NAME, ">> list = new ", ArrayList.class.getSimpleName(), "<Class<? extends ", AXYNACOMMAND_SIMPLE_CLASS_NAME, ">>()");
    cb.addLine("Class<? extends ", AXYNACOMMAND_SIMPLE_CLASS_NAME, "> nextClass");
    for (GeneratedJava s : resultingFqClassNames) {
      cb.addLine("nextClass = (Class<? extends ", AXYNACOMMAND_SIMPLE_CLASS_NAME, ">) Class.forName(\"",
                 s.getFqClassName(), "\")");
      cb.addLine("list.add(nextClass)");
    }
    cb.addLine("return list");
    cb.addLine("}").addLB();

    cb.addLine("public static void onDeployment() {");
    cb.addLine("try {");
    cb.addLine("for (Class<? extends ", AXYNACOMMAND_SIMPLE_CLASS_NAME, "> command : getCommands() ) {");
    cb.addLine(CLIREGISTRY_CLASSNAME+".getInstance().registerCLICommand(command);");
    cb.addLine("}");
    cb.addLine("} catch ("+ClassNotFoundException.class.getSimpleName()+" e) {");
    cb.addLine("throw new "+RuntimeException.class.getSimpleName()+"(\"could not register cli commands.\", e);");
    cb.addLine("}");
    cb.addLine("}").addLB();
    
    cb.addLine("public static void onUndeployment() {");
    cb.addLine("try {");
    cb.addLine("for (Class<? extends ", AXYNACOMMAND_SIMPLE_CLASS_NAME, "> command : getCommands() ) {");
    cb.addLine(CLIREGISTRY_CLASSNAME+".getInstance().unregisterCLICommand(command);");
    cb.addLine("}");
    cb.addLine("} catch ("+ClassNotFoundException.class.getSimpleName()+" e) {");
    cb.addLine("throw new "+RuntimeException.class.getSimpleName()+"(\"could not register cli commands.\", e);");
    cb.addLine("}");
    cb.addLine("}").addLB();
    
    cb.addLine("}");

    File targetOverallFile =
        new File(pathToXmcpXfcli + fileSeparator + "generated" + fileSeparator + className
            + ".java");
    try (FileOutputStream fos = new FileOutputStream(targetOverallFile)) {
      fos.write(cb.toString().getBytes(DEFAULT_ENCODING));
    }

  }  


  private static GeneratedJava generateImplTemplateJava(String commandName, String targetPackage) {

    CodeBuffer cb = new CodeBuffer(XMCP_SIMPLE_CLASS_NAME);

    cb.addLine("package ", targetPackage, ".impl").addLB();

    for (String s : getImplTemplateImports()) {
      cb.addLine("import " + s);
    }
    cb.addLine("import ", targetPackage, ".generated.", getClassNameByCommandName(commandName));
    cb.addLB(3);

    cb.addLine("public class ", getImplClassNameByCommandName(commandName), " extends ",
               XYNA_COMMAND_IMPLEMENTATION_SIMPLE_CLASS_NAME, "<", getClassNameByCommandName(commandName), "> {")
        .addLB();

    cb.addLine("public void execute(", OutputStream.class.getSimpleName(), " statusOutputStream, ",
               getClassNameByCommandName(commandName), " payload) throws ", XynaException.class.getSimpleName(), " {");
    cb.addLine("// TODO implementation");
    cb.addLine("}").addLB();

    cb.addLine("}");

    return new GeneratedJava(commandName, getImplClassNameByCommandName(commandName), cb.toString(), targetPackage);

  }


  private static GeneratedJava generateParserJava(Document xmlDefinition, String targetPackage) {

    Element root = xmlDefinition.getDocumentElement();
    if (!root.getNodeName().equals(ELEMENT_COMMAND)) {
      throw new RuntimeException("Unexpected root element name: <" + root.getNodeName() + ">");
    }

    Element commandNameElement = XMLUtils.getChildElementByName(root, ELEMENT_COMMAND_NAME);
    if (commandNameElement == null) {
      throw new RuntimeException("Missing element: <" + ELEMENT_COMMAND_NAME + ">");
    }
    Element extendedDescriptionElement = XMLUtils.getChildElementByName(commandNameElement, ELEMENT_EXTENDEDDESCRIPTION);
    String extendedDescription = null;
    if (extendedDescriptionElement != null) {
      extendedDescription = XMLUtils.getTextContent(extendedDescriptionElement);
    }

    String commandName = commandNameElement.getAttribute(ATTRIBUTE_NAME);
    String commandDescription = commandNameElement.getAttribute(ATTRIBUTE_DESCRIPTION);
    String groupsString = commandNameElement.getAttribute(ATTRIBUTE_GROUPS);

    String commandDepcrecatedString = commandNameElement.getAttribute(ATTRIBUTE_DEPRECATED);
    boolean commandDeprecated = commandDepcrecatedString == null ? false : Boolean.valueOf(commandDepcrecatedString);

    String dependenciesString = commandNameElement.getAttribute(ATTRIBUTE_DEPENDENCIES);
    String []dependencies = dependenciesString.split(",");
    String[] groups = groupsString.split(",");
    for (int i=0; i<groups.length; i++) {
      groups[i] = groups[i].trim();
    }
    if (groups.length == 1 && groups[0].trim().length() == 0) {
      groups = new String[0];
    }
    
    for (int i=0; i<dependencies.length; i++) {
      dependencies[i] = dependencies[i].trim();
    }
    if (dependencies.length == 1 && dependencies[0].trim().length() == 0) {
      dependencies = new String[0];
    }

    List<Element> argumentElements = XMLUtils.getChildElementsByName(root, ELEMENT_ARGUMENT);
    List<ArgumentOptionInformation> deprecatedArgs = new ArrayList<ArgumentOptionInformation>();
    List<ArgumentOptionInformation> argumentInfoList = new ArrayList<ArgumentOptionInformation>();
    for (Element e : argumentElements) {
      String argumentName = e.getAttribute(ATTRIBUTE_NAME);
      String description = e.getAttribute(ATTRIBUTE_DESCRIPTION);
      String argumentValueName = e.getAttribute(ATTRIBUTE_ARGUMENT_NAME);
      boolean isOptional = XMLUtils.isTrue(e, ATTRIBUTE_OPTIONAL);
      boolean multipleValues = XMLUtils.isTrue(e, ATTRIBUTE_MULTIPLE_VALUES);
      boolean isDeprecated = XMLUtils.isTrue(e, ATTRIBUTE_DEPRECATED);
      ArgumentOptionInformation aoi =
          new ArgumentOptionInformation(argumentName, description, argumentValueName, isOptional, multipleValues);
      if (isDeprecated) {
        deprecatedArgs.add(aoi);
      } else {
        argumentInfoList.add(aoi);
      }
    }

    List<Element> boolOptionElements = XMLUtils.getChildElementsByName(root, ELEMENT_BOOL_OPTION);
    List<BooleanOptionInformation> boolOptionInfoList = new ArrayList<BooleanOptionInformation>();
    for (Element e : boolOptionElements) {
      String name = e.getAttribute(ATTRIBUTE_NAME);
      String longName = e.getAttribute(ATTRIBUTE_LONG_NAME);
      if (longName == null) {
        throw new RuntimeException("Attribute \"LongName\" must be specified for boolean options.");
      }
      String description = e.getAttribute(ATTRIBUTE_DESCRIPTION);
      boolOptionInfoList.add(new BooleanOptionInformation(name, longName, description));
    }


    CodeBuffer cb = new CodeBuffer(XMCP_SIMPLE_CLASS_NAME);

    cb.addLine("package ", targetPackage, ".generated").addLB();

    for (String s : getParserImports()) {
      cb.addLine("import " + s);
    }
    cb.addLine("import ", targetPackage, ".impl.", getImplClassNameByCommandName(commandName));
    cb.addLB(2);

    cb.addLine("/*");
    cb.addLine("* THIS FILE IS GENERATED AUTOMATICALLY");
    cb.addLine("* DO NOT MODIFY OR ADD TO SVN");
    cb.addLine("*/");
    cb.addLine("public class ", getClassNameByCommandName(commandName),
               " extends " + AXYNACOMMAND_SIMPLE_CLASS_NAME + " {").addLB();

    cb.addLine("public static final String COMMAND_", getClassNameByCommandName(commandName), " = \"", commandName,
               "\"").addLB();

    cb.addLine("private static volatile ", Options.class.getSimpleName(), " allOptions = null").addLB();
    cb.add("private static final String[] groups = new String[] {");
    for (String group : groups) {
      cb.addListElement("\"" + group + "\"");
    }
    cb.add("};").addLB();
    cb.add("private static final String[] dependencies = new String[] {");
    for (String dependency : dependencies) {
      cb.addListElement("\"" + dependency + "\"");
    }
    cb.add("};").addLB();


    cb.addLine("private static volatile ", XYNA_COMMAND_IMPLEMENTATION_SIMPLE_CLASS_NAME, "<",
               getClassNameByCommandName(commandName), "> executor = null");

    for (ArgumentOptionInformation s : argumentInfoList) {
      if (!s.multipleValues) {
        cb.addLine("private String ", s.name);
      } else {
        cb.addLine("private String[] ", s.name);
      }
    }
    for (BooleanOptionInformation info: boolOptionInfoList) {
      cb.addLine("private boolean ", info.name);
    }
    cb.addLB();

    for (ArgumentOptionInformation s : argumentInfoList) {
      if (!s.multipleValues) {
        cb.addLine("public void set", firstCharacterToUpperCase(s.name), "(String value) {");
      } else {
        cb.addLine("public void set", firstCharacterToUpperCase(s.name), "(String[] value) {");
      }
      cb.addLine("this.", s.name, " = value");
      cb.addLine("}").addLB();
    }

    for (ArgumentOptionInformation s : argumentInfoList) {
      if (!s.multipleValues) {
        cb.addLine("public String get", firstCharacterToUpperCase(s.name), "() {");
      } else {
        cb.addLine("public String[] get", firstCharacterToUpperCase(s.name), "() {");
      }
      cb.addLine("return this.", s.name);
      cb.addLine("}").addLB();
    }
    
    if (deprecatedArgs.size() > 0) {
      cb.add("private static final String[] _oldOptionNames = new String[] {");
      for (ArgumentOptionInformation aoi : deprecatedArgs) {
        cb.addListElement("\"" + aoi.name + "\"");
      }
      cb.add("};");
      cb.addLB(2);
      cb.addLine("public String[] getOldOptionNames() {");
      cb.addLine("return _oldOptionNames");
      cb.addLine("}").addLB();
    }

    for (BooleanOptionInformation s : boolOptionInfoList) {
      cb.addLine("public boolean get", firstCharacterToUpperCase(s.longName), "() {");
      cb.addLine("return this.", s.name);
      cb.addLine("}").addLB();
    }

    //@Override
    cb.addLine("public String getCommandName() {");
    cb.addLine("return COMMAND_", getClassNameByCommandName(commandName));
    cb.addLine("}").addLB();

    //@Override
    cb.addLine("protected String getDescriptionString() {");
    cb.add("return ").addString(commandDescription).addLB();
    cb.addLine("}").addLB();

    //@Override
    cb.addLine("protected String getExtendedDescriptionString() {");
    if (extendedDescription != null) {
      cb.add("return ").addString(extendedDescription).addLB();
    } else {
      cb.addLine("return null");
    }
    cb.addLine("}").addLB();
    
    //@Override
    cb.addLine("protected String[] getGroups() {");
    cb.addLine("return groups"); // TODO return immutable string array?
    cb.addLine("}").addLB();

    //@Override
    cb.addLine("public static String[] ", METHODNAME_GETDEPENDENCIES, "() {");
    cb.addLine("return dependencies");
    cb.addLine("}").addLB();


    //@Override
    cb.addLine("public ", Options.class.getSimpleName(), " getAllOptions() {");
    cb.addLine("if (allOptions == null) {");

    cb.addLine("synchronized(", getClassNameByCommandName(commandName), ".class) {");
    cb.addLine("if (allOptions != null) {");
    cb.addLine("return allOptions");
    cb.addLine("}");
    cb.addLine("synchronized(", AXYNACOMMAND_SIMPLE_CLASS_NAME, ".class) {"); //wegen den statischen aufrufen auf OptionsBuilder

    cb.addLine(Options.class.getSimpleName(), " allOptionsTmp = new ", Options.class.getSimpleName(), "()");
    for (ArgumentOptionInformation s : argumentInfoList) {
      cb.add("allOptionsTmp.addOption(");
      appendArgumentOption(cb, s);
      cb.add(")").addLB();
    }
    for (BooleanOptionInformation s : boolOptionInfoList) {
      cb.add("allOptionsTmp.addOption(");
      appendBooleanOption(cb, s);
      cb.add(")").addLB();
    }
    cb.addLine("allOptions = allOptionsTmp");
    cb.addLine("}");// end synchronized
    cb.addLine("}");// end synchronized
    cb.addLine("}");
    cb.addLine("return allOptions");

    cb.addLine("}").addLB(); // end getAllOptions

    cb.addLine("protected void setFieldsByParsedOptions(", Option.class.getSimpleName(), "[] options) {");
    cb.addLine("for (", Option.class.getSimpleName(), " o: options) {");
    for (ArgumentOptionInformation info : argumentInfoList) {
      cb.addLine("if (\"", info.name, "\".equals(o.getOpt())) {");
      if (!info.multipleValues) {
        cb.addLine("this.", info.name, " = o.getValue()");
      } else {
        cb.addLine("this.", info.name, " = o.getValues()");
      }
      cb.addLine("continue");
      cb.addLine("}");
    }
    for (BooleanOptionInformation info : boolOptionInfoList) {
      cb.addLine("if (\"", info.name, "\".equals(o.getOpt())) {");
      cb.addLine("this.", info.name, " = true");
      cb.addLine("continue");
      cb.addLine("}");
    }
    cb.addLine("}"); // for loop
    cb.addLine("}").addLB(); // setFieldsByParsedOptions


    int numberOfRequiredOptions = 0;
    for (ArgumentOptionInformation info: argumentInfoList) {
      if (!info.isOptional) {
        numberOfRequiredOptions++;
      }
    }
    
    //helpermethode
    cb.addLine("private ", INVALID_PARAMETERNUMBER_SIMPLE_CLASS_NAME, " createInvalidParaEx() throws ", INVALID_PARAMETERNUMBER_SIMPLE_CLASS_NAME, "{"); 
    cb.add("return new ", INVALID_PARAMETERNUMBER_SIMPLE_CLASS_NAME, "(\"");
    for (int i = 0; i < argumentInfoList.size(); i++) {
      cb.add("<", argumentInfoList.get(i).name, ">");
      if (i < argumentInfoList.size() - 1) {
        cb.add(" ");
      }
    }
    cb.add("\")").addLB();
    cb.addLine("}").addLB();
    
    //@Override
    cb.addLine("protected void parseUnrecognizedDataArguments(String[] args) throws ",
               INVALID_PARAMETERNUMBER_SIMPLE_CLASS_NAME, " {");
    cb.addLine("// compare with the number of non-optional arguments");
    cb.addLine("if (args.length < " + numberOfRequiredOptions + ") {");
    cb.addLine("throw createInvalidParaEx()");
    cb.addLine("}");

    boolean multipleValuesOptionEncountered = false;
    for (int i = 0; i < argumentInfoList.size(); i++) {
      ArgumentOptionInformation currentInformation = argumentInfoList.get(i);
      if (currentInformation.multipleValues) {
        cb.addLine(List.class.getSimpleName(), "<String> argsAsList = new ", ArrayList.class.getSimpleName(), "<String>()");
        cb.addLine("for (int i=", i + "; i< args.length; i++) {");
        cb.addLine("argsAsList.add(args[i])");
        cb.addLine("}");
        cb.addLine("this.", currentInformation.name, " = argsAsList.toArray(new String[argsAsList.size()])");
        multipleValuesOptionEncountered = true;
        break; // no other arguments can be parsed after that
      } else {
        cb.addLine("if (" + i, " < args.length) {");
        cb.addLine("this.", currentInformation.name, " = args[", i + "]");
        cb.addLine("}");
      }
    }
    if (!multipleValuesOptionEncountered) {
      cb.addLine("for (int k=" + argumentInfoList.size() + "; k<args.length; k++) {");
      for (int i = 0; i < boolOptionInfoList.size(); i++) {
        BooleanOptionInformation currentInformation = boolOptionInfoList.get(i);
        cb.addLine("if (\"-", currentInformation.name + "\".equals(args[k])) {");
        cb.addLine("this.", currentInformation.name + " = true");
        cb.addLine("}");
      }
      cb.addLine("}");
    }
    cb.addLine("}").addLB();

    //@Override
    cb.addLine("public void executeInternally(", OutputStream.class.getSimpleName(), " statusOutputStream) throws ",
               XynaException.class.getSimpleName(), " {");
    cb.addLine("if (executor == null) {");
    cb.addLine("synchronized(getClass()) {");
    cb.addLine("if (executor == null) {");
    cb.addLine("executor = new ", getImplClassNameByCommandName(commandName), "()");
    cb.addLine("}"); // if still null
    cb.addLine("}"); // synchronized
    cb.addLine("}"); // if null
    cb.addLine("executor.execute(statusOutputStream, this)");
    cb.addLine("}").addLB();
    
    //commandline als text
    cb.addLine("public String getCommandAsString() throws ", INVALID_PARAMETERNUMBER_SIMPLE_CLASS_NAME," {");
    cb.addLine("StringBuilder sb = new StringBuilder(getCommandName())");
    cb.addLine("for (", Option.class.getSimpleName(), " o: (Collection<", Option.class.getSimpleName(), ">)getAllOptions().getOptions()) {");
    for (ArgumentOptionInformation info : argumentInfoList) {
      cb.addLine("if (\"", info.name, "\".equals(o.getOpt())) {");
      cb.addLine("if (", info.name, " == null) {");
      cb.addLine("if (o.isRequired()) {");
      cb.addLine("throw createInvalidParaEx()");
      cb.addLine("}"); //required
      cb.addLine("} else {");
      cb.add("sb.append(\" -\").append(o.getOpt())");
      if (info.multipleValues) {
        cb.addLB();
        cb.addLine("for (String _1", info.name, " : ", info.name, ") {");
        cb.addLine("sb.append(\" \").append(_1", info.name, ")");
        cb.addLine("}");
      } else {
        cb.add(".append(\" \").append(", info.name, ")").addLB();
      }
      cb.addLine("}"); //else
      cb.addLine("continue");
      cb.addLine("}"); //name.equals
    }
    cb.addLine("}"); //for
    cb.addLine("return sb.toString()");
    cb.addLine("}").addLB();

    if (commandDeprecated) {
      cb.addLine("public boolean isDeprecated() {");
      cb.addLine("return true");
      cb.addLine("}");
    }

    cb.addLine("}"); // end of class

    return new GeneratedJava(commandName, getClassNameByCommandName(commandName), cb.toString(), targetPackage);

  }


  private static Set<String> getImplTemplateImports() {
    Set<String> result = new HashSet<String>();
    result.add(XynaException.class.getName());
    result.add(XYNA_COMMAND_IMPLEMENTATION_CLASS_NAME);
    result.add(OutputStream.class.getName());
    return result;
  }


  private static Set<String> getParserImports() {
    Set<String> result = new HashSet<String>();
    result.add(XynaException.class.getName());
    result.add(INVALID_PARAMETERNUMBER_CLASS_NAME);
    result.add(Options.class.getName());
    result.add(Option.class.getName());
    result.add(AXYNACOMMAND_CLASS_NAME);
    result.add(XYNA_COMMAND_IMPLEMENTATION_CLASS_NAME);
    result.add(OutputStream.class.getName());
    result.add(OptionBuilder.class.getName());
    result.add(List.class.getName());
    result.add(ArrayList.class.getName());
    result.add(Collection.class.getName());
    return result;
  }


  public static void validateDocument(Document xmlDefinition) {

  }


  private static class GeneratedJava {

    public String commandName;
    public String javaCode;
    public String className;
    private String targetPackage;

    public GeneratedJava(String commandName, String className, String javaCode, String targetPackage) {
      this.commandName = commandName;
      this.className = className;
      this.javaCode = javaCode;
      this.targetPackage = targetPackage;
    }


    public String getFqClassName() {
      return targetPackage + ".generated." + className;
    }

  }


  private static class ArgumentOptionInformation {

    public String name;
    public String description;
    public String argumentValueName;
    public boolean isOptional;
    public boolean multipleValues;

    public ArgumentOptionInformation(String argumentName, String description, String argumentValueName,
                                     boolean isOptional, boolean multipleValues) {
      this.name = argumentName;
      this.description = description;
      this.argumentValueName = argumentValueName;
      this.isOptional = isOptional;
      this.multipleValues = multipleValues;
    }

  }


  private static class BooleanOptionInformation {

    public BooleanOptionInformation(String name, String longName, String description) {
      this.name = name;
      this.longName = longName;
      this.description = description;
    }


    public String name;
    public String longName;
    public String description;

  }


  private static String getClassNameByCommandName(String commandName) {
    return firstCharacterToUpperCase(commandName);
  }


  private static String getImplClassNameByCommandName(String commandName) {
    return getClassNameByCommandName(commandName) + "Impl";
  }


  private static void appendArgumentOption(CodeBuffer cb, ArgumentOptionInformation info) {
    cb.add(OptionBuilder.class.getSimpleName());
    if (!info.isOptional) {
      cb.add(".isRequired()");
    }
    if (info.multipleValues) {
      cb.add(".hasArgs()");
    } else {
      cb.add(".hasArg()");
    }
    cb.add(".withDescription(\"", info.description, "\")");
    if (info.argumentValueName != null && info.argumentValueName.length() > 0) {
      cb.add(".withArgName(\"", info.argumentValueName, "\")");
    } else {
      cb.add(".withArgName(");
      if (info.multipleValues) {
        cb.add("\"args\")");
      } else {
        cb.add("\"arg\")");
      }
    }
    cb.add(".create(\"", info.name, "\")");
  }


  private static void appendBooleanOption(CodeBuffer cb, BooleanOptionInformation info) {
    cb.add("new ", Option.class.getSimpleName(), "(\"", info.name, "\", \"", info.longName, "\", false, \"",
           info.description, "\")");
  }


  private static String firstCharacterToUpperCase(String input) {
    if (input == null) {
      return null;
    }
    if (input.length() < 2) {
      return input.toUpperCase();
    }
    return new StringBuilder().append(input.substring(0, 1).toUpperCase()).append(input.substring(1, input.length()))
        .toString();
  }

}
