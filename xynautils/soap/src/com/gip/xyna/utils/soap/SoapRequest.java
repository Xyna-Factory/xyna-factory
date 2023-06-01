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
package com.gip.xyna.utils.soap;

import com.gip.xyna.utils.soap.serializer.SoapBody;
import com.gip.xyna.utils.soap.serializer.SoapEnvelope;
import com.gip.xyna.utils.soap.serializer.SoapEnvelopeSerializer;

/**
 * kapselt das xml, welches bei soap verschickt wird: body, header, payload, fault etc.
 * TODO: headers, faults
 */
public class SoapRequest {

  private SoapEnvelope se;
  //  private SOAPVersion version = SOAPVersion.SOAP11; //TODO verwenden

  public SoapRequest() {
  }
  
  public SoapRequest(String payload) {
    se = new SoapEnvelope();
    SoapBody body = new SoapBody();
    body.setXMLPayload(payload);
    se.setBody(body);
  }

  public SoapRequest(SoapEnvelope se) {
    this.se = se;
  }

  public void setPayload(String xml) {
    if (se == null) {
      se = new SoapEnvelope();
    }
    if (se.getBody() == null) {
      SoapBody body = new SoapBody();
      se.setBody(body);
    }
    se.getBody().setXMLPayload(xml);
  }

  public String getAsXML() throws Exception {
    return new SoapEnvelopeSerializer(se).toXMLString();
  }

  void setMessageWithSoapEnv(String message) throws Exception {
    se = new SoapEnvelopeSerializer(message).toBean();
  }
}
