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



import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FileUtils;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.exceptions.XACT_LibOfTriggerImplNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerImplClassNotFoundException;
import com.gip.xyna.xact.exceptions.XACT_TriggerNotFound;
import com.gip.xyna.xact.trigger.Trigger;
import com.gip.xyna.xact.trigger.XynaActivationTrigger;
import com.gip.xyna.xdev.exceptions.XDEV_InvalidProjectTemplateParametersException;
import com.gip.xyna.xdev.exceptions.XDEV_TRIGGER_JAR_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.TriggerConnection;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xlibdev.supp4eclipse.Support4Eclipse;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.exceptions.XFMG_SHARED_LIB_NOT_FOUND;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.versionmgmt.VersionManagement.PathType;
import com.gip.xyna.xmcp.exceptions.XMCP_INVALID_PARAMETERNUMBER;
import com.gip.xyna.xmcp.xfcli.generated.Addfilter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;



public class FilterImplementationTemplate extends ImplementationTemplate {

  private static final Logger logger = CentralFactoryLogging.getLogger(FilterImplementationTemplate.class);

  private static final String FILTER_LOCATION = "com.gip.xyna.xact.filter";
  private final String filterName;
  private final String triggerName;
  public static final String IMPL_KIND = "filterimpl";
  public static final String IMPL_KIND_BUILD_SCRIPT = "buildFilter.xml";


  public FilterImplementationTemplate(String filterName, String triggerName, Long revision) {
    super(revision);
    if (filterName.endsWith("Filter")) {
      filterName = filterName.substring(0, filterName.lastIndexOf("Filter"));
    }
    this.filterName = filterName;
    this.triggerName = triggerName;
  }


  public String getFilterName() {
    return filterName;
  }


  public String getTriggerName() {
    return triggerName;
  }


  public String getProjectName() {
    return filterName + "Filter";
  }


  public String getProjectKindFolder() {
    return IMPL_KIND;
  }
  
  
  public String getBuildScriptName() {
    return IMPL_KIND_BUILD_SCRIPT;
  }


  public String getFQCodeClass() throws XynaException {
    return GenerationBase.transformNameForJava(FILTER_LOCATION + "." + filterName.toLowerCase() + ".exceptions.Codes");
  }


