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
package com.gip.xyna.xact.filter.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xact.filter.xmom.datatypes.json.Utils;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xact.trigger.SocketNotAvailableException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;


/**
 *
 */
public class ObjectsAction extends RuntimeContextDependendAction {

  private final static String BASE_PATH = "/" + PathElements.XMOM + "/" + PathElements.OBJECTS;
  private final static String SEARCH_VALUE_PARAMETER = "q";
  private final static String DB_WILDCARD = "%";
  private final static String GUI_WILDCARD = "*";
  private final static int DEFAULT_MAX_RESULTS = 100;
  private final static String JAVA_WILDCARD = ".*";
  private final static String ANYTYPEFQN = (GenerationBase.ANYTYPE_REFERENCE_PATH + "." + GenerationBase.ANYTYPE_REFERENCE_NAME).toLowerCase();
  private final XMOMLoader xmomLoader;

  private static final Logger logger = CentralFactoryLogging.getLogger(ObjectsAction.class);


  public ObjectsAction(XMOMLoader xmomLoader) {
    super();
    this.xmomLoader = xmomLoader;
  }
  
  @Override
  protected boolean matchRuntimeContextIndependent( URLPath url, Method method) {
    return url.getPath().startsWith(BASE_PATH)
        && url.getPathLength() == 2
        && method == Method.POST;
  }

  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    String[] rights = {GuiRight.PROCESS_MODELLER.getKey(), Rights.EDIT_MDM.toString()};

    if (!checkLoginAndRights(tc, jfai, rights)) {
      return jfai;
    }

