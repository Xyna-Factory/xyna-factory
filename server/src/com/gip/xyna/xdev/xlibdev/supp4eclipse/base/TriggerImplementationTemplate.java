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

package com.gip.xyna.xdev.xlibdev.supp4eclipse.base;



import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileWriteException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStartedException;
import com.gip.xyna.xact.exceptions.XACT_TriggerCouldNotBeStoppedException;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.exceptions.XDEV_InvalidProjectTemplateParametersException;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.StartParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xmcp.exceptions.XMCP_INVALID_PARAMETERNUMBER;
import com.gip.xyna.xmcp.xfcli.generated.Addtrigger;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class TriggerImplementationTemplate extends ImplementationTemplate {

  private static final Logger logger = CentralFactoryLogging.getLogger(TriggerImplementationTemplate.class);

  private static final String TRIGGER_JAVA_FILE = "Trigger.java";
  private static final String TRIGGER_SUFFIX = "Trigger";
  private static final String TRIGGER_CONNECTION_JAVA_FILE = "TriggerConnection.java";
  private static final String STARTPARAMETER = "StartParameter";
  public static final String IMPL_KIND = "triggerimpl";
  public static final String IMPL_KIND_BUILD_SCRIPT = "buildTrigger.xml";

  private final String triggerName;


  /**
   * @param triggerName - Der Name des Triggers in einer Form, so dass klassennamen &lt;name&gt;Trigger,
   *          &lt;name&gt;Startparameter, &lt;name&gt;TriggerConnection Sinn ergeben.
   */
  public TriggerImplementationTemplate(String triggerName, Long revision) {
    super(revision);
    if (triggerName.endsWith("Trigger")) {
      triggerName = triggerName.substring(0, triggerName.lastIndexOf("Trigger"));
    }
    this.triggerName = triggerName;
  }

  
  public String getTriggerName() {
    return triggerName;
  }


  public String getProjectName() {
    return triggerName + TRIGGER_SUFFIX;
  }


  public String getProjectKindFolder() {
    return IMPL_KIND;
  }

  
  public String getBuildScriptName() {
    return IMPL_KIND_BUILD_SCRIPT;
  }
  

  public String getFQCodeClass() throws XPRC_InvalidPackageNameException  {
    return GenerationBase.transformNameForJava(Support4Eclipse.TRIGGER_LOCATION + "." + triggerName.toLowerCase()
        + ".exceptions.Codes");
  }


  public String updateBuildXmlDeployTarget(File projectLocationDirectory, String line) throws XDEV_InvalidProjectTemplateParametersException {
    String revisionDir = getRevisionDir();
    String pathToSavedXml =
        XynaActivationTrigger.getTriggerXmlLocationByTriggerFqClassName(getTriggerName() + "Trigger", revision);
    String pathOnly = pathToSavedXml.substring(0, pathToSavedXml.lastIndexOf(File.separator));
    String xmlNameOnly = pathToSavedXml.substring(pathToSavedXml.lastIndexOf(File.separator) + 1);
    line =
        line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_XML_PATH),
                        Matcher.quoteReplacement("${server.path}" + File.separator 
                                                 + "${revision.dir}" + pathOnly.substring(revisionDir.length())));
    line = line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_XML_FILE_NAME), xmlNameOnly);

    if (line.contains(TEMPLATE_DEPLOY_STATEMENT)) {
      Addtrigger at = new Addtrigger();
      at.setFqClassName("${fqclassname}");
      at.setTriggerName(getTriggerName() + "Trigger");
      at.setJarFiles(new String[] {"${deploy.jars}"});
      at.setSharedLibs("${deploy.sharedlibs}");
      at.setWorkspaceName("'${workspacename}'");
      String commandString;
      try {
        commandString = at.getCommandAsString();
      } catch (XMCP_INVALID_PARAMETERNUMBER e) {
        logger.warn("could not generate cli command for generated build.xml", e);
        commandString = "could not be generated. see logfile on server.";
      }
      line = line.replaceAll(TEMPLATE_DEPLOY_STATEMENT, Matcher.quoteReplacement(commandString));
    } else {
      String triggerDeploymentDir = RevisionManagement.getPathForRevision(PathType.TRIGGER, revision);
      line =
          line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_FQ_CLASSNAME),
                          Matcher.quoteReplacement(Support4Eclipse.TRIGGER_LOCATION + "." + getTriggerName()
                              + "Trigger"));
      line =
          line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_PATH),
                          Matcher.quoteReplacement("${server.path}" + File.separator 
                              + "${revision.dir}" + triggerDeploymentDir.substring(revisionDir.length())
                              + File.separator + getTriggerName() + "Trigger"));
      line =
          line.replaceAll(Pattern.quote(TEMPLATE_DEPLOY_TARGET_PATH),
                          Matcher.quoteReplacement("${revision.dir}" + triggerDeploymentDir.substring(revisionDir.length())
                              + File.separator + getTriggerName() + "Trigger"));
      line =
          line.replaceAll(Pattern.quote(TEMPLATE_DEPLOY_MAIN_JAR),
                          Matcher.quoteReplacement("." + File.separator + Constants.LIB_DIR + File.separator
                              + getTriggerName() + "Trigger.jar"));
      line =
          line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_WORKSPACE_NAME),
                          Matcher.quoteReplacement(getWorkspaceName()));
      line = 
          line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_REVISION_DIR),
                          Matcher.quoteReplacement(revisionDir));
    }
    return line;
  }


  public void writeTemplateFiles(File projectLocationDirectory, boolean legacy) throws Ex_FileWriteException {
    // create trigger template strings
    String[] templates = generateTriggerImplTemplates(this);

    // write template strings to files on disk
    FileUtils.writeStringToFile(templates[0], new File(projectLocationDirectory, Support4Eclipse.SOURCE_FOLDER
        + File.separator + (Support4Eclipse.TRIGGER_LOCATION).replaceAll("\\.", File.separator) + File.separator
        + getTriggerName() + TRIGGER_JAVA_FILE));
    FileUtils.writeStringToFile(templates[1], new File(projectLocationDirectory, Support4Eclipse.SOURCE_FOLDER
        + File.separator + (Support4Eclipse.TRIGGER_LOCATION).replaceAll("\\.", File.separator) + File.separator
        + getTriggerName() + TRIGGER_CONNECTION_JAVA_FILE));
    FileUtils.writeStringToFile(templates[2], new File(projectLocationDirectory, Support4Eclipse.SOURCE_FOLDER
        + File.separator + (Support4Eclipse.TRIGGER_LOCATION).replaceAll("\\.", File.separator) + File.separator
        + getTriggerName() + STARTPARAMETER + ".java"));

    File remoteXmlFile =
        new File(projectLocationDirectory, Support4Eclipse.XML_DEFINITION_FOLDER + File.separator + getTriggerName()
            + "Trigger.xml");
    String newXmlTriggerDef = "<" + XynaActivationTrigger.XmlElements.TRIGGER + ">\n";
    newXmlTriggerDef += "\t<" + GenerationBase.EL.ADDITIONALDEPENDENCIES + ">\n";
    newXmlTriggerDef +=
        "\t\t<!-- <" + GenerationBase.EL.DEPENDENCY_DATATYPE + ">fullyQualifiedMdmType</"
            + GenerationBase.EL.DEPENDENCY_DATATYPE + ">\n";
    newXmlTriggerDef +=
        "\t\t<" + GenerationBase.EL.DEPENDENCY_XYNA_PROPERTY + ">propertyName</"
            + GenerationBase.EL.DEPENDENCY_XYNA_PROPERTY + "> -->\n";
    newXmlTriggerDef += "\t</" + GenerationBase.EL.ADDITIONALDEPENDENCIES + ">\n";
    newXmlTriggerDef += "</" + XynaActivationTrigger.XmlElements.TRIGGER + ">";
    FileUtils.writeStringToFile(newXmlTriggerDef, remoteXmlFile);
  }


  /**
   * @param tParas
   * @return array mit 3 elemente: triggerklasse, triggerconnection-klasse und startparameter-klasse.
   */
  public static String[] generateTriggerImplTemplates(TriggerImplementationTemplate tParas) {
    // trigger class
    CodeBuffer cb = new CodeBuffer("Activation");
    cb.addLine("package ", Support4Eclipse.TRIGGER_LOCATION).addLB();
    cb.addLine("import ", EventListener.class.getName());
    cb.addLine("import ", CentralFactoryLogging.class.getName());
    cb.addLine("import ", Logger.class.getName());
    cb.addLine("import ", XACT_TriggerCouldNotBeStartedException.class.getName());
    cb.addLine("import ", XACT_TriggerCouldNotBeStoppedException.class.getName());
    cb.addLB();

    cb.addLine("public class ", tParas.getTriggerName(), "Trigger extends ", EventListener.class.getSimpleName(), "<",
               tParas.getTriggerName(), "TriggerConnection, ", tParas.getTriggerName(), STARTPARAMETER, "> {");
    cb.addLB();

    // logger
    cb.addLine("private static Logger logger = ", CentralFactoryLogging.class.getSimpleName(), ".getLogger(",
               tParas.getTriggerName(), "Trigger.class)");
    cb.addLB();

    // konstruktor
    cb.addLine("public ", tParas.getTriggerName(), "Trigger() {").addLine("}").addLB();

    // start
    cb.addLine("public void start(", tParas.getTriggerName(), STARTPARAMETER + " sp) throws ",
               XACT_TriggerCouldNotBeStartedException.class.getSimpleName(), " {");
    cb.addLine("//TODO implementation").addLine("//TODO update dependency xml file");
    cb.addLine("}").addLB();

    // receive
    cb.addLine("public ", tParas.getTriggerName(), "TriggerConnection receive() {");
    cb.addLine("//TODO implementation").addLine("//TODO update dependency xml file");
    cb.addLB();
    cb.addLine("try {");
    cb.addLine("Thread.sleep(2000)");
    cb.addLine("} catch (InterruptedException e) {");
    cb.addLine("}");
    cb.addLine("return new " + tParas.getTriggerName() + "TriggerConnection()").addLine("}").addLB();

    // rejected execution
    cb.addLine("/**");
    cb.addLine(" * Called by Xyna Processing if there are not enough system capacities to process the request.");
    cb.addLine(" */");
    cb.addLine("protected void onProcessingRejected(String cause, ", tParas.getTriggerName(),
               "TriggerConnection con) {");
    cb.addLine("// TODO implementation");
    cb.addLine("}").addLB();

    // stop
    cb.addLine("/**");
    cb.addLine(" * called by Xyna Processing to stop the Trigger.");
    cb.addLine(" * should make sure, that start() may be called again directly afterwards. connection instances");
    cb.addLine(" * returned by the method receive() should not be expected to work after stop() has been called.");
    // classcastexception gibts dann, weil von altem classloader geladen
    cb.addLine(" */");
    cb.addLine("public void stop() throws ", XACT_TriggerCouldNotBeStoppedException.class.getSimpleName(), " {")
        .addLine("//TODO implementation").addLine("//TODO update dependency xml file").addLine("}").addLB();

    // onNoFilterFound
    cb.addLine("/**");
    cb.addLine(" * called when a triggerconnection generated by this trigger was not accepted by any filter");
    cb.addLine(" * registered to this trigger");
    cb.addLine(" * @param con corresponding triggerconnection");
    cb.addLine(" */");
    cb.addLine("public void onNoFilterFound(" + tParas.getTriggerName() + "TriggerConnection con) {");
    cb.addLine("//TODO implementation").addLine("//TODO update dependency xml file").addLine("}").addLB();

    // getclassdescription
    cb.addLine("/**");
    cb.addLine(" * @return description of this trigger");
    cb.addLine(" */");
    cb.addLine("public String getClassDescription() {");
    cb.addLine("//TODO implementation");
    cb.addLine("//TODO update dependency xml file");
    cb.addLine("return null");
    cb.addLine("}").addLB();

    // end of class
    cb.addLine("}");
    String triggerString = cb.toString();

    // triggerconnection class
    cb = new CodeBuffer("Activation");
    cb.addLine("package " + Support4Eclipse.TRIGGER_LOCATION).addLB();
    cb.addLine("import " + TriggerConnection.class.getName());
    cb.addLine("import " + CentralFactoryLogging.class.getName());
    cb.addLine("import " + XynaException.class.getName());
    cb.addLine("import " + Logger.class.getName());
    cb.addLB();
    cb.addLine("public class " + tParas.getTriggerName() + "TriggerConnection extends "
        + TriggerConnection.class.getSimpleName() + " {");
    cb.addLB();
    cb.addLine("private static Logger logger = " + CentralFactoryLogging.class.getSimpleName() + ".getLogger("
        + tParas.getTriggerName() + "TriggerConnection.class)");
    cb.addLB();
    cb.addLine("// arbitrary constructor");
    cb.addLine("public " + tParas.getTriggerName() + "TriggerConnection() {");
    cb.addLine("}").addLB();
    cb.addLine("}");
    String triggerConString = cb.toString();

    // startparameter class
    cb = new CodeBuffer("Activation");
    cb.addLine("package " + Support4Eclipse.TRIGGER_LOCATION).addLB();
    cb.addLine("import " + StartParameter.class.getName());
    cb.addLine("import " + CentralFactoryLogging.class.getName());
    cb.addLine("import " + XACT_InvalidStartParameterCountException.class.getName());
    cb.addLine("import " + XACT_InvalidTriggerStartParameterValueException.class.getName());
    cb.addLine("import " + Logger.class.getName());
    cb.addLB();
    cb.addLine("public class " + tParas.getTriggerName() + STARTPARAMETER + " implements "
        + StartParameter.class.getSimpleName() + " {");
    cb.addLB();
    cb.addLine("private static Logger logger = " + CentralFactoryLogging.class.getSimpleName() + ".getLogger("
        + tParas.getTriggerName() + STARTPARAMETER + ".class)");
    cb.addLB();
    cb.addLine("// the empty constructor may not be removed or throw exceptions! additional ones are possible, though.");
    cb.addLine("public " + tParas.getTriggerName() + STARTPARAMETER + "() {");
    cb.addLine("}").addLB();
    cb.addLine("/**");
    cb.addLine("* Is called by XynaProcessing with the parameters provided by the deployer");
    cb.addLine("* @return StartParameter Instance which is used to instantiate corresponding Trigger");
    cb.addLine("*/");
    cb.addLine("public " + StartParameter.class.getSimpleName() + " build(String ... args) throws ",
               XACT_InvalidStartParameterCountException.class.getSimpleName(), ", ",
               XACT_InvalidTriggerStartParameterValueException.class.getSimpleName(), " {");
    cb.addLine("//TODO implementation").addLine("return new " + tParas.getTriggerName() + STARTPARAMETER + "();")
        .addLine("}").addLB();
    // get ParameterDescriptions
    cb.addLine("/**");
    cb.addLine("* ");
    cb.addLine("* @return array of valid lists of descriptions of parameters. example: if parameters (A,B) and (A,C,D)");
    cb.addLine("*    are valid, then this method should return new String[]{{\"descriptionA\", \"descriptionB\"},");
    cb.addLine("*     {\"descriptionA\", \"descriptionC\", \"descriptionD\"}}");
    cb.addLine("*/");
    cb.addLine("public String[][] getParameterDescriptions() {");
    cb.addLine("//TODO implementation").addLine("return null").addLine("}").addLB();
    /**
     * zur dokumentation
     * @return liste der startparameterbeschreibungen sortiert nach m�glichen kombinationen. falls es die
     *         startparameterm�glichkeiten (A,B) und (A,C,D) gibt, w�rde hier zur�ckgegeben werden: new String[][]{{"A",
     *         "B"}, {"A", "C", "D"}}.
     */
    cb.addLine("}");
    return new String[] {triggerString, triggerConString, cb.toString()};
  }


  public String getTriggerFQClassName() {
    return Support4Eclipse.TRIGGER_LOCATION + "." + getTriggerName() + "Trigger";
  }

}
