/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2025 Xyna GmbH, Germany
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
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser;
import com.gip.xyna.utils.misc.JsonParser.InvalidJSONException;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.HTMLBuilder.HTMLPart;
import com.gip.xyna.xact.filter.JsonFilterActionInstance;
import com.gip.xyna.xact.filter.RuntimeContextDependendAction;
import com.gip.xyna.xact.filter.URLPath;
import com.gip.xyna.xact.filter.actions.DomOrExceptionStructureAction.SubtypesStructureRequest;
import com.gip.xyna.xact.filter.actions.DomOrExceptionStructureAction.SubtypesStructureRequestParser;
import com.gip.xyna.xact.filter.actions.DomOrExceptionStructureAction.XMOMObjectIdentifier;
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.util.xo.MetaInfo;
import com.gip.xyna.xact.filter.util.xo.Util;
import com.gip.xyna.xact.filter.util.xo.XynaObjectVisitor;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.Rights;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;



public class DomOrExceptionSubtypesAction extends RuntimeContextDependendAction {

  private static final Logger logger = CentralFactoryLogging.getLogger(DomOrExceptionSubtypesAction.class);

  private static RuntimeContextDependencyManagement rcdm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
  private static RevisionManagement rm = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement();

  public void appendIndexPage(HTMLPart arg0) {
  }


  public String getTitle() {
    return "Test";
  }


  public boolean hasIndexPageChanged() {
    return false;
  }


  @Override
  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return url.getPath().equals("/subtypes") && Method.POST == method;
  }


  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();

    if (!checkLoginAndRights(tc, jfai, Rights.READ_MDM.toString())) {
      return jfai;
    }

    Long rev = rm.getRevision(rc);

    JsonParser jp = new JsonParser();

    SubtypesStructureRequest gr;
    try {
      gr = jp.parse(tc.getPayload(), new SubtypesStructureRequestParser());
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      AuthUtils.replyError(tc, jfai, e);
      return jfai;
    }

    GenerationBaseCache commonCache = new GenerationBaseCache();
    JsonBuilder jb = new JsonBuilder();
    jb.startObject(); {
      for (XMOMObjectIdentifier id : gr.objects) {
        jb.addListAttribute(id.fqn); {
          appendVariance(jb, id.fqn, commonCache, rev);
        } jb.endList();
      }
    } jb.endObject();

    jfai.sendJson(tc, jb.toString());
    return jfai;
  }


  public static void appendVariance(JsonBuilder jb, String fqn, GenerationBaseCache commonCache, Long rootRev) {
    try {
      DomOrExceptionGenerationBase gb = fqn.equals(DatatypeVariable.ANY_TYPE) ?
          DOM.getOrCreateInstance(fqn, commonCache, rootRev) :
          (DomOrExceptionGenerationBase)GenerationBase.getOrCreateInstance(fqn, commonCache, rootRev);
      gb.parseGeneration(true /*deployed*/, false, false);

      List<GenerationBase> subTypes = new ArrayList<>(Utils.getSubTypes(fqn, commonCache, rootRev));
      subTypes.add(gb); // the supertype should also be returned and sorted
      subTypes.sort((gb1,gb2) -> gb1.getOriginalFqName().compareToIgnoreCase(gb2.getOriginalFqName())); //Subtypen sortieren

      for (GenerationBase subType : subTypes) {
        if (!isReachable(subType, rootRev)) {
          // don't return subtypes that are not accessible in this revision
          continue;
        }
        write(jb, subType, rootRev);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private static void write(JsonBuilder jb, GenerationBase subType, Long rootRev) {
    jb.startObject(); {
      jb.addStringAttribute(MetaInfo.FULL_QUALIFIED_NAME, subType.getOriginalFqName());
      if (!rootRev.equals(subType.getRevision())) {
        try {
          Util.writeRuntimeContext(jb, rm.getRuntimeContext(subType.getRevision()));
        } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
          logger.warn("Could not evaluate Runtime Context for revision " + subType.getRevision());
        }
      }
      jb.addStringAttribute(XynaObjectVisitor.LABEL_TAG, subType.getLabel());
      if (subType instanceof DomOrExceptionGenerationBase) {
        jb.addBooleanAttribute(XynaObjectVisitor.IS_ABSTRACT_TAG, ((DomOrExceptionGenerationBase) subType).isAbstract());
        jb.addStringAttribute(XynaObjectVisitor.DOCU_TAG, ((DomOrExceptionGenerationBase) subType).getDocumentation());
      }
    }
    jb.endObject();
  }


  private static boolean isReachable(GenerationBase subType, Long rev) {
    if (subType.getRevision().equals(rev)) {
      return true;
    }
    return rcdm.isDependency(rev, subType.getRevision());
  }

}
