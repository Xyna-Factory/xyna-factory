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

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xtfutils.xmltools.XmlNamespace;
import com.gip.xtfutils.xmltools.build.XmlBuilder;
import com.gip.xtfutils.xmltools.build.XmlStringBuilder;
import com.gip.xtfutils.xmltools.nav.XmlNavigator;
import com.gip.xtfutils.xmltools.nav.axiom.AxiomNavigator;
import com.gip.xtfutils.xmltools.nav.jdom.JdomNavigator;
import com.gip.xtfutils.xmltools.nav.w3cdom.W3cDomNavigator;

import static org.junit.Assert.*;
import org.junit.Test;


public class TestXmlNav {

  public static Logger _logger = Logger.getLogger(TestXmlNav.class);


  public static String readFile(String filename) throws Exception {
    try {
      String line;
      StringBuilder builder = new StringBuilder("");
      BufferedReader f = new BufferedReader(new FileReader(filename));
      while ((line = f.readLine()) != null) {
        builder.append(line).append('\n');
      }
      return builder.toString();
    } catch (Exception e) {
      throw e;
    }
  }


  @Test
  public void testJdomNav() throws Exception {
    String xml = readFile("test/data/cwmp_inform_1.xml");
    //_logger.debug("Read file: \n " + xml);
    XmlNavigator nav = new JdomNavigator(xml);
    assertTrue(nav.isRoot());

    nav.descend("Body").descend("Inform").descend("DeviceId").descend("OUI");
    assertFalse(nav.isEmpty());
    assertTrue("00040E".equals(nav.getText()));
    _logger.debug("OUI=" + nav.getText());

    XmlNavigator cloned = nav.clone();

    nav.ascend(2);
    assertTrue("Inform".equals(nav.getTagName()));

    assertTrue("00040E".equals(cloned.getText()));
    assertTrue("OUI".equals(cloned.getTagName()));

    nav.descend("ParameterList");
    assertTrue("cwmp:ParameterValueStruct[8]".equals(nav.getAttributeValue("arrayType")));
    _logger.debug("ParameterList.@arrayType=" + nav.getAttributeValue("arrayType"));


    nav.descend("ParameterValueStruct", 2).descend("Value");
    assertTrue("74.05.05".equals(nav.getText()));
    _logger.debug("ParameterValueStruct[2].Value=" + nav.getText());

    nav.ascend().ascend();
    List<XmlNavigator> kids = nav.getChildrenByName("ParameterValueStruct");
    assertTrue(kids.size() == 8);

    nav.descend("xyz");
    assertTrue(nav.isEmpty());
    nav.descend("dummy");
    assertTrue(nav.isEmpty());

    nav.gotoRoot();
    assertFalse(nav.isEmpty());
    _logger.debug("root node: " + nav.getTagName());
    assertTrue("Envelope".equals(nav.getTagName()));
    assertTrue(nav.isRoot());

    cloned.gotoRoot().descend("Body").descend("Inform").descend("DeviceId");
    _logger.debug("xml sub-document (deviceId): \n" + cloned.getSelfDescendantString());

    nav.ascend();
    assertTrue(nav.isEmpty());
  }



