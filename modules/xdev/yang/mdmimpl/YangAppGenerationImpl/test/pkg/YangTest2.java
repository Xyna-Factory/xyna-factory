


package pkg;

//import org.junit.jupiter.api.Test;  // if Junit 5 is used?
import static org.junit.Assert.assertEquals;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.yangcentral.yangkit.base.YangElement;
import org.yangcentral.yangkit.common.api.QName;
import org.yangcentral.yangkit.model.api.schema.YangSchemaContext;
import org.yangcentral.yangkit.model.api.stmt.Augment;
import org.yangcentral.yangkit.model.api.stmt.Deviate;
import org.yangcentral.yangkit.model.api.stmt.Deviation;
import org.yangcentral.yangkit.model.api.stmt.Include;
import org.yangcentral.yangkit.model.api.stmt.Module;
import org.yangcentral.yangkit.model.api.stmt.Revision;
import org.yangcentral.yangkit.model.api.stmt.SchemaNode;
import org.yangcentral.yangkit.model.api.stmt.Uses;
import org.yangcentral.yangkit.model.api.stmt.YangStatement;
import org.yangcentral.yangkit.parser.YangYinParser;

import xdev.yang.impl.YangStatementTranslator.YangStatementTranslation;


public class YangTest2 {

  
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
  
  private ByteArrayInputStream textAsByteStream(String text) throws IOException {
    java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(text.getBytes());
    return is;
  }
  
  private ByteArrayInputStream fileAsByteStream(File file) throws IOException {
    String text = readFile(file.getPath());
    java.io.ByteArrayInputStream is = new java.io.ByteArrayInputStream(text.getBytes());
    return is;
  }
  
  private void log(String txt) {
    System.out.println(txt);
  }
  
  private String getDataFilePath(String filename) throws Exception {
    Path path = getBasePath();  // classes dir
    path = path.getParent().resolve("test").resolve("data").resolve(filename);
    return path.toString();
  }
  
  private String getDataFile(String filename) throws Exception {
    String path = getDataFilePath(filename);
    return readFile(path);
  }
  
  private Path getBasePath() throws Exception {
    return Path.of(getClass().getClassLoader().getResource("").toURI());
  }
  
  
  public void test1() throws Exception {    
    try {
      String txt = getDataFile("test_module_zb_1_a.yang");
      YangSchemaContext context = null;
      context = YangYinParser.parse(textAsByteStream(txt), "module.yang", context);
      
      List<Module> found = context.getModule("test_module_zb_1_a");
      assertEquals(found.size(), 1);
      Module mod = found.get(0);
      log("module: " + mod.getModuleId().getModuleName() + " | " + mod.getModuleId().getRevision());
      
      for (YangElement elem :  mod.getSubElements()) {
        logElement(elem, 0);
      }
      List<YangStatement> list = mod.getSubStatement(new QName("http://www.gip.com/xyna/yang/test/testrpc_zb_1", "group_a"));
      for (YangElement elem :  list) {
        //logElement(elem, 0);
      }
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  
  private void logElement(YangElement elem, int layer) {
    if (elem == null) { return; }
    if (elem instanceof YangStatement) {
      YangStatement ys = (YangStatement) elem;
      String nsp = "";
      if (ys.getContext() != null) {
        nsp = YangStatementTranslation.getNamespace(ys);
      }
      log(layer + " ## YangStatement: " + elem.toString()+ " / " + ys.getArgStr() +
                           " ### " + ys.getClass().getName() + 
                           " ### " + nsp +
                           " ### " + ys.getYangKeyword().getQualifiedName() + " | " + ys.getYangKeyword().getNamespace()
          );
      for (YangElement child : this.getSubStatements(ys)) {
        logElement(child, layer + 1);
      }
    }
    else {
      log(layer + " YangElement: " + elem.toString());
    }
  }
  
  private List<YangElement> getSubStatements(YangStatement statement) {
    if (statement instanceof Uses) {
      Uses uses = (Uses) statement; 
      if (uses.getRefGrouping() == null) {
        return new ArrayList<YangElement>();
      }
      return uses.getRefGrouping().getSubElements();
    } else {
      return statement.getSubElements();
    }
  }
  
  public static void main(String[] args) {
    try {
      new YangTest2().test1();
    }
    catch (Throwable e) {
      e.printStackTrace();
    }
  }
  
}
