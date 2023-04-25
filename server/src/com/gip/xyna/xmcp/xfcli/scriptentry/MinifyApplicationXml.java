/*----------------------------------------------------
* Xyna 6.1 (Black Edition)
* Xyna Multi-Channel Portal
*----------------------------------------------------
* Copyright GIP AG 2015
* (http://www.gip.com)
* Hechtsheimer Str. 35-37
* 55131 Mainz
*----------------------------------------------------
* $Revision$
* $Date$
*----------------------------------------------------
*/
package com.gip.xyna.xmcp.xfcli.scriptentry;



import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import org.w3c.dom.Document;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlHandler.ApplicationXmlMinifier;



public class MinifyApplicationXml {

  public static void main(String[] args) {
    if (args.length < 1) {
      System.out.println("Expecting one argument: Path to application.xml and an optional argument: backup original file to");
      System.exit(1);
    }
    File f = new File(args[0]);

    System.out.println("Minify application xml at " + f.getAbsolutePath());
    if (args.length == 2) {
      System.out.println("Backup original file to " + new File(args[1]).getAbsolutePath());
    }


    if (!f.exists()) {
      System.out.println("File " + f.getAbsolutePath() + " not found.");
      System.exit(1);
    }

    ApplicationXmlMinifier impl = new ApplicationXmlMinifier();
    Document doc = null;
    FileWriter fw = null;
    try {
      if (args.length == 2) {
        Files.copy(f.toPath(), new File(args[1]).toPath());
      }
      String in = Files.readString(f.toPath());
      doc = XMLUtils.parseString(in);
      fw = new FileWriter(f);
    } catch (XynaException | IOException e) {
      System.out.println("Error: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }

    impl.minifyApplicationXml(doc);
    XMLUtils.saveDomToWriter(fw, doc);
  }
}