  @Test
  public void testW3cDomNav() throws Exception {
    String xml = readFile("test/data/cwmp_inform_1.xml");
    //_logger.debug("Read file: \n " + xml);
    XmlNavigator nav = new W3cDomNavigator(xml);
    assertTrue(nav.isRoot());
    _logger.debug("root=" + nav.getTagName());

    nav.descend("Body");
    _logger.debug("body=" + nav.getTagName());
    assertFalse(nav.isEmpty());

    nav.descend("Inform").descend("DeviceId").descend("OUI");
    assertFalse(nav.isEmpty());
    assertTrue("00040E".equals(nav.getText()));
    _logger.debug("OUI=" + nav.getText());

    XmlNavigator cloned = nav.clone();

    nav.ascend(2);
    assertTrue("Inform".equals(nav.getTagName()));

    assertTrue("00040E".equals(cloned.getText()));
    assertTrue("OUI".equals(cloned.getTagName()));

    nav.descend("ParameterList");
    _logger.debug("ParameterList.@arrayType=" + nav.getAttributeValue("arrayType"));
    assertTrue("cwmp:ParameterValueStruct[8]".equals(nav.getAttributeValue("arrayType")));


    nav.descend("ParameterValueStruct", 2).descend("Value");
    assertTrue("74.05.05".equals(nav.getText()));
    _logger.debug("ParameterValueStruct[2].Value=" + nav.getText());

    nav.ascend().ascend();
    List<XmlNavigator> kids = nav.getChildrenByName("ParameterValueStruct");
    assertTrue(kids.size() == 8);

    nav.descend("xyz");
    assertTrue(nav.isEmpty());
    nav.descend("dummy");
    assertTrue(nav.isEmpty());

    nav.gotoRoot();
    assertFalse(nav.isEmpty());
    _logger.debug("root node: " + nav.getTagName());
    assertTrue("Envelope".equals(nav.getTagName()));
    assertTrue(nav.isRoot());

    cloned.gotoRoot().descend("Body").descend("Inform").descend("DeviceId");
    _logger.debug("xml sub-document (deviceId): \n" + cloned.getSelfDescendantString());

    nav.ascend();
    assertTrue(nav.isEmpty());
  }


  @Test
  public void testAxiomNav() throws Exception {
    _logger.debug("Testing axiom nav...");

    String xml = readFile("test/data/cwmp_inform_1.xml");
    //_logger.debug("Read file: \n " + xml);
    XmlNavigator nav = new AxiomNavigator(xml);
    assertTrue(nav.isRoot());
    _logger.debug("root=" + nav.getTagName());

    nav.descend("Body");
    _logger.debug("body=" + nav.getTagName());
    assertFalse(nav.isEmpty());

    nav.descend("Inform").descend("DeviceId").descend("OUI");
    assertFalse(nav.isEmpty());
    assertTrue("00040E".equals(nav.getText()));
    _logger.debug("OUI=" + nav.getText());

    XmlNavigator cloned = nav.clone();

    nav.ascend(2);
    assertTrue("Inform".equals(nav.getTagName()));

    assertTrue("00040E".equals(cloned.getText()));
    assertTrue("OUI".equals(cloned.getTagName()));

    nav.descend("ParameterList");
    _logger.debug("ParameterList.@arrayType=" + nav.getAttributeValue("arrayType"));
    assertTrue("cwmp:ParameterValueStruct[8]".equals(nav.getAttributeValue("arrayType")));

    nav.descend("ParameterValueStruct", 2);
    _logger.debug("ParameterValueStruct[2]=" + nav.getTagName());

    nav.descend("Value");
    _logger.debug("ParameterValueStruct[2].Value=" + nav.getText());
    assertTrue("74.05.05".equals(nav.getText()));

    nav.ascend().ascend();
    List<XmlNavigator> kids = nav.getChildrenByName("ParameterValueStruct");
    assertTrue(kids.size() == 8);

    nav.descend("xyz");
    assertTrue(nav.isEmpty());
    nav.descend("dummy");
    assertTrue(nav.isEmpty());

    nav.gotoRoot();
    assertFalse(nav.isEmpty());
    _logger.debug("root node: " + nav.getTagName());
    assertTrue("Envelope".equals(nav.getTagName()));
    assertTrue(nav.isRoot());

    cloned.gotoRoot().descend("Body").descend("Inform").descend("DeviceId");
    _logger.debug("xml sub-document (deviceId): \n" + cloned.getSelfDescendantString());

    nav.ascend();
    assertTrue(nav.isEmpty());
  }



  public static void testPerformance1() throws Exception {
    XmlBuilder xml = new XmlStringBuilder();
    buildLoopXml(xml);
    String out = xml.buildXmlString();

    long startTime = System.currentTimeMillis();

    XmlNavigator nav = new JdomNavigator(out);
    nav.descend("Body").descend(Constant.UPLOAD);
    //_logger.info("upload nav: " + nav.isEmpty());
    nav.descend("FileType" + Constant.SELECT_CHILD_INDEX);
    _logger.info("FileType" + Constant.SELECT_CHILD_INDEX + ": "+ nav.getText());

    long endTime = System.currentTimeMillis();
    long diff = endTime - startTime;

    //_logger.info("xml: \n" + out);

    _logger.info("time diff for JdomNavigator (millis): " + diff);
  }


