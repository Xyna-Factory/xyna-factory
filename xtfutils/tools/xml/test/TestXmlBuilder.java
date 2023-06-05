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

import org.apache.log4j.Logger;

import com.gip.xtfutils.xmltools.XmlNamespace;
import com.gip.xtfutils.xmltools.build.XmlBuilder;
import com.gip.xtfutils.xmltools.build.XmlStringBuilder;
import com.gip.xtfutils.xmltools.build.jdom.XmlJdomBuilder;
import com.gip.xtfutils.xmltools.build.w3cdom.XmlW3cDomBuilder;



public class TestXmlBuilder {

  public static Logger _logger = Logger.getLogger(TestXmlBuilder.class);

  private static final int LOOP_MAX = 100000;
  //private static final int LOOP_MAX = 1000;


  public static void test1() throws Exception {
    XmlBuilder xml = new XmlJdomBuilder();

    XmlNamespace cwmp = XmlNamespace.setUri("urn:dslforum-org:cwmp-1-0").setPrefix("cwmp");
    xml.openTag("Upload", cwmp);
    {
      xml.addChildElementWithInnerText("CommandKey", "mycommandKey_1");
      xml.addChildElementWithInnerText("FileType", "myfileType_1");
    }
    xml.closeTag("Upload");

    _logger.info("xml: \n " + xml.buildXmlString());
  }


  public static void test2() throws Exception {
    XmlBuilder xml = new XmlW3cDomBuilder();

    XmlNamespace cwmp = XmlNamespace.setUri("urn:dslforum-org:cwmp-1-0").setPrefix("cwmp");
    XmlNamespace xsi = XmlNamespace.setUri("http://www.w3.org/2001/XMLSchema-instance").setPrefix("xsi");

    xml.openTag("Upload", cwmp).addNamespaceDeclaration(xsi);
    {
      xml.addChildElementWithInnerText("CommandKey", "mycommandKey_1").
          addAttribute("type", "xsd:string", xsi);
      xml.addChildElementWithInnerText("FileType", "myfileType_1");
    }
    xml.closeTag("Upload");

    _logger.info("xml: \n" + xml.buildXmlString());
  }


  public static void test3() throws Exception {
    XmlBuilder xml = new XmlJdomBuilder();

    XmlNamespace cwmp = XmlNamespace.setUri("urn:dslforum-org:cwmp-1-0").setPrefix("cwmp");
    XmlNamespace xsi = XmlNamespace.setUri("http://www.w3.org/2001/XMLSchema-instance").setPrefix("xsi");

    xml.openTag(Constant.UPLOAD, cwmp).addNamespaceDeclaration(xsi);
    {
      xml.addChildElementWithInnerText(Constant.COMMAND_KEY, "mycommandKey_1").
          addAttribute(Constant.TYPE, "xsd:string", xsi);
      xml.addChildElementWithInnerText(Constant.FILE_TYPE, "myfileType_1");
    }
    xml.closeTag(Constant.UPLOAD);

    _logger.info("xml: \n" + xml.buildXmlString());
  }



  public static void test4() throws Exception {
    XmlBuilder xml = new XmlStringBuilder();

    XmlNamespace cwmp = XmlNamespace.setUri("urn:dslforum-org:cwmp-1-0").setPrefix("cwmp");
    XmlNamespace xsi = XmlNamespace.setUri("http://www.w3.org/2001/XMLSchema-instance").setPrefix("xsi");

    xml.openTag(Constant.UPLOAD, cwmp).addNamespaceDeclaration(xsi);
    {
      xml.addChildElementWithInnerText(Constant.COMMAND_KEY, "mycommandKey_1").
          addAttribute(Constant.TYPE, "xsd:string", xsi);
      xml.addChildElementWithInnerText(Constant.FILE_TYPE, "myfileType_1");
    }
    xml.closeTag(Constant.UPLOAD);

    _logger.info("xml: \n" + xml.buildXmlString());
  }


  public static void testPerformance1() throws Exception {
    long startTime = System.currentTimeMillis();

    XmlBuilder xml = new XmlStringBuilder();
    buildLoopXml(xml);
    String out = xml.buildXmlString();

    long endTime = System.currentTimeMillis();
    long diff = endTime - startTime;

    //_logger.info("xml: \n" + out);
    _logger.info("time diff for XmlStringBuilder (millis): " + diff);
  }


  public static void testPerformance2() throws Exception {
    long startTime = System.currentTimeMillis();

    XmlBuilder xml = new XmlW3cDomBuilder();
    buildLoopXml(xml);
    String out = xml.buildXmlString();

    long endTime = System.currentTimeMillis();
    long diff = endTime - startTime;

    //_logger.info("xml: \n" + out);
    _logger.info("time diff for XmlW3cDomBuilder (millis): " + diff);
  }


  public static void testPerformance3() throws Exception {
    long startTime = System.currentTimeMillis();

    StringBuilder xml = new StringBuilder("");
    buildLoopXml2(xml);
    String out = xml.toString();

    long endTime = System.currentTimeMillis();
    long diff = endTime - startTime;

    //_logger.info("xml: \n" + out);

    _logger.info("time diff for StringBuilder (millis): " + diff);
  }


  protected static void buildLoopXml(XmlBuilder xml) throws Exception {
    XmlNamespace cwmp = XmlNamespace.setUri("urn:dslforum-org:cwmp-1-0").setPrefix("cwmp");
    XmlNamespace xsi = XmlNamespace.setUri("http://www.w3.org/2001/XMLSchema-instance").setPrefix("xsi");

    xml.openTag(Constant.UPLOAD, cwmp).addNamespaceDeclaration(xsi);
    {
      for (int i = 1; i <= LOOP_MAX; i++) {
        xml.addChildElementWithInnerText(Constant.COMMAND_KEY + i, "mycommandKey_" + i).
            addAttribute(Constant.TYPE, "xsd:string", xsi);
        xml.addChildElementWithInnerText(Constant.FILE_TYPE + i, "myfileType_" + i);
      }
    }
    xml.closeTag(Constant.UPLOAD);
  }


  protected static void buildLoopXml2(StringBuilder sb) throws Exception {
    sb.append("<cwmp:Upload xmlns:cwmp=\"urn:dslforum-org:cwmp-1-0\">\n");
    {
      for (int i = 1; i <= LOOP_MAX; i++) {
        sb.append("      <CommandKey_" + i + " xsi:type=\"xsd:string\">"+ "mycommandKey_" + i +
                  "</CommandKey_" + i + ">\n");
        sb.append("      <FileType_" + i + ">"+ "myfileType_" + i + "</FileType_" + i + ">\n");
      }
    }
    sb.append("</cwmp:Upload>\n");
  }


  private static class Constant {
    public static final String UPLOAD = "Upload";
    public static final String COMMAND_KEY = "CommandKey";
    public static final String TYPE = "type";
    public static final String FILE_TYPE = "FileType";
  }

  public static void main(String[] args) {
    try {
      //test3();
      testPerformance1();
      //testPerformance2();
    }
    catch (Throwable e) {
      _logger.error("", e);
    }
  }

}
