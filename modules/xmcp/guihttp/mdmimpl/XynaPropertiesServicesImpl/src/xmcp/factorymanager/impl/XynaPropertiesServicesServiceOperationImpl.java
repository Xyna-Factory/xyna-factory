/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 Xyna GmbH, Germany
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
package xmcp.factorymanager.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyWithDefaultValue;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Action;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.ScopedRight;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import xfmg.xopctrl.UserAuthenticationRight;
import xmcp.Documentation;
import xmcp.factorymanager.XynaPropertiesServicesServiceOperation;
import xmcp.factorymanager.shared.InsufficientRights;
import xmcp.factorymanager.xynaproperties.XynaProperty;
import xmcp.factorymanager.xynaproperties.XynaPropertyKey;
import xmcp.factorymanager.xynaproperties.exceptions.PropertyCreateException;
import xmcp.factorymanager.xynaproperties.exceptions.PropertyDeleteException;
import xmcp.factorymanager.xynaproperties.exceptions.PropertyLoadException;
import xmcp.factorymanager.xynaproperties.exceptions.PropertyUpdateException;
import xmcp.tables.datatypes.TableColumn;
import xmcp.tables.datatypes.TableInfo;
import xmcp.zeta.TableHelper;
import xmcp.zeta.TableHelper.Filter;
import xmcp.zeta.TableHelper.LogicalOperand;


public class XynaPropertiesServicesServiceOperationImpl implements ExtendedDeploymentTask, XynaPropertiesServicesServiceOperation {

