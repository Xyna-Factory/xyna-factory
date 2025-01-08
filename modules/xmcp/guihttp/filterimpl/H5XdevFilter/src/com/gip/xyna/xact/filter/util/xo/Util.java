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
package com.gip.xyna.xact.filter.util.xo;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.filter.util.Utils;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DatatypeVariable;
import com.gip.xyna.xprc.xfractwfe.generation.DomOrExceptionGenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xprcods.workflowdb.WorkflowDatabase;

public class Util {

  public static final String EXCEPTION_BASE_TYPE = "core.exception.XynaException";
  public static final String EXCEPTION_BASE_TYPE_GUI = "core.exception.XynaExceptionBase";

  private static final Logger logger = CentralFactoryLogging.getLogger(Util.class);

  private Util() {}


  public static void distributeMetaInfo(GenericResult gr, Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, UnexpectedJSONContentException {
    RuntimeContext rc = null;
    if (revision != null) {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    }
    distributeMetaInfoRecursivly(gr, rc);
  }
  
  
  private static void distributeMetaInfoRecursivly(GenericResult gr, RuntimeContext parentRc) throws UnexpectedJSONContentException {
    if(gr == null)
      return;
    
    RuntimeContext nextRC = parentRc;
    if (shouldContainMeta(gr)) {
      GenericResult meta = gr.getObject(XynaObjectVisitor.META_TAG);
      if (meta == null) {
        meta = new GenericResult(new HashMap<String, Pair<String, Type>>(),
                                 new HashMap<String, Pair<List<String>, Type>>(),
                                 new HashMap<String, GenericResult>(),
                                 new HashMap<String, List<GenericResult>>(), new HashSet<String>());
        gr.getObjects().put(XynaObjectVisitor.META_TAG, meta);
      }
      GenericResult rc = meta.getObjects().get(MetaInfo.RUNTIME_CONTEXT);
      if (rc == null) {
        if (parentRc == null) {
          throw new RuntimeException("Not both null!");
        }
        rc = new GenericResult(new HashMap<String, Pair<String, Type>>(),
                               new HashMap<String, Pair<List<String>, Type>>(),
                               new HashMap<String, GenericResult>(),
                               new HashMap<String, List<GenericResult>>(), new HashSet<String>());
        RuntimeContext relevantRuntimeContext = deriveRelevantRuntimeContext(meta, parentRc);
        if (relevantRuntimeContext instanceof Workspace) {
          rc.getAttributes().put(RuntimeContextVisitor.WORKSPACE_LABEL, Pair.of(relevantRuntimeContext.getName(), Type.String));
        } else {
          rc.getAttributes().put(RuntimeContextVisitor.APPLICATION_LABEL, Pair.of(relevantRuntimeContext.getName(), Type.String));
          rc.getAttributes().put(RuntimeContextVisitor.VERSION_LABEL, Pair.of(((Application)relevantRuntimeContext).getAdditionalIdentifier(), Type.String));
        }
        meta.getObjects().put(MetaInfo.RUNTIME_CONTEXT, rc);
      }
      nextRC = rc.visit(new RuntimeContextVisitor());
      // TODO jetzt noch fqName ableiten
    }
    
    for (Entry<String, GenericResult> entry : gr.getObjects().entrySet()) {
      if (entry.getKey().equals(XynaObjectVisitor.META_TAG)) {
        continue;
      }
      distributeMetaInfoRecursivly(entry.getValue(), nextRC);
    }
    for (Entry<String, List<GenericResult>> entry : gr.getObjectLists().entrySet()) {
      if (entry.getKey().equals(XynaObjectVisitor.META_TAG)) {
        continue;
      }
      for (GenericResult listElement : entry.getValue()) {
        distributeMetaInfoRecursivly(listElement, nextRC);
      }
    }
  }


  private static RuntimeContext deriveRelevantRuntimeContext(GenericResult meta, RuntimeContext parentRc) {
    Pair<String, Type> fqPair = meta.getAttribute(MetaInfo.FULL_QUALIFIED_NAME);
    if (fqPair == null) {
      //throw new RuntimeException("No fqName");
      return parentRc;
    }
    try {
      Long parentRev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(parentRc);
      long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement()
                      .getRevisionDefiningXMOMObjectOrParent(fqPair.getFirst(), parentRev);
      return XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(rev);
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      return parentRc;
    }
  }


  private static boolean shouldContainMeta(GenericResult gr) {
    if(gr == null)
      return false;
    
    return gr.getAttributes().size() > 0 ||
           gr.getLists().size() > 0 ||
           gr.getObjects().size() > 0;
  }


  public static void writeRuntimeContext(JsonBuilder jb, RuntimeContext rtc) {
    jb.addObjectAttribute(MetaInfo.RUNTIME_CONTEXT); {
      if (rtc instanceof Application) {
        jb.addStringAttribute(RuntimeContextVisitor.APPLICATION_LABEL, rtc.getName());
        jb.addStringAttribute(RuntimeContextVisitor.VERSION_LABEL, ((Application) rtc).getVersionName());
      } else {
        jb.addStringAttribute(RuntimeContextVisitor.WORKSPACE_LABEL, rtc.getName());
      }
    } jb.endObject();
  }
  
  public static void writeMetaData(JsonBuilder jb, String fqn, boolean includeRtc) {
    jb.addObjectAttribute(Tags.META); {
      jb.addStringAttribute(Tags.FQN, fqn);
      
      if (includeRtc) {
        jb.addObjectAttribute(Tags.RTC); {
          getRtcXmomContainers().toJson(jb);
        } jb.endObject();
      }
    } jb.endObject();
  }


  private static RuntimeContextJson getRtcXmomContainers() {
    return new RuntimeContextJson(Utils.getGuiHttpRtc());
  }


  public static Set<GenerationBase> getSubTypes(String fqn, GenerationBaseCache commonCache, Long rootRev) throws XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException {
    if (!fqn.equals(DatatypeVariable.ANY_TYPE)) {
      DomOrExceptionGenerationBase gb = (DomOrExceptionGenerationBase)GenerationBase.getOrCreateInstance(fqn, commonCache, rootRev);
      return gb.getSubTypes(commonCache);
    }

    // get all data types and exception types
    WorkflowDatabase workflowDatabase = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getWorkflowDatabase();

    // filter out the ones that are visible from the given RTC

    Set<GenerationBase> subTypes = new HashSet<>();
    Set<Long> visibleRevs = new HashSet<>();
    visibleRevs.add(rootRev);
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getDependenciesRecursivly(rootRev, visibleRevs);

    HashMap<Long, List<String>> dtsByRev = workflowDatabase.getDeployedDatatypes();
    for (long rev : dtsByRev.keySet()) {
      if (visibleRevs.contains(rev)) {
        for (String visibleFqn : dtsByRev.get(rev)) {
          if (isExcludedType(visibleFqn) ) {
            continue;
          }

          try {
            DOM dom = DOM.getOrCreateInstance(visibleFqn, commonCache, rev);
            dom.parseGeneration(true /*deployed*/, false, false);
            subTypes.add(dom);
          } catch (Exception e) {
            logger.warn("Could not parse type " + visibleFqn, e);
          }
        }
      }
    }

    HashMap<Long, List<String>> exceptionsByRev = workflowDatabase.getDeployedExceptions();
    for (long rev : exceptionsByRev.keySet()) {
      if (visibleRevs.contains(rev)) {
        for (String visibleFqn : exceptionsByRev.get(rev)) {
          if (isExcludedType(visibleFqn) ) {
            continue;
          }

          try {
            ExceptionGeneration exceptionGeneration = ExceptionGeneration.getOrCreateInstance(visibleFqn, commonCache, rev);
            exceptionGeneration.parseGeneration(true /*deployed*/, false, false);
            subTypes.add(exceptionGeneration);
          } catch (Exception e) {
            logger.warn("Could not parse type " + visibleFqn, e);
          }
        }
      }
    }

    return subTypes;
  }


  public static boolean isSubtypeOf(String fqnSubClass, String fqnSuperClass, Long rootRev) throws XPRC_InvalidPackageNameException, Ex_FileAccessException, XPRC_XmlParsingException {
    Set<GenerationBase> subTypes = getSubTypes(fqnSuperClass, new GenerationBaseCache(), rootRev);
    for (GenerationBase subType : subTypes) {
      if (subType.getOriginalFqName().equals(fqnSubClass)) {
        return true;
      }
    }

    return false;
  }


  public static boolean isExcludedType(String fqn) {
    return GenerationBase.CORE_EXCEPTION.equals(fqn) ||
        EXCEPTION_BASE_TYPE.equals(fqn) ||
        EXCEPTION_BASE_TYPE_GUI.equals(fqn);
  }

}
