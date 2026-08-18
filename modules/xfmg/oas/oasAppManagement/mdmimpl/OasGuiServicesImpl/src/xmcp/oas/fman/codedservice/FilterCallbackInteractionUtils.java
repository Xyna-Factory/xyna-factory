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
package xmcp.oas.fman.codedservice;

import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xact.trigger.RunnableForFilterAccess;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;

import base.KeyValue;
import xact.http.URLPath;
import xact.http.enums.httpmethods.GET;
import xact.http.enums.httpmethods.HTTPMethod;
import xact.http.enums.httpmethods.POST;
import xact.http.enums.httpmethods.PUT;
import xmcp.oas.fman.codedservice.parameter.CreateImplWfParameter;
import xmcp.oas.fman.codedservice.parameter.CreateImplWfParameter.SignatureVariable;

public class FilterCallbackInteractionUtils {
  
  
  @SuppressWarnings("unchecked")
  public static List<KeyValue> getReferenceCanddates(XynaOrderServerExtension order, String dtFqn, String opName, Long revision) 
  throws Exception {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceName = getWorkspaceNameEncoded(revision);
    String dtPath = dtFqn.substring(0, dtFqn.lastIndexOf("."));
    String dtName = dtFqn.substring(dtFqn.lastIndexOf(".")+1);
    
    //open Datatype
    URLPath url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s",  workspaceName, dtPath, dtName), null, null);
    HTTPMethod method = new GET();
    String payload;
    String msg = "could not process impl dt";
    GeneralXynaObject result = (GeneralXynaObject) executeRunnable(runnable, url, method, null, msg);
    
    String memberMethodId = findMemberMethodId(result, opName);
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/objects/%s/referenceCandidates",  workspaceName, dtPath, dtName, memberMethodId), null, null);
    method = new GET();
    result = (GeneralXynaObject) executeRunnable(runnable, url, method, null, "could not get reference candidates");
    
    //close
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/close", workspaceName, dtPath, dtName), null, null);
    method = new POST();
    payload = "{\"force\":false,\"revision\":2}";
    executeRunnable(runnable, url, method, payload, msg);
    
    if(result != null) {
      List<GeneralXynaObject> candidates = ((List<GeneralXynaObject>)result.get("candidates"));
      List<KeyValue> ret = new ArrayList<>();
      for(GeneralXynaObject candidate : candidates) {
        String fqn = (String)candidate.get("fqn");
        ret.add(new KeyValue.Builder().key(fqn).value(fqn).instance());
      }
      return ret;
    }
    
    return new ArrayList<KeyValue>();
  }

  public static void createImplDt(XynaOrderServerExtension order, String label, String path, String parentFqn, Long revision) 
  throws Exception {
    RunnableForFilterAccess runnable = order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceName = getWorkspaceNameEncoded(revision);
        
    //open
    URLPath url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes",  workspaceName), null, null);
    HTTPMethod method = new POST();
    String payload = "{\"label\":\"New Data Type\"}";
    String msg = "could not create impl dt";
    GeneralXynaObject result = (GeneralXynaObject) executeRunnable(runnable, url, method, payload, msg);
    String tmpFqn = (String)result.get("xmomItem.fqn");
    String tmpPath = tmpFqn.substring(0, tmpFqn.lastIndexOf("."));
    String tmpName = tmpFqn.substring(tmpFqn.lastIndexOf(".")+1);
    
    //set parent
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/objects/typeInfoArea/change", workspaceName, tmpPath, tmpName), null, null);
    method = new PUT();
    payload = String.format("{\"baseType\":\"%s\"}", parentFqn);
    executeRunnable(runnable, url, method, payload, msg);
    
    //save
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/save", workspaceName, tmpPath, tmpName), null, null);
    method = new POST();
    payload = String.format("{\"force\":false,\"revision\":2,\"path\":\"%s\",\"label\":\"%s\"}", path, label);
    result = (GeneralXynaObject) executeRunnable(runnable, url, method, payload, msg);
    //read actual FQN
    String actualFqn = (String)((GeneralXynaObject)((List<?>)result.get("updates")).get(0)).get("fqn");
    String actualPath = actualFqn.substring(0, actualFqn.lastIndexOf("."));
    String actualName = actualFqn.substring(actualFqn.lastIndexOf(".")+1);
    
    //deploy
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/deploy", workspaceName, actualPath, actualName), null, null);
    method = new POST();
    payload = "{\"revision\":2}";
    executeRunnable(runnable, url, method, payload, msg);
    
    //close
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/close", workspaceName, actualPath, actualName), null, null);
    method = new POST();
    payload = "{\"force\":false,\"revision\":2}";
    executeRunnable(runnable, url, method, payload, msg);
  }
  
