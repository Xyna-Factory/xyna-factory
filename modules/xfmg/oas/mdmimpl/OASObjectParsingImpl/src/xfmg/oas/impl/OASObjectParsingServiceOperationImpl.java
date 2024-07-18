/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2024 Xyna GmbH, Germany
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
package xfmg.oas.impl;



import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;

import xact.templates.Document;
import xfmg.xfctrl.datamodel.json.JSONDatamodelServices;
import xfmg.xfctrl.datamodel.json.JSONKeyValue;
import xfmg.xfctrl.datamodel.json.JSONObject;
import xfmg.xfctrl.datamodel.json.JSONValue;
import xfmg.xfctrl.datamodel.json.parameter.JSONWritingOptions;
import xfmg.oas.OASObjectParsingServiceOperation;



public class OASObjectParsingServiceOperationImpl implements ExtendedDeploymentTask, OASObjectParsingServiceOperation {

  private static final String additionalPropertiesMemberName = "Additional Properties";
  
  public void onDeployment() throws XynaException {
  }


  public void onUndeployment() throws XynaException {
  }


  public Long getOnUnDeploymentTimeout() {
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    return null;
  }

  @Override
  public Document convertOasObjectToJson(GeneralXynaObject anyType, JSONWritingOptions options) {
    Document doc = JSONDatamodelServices.writeJSONWithOptions(anyType, options);
    JSONObject json = JSONObject.fromJson(doc);
    replaceAdditionalPropertiesInObject(json);
    return JSONDatamodelServices.writeJSONWithOptions(json, options);
  }
  

  private void replaceAdditionalPropertiesInObject(JSONObject obj) {
    for (JSONKeyValue kvp : obj.getMembers()) {
      JSONObject innerObj = kvp.getValue().getObjectValue();
      if (innerObj instanceof JSONObject) {
        replaceAdditionalPropertiesInObject(innerObj);
      }
    }
    
    JSONValue addProp = obj.getMember(additionalPropertiesMemberName);
    if (addProp != null) {
      replaceAdditionalProperties(obj, addProp);
    }
  }


  @SuppressWarnings("unchecked")
  private void replaceAdditionalProperties(JSONObject obj, JSONValue addProp) {
    List<JSONKeyValue> members = (List<JSONKeyValue>) obj.getMembers();
    members.removeIf(x -> x.getKey().equals(additionalPropertiesMemberName));
    List<? extends JSONValue> values = addProp.getArrayValue();
    if (values == null) {
      return;
    }

    for (JSONValue val : values) {
      JSONObject jsonObj = val.getObjectValue();
      if (jsonObj == null) {
        continue;
      }

      JSONValue key = jsonObj.getMember("Key");
      JSONValue value = jsonObj.getMember("Value");
      if (key == null || key.getStringOrNumberValue() == null) {
        continue;
      }

      members.add(new JSONKeyValue.Builder().key(key.getStringOrNumberValue()).value(value).instance());
    }

    obj.setMembers(members);
  }
}