  private static final XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal)XynaFactory.getInstance().getXynaMultiChannelPortal();
  private static final Configuration configuration = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration();
  
  private static final String PATH_KEY = "key";
  private static final String PATH_VALUE = "value";
  private static final String PATH_DEFAULT_VALUE = "defaultValue";
  private static final String PATH_DOCUMENTATION = "gUIDocumentation";
  
  @Override
  public void onDeployment() throws XynaException {
    // do something on deployment, if required
    // This is executed again on each classloader-reload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  @Override
  public void onUndeployment() throws XynaException {
    // do something on undeployment, if required
    // This is executed again on each classloader-unload, that is each
    // time a dependent object is redeployed, for example a type of an input parameter.
  }

  @Override
  public Long getOnUnDeploymentTimeout() {
    // The (un)deployment runs in its own thread. The service may define a timeout
    // in milliseconds, after which Thread.interrupt is called on this thread.
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }

  @Override
  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // Defines the behavior of the (un)deployment after reaching the timeout and if this service ignores a Thread.interrupt.
    // - BehaviorAfterOnUnDeploymentTimeout.EXCEPTION: Deployment will be aborted, while undeployment will log the exception and NOT abort.
    // - BehaviorAfterOnUnDeploymentTimeout.IGNORE: (Un)Deployment will be continued in another thread asynchronously.
    // - BehaviorAfterOnUnDeploymentTimeout.KILLTHREAD: (Un)Deployment will be continued after calling Thread.stop on the thread.
    //   executing the (Un)Deployment.
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }

  @Override
  public void createProperty(XynaProperty xynaPropertyDetails) throws PropertyCreateException {
    
    String testExists = multiChannelPortal.getProperty(xynaPropertyDetails.getKey());
    if(testExists != null)
      throw new PropertyCreateException("Property already exists");
    
    Map<DocumentationLanguage, String> doc = null;
    Map<DocumentationLanguage, String> defDoc = null;

    if(xynaPropertyDetails.getDocumentation() != null && !xynaPropertyDetails.getDocumentation().isEmpty() ) {
      doc = new EnumMap<>(DocumentationLanguage.class);
      for(xmcp.Documentation docu: xynaPropertyDetails.getDocumentation()) {
          doc.put(DocumentationLanguage.valueOf(docu.getLanguage().convert()), docu.getDocumentation() == null ? "" : docu.getDocumentation());
      }
    }

    XynaPropertyWithDefaultValue property = new XynaPropertyWithDefaultValue(
      xynaPropertyDetails.getKey(), 
      xynaPropertyDetails.getValue() == null ? 
        xynaPropertyDetails.getDefaultValue() == null ? "" : xynaPropertyDetails.getDefaultValue() : 
        xynaPropertyDetails.getValue(), 
      xynaPropertyDetails.getDefaultValue(), 
      doc, 
      defDoc);
    
    try {
        multiChannelPortal.setProperty(property);
    } catch (Exception e) {
        throw new PropertyCreateException(e.toString());
    }
    
  }
   
  @Override
  public XynaProperty getEntryDetails(XynaPropertyKey xynaPropertiesListEntry) throws PropertyLoadException {
    if(xynaPropertiesListEntry == null || xynaPropertiesListEntry.getKey() == null)
      throw new PropertyLoadException("No property key given.");
  
    XynaPropertyWithDefaultValue data = multiChannelPortal.getPropertyWithDefaultValue(xynaPropertiesListEntry.getKey());
    
    if(data == null)
      throw new PropertyLoadException("No property found.");
    
    XynaProperty result = new XynaProperty();
    result.setKey(data.getName());
    result.setValue(data.getValue());
    result.setDocumentation(getDocuAsArray(data.getDocumentation(), data.getDefDocumentation()));
    result.setDefaultValue(data.getDefValue());
    return result;
  }

  private List<String> getAccessibleProperties(XynaOrderServerExtension xo) throws InsufficientRights {
    if (xo == null || xo.getCreationRole() == null) {
      throw new InsufficientRights(Arrays.asList(new UserAuthenticationRight(ScopedRight.XYNA_PROPERTY.getKey())));
    }

    List<String> propertyNames = new ArrayList<>();
    final String READ_RIGHT = ScopedRight.XYNA_PROPERTY.getKey() + ":" + Action.read.toString();
    final String ALL_RIGHT = ScopedRight.XYNA_PROPERTY.getKey() + ":*";

    Role role = xo.getCreationRole();
    for (String rightName : role.getScopedRights()) {
      if (rightName.startsWith(READ_RIGHT) && rightName.length() > READ_RIGHT.length()+1) {
        propertyNames.add(rightName.substring(READ_RIGHT.length()+1));
      } else if (rightName.startsWith(ALL_RIGHT) && rightName.length() > ALL_RIGHT.length()+1) {
        propertyNames.add(rightName.substring(ALL_RIGHT.length()+1));
      }
    }

    if (propertyNames.isEmpty()) {
      throw new InsufficientRights(Arrays.asList(new UserAuthenticationRight(ScopedRight.XYNA_PROPERTY.getKey())));
    }

    return propertyNames;
  }

  @Override
  public List<? extends XynaProperty> getListEntries(XynaOrderServerExtension xo, TableInfo tableInfo, xmcp.DocumentationLanguage language) throws InsufficientRights {
    List<String> accessibleProperties = getAccessibleProperties(xo);

    xmcp.DocumentationLanguage lang = language;
    if (lang == null)
      lang = new xmcp.DocumentationLanguage();

    if (lang.getLanguageTag() == null || lang.getLanguageTag().length() == 0) {
      lang.setLanguageTag("en-US");  //default
    }

    final xmcp.DocumentationLanguage documentationLanguage = lang;

    // add filtering for right xfmg.xfctrl.XynaProperties:[read, write, insert, delete, *]:/.*/
    List<Function<TableInfo, List<Filter>>> rightsFilters = new ArrayList<>();
    for (String accessibleProperty : accessibleProperties) {
      Function<TableInfo, List<Filter>> rightFilter = ti -> 
      ti.getColumns().stream()
      .filter(tableColumn -> PATH_KEY.equals(tableColumn.getPath()))
      .map(tc -> new TableHelper.Filter(tc.getPath(), accessibleProperty, true))
      .collect(Collectors.toList());
      rightsFilters.add(rightFilter);
    }

    TableHelper<XynaProperty, TableInfo> tableHelper = TableHelper.<XynaProperty, TableInfo>init(tableInfo)
        .limitConfig(TableInfo::getLimit)
        .sortConfig(ti -> {
          for (TableColumn tc : ti.getColumns()) {
            TableHelper.Sort sort = TableHelper.createSortIfValid(tc.getPath(), tc.getSort());
            if(sort != null)
              return sort;
          }
          return null;
        })
        .filterConfig(ti -> 
          ti.getColumns().stream()
          .filter(tableColumn -> 
            !tableColumn.getDisableFilter() && tableColumn.getPath() != null && tableColumn.getFilter() != null && tableColumn.getFilter().length() > 0
          )
          .map(tc -> new TableHelper.Filter(tc.getPath(), tc.getFilter(), tableInfo.getFilterCaseSensitive()))
          .collect(Collectors.toList())
        )
        .secondaryFilterConfig(rightsFilters, LogicalOperand.OR)
        .addSelectFunction(PATH_KEY, XynaProperty::getKey)
        .addSelectFunction(PATH_VALUE, XynaProperty::getValue)
        .addSelectFunction(PATH_DEFAULT_VALUE, XynaProperty::getDefaultValue)
        .addSelectFunction(PATH_DOCUMENTATION, XynaProperty::getGUIDocumentation);

    Collection<XynaPropertyWithDefaultValue> data = multiChannelPortal.getPropertiesWithDefaultValuesReadOnly();
    List<XynaProperty> result = data.stream()
        .map(property -> {
          XynaProperty entry = new XynaProperty();
          entry.setKey(property.getName());
          entry.setValue(property.getValueOrDefValue());
          entry.setDefaultValue(property.getDefValue());
          entry.setOverwrittenDefaultValue(property.getValue() != null && property.getDefValue() != null);
          entry.setGUIDocumentation(getDocuWithLangauge(property.getDocumentation(), property.getDefDocumentation(), documentationLanguage));
          entry.setDocumentation(getDocuAsArray(property.getDocumentation(), property.getDefDocumentation()));
          return entry;
        })
        .filter(tableHelper.filter())
        .collect(Collectors.toList());
    tableHelper.sort(result);
    return tableHelper.limit(result);
  }
 
  private String getDocuAsString(Map<DocumentationLanguage, String> docuMap, Map<DocumentationLanguage, String> defDocuMap) {
    String result = null;
    if(docuMap != null && docuMap.size() > 0) {
      result = String.join("\n", docuMap.values());
      return result;
    }
    
    if(defDocuMap == null || defDocuMap.size() == 0)
      return "";
    
    result = String.join("\n", defDocuMap.values());
    return result;
  }

  private String getDocuWithLangauge(Map<DocumentationLanguage, String> docuMap, Map<DocumentationLanguage, String> defDocuMap, xmcp.DocumentationLanguage language) {
  
    String result;
    result = getDocuWithLanguageFromMap(docuMap, language);
    if(result == null)
      result = getDocuWithLanguageFromMap(defDocuMap, language);
    if(result == null)
      return "";
  
    return result;
  }
  
  private String getDocuWithLanguageFromMap(Map<DocumentationLanguage, String> docuMap, xmcp.DocumentationLanguage language) {
    DocumentationLanguage lang =  DocumentationLanguage.valueOf(language.convert());
    if(lang == null) {
      return null;
    }
  
    if(docuMap != null && docuMap.size() > 0) {
      if(docuMap.containsKey(lang)){
        return docuMap.get(lang);
      }
      else {
        return getDocuAsString(docuMap, null);
      }
    }
  
    return null;
  }
  
  private List<Documentation> getDocuAsArray(Map<DocumentationLanguage, String> docuMap, Map<DocumentationLanguage, String> defDocuMap) {
    List<Documentation> result = new ArrayList<>();
    Set<Map.Entry<DocumentationLanguage, String>> entries = null;
    
    if(docuMap != null && docuMap.size() > 0)
      entries = docuMap.entrySet();
    else if(defDocuMap != null && defDocuMap.size() > 0)
      entries = defDocuMap.entrySet();
    if(entries == null)
      entries = new HashSet<>();
    
    for(Map.Entry<DocumentationLanguage, String> entry: entries) {
        Documentation docu = new Documentation();
        xmcp.DocumentationLanguage docuLang = xmcp.DocumentationLanguage.convertToGuiDocumentationLanguage(entry.getKey().name());
        docu.setLanguage(docuLang);
        docu.setDocumentation(entry.getValue());
        result.add(docu);
      }
    
    return result;
  }
  
  @Override
  public void changeProperty(XynaProperty newProperty) throws PropertyUpdateException {
    try {
        multiChannelPortal.setProperty(newProperty.getKey(), newProperty.getValue());
        if(newProperty.getValue() != null) {
          for(xmcp.Documentation docu : newProperty.getDocumentation()) {
            configuration.addPropertyDocumentation(
              newProperty.getKey(), 
              DocumentationLanguage.valueOf(docu.getLanguage().convert()),
              docu.getDocumentation() != null ? docu.getDocumentation() : "");
          }
        }
    } catch (Exception e1) {
      throw new PropertyUpdateException(e1.toString(), e1);
    }
  }
  @Override
  public void removeProperty(XynaPropertyKey arg0) throws PropertyDeleteException {
    String testExists = multiChannelPortal.getProperty(arg0.getKey());
    if(testExists == null)
      throw new PropertyDeleteException("Property doesn't exist.");
    try {
      multiChannelPortal.removeProperty(arg0.getKey());
    } catch (PersistenceLayerException e) {
      throw new PropertyDeleteException(e.toString(), e);
    }
  }
}