  public static void testPerformance2() throws Exception {
    XmlBuilder xml = new XmlStringBuilder();
    buildLoopXml(xml);
    String out = xml.buildXmlString();

    long startTime = System.currentTimeMillis();

    XmlNavigator nav = new AxiomNavigator(out);
    nav.descend("Body").descend(Constant.UPLOAD);
    //_logger.info("upload nav: " + nav.isEmpty());
    nav.descend("FileType" + Constant.SELECT_CHILD_INDEX);
    _logger.info("FileType" + Constant.SELECT_CHILD_INDEX + ": "+ nav.getText());

    long endTime = System.currentTimeMillis();
    long diff = endTime - startTime;
    _logger.info("time diff for AxiomNavigator (millis): " + diff);
  }


  public static void testPerformance3() throws Exception {
    XmlBuilder xml = new XmlStringBuilder();
    buildLoopXml(xml);
    String out = xml.buildXmlString();

    long startTime = System.currentTimeMillis();

    XmlNavigator nav = new W3cDomNavigator(out);
    nav.descend("Body").descend(Constant.UPLOAD);
    //_logger.info("upload nav: " + nav.isEmpty());
    nav.descend("FileType" + Constant.SELECT_CHILD_INDEX);
    _logger.info("FileType" + Constant.SELECT_CHILD_INDEX + ": "+ nav.getText());

    long endTime = System.currentTimeMillis();
    long diff = endTime - startTime;
    _logger.info("time diff for W3cDomNavigator (millis): " + diff);
  }


  protected static void buildLoopXml(XmlBuilder xml) throws Exception {
    XmlNamespace cwmp = XmlNamespace.setUri("urn:dslforum-org:cwmp-1-0").setPrefix("cwmp");
    XmlNamespace xsi = XmlNamespace.setUri("http://www.w3.org/2001/XMLSchema-instance").setPrefix("xsi");
    XmlNamespace soap = XmlNamespace.setUri("http://schemas.xmlsoap.org/soap/envelope/").setPrefix("soap");

    xml.openTag("Envelope", soap).addNamespaceDeclaration(cwmp);
    {
      xml.openTag("Header");
      {
        xml.addChildElementWithInnerText("ID", "my_new_id_1", cwmp).
          addAttribute("mustUnderstand", "attr_val_1", soap);
      }
      xml.closeTag("Header");
      xml.openTag("Body");
      {
        xml.openTag(Constant.UPLOAD, cwmp).addNamespaceDeclaration(xsi);
        {
          for (int i = 1; i <= Constant.LOOP_MAX; i++) {
            xml.addChildElementWithInnerText(Constant.COMMAND_KEY + i, "mycommandKey_" + i).
                addAttribute(Constant.TYPE, "xsd:string", xsi);
            xml.addChildElementWithInnerText(Constant.FILE_TYPE + i, "myfileType_" + i);
          }
        }
        xml.closeTag(Constant.UPLOAD);
      }
      xml.closeTag("Body");
    }
    xml.closeTag("Envelope");
  }


  public static void testRecursive() throws Exception {
    String xml = readFile("test/data/cwmp_inform_1.xml");
    //_logger.debug("Read file: \n " + xml);
    XmlNavigator nav = new W3cDomNavigator(xml);
    List<XmlNavigator> list = nav.getChildElementsRecursively("Value");
    for (XmlNavigator item : list) {
      _logger.info("## value: " + item.getText());
    }
    XmlNavigator tmp = nav.getFirstDescendantWithName("OUI");
    _logger.info("## oui: " + tmp.getText());
  }


  private static class Constant {
    //private static final int LOOP_MAX = 1000;
    private static final int LOOP_MAX = 15000;
    private static final int SELECT_CHILD_INDEX = 14000;
    public static final String UPLOAD = "Upload";
    public static final String COMMAND_KEY = "CommandKey";
    public static final String TYPE = "type";
    public static final String FILE_TYPE = "FileType";
  }

  public static void main(String[] args) {
    try {
      //new TestXmlNav().test1();
      //testPerformance2();
      testRecursive();
    }
    catch (Throwable e) {
      _logger.error("", e);
    }
  }

}