  public String updateBuildXmlDeployTarget(File projectLocationDirectory, String line)
      throws XDEV_InvalidProjectTemplateParametersException {

    String revisionDir = getRevisionDir();
    String pathToSavedXml = XynaActivationTrigger.getFilterXmlLocationByFqFilterClassName(getFqFilterClassName(), revision);
    String pathOnly = pathToSavedXml.substring(0, pathToSavedXml.lastIndexOf(File.separator));
    String xmlNameOnly = pathToSavedXml.substring(pathToSavedXml.lastIndexOf(File.separator) + 1);
    line =
        line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_XML_PATH),
                        Matcher.quoteReplacement("${server.path}" + File.separator 
                                                 + "${revision.dir}" + pathOnly.substring(revisionDir.length())));
    line = line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_XML_FILE_NAME), xmlNameOnly);

    if (line.contains(TEMPLATE_DEPLOY_STATEMENT)) {
      Addfilter af = new Addfilter();
      af.setFilterName(getFilterName() + "Filter");
      af.setFqClassName("${fqclassname}");
      af.setTriggerName(getTriggerName());
      af.setSharedLibs("${deploy.sharedlibs}");
      af.setJarFiles(new String[] {"${deploy.jars}"});
      af.setWorkspaceName("'${workspacename}'");
      String commandString;
      try {
        commandString = af.getCommandAsString();
      } catch (XMCP_INVALID_PARAMETERNUMBER e) {
        logger.warn("could not generate cli command for generated build.xml", e);
        commandString = "could not be generated. see logfile on server.";
      }
      line = line.replaceAll(TEMPLATE_DEPLOY_STATEMENT, Matcher.quoteReplacement(commandString));
    } else {
      String filterDeploymentDir = RevisionManagement.getPathForRevision(PathType.FILTER, revision);
      line =
          line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_FQ_CLASSNAME),
                          Matcher.quoteReplacement(FILTER_LOCATION + "." + getFilterName() + "Filter"));
      line =
          line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_SERVER_MDM_PATH),
                          Matcher.quoteReplacement("${server.path}" + File.separator 
                              + "${revision.dir}" + filterDeploymentDir.substring(revisionDir.length())
                              + File.separator + getFilterName() + "Filter"));
      line =
          line.replaceAll(Pattern.quote(TEMPLATE_DEPLOY_TARGET_PATH),
                          Matcher.quoteReplacement("${revision.dir}" + filterDeploymentDir.substring(revisionDir.length()) 
                                                   + File.separator + getFilterName() + "Filter"));
      line =
          line.replaceAll(Pattern.quote(TEMPLATE_DEPLOY_MAIN_JAR),
                          Matcher.quoteReplacement("." + File.separator + Constants.LIB_DIR + File.separator
                              + getFilterName() + "Filter.jar"));
      line =
          line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_WORKSPACE_NAME),
                          Matcher.quoteReplacement(getWorkspaceName()));
      line = 
          line.replaceAll(Pattern.quote(Support4Eclipse.TEMPLATE_REVISION_DIR),
                          Matcher.quoteReplacement(revisionDir));
    }
    return line;
  }


  private String getPathToTrigger() {
    Trigger trigger;
    try {
      trigger = XynaFactory.getInstance().getActivation().getActivationTrigger().getTrigger(revision, getTriggerName(), true);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XACT_TriggerNotFound e) {
      throw new RuntimeException(e);
    }
    String pathToTrigger = RevisionManagement.getPathForRevision(PathType.TRIGGER, trigger.getRevision()) + File.separator;
    if (trigger.getFQTriggerClassName().split("\\.").length > 0) {
      String[] parts = trigger.getFQTriggerClassName().split("\\.");
      return pathToTrigger + parts[parts.length - 1];
    }
    throw new RuntimeException("FQClassname of trigger " + getTriggerName() + " is empty");
  }


  public void writeTemplateFiles(File projectLocationDirectory, boolean legacy) throws XDEV_InvalidProjectTemplateParametersException,
      Ex_FileAccessException {

    // create filter template string
    String[] templates = generateFilterImplTemplate(this);

    // write template string to a file on disk
    FileUtils.writeStringToFile(templates[0], new File(projectLocationDirectory, Support4Eclipse.SOURCE_FOLDER + File.separator
        + (Support4Eclipse.FILTER_LOCATION).replaceAll("\\.", File.separator) + File.separator
        + getFilterName() + "Filter.java"));
    FileUtils.writeStringToFile(templates[1], new File(projectLocationDirectory, Support4Eclipse.SOURCE_FOLDER + File.separator
        + (Support4Eclipse.FILTER_LOCATION).replaceAll("\\.", File.separator) + File.separator
        + getFilterName() + "ConfigurationParameter.java"));

    
    // copy trigger libs as set in the .classpath file (see above)
    String pathToTrigger = getPathToTrigger();
    File triggerDir = new File(pathToTrigger);
    if (triggerDir.exists()) {
      FileUtils.copyRecursively(new File(pathToTrigger), new File(projectLocationDirectory, Support4Eclipse.PROJECT_LIB_XYNA_FOLDER));
    }

    File remoteXmlFile =
        new File(projectLocationDirectory, Support4Eclipse.XML_DEFINITION_FOLDER + File.separator + getFilterName()
            + "Filter.xml");
    String newXmlFilterDef = "<" + XynaActivationTrigger.XmlElements.FILTER + ">\n";
    newXmlFilterDef += "\t<" + GenerationBase.EL.ADDITIONALDEPENDENCIES + ">\n";
    newXmlFilterDef +=
        "\t\t<!-- <" + GenerationBase.EL.DEPENDENCY_DATATYPE + ">fullyQualifiedMdmType</"
            + GenerationBase.EL.DEPENDENCY_DATATYPE + ">\n";
    newXmlFilterDef +=
        "\t\t<" + GenerationBase.EL.DEPENDENCY_XYNA_PROPERTY + ">propertyName</"
            + GenerationBase.EL.DEPENDENCY_XYNA_PROPERTY + ">\n";
    newXmlFilterDef +=
        "\t\t<" + GenerationBase.EL.DEPENDENCY_WORKFLOW + ">workflow</" + GenerationBase.EL.DEPENDENCY_WORKFLOW
            + "> -->\n";
    newXmlFilterDef += "\t</" + GenerationBase.EL.ADDITIONALDEPENDENCIES + ">\n";
    newXmlFilterDef += "</" + XynaActivationTrigger.XmlElements.FILTER + ">";
    FileUtils.writeStringToFile(newXmlFilterDef, remoteXmlFile);
  }


  @SuppressWarnings("unchecked")
  public static String[] generateFilterImplTemplate(FilterImplementationTemplate tParas)
      throws XDEV_InvalidProjectTemplateParametersException {
    Trigger trigger;
    try {
      trigger = XynaFactory.getInstance().getActivation().getActivationTrigger().getTrigger(tParas.getRevision(), tParas.getTriggerName(), true);
    } catch (PersistenceLayerException e) {
      throw new RuntimeException(e);
    } catch (XACT_TriggerNotFound e) {
      throw new RuntimeException(e);
    }
    Class<EventListener<?, ?>> c = null;
    try {
      c = trigger.getEventListenerClass();
    } catch (XACT_TriggerImplClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (XFMG_SHARED_LIB_NOT_FOUND e) {
      throw new RuntimeException(e);
    } catch (XACT_LibOfTriggerImplNotFoundException e) {
      throw new RuntimeException(e);
    }
    if (c == null) {
      throw new XDEV_InvalidProjectTemplateParametersException("triggerName", tParas.getTriggerName());
    }
    ParameterizedType t = (ParameterizedType) c.getGenericSuperclass();
    Type[] ts = t.getActualTypeArguments();
    Class<TriggerConnection> tcclass = (Class<TriggerConnection>) ts[0];
    String tc = tcclass.getSimpleName();
    String cfg = tParas.getFilterName()+"ConfigurationParameter";
    
    CodeBuffer cb = new CodeBuffer("Activation");
    cb.addLine("package " + Support4Eclipse.FILTER_LOCATION).addLB();
    cb.addLine("import " + tcclass.getName());
    cb.addLine("import " + ConnectionFilter.class.getName());
    cb.addLine("import " + FilterConfigurationParameter.class.getName());
    cb.addLine("import " + XynaOrder.class.getName());
    cb.addLine("import " + XynaObject.class.getName());
    cb.addLine("import " + GeneralXynaObject.class.getName());
    cb.addLine("import " + CentralFactoryLogging.class.getName());
    cb.addLine("import " + XynaException.class.getName());
    cb.addLine("import " + Logger.class.getName());
    cb.addLine("import " + XynaException.class.getName());
    cb.addLine("import " + EventListener.class.getName());

    cb.addLB();
    cb.addLine("public class " + tParas.getFilterName() + "Filter extends ConnectionFilter<" + tc + "> {");
    cb.addLB();
    cb.addLine("private static final long serialVersionUID = 1L");
    cb.addLB();
    cb.addLine("private static Logger logger = " + CentralFactoryLogging.class.getSimpleName() + ".getLogger("
        + tParas.getFilterName() + "Filter.class)");
    cb.addLB();
    cb.addLine("/**");
    cb.addLine(" * Called to create a configuration template to parse configuration and show configuration options.");
    cb.addLine(" * @return "+cfg +" template");
    cb.addLine(" */");
    cb.addLine("@Override");
    cb.addLine("public "+FilterConfigurationParameter.class.getSimpleName()+" createFilterConfigurationTemplate() {");
    cb.addLine("return new "+cfg+"()");
    cb.addLine("}");
    cb.addLB();
    cb.addLine("/**");
    cb.addLine(" * Analyzes TriggerConnection and creates XynaOrder if it accepts the connection.");
    cb.addLine(" * This method returns a FilterResponse object, which includes the XynaOrder if the filter is responsible for the request.");
    cb.addLine(" * # If this filter is not responsible the returned object must be: FilterResponse.notResponsible()");
    cb.addLine(" * # If this filter is responsible the returned object must be: FilterResponse.responsible(XynaOrder order)");
    cb.addLine(" * # If this filter is responsible but the request is handled without creating a XynaOrder the ");
    cb.addLine(" *   returned object must be: FilterResponse.responsibleWithoutXynaorder()");
    cb.addLine(" * # If this filter is responsible but the request should be handled by an older version of the filter in another application version, the returned");
    cb.addLine(" *    object must be: FilterResponse.responsibleButTooNew().");
    cb.addLine(" * @param tc");
    cb.addLine(" * @return FilterResponse object");
    cb.addLine(" * @throws XynaException caused by errors reading data from triggerconnection or having an internal error.");
    cb.addLine(" *         Results in onError() being called by Xyna Processing.");
    cb.addLine(" */");
    cb.addLine("@Override");
    cb.addLine("public FilterResponse createXynaOrder(" + tc + " tc, "+FilterConfigurationParameter.class.getSimpleName()+" baseConfig ) throws XynaException {");
    cb.addLine("//"+cfg+" config = ("+cfg+")baseConfig;");
    cb.addLine("//return FilterResponse.notResponsible() if next filter should be tried");
    cb.addLine("//TODO implementation");
    cb.addLine("//TODO update dependency xml file");
    cb.addLine("return FilterResponse.notResponsible()");
    cb.addLine("}").addLB();
    cb.addLine("/**");
    cb.addLine(" * Called when above XynaOrder returns successfully.");
    cb.addLine(" * @param response by XynaOrder returned GeneralXynaObject");
    cb.addLine(" * @param tc corresponding triggerconnection");
    cb.addLine(" */");
    cb.addLine("@Override");
    cb.addLine("public void onResponse(", GeneralXynaObject.class.getSimpleName(), " response, " + tc + " tc) {");
    cb.addLine("//TODO implementation").addLine("//TODO update dependency xml file").addLine("}").addLB();
    cb.addLine("/**");
    cb.addLine(" * Called when above XynaOrder returns with error or if an XynaException occurs in generateXynaOrder().");
    cb.addLine(" * @param e");
    cb.addLine(" * @param tc corresponding triggerconnection");
    cb.addLine(" */");
    cb.addLine("public void onError(XynaException[] e, " + tc + " tc) {");
    cb.addLine("//TODO implementation").addLine("//TODO update dependency xml file").addLine("}").addLB();
    cb.addLine("/**");
    cb.addLine(" * @return description of this filter");
    cb.addLine(" */");
    cb.addLine("public String getClassDescription() {");
    cb.addLine("//TODO implementation").addLine("//TODO update dependency xml file").addLine("return null")
        .addLine("}").addLB();
    cb.addLine("/**");
    cb.addLine(" * Called once for each filter instance when it is deployed and again on each classloader change (e.g. when changing corresponding implementation jars).");
    cb.addLine(" * @param triggerInstance trigger instance this filter instance is registered to");
    cb.addLine(" */");
    cb.addLine("@Override");
    cb.addLine("public void onDeployment(EventListener triggerInstance) {");
    cb.addLine("super.onDeployment(triggerInstance)");
    cb.addLine("}").addLB();
    cb.addLine("/**");
    cb.addLine(" * Called once for each filter instance when it is undeployed and again on each classloader change (e.g. when changing corresponding implementation jars).");
    cb.addLine(" * @param triggerInstance trigger instance this filter instance is registered to");
    cb.addLine(" */");
    cb.addLine("@Override");
    cb.addLine("public void onUndeployment(EventListener triggerInstance) {");
    cb.addLine("super.onUndeployment(triggerInstance)");
    cb.addLine("}").addLB();
    cb.addLine("}");
    
    String filterString = cb.toString();
    String filterConfigParamName = tParas.getFilterName() + "ConfigurationParameter";
    
    cb = new CodeBuffer("Activation");
    cb.addLine("package " + Support4Eclipse.FILTER_LOCATION).addLB();
    cb.addLine("import " + java.util.List.class.getName());
    cb.addLine("import " + java.util.Map.class.getName());
    cb.addLine("import " + com.gip.xyna.utils.misc.Documentation.class.getName());
    cb.addLine("import " + com.gip.xyna.utils.misc.StringParameter.class.getName());
    cb.addLine("import " + com.gip.xyna.xact.exceptions.XACT_InvalidFilterConfigurationParameterValueException.class.getName());
    cb.addLine("import " + com.gip.xyna.xdev.xfractmod.xmdm.FilterConfigurationParameter.class.getName());
    cb.addLB();
    cb.addLine("public class " + filterConfigParamName + " extends FilterConfigurationParameter {");
    cb.addLB();
    cb.addLine("private static final long serialVersionUID = 1L");
    cb.addLB();
    
    cb.addLine("public static final StringParameter<String> ORDER_TYPE = \n"+
    "    StringParameter.typeString(\"orderType\").\n"+
    "    documentation( Documentation.\n"+
    "        de(\"OrderType als Beispiel einer Filter-Konfiguration\").\n"+
    "        en(\"Order type as an example for filter configuration\").build() ).\n"+
    "    optional().build()");
    cb.addLB();
    cb.addLine("protected static final List<StringParameter<?>> ALL_PARAMETERS = \n"+
    "    StringParameter.asList( ORDER_TYPE );");
    cb.addLB();
    cb.addLine("private String orderType;");
    cb.addLB();
    cb.addLine("@Override");
    cb.addLine("public List<StringParameter<?>> getAllStringParameters() {");
    cb.addLine("return ALL_PARAMETERS");
    cb.addLine("}");
    cb.addLB();
    cb.addLine(" @Override");
    cb.addLine("public "+filterConfigParamName+" build(Map<String, Object> paramMap) throws XACT_InvalidFilterConfigurationParameterValueException {");
    cb.addLine(filterConfigParamName+" param = new "+filterConfigParamName+"();");
    cb.addLine("param.orderType = ORDER_TYPE.getFromMap(paramMap);");
    cb.addLine("return param;");
    cb.addLine("}");
    cb.addLB();
    cb.addLine("public String getOrderType() {");
    cb.addLine("return orderType;");
    cb.addLine("}");
    cb.addLB();
    cb.addLine("}");
    String configString = cb.toString();
    return new String[]{filterString, configString};
  }


  @Override
  public void writeToClasspathFile(BufferedWriter bw, boolean legacy) throws IOException {
    super.writeToClasspathFile(bw, legacy);
    File[] jars = new File(getPathToTrigger()).listFiles(FILENAMEFILTER_JARS);
    if (jars == null) {
      throw new RuntimeException(new XDEV_TRIGGER_JAR_NOT_FOUND(getPathToTrigger()));
    }
    for (File j : jars) {
      bw.write("  <classpathentry kind=\"lib\" path=\"lib/xyna/" + j.getName() + "\"/>\n");
    }
  }


  public String getFqFilterClassName() {
    return Support4Eclipse.FILTER_LOCATION + "." + getFilterName() + "Filter";
  }

}