    return search(rc, revision, url, tc);
  }

  @Override
  public String getTitle() {
    return "XMOM-Objects-TypePath";
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
    HTMLPart paragraphDef = body.paragraph();
    paragraphDef.link(BASE_PATH + "/xact.http", "xact.http");
  }

  @Override
  public boolean hasIndexPageChanged() {
    return false;
  }
  
  private FilterActionInstance search(RuntimeContext rc, Long revision, URLPath url, HTTPTriggerConnection tc) throws SocketNotAvailableException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    try {
      Properties parameter = tc.getParas();
      XMOMDatabaseSearchResult result = select(parameter, tc.getPayload(), revision);
      
      List<ObjectIdentifierJson> objects = new ArrayList<>();
      int operationCount = fillObjects(result, objects, rc);
      
      if(shouldAddAnyType(parameter, tc.getPayload())) {
        operationCount++;
        
        ObjectIdentifierJson anyTypeJson = Utils.createAnyTypeIdentifier(revision);
        objects.add(anyTypeJson);
      }
      
      //Collections.sort(objects, ObjectIdentifierJson.sortByLabel());
      String searchValue = "";
      if (parameter.containsKey(SEARCH_VALUE_PARAMETER)) searchValue = parameter.getProperty(SEARCH_VALUE_PARAMETER).toLowerCase();
      Collections.sort(objects, ObjectIdentifierJson.sortByFQN(searchValue));

      jfai.sendJson(tc, writeJson(result, objects, operationCount) );
    } catch (Exception e) {
      jfai.sendError(tc, e);
    }
    return jfai;
  }
  

  private XMOMDatabaseSearchResult select(Properties parameter, String searchRequestMeta, Long revision) throws InvalidJSONException, UnexpectedJSONContentException, XNWH_InvalidSelectStatementException, PersistenceLayerException {
    String searchValue = createSearchValue(parameter.getProperty(SEARCH_VALUE_PARAMETER, null), DB_WILDCARD);
    JsonParser jp = new JsonParser();
    SearchMetaData smd = jp.parse(searchRequestMeta, new SearchMetaData());
    int maxResults = DEFAULT_MAX_RESULTS;
    if (smd != null &&
        smd.maxCount > 0) {
      maxResults = smd.maxCount;
    }
    
    List<XMOMDatabaseSelect> selects = new ArrayList<>();
    
    XMOMDatabaseSelect fqXmomSelect1 = ObjectIdentifierJson.fillSelects( new XMOMDatabaseSelect() );
    fqXmomSelect1.where(XMOMDatabaseEntryColumn.FQNAMELOWERCASE).isLike(searchValue);
    selects.add( addDesiredResultTypes(fqXmomSelect1, smd) );
    
    XMOMDatabaseSelect fqXmomSelect2 = ObjectIdentifierJson.fillSelects( new XMOMDatabaseSelect() );
    fqXmomSelect2.where(XMOMDatabaseEntryColumn.LABEL).isLike(searchValue);
    selects.add( addDesiredResultTypes(fqXmomSelect2, smd) );
    
     
    XMOMDatabase xmomDB = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
    
    return xmomDB.searchXMOMDatabase(selects, maxResults, revision);
  }
  
  private void addResultTypeIfSelected(XMOMDatabaseSelect xmomDatabaseSelect, XMOMDatabaseType type, boolean selected) {
    if(selected) xmomDatabaseSelect.addDesiredResultTypes(type);
  }

  private XMOMDatabaseSelect addDesiredResultTypes(XMOMDatabaseSelect xmomDatabaseSelect, SearchMetaData smd) {
    addResultTypeIfSelected(xmomDatabaseSelect, XMOMDatabaseType.DATATYPE, smd.dataType);
    addResultTypeIfSelected(xmomDatabaseSelect, XMOMDatabaseType.EXCEPTION, smd.exceptionType);
    addResultTypeIfSelected(xmomDatabaseSelect, XMOMDatabaseType.WORKFLOW, smd.workflow);
    addResultTypeIfSelected(xmomDatabaseSelect, XMOMDatabaseType.OPERATION, smd.operation); // bisher: service
    addResultTypeIfSelected(xmomDatabaseSelect, XMOMDatabaseType.SERVICEGROUP, smd.serviceGroup);
    return xmomDatabaseSelect;
  }
  
  
  
  private boolean shouldAddAnyType(Properties parameter, String searchRequestMeta) throws InvalidJSONException, UnexpectedJSONContentException {
    JsonParser jp = new JsonParser();
    SearchMetaData smd = jp.parse(searchRequestMeta, new SearchMetaData());

    //dataTypes not selected
    if (!smd.dataType) {
      return false;
    }

    String searchValue = createSearchValue(parameter.getProperty(SEARCH_VALUE_PARAMETER, null), JAVA_WILDCARD);
    searchValue = searchValue.replace(".", "\\.");
    searchValue = searchValue.replace("\\.*", ".*");
    searchValue = searchValue.toLowerCase();
    return ANYTYPEFQN.matches(searchValue);
  }
  
  
  /**
   * Pr�ft den angegebenen XMOMDatabaseSearchResultEntry entry auf Ambiguit�t bzgl. der �bergebenen Liste list.
   * Elemente mit Typ "codedService" oder "instanceService" (XMOMDatabaseType.OPERATION) sind gesondert zu behandeln.
   * @param entry XMOMDatabaseSearchResultEntry-Objekt, das auf Ambiguit�t gepr�ft wird
   * @param list Liste, die auf Duplicates von entry durchsucht wird
   * @param index Index von entry in list
   * @return true, wenn entry.getType() nicht XMOMDatabaseType.OPERATION ist und ein anderes Objekt other in list
   * (list.indexOf(entry) != list.indexOf(other)) mit gleichem FQN existiert, das keine operation ist, und false sonst
   */
  private boolean isAmbigue(XMOMDatabaseSearchResultEntry entry, List<XMOMDatabaseSearchResultEntry> list, int index) {
    XMOMDatabaseType type = entry.getType();
    //TODO: ggf. Behandlung von services/operations implementieren
    if(type.equals(XMOMDatabaseType.OPERATION)) return false;
    for(int i = 0; i < list.size(); i++) {
      if(i == index) continue;
      XMOMDatabaseSearchResultEntry other = list.get(i);
      if(!entry.getFqName().equals(other.getFqName())) continue;
      if((type.equals(XMOMDatabaseType.DATATYPE) || type.equals(XMOMDatabaseType.SERVICEGROUP))
              && other.getType().equals(XMOMDatabaseType.OPERATION))
        continue;
      //XMOMDatabaseType.OPERATION ist entweder codedService oder instanceService
      return true;
    }
    return false;
  }
  
  
  private int fillObjects(XMOMDatabaseSearchResult result, List<ObjectIdentifierJson> objects, RuntimeContext rc) throws XynaException {
    int operationCount = 0;
    List<XMOMDatabaseSearchResultEntry> resultEntries = result.getResult();

    for (int i = 0; i < resultEntries.size(); i++) {
      XMOMDatabaseSearchResultEntry resultEntry = resultEntries.get(i);
      ObjectIdentifierJson identifierJson;
      if(isAmbigue(resultEntry, resultEntries, i)) identifierJson = new ObjectIdentifierJson(rc, resultEntry, true);
      else identifierJson = new ObjectIdentifierJson(rc, resultEntry, false);
      
      if(Type.codedService == identifierJson.getType()) {
        String dataTypeFqn = identifierJson.getFQName().getTypePath() + "." + identifierJson.getFQName().getTypeName();
        try {
          GenerationBaseObject gbo = xmomLoader.loadNoCacheChange(new FQName(rc, dataTypeFqn));
          identifierJson.setDataTypeLabel(gbo.getDOM().getLabel());
          Operation operation = gbo.getDOM().getOperationByName(identifierJson.getFQName().getOperation());
          if(!operation.isStatic()) {
            identifierJson.setType(Type.instanceService);
          }
        } catch (Exception e) {
          com.gip.xyna.xact.filter.util.Utils.logError("Could not find " + dataTypeFqn, e);
          continue;
        }
      }
      if(Type.serviceGroup == identifierJson.getType()) {
        String dataTypeFqn = identifierJson.getFQName().getTypePath() + "." + identifierJson.getFQName().getTypeName();
        try {
          GenerationBaseObject gbo = xmomLoader.loadNoCacheChange(new FQName(rc, dataTypeFqn));
          identifierJson.setDataTypeLabel(gbo.getDOM().getLabel());

          List<Operation> operations = gbo.getDOM().getOperations();
          boolean hasStaticOperations = false;
          for (Operation operation : operations) {
            if(operation.isStatic()) {
              hasStaticOperations = true;
              break;
            }
          }
          if(!hasStaticOperations && !gbo.getDOM().isServiceGroupOnly()) {
            continue;
          }
        } catch (Exception e) {
          com.gip.xyna.xact.filter.util.Utils.logError("Could not find " + dataTypeFqn, e);
          continue;
        }
      }

      if (identifierJson.getType() == null) {
        if (resultEntry.getType() == XMOMDatabaseType.OPERATION) {
          operationCount++;
        }
        continue;
      }
      objects.add(identifierJson);
    }

    return operationCount;
  }

  private String writeJson(XMOMDatabaseSearchResult result, List<ObjectIdentifierJson> objects, int operationCount) {
    JsonBuilder jb = new JsonBuilder(); // TODO: Konstanten
    jb.startObject();{
      jb.addObjectAttribute("count");{
        jb.addIntegerAttribute("all", result.getCount() - operationCount);
        if (result.getCounts().get(XMOMDatabaseType.DATATYPE) != null) {
          jb.addIntegerAttribute("dataTypes", result.getCounts().get(XMOMDatabaseType.DATATYPE));
        }
        if (result.getCounts().get(XMOMDatabaseType.EXCEPTION) != null) {
          jb.addIntegerAttribute("exceptionTypes", result.getCounts().get(XMOMDatabaseType.EXCEPTION));
        }
        if (result.getCounts().get(XMOMDatabaseType.SERVICE) != null) {
          jb.addIntegerAttribute("workflows", result.getCounts().get(XMOMDatabaseType.SERVICE) - operationCount);
        }
        if (result.getCounts().get(XMOMDatabaseType.SERVICEGROUP) != null) {
          jb.addIntegerAttribute("codedServices", result.getCounts().get(XMOMDatabaseType.SERVICEGROUP));
        }
      } jb.endObject();
      jb.addIntegerAttribute("truncatedCount", objects.size());
      jb.addObjectListAttribute("objects", objects);
    }jb.endObject();
    return jb.toString();
  }

  private static class SearchMetaData extends EmptyJsonVisitor<SearchMetaData> {

    private int maxCount;
    
    private boolean workflow;
    private boolean dataType;
    private boolean exceptionType;
    private boolean serviceGroup;
    private boolean operation; // hei�t in GUI "Service"
    
    @Override
    public SearchMetaData get() {
      return this;
    }
    @Override
    public SearchMetaData getAndReset() {
      return this;
    }

    @Override
    public void attribute(String label, String value, Type type) {
      if( label.equals("maxCount") ) {
        maxCount = Integer.valueOf(value);
        return;
      }
      
      if(type.equals(Type.Boolean)) {
        if(label.equals("workflow")) {
          workflow = Boolean.valueOf(value);
          return;
        }
        if(label.equals("dataType")) {
          dataType = Boolean.valueOf(value);
          return;
        }
        if(label.equals("exceptionType")) {
          exceptionType = Boolean.valueOf(value);
          return;
        }
        if(label.equals("serviceGroup")) {
          serviceGroup = Boolean.valueOf(value);
          return;
        }
        if(label.equals("service")) {
          operation = Boolean.valueOf(value);
          return;
        }
      }
    }
  }


  private String createSearchValue(String searchValueIn, String targetWildcard) {
    String searchValueOut;
    if (searchValueIn != null) {
      searchValueOut = searchValueIn.toLowerCase();
      if (!searchValueIn.contains(GUI_WILDCARD)) {
        searchValueOut = targetWildcard + searchValueOut + targetWildcard;
      } else {
        searchValueOut = searchValueOut.replace(GUI_WILDCARD, targetWildcard);
      }
    } else {
      searchValueOut = targetWildcard;
    }
    return searchValueOut;
  }

}

