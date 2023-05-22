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
package xmcp.xacm.impl;



import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xopctrl.usermanagement.RightScope;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;

import base.locale.Locale;
import xmcp.xacm.RightManagementServiceOperation;
import xmcp.xacm.rightmanagement.datatypes.ModifyRightRequest;
import xmcp.xacm.rightmanagement.datatypes.Right;
import xmcp.xacm.rightmanagement.datatypes.RightParameter;
import xmcp.xacm.rightmanagement.datatypes.RightParameterDefinition;
import xmcp.xacm.rightmanagement.exceptions.FillParameterDefinitionException;
import xmcp.xacm.rightmanagement.exceptions.ModifyRightException;


public class RightManagementServiceOperationImpl implements ExtendedDeploymentTask, RightManagementServiceOperation {

  private static final XynaFactoryManagement factoryManagement = (XynaFactoryManagement) XynaFactory.getInstance().getFactoryManagement();

  private static final String regExpPattern = "^\\s*(\\/.*\\/)";
  private static final String optionsPattern = "^\\s*\\[(.*)\\]";

  private static final String optionsType = "options";
  private static final String regExpType = "regex";
  private static final String xynaType = "xyna";


  private void fillParameterDefinitionInternal(Right right, Locale locale, Map<String, RightScope> rights) {
    if (right.getParameterList() == null) {
      return;
    }

    String rightName = right.getRightName();
    RightScope rightScope = rights.get(rightName);
    if (rightScope == null) {
      return;
    }

    String[] split = rightScope.getDefinition().split(":");
    int i = 1;
    for (RightParameter parameter : right.getParameterList()) {
      List<RightParameterDefinition> parameterDefinitionList = new ArrayList<>();
      String definition = split[i];
      if (definition.matches(optionsPattern)) {
        parameter.unversionedSetType(optionsType);
        String[] defsplit = definition.replace("[", "").replace("]", "").split(",");
        for (String def : defsplit) {
          RightParameterDefinition rpd = new RightParameterDefinition();
          rpd.unversionedSetDefinition(def.trim());
          parameterDefinitionList.add(rpd);
        }
      } else {
        RightParameterDefinition rpd = new RightParameterDefinition();
        parameter.unversionedSetType(definition.matches(regExpPattern) ? regExpType : xynaType);
        rpd.unversionedSetDefinition(definition.trim());
        parameterDefinitionList.add(rpd);
      }
      parameter.unversionedSetParameterDefinitionList(parameterDefinitionList);
      i++;
    }
  }


  @Override
  public void fillParameterDefinition(Right right, Locale locale) throws FillParameterDefinitionException {
    if (right.getParameterList() != null) {
      Map<String, RightScope> rights = createScopeMap(locale);
      fillParameterDefinitionInternal(right, locale, rights);
    }
  }


  private Map<String, RightScope> createScopeMap(Locale locale) throws FillParameterDefinitionException {
    try {
      String language = getDocumentationLanguage(locale).toString();
      Collection<RightScope> rights = XynaFactory.getInstance().getXynaMultiChannelPortal().getRightScopes(language);
      Map<String, RightScope> result = createScopeMap(rights);
      return result;
    } catch (PersistenceLayerException e) {
      throw new FillParameterDefinitionException(e.getMessage(), e);
    }
  }


  private Map<String, RightScope> createScopeMap(Collection<RightScope> rights) {
    Map<String, RightScope> result = new HashMap<>();
    for (RightScope rightScope : rights) {
      result.put(rightScope.getName(), rightScope);
    }
    return result;
  }

  @Override
  public void fillParameterDefinitions(List<? extends Right> rights, Locale locale) throws FillParameterDefinitionException {
    Map<String, RightScope> factoryRights = createScopeMap(locale);

    for (Right right : rights) {
      fillParameterDefinitionInternal(right, locale, factoryRights);
    }
  }


  public void setDescriptionOfRight(ModifyRightRequest modifyRightRequest) throws ModifyRightException {
    try {
      String language = getDocumentationLanguage(modifyRightRequest.getLocale()).toString();
      factoryManagement.setDescriptionOfRight(modifyRightRequest.getRightName(), modifyRightRequest.getDocumentation(), language);
    } catch (XynaException e) {
      throw new ModifyRightException(e.getMessage(), e);
    }
  }

  private DocumentationLanguage getDocumentationLanguage(Locale locale) {
    if (locale == null || locale.getLanguage() == null) {
      return DocumentationLanguage.EN;
    } else if (locale.getLanguage().startsWith("de")) {
      return DocumentationLanguage.DE;
    } else {
      return DocumentationLanguage.EN;
    }
  }

  public void onDeployment() throws XynaException {
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public void onUndeployment() throws XynaException {
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

}
