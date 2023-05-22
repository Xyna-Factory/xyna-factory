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

package com.gip.xyna.xact.filter.util.xo;



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
import com.gip.xyna.xact.filter.actions.auth.utils.AuthUtils;
import com.gip.xyna.xact.filter.json.RuntimeContextJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection;
import com.gip.xyna.xact.trigger.HTTPTriggerConnection.Method;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement.GuiRight;
import com.gip.xyna.xprc.xfractwfe.generation.AVariable;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBaseCache;
import com.gip.xyna.xprc.xfractwfe.generation.WF;



public class ServiceSignature extends RuntimeContextDependendAction {

  private static final String SIGNATURE = "signature";
  private static final String ANYTYPE_FQN = GenerationBase.ANYTYPE_REFERENCE_PATH + "." + GenerationBase.ANYTYPE_REFERENCE_NAME;


  @Override
  public String getTitle() {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public void appendIndexPage(HTMLPart body) {
  }

  @Override
  public boolean hasIndexPageChanged() {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  protected boolean matchRuntimeContextIndependent(URLPath url, Method method) {
    return url.getPath().equals("/" + SIGNATURE) && Method.POST == method;
  }

  @Override
  protected FilterActionInstance act(RuntimeContext rc, Long revision, URLPath url, Method method, HTTPTriggerConnection tc) throws XynaException {
    JsonFilterActionInstance jfai = new JsonFilterActionInstance();
    
    if (!checkLoginAndRights(tc, jfai, GuiRight.PROCESS_MODELLER.getKey())) {
      return jfai;
    }

    // parsing
    JsonParser jp = new JsonParser();
    ServiceSignatureRequest ssr;
    try {
      ssr = jp.parse(tc.getPayload(), new ServiceSignatureRequestParser());
    } catch (InvalidJSONException | UnexpectedJSONContentException e) {
      AuthUtils.replyError(tc, jfai, e);
      return jfai;
    }

    GenerationBaseCache commonCache = new GenerationBaseCache();
    GenerationBase gb = GenerationBase.getOrCreateInstance(ssr.fqn, commonCache, revision);
    gb.parseGeneration(false/*TODO: korrekt?*/, false, false);

    /*TODO: Service Groups unterstuetzen:
       - ja mit DOM.getOrCreateIinstance sollte das entsprechend gehen
       - also f�r service groups
       - die einzelnen services sind dann darin abzufragen
       - also das xml zu einer servicegroup entspricht einem DOM
       - und darin sind dann viele services enthalten*/

    // rendering
    JsonBuilder jb = new JsonBuilder();
    jb.startObject(); {
      jb.addListAttribute(Tags.SIGNATURE_INPUTS); {
        for (AVariable inputVar : ((WF)gb).getInputVars()) { // TODO: kann auch DOM sein, siehe oben
          append(jb, inputVar);
        }
      } jb.endList();

      jb.addListAttribute(Tags.SIGNATURE_OUTPUTS); {
        for (AVariable outputVar : ((WF)gb).getOutputVars()) { // TODO: kann auch DOM sein, siehe oben
          append(jb, outputVar);
        }
      } jb.endList();

      // TODO: Exceptions
    } jb.endObject();

    jfai.sendJson(tc, jb.toString());

    return jfai;
  }


  private void append(JsonBuilder jb, AVariable inputVar) {
    jb.startObject(); {
      String fqn = inputVar.getOriginalPath() + "." + inputVar.getOriginalName();
      jb.addStringAttribute(Tags.FQN, fqn);
      jb.addBooleanAttribute(Tags.IS_LIST, inputVar.isList());

      if (!ANYTYPE_FQN.equals(fqn)) {
        jb.addObjectAttribute(Tags.RTC); {
          RuntimeContext rtc = inputVar.getDomOrExceptionObject().getRuntimeContext();
          RuntimeContextJson rtcJson = new RuntimeContextJson(rtc);
          rtcJson.toJson(jb);
        } jb.endObject();
      }
    } jb.endObject();
  }


  public static class ServiceSignatureRequest {
    public String fqn;
  }


  public static class ServiceSignatureRequestParser extends EmptyJsonVisitor<ServiceSignatureRequest> {

    private static final String FQN = "fqn";

    private ServiceSignatureRequest req = new ServiceSignatureRequest();

    @Override
    public ServiceSignatureRequest get() {
      return req;
    }

    @Override
    public ServiceSignatureRequest getAndReset() {
      req = new ServiceSignatureRequest();
      return req;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if (label.equals(FQN)) {
        req.fqn = value;
      }
    }

  }

}
