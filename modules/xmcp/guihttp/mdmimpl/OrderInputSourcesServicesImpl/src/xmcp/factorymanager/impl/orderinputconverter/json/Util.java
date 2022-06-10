/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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
package xmcp.factorymanager.impl.orderinputconverter.json;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor.Type;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;

public class Util {

  public static void distributeMetaInfo(GenericResult gr, Long revision) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, UnexpectedJSONContentException {
    RuntimeContext rc = null;
    if (revision != null) {
      rc = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    }
    distributeMetaInfoRecursivly(gr, rc);
  }
  
  
  private static void distributeMetaInfoRecursivly(GenericResult gr, RuntimeContext parentRc) throws UnexpectedJSONContentException {
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
  
}