  public static void createImplWf(CreateImplWfParameter parameter) throws Exception {
    RunnableForFilterAccess runnable = parameter.order.getRunnableForFilterAccess("H5XdevFilter");
    String workspaceName = getWorkspaceNameEncoded(parameter.revision);
    String dtFqn = parameter.parentFqn;
    String wfPath = parameter.wfPath;
    String wfLabel = parameter.wfLabel;
    
    //open
    URLPath url = new URLPath(String.format("/runtimeContext/%s/xmom/workflows",  workspaceName), null, null);
    HTTPMethod method = new POST();
    String msg = "could not create impl wf";
    String payload = createImplWfOpenPayload(parameter);
    GeneralXynaObject result = (GeneralXynaObject) executeRunnable(runnable, url, method, payload, msg);
    String tmpFqn = (String)result.get("xmomItem.fqn");
    String tmpPath = tmpFqn.substring(0, tmpFqn.lastIndexOf("."));
    String tmpName = tmpFqn.substring(tmpFqn.lastIndexOf(".")+1);
    
    //save
    url = new URLPath(String.format("/runtimeContext/%s/xmom/workflows/%s/%s/save", workspaceName, tmpPath, tmpName), null, null);
    method = new POST();
    payload = String.format("{\"force\":false,\"revision\":2,\"path\":\"%s\",\"label\":\"%s\"}", wfPath, wfLabel);
    result = (GeneralXynaObject) executeRunnable(runnable, url, method, payload, msg);
    //read actual FQN
    String actualFqn = (String)((GeneralXynaObject)((List<?>)result.get("updates")).get(0)).get("fqn");
    String actualPath = actualFqn.substring(0, actualFqn.lastIndexOf("."));
    String actualName = actualFqn.substring(actualFqn.lastIndexOf(".")+1);
    
    //close
    url = new URLPath(String.format("/runtimeContext/%s/xmom/workflows/%s/%s/close", workspaceName, actualPath, actualName), null, null);
    method = new POST();
    payload = "{\"force\":false,\"revision\":2}";
    executeRunnable(runnable, url, method, payload, msg);

    String dtPath = dtFqn.substring(0, dtFqn.lastIndexOf("."));
    String dtName = dtFqn.substring(dtFqn.lastIndexOf(".")+1);
    LinkWorkflow(runnable, parameter.serviceName, dtName, dtPath, actualFqn, parameter.revision);
  }
  
