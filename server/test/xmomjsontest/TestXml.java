

package xmomjsontest;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.gip.xyna.xprc.xfractwfe.generation.CodeBuffer;
import com.gip.xyna.xprc.xfractwfe.generation.DOM;
import com.gip.xyna.xprc.xfractwfe.generation.DomForTest;
import com.gip.xyna.xprc.xfractwfe.generation.WfForTest;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.xml.Workflow;
import com.gip.xyna.xprc.xfractwfe.generation.xml.WorkflowOperation;
import com.gip.xyna.xprc.xfractwfe.generation.xml.XmomType;

public class TestXml {

  
  public String readFile(String filename) {
    try {
      String line;
      StringBuilder builder = new StringBuilder("");
      BufferedReader f = new BufferedReader(
           new InputStreamReader(new FileInputStream(filename), "UTF8"));
      try {
        while ((line = f.readLine()) != null) {
          builder.append(line).append("\n");
        }
      }
      finally {
        f.close();
      }
      return builder.toString();
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void test1() throws Exception {
    try {
      String txt = readFile("test/xmomjsontest/TestWf1.xml");
      log(txt);
      
      Document doc = XMLUtils.parseString(txt, true);
      Element elem = XMLUtils.getChildElementByName(doc.getDocumentElement(), "Operation", 
                                                    doc.getDocumentElement().getNamespaceURI());
      log(elem.getNodeName());
      log(elem.getLocalName());
      log(elem.getTagName());
      log(elem.getNamespaceURI());
      log(elem.getPrefix());
      log(elem.lookupNamespaceURI(elem.getPrefix()));
      
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeException(e);
    }
  }
  
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  
  public static void main(String[] args) {
    try {
      new XmomJsonTest().test1();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
