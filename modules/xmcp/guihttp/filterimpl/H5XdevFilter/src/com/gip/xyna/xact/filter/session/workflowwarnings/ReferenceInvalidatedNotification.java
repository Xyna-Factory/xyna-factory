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
package com.gip.xyna.xact.filter.session.workflowwarnings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.session.gb.references.ReferenceType;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xmcp.XynaMultiChannelPortal;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;
import com.gip.xyna.xprc.xfractwfe.generation.WF;
import com.gip.xyna.xprc.xfractwfe.generation.WorkflowCall;

import xmcp.processmodeller.datatypes.Warning;


// called after changes have taken place - compare with current state of workflow

public class ReferenceInvalidatedNotification implements WarningsChangeNotification {
 
  private FQName wfFqName;
  private WF wf;
  private static final Logger logger = CentralFactoryLogging.getLogger(ReferenceInvalidatedNotification.class);
  
  public ReferenceInvalidatedNotification(FQName wfFqName, WF wf) {
    this.wfFqName = wfFqName;
    this.wf = wf;
  }
  
  @Override
  public void handle(ObjectId objectId, WorkflowWarningsHandler handler) {
    
    //update warnings in handler
    handler.deleteAllWarningsOfType(ObjectType.reference);
    
    String wfFqn = wfFqName.getFqName();
    Long revision = wfFqName.getRevision();
    XMOMDatabaseSearchResult response = null;
    try {
      //figure out if we are referenced and where
      XynaMultiChannelPortal multiChannelPortal = (XynaMultiChannelPortal) XynaFactory.getInstance().getXynaMultiChannelPortal();
      HashMap<String, String> filters = new HashMap<>();
      filters.put("fqname", wfFqn);
      SearchRequestBean srb = Utils.createReferencesSearchRequestBean(ReferenceType.instanceServiceReferenceOf.getSelection(), filters);
      XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);
      select.addAllDesiredResultTypes(Arrays.asList(XMOMDatabaseType.DATATYPE));
      response = multiChannelPortal.searchXMOMDatabase(Arrays.asList(select), -1, revision);
    } catch (Exception e) {
      if(logger.isDebugEnabled()) {
        logger.debug("Exception determining reference warnings.", e);
      }
      return;
    }
    //check if that reference is valid
    for(XMOMDatabaseSearchResultEntry entry : response.getResult()) {
      String fqn = entry.getFqName();
      List<String> invalidReferences = checkInvalidReferences(wfFqn, fqn, revision);
      for(String op : invalidReferences) {
        ObjectId warningId = ObjectId.createWarningId(WorkflowWarningsHandler.warningIdx++);
        ObjectId warningObjectId = ObjectId.createReferenceId(fqn + ":" + op);
        handler.addWarning(new Warning(warningObjectId.getObjectId(), warningId.getObjectId(), WorkflowWarningMessageCode.REFERENCE_INVALID));
      }
    }
  }

  private List<String> checkInvalidReferences(String wfFqn, String dtFqn, Long revision) {
    List<String> result = new ArrayList<String>();
    DOM dom = null;
    
    try {
      dom = DOM.getInstance(dtFqn, revision);
      dom.parse(true);
    } catch (Exception e) {
      if(logger.isDebugEnabled()) {
        logger.debug("Exception determining reference warnings.", e);
      }
      return result;
    }
    
    for(Operation op : dom.getOperations()) {
      if(op instanceof WorkflowCall) {
        WF configuredWf = ((WorkflowCall)op).getWf();
        if(configuredWf.getOriginalFqName().equals(wfFqn) && configuredWf.getRevision().equals(revision)) {
          if(!Utils.isValidWorkflowReference(dom, op, this.wf)) {
            result.add(op.getName());
          }
        }
      }
    }
    
    return result;
  }

}
