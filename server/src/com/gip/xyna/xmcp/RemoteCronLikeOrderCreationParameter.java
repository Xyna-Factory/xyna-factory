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
package com.gip.xyna.xmcp;




import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter;
import com.gip.xyna.xprc.CronLikeOrderCreationParameter.CronLikeOrderCreationParameterBuilder.Type;
import com.gip.xyna.xprc.CustomStringContainer;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidXMLForObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_MDMObjectCreationException;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;



public final class RemoteCronLikeOrderCreationParameter extends CronLikeOrderCreationParameter {

  private static final long serialVersionUID = -3460618164164891209L;

  
  public RemoteCronLikeOrderCreationParameter(DestinationKey dk, Long startTime, Long interval, GeneralXynaObject... inputPayload) {
    super(dk, startTime, interval, inputPayload);
  }

  private RemoteCronLikeOrderCreationParameter(DestinationKey dk, GeneralXynaObject... inputPayload) {
    super(dk, inputPayload);
  }

  
  private String inputPayloadAsXmlString;
  
  @Override
  public void setInputPayload(GeneralXynaObject payload) {
    if (payload != null) {
      inputPayloadAsXmlString = payload.toXml();
      if (payload instanceof Container) {
        inputPayloadAsXmlString = "<baum>" + inputPayloadAsXmlString + "</baum>";
      }
    }
    setInputPayloadNull();
  }
  
  
  @Override
  public void setInputPayload(GeneralXynaObject... payload) {
    if (payload != null) {
      inputPayloadAsXmlString = "<baum>" + new Container(payload).toXml() + "</baum>";
      setInputPayloadNull();
    }
  }
  
  
  public void setInputPayload(String payload) {
    inputPayloadAsXmlString = payload;
    setInputPayloadNull();
  }
  
  
  public String getInputPayloadAsString() {
    return inputPayloadAsXmlString;
  }
  
  
  public void convertInputPayload() throws XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException {
    if (inputPayloadAsXmlString == null) {
      super.setInputPayload(new Container());
    } else {
      Long revision;
      try {
        revision = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRevision(getDestinationKey().getRuntimeContext());
      } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
        CentralFactoryLogging.getLogger(RemoteXynaOrderCreationParameter.class).warn("Could not find revision for destinationKey trying WorkingSet",e);
        revision = RevisionManagement.REVISION_DEFAULT_WORKSPACE;
      }
      super.setInputPayload(XynaObject.generalFromXml(inputPayloadAsXmlString, revision));
    }
  }
  
  
  /**
   * Liefert einen CronLikeOrderCreationParameterBuilder, mit einem RemoteCronLikeOrderCreationParameter, in dem
   * DestinationKey und InputPaylod gesetzt sind.
   * @param dk
   * @param inputPayload
   * @return
   */
  public static CronLikeOrderCreationParameterBuilder<RemoteCronLikeOrderCreationParameter> newRemoteClocpForCreate(DestinationKey dk, GeneralXynaObject... inputPayload) {
    return new CronLikeOrderCreationParameterBuilder<RemoteCronLikeOrderCreationParameter>(new RemoteCronLikeOrderCreationParameter(dk, inputPayload), Type.Create);
  }

  /**
   * Liefert einen CronLikeOrderCreationParameterBuilder, mit einem leeren RemoteCronLikeOrderCreationParameter.
   * @return
   */
  public static CronLikeOrderCreationParameterBuilder<RemoteCronLikeOrderCreationParameter> newRemoteClocpForModify() {
    RemoteCronLikeOrderCreationParameter rclocp = new RemoteCronLikeOrderCreationParameter(new DestinationKey(""), new Container());
    rclocp.setDestinationKeyNull();
    rclocp.setInputPayloadNull();
    rclocp.inputPayloadAsXmlString = null;
    rclocp.setCustomStringContainer(new CustomStringContainer(null, null, null, null));
    return new CronLikeOrderCreationParameterBuilder<RemoteCronLikeOrderCreationParameter>(rclocp, Type.Modify);
  }
  
}
