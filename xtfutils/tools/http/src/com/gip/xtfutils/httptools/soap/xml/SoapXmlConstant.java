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


package com.gip.xtfutils.httptools.soap.xml;


public class SoapXmlConstant {

  public static class XmlTagName {
    public static final String BODY = "Body";
    public static final String ENVELOPE = "Envelope";
    public static final String HEADER = "Header";
  }

  public static class XmlNamespace {
    public static class Soap_1_1 {
      // soap 1.1
      public static final String ENVELOPE = "http://schemas.xmlsoap.org/soap/envelope/";
    }
    public static class Soap_1_2 {
      //soap 1.2
      public static final String ENVELOPENAMESPACE_2003 = "http://www.w3.org/2003/05/soap-envelope";
    }
  }

  public static final String SOAPPREFIX = "soap";
}
