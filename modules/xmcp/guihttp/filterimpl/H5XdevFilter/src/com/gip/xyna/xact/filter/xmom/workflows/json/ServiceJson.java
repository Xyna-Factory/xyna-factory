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
package com.gip.xyna.xact.filter.xmom.workflows.json;

import java.util.Arrays;
import java.util.List;

import com.gip.xyna.utils.misc.JsonParser.EmptyJsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.JsonParserUtils;
import com.gip.xyna.utils.misc.JsonParser.JsonVisitor;
import com.gip.xyna.utils.misc.JsonParser.UnexpectedJSONContentException;
import com.gip.xyna.xact.filter.json.FQNameJson;
import com.gip.xyna.xact.filter.xmom.XMOMGuiJson;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidServiceIdException;
import com.gip.xyna.xprc.xfractwfe.generation.Service;
import com.gip.xyna.xprc.xfractwfe.generation.Step;
import com.gip.xyna.xprc.xfractwfe.generation.StepFunction;

public class ServiceJson extends XMOMGuiJson {

  private boolean isPrototype;
  private String label;
  private FQNameJson fqName;
  private String service;
  public boolean readOnly;
  
  private ServiceJson() {}

  public ServiceJson(String label) {
    //this.isPrototype = true;
    this.label = label;
  }

  public ServiceJson(Step step) throws XPRC_InvalidServiceIdException {
    label = step.getLabel();

    if (step instanceof StepFunction) {
      StepFunction stepFunction = (StepFunction)step; 
      step.getParentScope().identifyService(stepFunction.getServiceId()).service.getServicePath();
      isPrototype = stepFunction.isPrototype();
      if (isPrototype) {
        fqName = null;
      } else {
        Service identifiedService = stepFunction.getParentScope().identifyService(stepFunction.getServiceId()).service;
        if(stepFunction.getOperationName() != null) {
          fqName = new FQNameJson(identifiedService.getServicePath(), identifiedService.getServiceName(), stepFunction.getOperationName());
        } else {
          fqName = new FQNameJson(identifiedService.getServicePath(), identifiedService.getServiceName());
        }
      }
    }
  }

  public String getLabel() {
    return label;
  }
  public boolean isPrototype() {
    return isPrototype;
  }
  public void setPrototype(boolean isPrototype) {
    this.isPrototype = isPrototype;
  }
  public FQNameJson getFQName() {
    return fqName;
  }
  public void setFQName(FQNameJson fqName) {
    this.fqName = fqName;
  }
  public String getService() {
    return service;
  }
  
  public static class ServiceJsonVisitor extends EmptyJsonVisitor<ServiceJson> {
    private static List<String> usedTags = Arrays.asList(Tags.SERVICE_LABEL);
    ServiceJson sj = new ServiceJson();

    @Override
    public ServiceJson get() {
      return sj;
    }

    @Override
    public ServiceJson getAndReset() {
      ServiceJson ret = sj;
      sj = new ServiceJson();
      return ret;
    }

    @Override
    public void attribute(String label, String value, Type type) throws UnexpectedJSONContentException {
      if( label.equals(Tags.TYPE) ) {
        if( !(value.equals(Tags.SERVICE)) ) {
          throw new UnexpectedJSONContentException(label + ": " + value + ", expected: " + Tags.SERVICE);
        } else {
          return;
        }
      }

      if( label.equals(Tags.LABEL) ) {
        sj.label = value;
        return;
      }
      if( label.equals(Tags.IS_PROTOTYPE) ) {
        sj.isPrototype = Boolean.parseBoolean(value);
        return;
      }
      if( label.equals(Tags.READONLY) ) {
        sj.readOnly = Boolean.parseBoolean(value);
        return;
      }
      if( label.equals(Tags.SERVICE) ) {
        sj.service = value;
        return;
      }
      if( FQNameJson.useLabel(label) ) {
        sj.fqName = FQNameJson.parseAttribute(sj.fqName, label, value);
        return;
      }

      JsonParserUtils.checkAllowedLabels(usedTags, label);
    }

    @Override
    public void objectList(String label, List<Object> values) throws UnexpectedJSONContentException {
      JsonParserUtils.checkAllowedLabels(usedTags, label);
    }
    
    @Override
    public JsonVisitor<?> objectStarts(String label) throws UnexpectedJSONContentException {
      JsonParserUtils.checkAllowedLabels(usedTags, label);
      return null;
    }
    
  }

}
