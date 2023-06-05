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
package com.gip.xyna.utils.soap.test;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.gip.xyna.utils.soap.SOAPUtils;
import com.gip.xyna.utils.xml.Attribute;
import com.gip.xyna.utils.xml.XMLDocument;

/**
 * perform tests against com.gip.xyna.utils.soap.
 */
public class SoapUtilsTest {

  /**
   * global fixtures.
   */
  private String[][] urlTestData = {
      {"true", "http://www.gip.com"},
      {"malFormedUrlException", "www.gip.com"},
      {"malFormedUrlException", "gip.com"},
      {"malFormedUrlException", "הצü.הצüהצ.de"}
  };

  /**
   * XML-Message as String. 
   */
  private String xml = "";

  /**
   * XML-Message as XML-Document. 
   */
  private XMLDocument xmlDoc = new XMLDocument(0);

  /**
   * SOAPUtils Object to be tested. 
   */
  private SOAPUtils soapUtils = new SOAPUtils();

  /**
   *Expected result as String. 
   */
  private String expected = "";


  /**
   * set up everything, fill variables, etc.
   */
  @Before
  public final void setUp() {
    System.out.println("Preparing for tests");
    //XML-Message as String
    xml = "<Request1><element1 attribute1=\"testAttribute\"/></Request1>";
    expected = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"+
    "  <soapenv:Body>\n" + xml + "  </soapenv:Body>\n</soapenv:Envelope>\n";

    //XML-Message as xml-Document (xyna.utils.xml)
    Attribute attribute = new Attribute();
    attribute.setName("attribute1");
    attribute.setValue("testAttribute");
    xmlDoc.addDeclaration("1.0", "UTF-8");
    xmlDoc.setNamespace("http://www.test.com");
    xmlDoc.addStartTag("Request1");{
      xmlDoc.addElement("element1", attribute);
    }xmlDoc.addEndTag("Request1");
  }

  /**
   * test method setUrl with various possible inputs
   */
  @Test 
  public void testSetUrl(){
    int i = 0;
    String result = "";
    for (i = 0 ; i < urlTestData.length; i++){
        System.out.println("Using: " + urlTestData[i][1]);
        soapUtils.setURL(urlTestData[i][1]);
      result = soapUtils.getProtocol() + "//" + soapUtils.getHostname() + ":"  + soapUtils.getPort() + soapUtils.getService();
      assertEquals("testSetURL - Run [" + i  + "] ", urlTestData[i][1], result);
    }
  }


  /**
   * test generateSOAPMessage using a String as input and compare result
   */
  @Test 
  public void testGenerateSOAPMessageFromString(){

    String result = "";

    result = soapUtils.generateSOAPMessage(xml);

    assertEquals("testGenerateSOAPMessageFromString", expected, result);
  }

  /**
   * test generateSOAPMessage using and XML-Doc as input and compare result
   */
  @Test
  public void testGenerateSOAPMessageFromXML(){
    String result = "";
    
    result = soapUtils.generateSOAPMessage(xmlDoc);
    
    assertEquals("testGenerateSOAPMessageFromXML", expected, result);
  }

  /**
   * test stripSOAPMessage to see if header gets removed cleanly
   */
  @Test
  public void testStripSOAPMessage(){
    
    String messageToStart = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
    "<soapenv:Envelope xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"+
    "  <soapenv:Body>\n" + xml + "  </soapenv:Body>\n</soapenv:Envelope>\n";

    assertEquals("stripSOAPMessage", xml, soapUtils.stripSOAPMessage(messageToStart));
  }


  //TODO:
  /*
   * missing test for sendSOAPMessage - how to mock httpConnection?
   */

  /**
   * clean up if necessary
   */
  @After
  public void tearDown(){
    System.out.println("Cleaning up after tests");
  }

}
