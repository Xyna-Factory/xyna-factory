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
package com.gip.xyna.xact.trigger;



import junit.framework.TestCase;



public class HTTPTriggerConnectionTest extends TestCase {

  /* private HttpURLConnection urlCon;
   
   private void startTrigger() {
   }
   
   private void stopTrigger() {
     
   }
   
   private String getLocalIp() throws XynaException {
     return NetworkInterfaceUtils.getHostName(0);
   }
   
   private String getPort() {
     return "5531";
   }
   
   private String getUrl() {
     return "testurl";
   }
   
   private void sendMessage(String testmessage) throws XynaException, IOException {
     URL url = new URL("http://" + getLocalIp() + ":" + getPort() + "/" + getUrl());
     urlCon = (HttpURLConnection)url.openConnection();
     urlCon.setRequestMethod("POST");
     urlCon.setDoInput(true);
     urlCon.setDoOutput(true);
     urlCon.setUseCaches(false);
     urlCon.setRequestProperty("Content-type", "text/html");
     urlCon.getOutputStream().write(testmessage.getBytes());
     urlCon.getOutputStream().flush();
   }
   
   private String getResult() throws IOException {    
     ByteArrayOutputStream baos = new ByteArrayOutputStream();
     byte[] buffer = new byte[512];
     int read = 0;
     while (0 != (read = urlCon.getInputStream().read(buffer))) {
       baos.write(buffer, 0, read);
     }
     return baos.toString();    
   }
   
   public void testUmlautsPost() throws XynaException, IOException {
     startTrigger();
     try {
       sendMessage("Meine Testnachricht mit Umlauten ÄÖÜ@-x - so das wars.");
       String result = getResult();
       assertEquals("", result);
     } finally {
       stopTrigger();
     }    
   }*/

  public void testParseStringValueOutOfCommaSeparatedKeyValueList() {
    String headerString = "Digest username=\"df\", realm=\"\", nonce=\"709418e8e292f776a06bb1a76be995d4\", uri=\"/config.php?version=2.5.7&lang=de_DE\", cnonce=\"MTkyMTU5\", nc=00000001,  qop=\"auth\", "
                    + "response=\"329b4ccc09c3641b6c74b4b7842fe543\", opaque=\"alsidufganlsjk34589zm345mv3mz459\", algorithm=\"MD5\"";
    assertEquals("df", DigestAuthentificationInformation.parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "username"));
    assertEquals("", DigestAuthentificationInformation.parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "realm"));
    assertEquals("709418e8e292f776a06bb1a76be995d4", DigestAuthentificationInformation
                    .parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "nonce"));
    assertEquals("/config.php?version=2.5.7&lang=de_DE", DigestAuthentificationInformation
                    .parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "uri"));
    assertEquals("MTkyMTU5", DigestAuthentificationInformation.parseStringValueOutOfCommaSeparatedKeyValueList(headerString,
                                                                                                   "cnonce"));
    assertEquals("00000001", DigestAuthentificationInformation.parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "nc"));
    assertEquals("auth", DigestAuthentificationInformation.parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "qop"));
    assertEquals("329b4ccc09c3641b6c74b4b7842fe543", DigestAuthentificationInformation
                    .parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "response"));
    assertEquals("alsidufganlsjk34589zm345mv3mz459", DigestAuthentificationInformation
                    .parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "opaque"));
    assertEquals("MD5", DigestAuthentificationInformation
                    .parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "algorithm"));
    assertNull(DigestAuthentificationInformation.parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "auth"));
    assertNull(DigestAuthentificationInformation.parseStringValueOutOfCommaSeparatedKeyValueList(headerString, "a"));
  }

}
