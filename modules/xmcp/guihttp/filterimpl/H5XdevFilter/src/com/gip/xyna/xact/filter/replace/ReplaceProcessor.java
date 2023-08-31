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
package com.gip.xyna.xact.filter.replace;



import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xact.filter.session.gb.references.ReferenceType;
import com.gip.xyna.xact.filter.session.save.Persistence;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;



public class ReplaceProcessor {


  public List<ReplaceResult> replace(String fromFqn, String toFqn, Long revision, RuntimeContext runtimeContext) throws Exception {
    List<String> objects = findObjectsToReplace(fromFqn, revision, runtimeContext);
    List<ReplaceResult> result = new ArrayList<ReplaceResult>();
    DOM newDom = DOM.getInstance(toFqn, revision);
    for (String objectFqn : objects) {
      try {
        replaceInObject(objectFqn, newDom, fromFqn, toFqn, revision);
        result.add(new ReplaceResult(objectFqn, true));
      } catch (Exception e) {
        result.add(new ReplaceResult(objectFqn, false));
      }
    }

    return result;
  }


  private List<String> findObjectsToReplace(String fqn, Long revision, RuntimeContext rtc) throws Exception {
    HashMap<String, String> filters = new HashMap<>();
    filters.put("fqname", fqn);
    ReferenceType referenceType = ReferenceType.usedInImplOf;

    XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
    SearchRequestBean srb = Utils.createReferencesSearchRequestBean(referenceType.getSelection(), filters);
    XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
    select.addAllDesiredResultTypes(Arrays.asList(XMOMDatabaseType.DATATYPE, XMOMDatabaseType.EXCEPTION));

    XMOMDatabaseSearchResult searchResult = multiChannelPortal.searchXMOMDatabase(Arrays.asList(select), -1, revision);
    List<XMOMDatabaseSearchResultEntry> results = searchResult.getResult();
    return results.stream().filter(x -> rtc.equals(x.getRuntimeContext())).map(x -> x.getFqName()).collect(Collectors.toList());
  }


  private void replaceInObject(String objectFqn, DOM newDom, String fromFqn, String toFqn, Long revision) throws Exception {
    FQName fqName = new FQName(revision, objectFqn);
    GenerationBase obj = XMOMLoader.loadNewGB(fqName);
    if (obj instanceof DomOrExceptionGenerationBase) {
      DomOrExceptionGenerationBase doe = (DomOrExceptionGenerationBase) obj;
      replaceUsage(doe, newDom, fromFqn, toFqn, revision);
      if (doe instanceof DOM) {
        replaceInServices((DOM) doe, newDom, fromFqn, toFqn, revision);
      }
      String path = doe.getOriginalPath();
      String name = doe.getOriginalSimpleName();
      String label = doe.getLabel();
      XmomType type = new XmomType(path, name, label);
      String xml = obj instanceof DOM ? Persistence.createDatatypeXML((DOM) doe, type) : Persistence
          .createExceptionTypeXML((ExceptionGeneration) doe, type);
      XynaFactory.getInstance().getXynaMultiChannelPortal().saveMDM(xml, revision);
    }
  }


  private void replaceInServices(DOM dom, DOM newDom, String fromFqn, String toFqn, Long revision) throws Exception {
    for (List<Operation> services : dom.getServiceNameToOperationMap().values()) {
      for (Operation service : services) {
        replaceInAVarList(service.getInputVars(), fromFqn, newDom);
        replaceInAVarList(service.getOutputVars(), fromFqn, newDom);
      }
    }
  }


  private void replaceInAVarList(List<AVariable> list, String fromFqn, DOM newDom) {
    for (AVariable member : list) {
      if (member.getFQClassName().equals(fromFqn)) {
        member.replaceDOM(newDom, member.getLabel());
      }
    }
  }


  private void replaceUsage(DomOrExceptionGenerationBase dom, DOM newDom, String fromFqn, String toFqn, Long revision) throws Exception {
    replaceInAVarList(dom.getMemberVars(), fromFqn, newDom);
  }


  public static class ReplaceResult {

    private final String objectFqn;
    private final boolean success;


    public ReplaceResult(String objectFqn, boolean success) {
      this.objectFqn = objectFqn;
      this.success = success;
    }


    public String getObjectFqn() {
      return objectFqn;
    }


    public boolean isSuccess() {
      return success;
    }
  }
}