  public static void LinkWorkflow(RunnableForFilterAccess runnable, String opName, String dtName, String dtPath, String workflowFqname, Long revision) throws Exception {
    String workspaceName = getWorkspaceNameEncoded(revision);
    
    //open Datatype
    URLPath url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s",  workspaceName, dtPath, dtName), null, null);
    HTTPMethod method = new GET();
    String payload;
    String msg = "could not process impl dt";
    GeneralXynaObject result = (GeneralXynaObject) executeRunnable(runnable, url, method, null, msg);
    
    String memberMethodId = findMemberMethodId(result, opName);
    
    //set reference
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/objects/%s/move", workspaceName, dtPath, dtName, memberMethodId), null, null);
    method = new POST();
    payload = "{\"index\":-1,\"targetId\":\"overriddenMethodsArea\",\"relativePosition\":\"inside\"}";
    executeRunnable(runnable, url, method, payload, msg);
    
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/objects/%s/change", workspaceName, dtPath, dtName, memberMethodId), null, null);
    method = new PUT();
    payload = String.format("{\"implementationType\":\"reference\",\"reference\":\"%s\"}", workflowFqname);
    executeRunnable(runnable, url, method, payload, msg);
    
    
    //save
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/save", workspaceName, dtPath, dtName), null, null);
    method = new POST();
    payload = "{\"force\":false,\"revision\":2}";
    executeRunnable(runnable, url, method, payload, msg);
    
    //deploy
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/deploy", workspaceName, dtPath, dtName), null, null);
    method = new POST();
    payload = "{\"revision\":2}";
    executeRunnable(runnable, url, method, payload, msg);
    
    //close
    url = new URLPath(String.format("/runtimeContext/%s/xmom/datatypes/%s/%s/close", workspaceName, dtPath, dtName), null, null);
    method = new POST();
    payload = "{\"force\":false,\"revision\":2}";
    executeRunnable(runnable, url, method, payload, msg);
  }
  
  @SuppressWarnings("unchecked")
  private static String findMemberMethodId(GeneralXynaObject result, String opName) throws Exception {
    List<GeneralXynaObject> candidates;
   
    List<GeneralXynaObject> areas = (List<GeneralXynaObject>)result.get("xmomItem.areas");
  
    GeneralXynaObject memberdMethodsArea = areas.get(8);
    candidates = (List<GeneralXynaObject>)memberdMethodsArea.get("items");
    String id = findMethodInCandidates(opName, candidates);
    if(id != null) {
      return id;
    }
    
    
    GeneralXynaObject inheritedMethodsArea = areas.get(7);
    candidates = (List<GeneralXynaObject>)inheritedMethodsArea.get("items");
    id = findMethodInCandidates(opName, candidates);
    if(id != null) {
      return id;
    }

    throw new RuntimeException("did not find operation " + opName);
  }
  
  private static String findMethodInCandidates(String opName, List<GeneralXynaObject> candidates) throws Exception {
    if(candidates == null) {
      return null;
    }
    for(GeneralXynaObject candidate : candidates) {
      if(opName.equals((String)candidate.get("name"))) {
        return (String)candidate.get("id");
      }
    }
    return null;
  }

  private static String createImplWfOpenPayload(CreateImplWfParameter parameter) {
    StringBuilder sb = new StringBuilder();
    sb.append("{");
    sb.append("\"input\": [");
    appendSignatureVariables(sb, parameter.inputs);
    sb.append("],");
    sb.append("\"label\": \"");
    sb.append(parameter.wfLabel);
    sb.append("\",");
    sb.append("\"output\": [");
    appendSignatureVariables(sb, parameter.outputs);
    sb.append("]");
    sb.append("}");
    return sb.toString();
  }
  
  private static void appendSignatureVariables(StringBuilder sb, List<SignatureVariable> vars) {
    if(vars.isEmpty()) {
      return;
    }
    for( SignatureVariable var : vars) { 
      sb.append("{");
      sb.append("\"fqn\": \"");
      sb.append(var.fqn);
      sb.append("\", ");
      sb.append("\"isAbstract\": false,");
      sb.append("\"isList\": false,");
      sb.append("\"label\": \"");
      sb.append(var.label);
      sb.append("\", ");
      sb.append("\"type\": \"variable\"");
      sb.append("},");
    }
    sb.setLength(sb.length()-1); // remove last ","
  }
  
  private static String getWorkspaceNameEncoded(Long revision) throws Exception {
    String name = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getWorkspace(revision).getName();
    return urlEncode(name);
  }
  
  private static Object executeRunnable(RunnableForFilterAccess runnable, URLPath url, HTTPMethod method, String payload, String msg) {
    try {
      return runnable.execute(url, method, payload);
    } catch (XynaException e) {
      throw new RuntimeException(msg, e);
    }
  }
  
  public static String urlEncode(String in) {
    return URLEncoder.encode(in, Charset.forName("UTF-8"));
  }
}
