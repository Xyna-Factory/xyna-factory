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
package com.gip.xyna.xnwh.persistence;

import java.io.File;

import junit.framework.Assert;

import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.xmlshell.XynaXMLShellPersistenceLayer;

public class XMLShellPersistenceLayerTest extends AbstractPersistenceLayerTest {
  
  
  public void setUp() {
    File tmpDirFile = new File(XynaProperty.PERSISTENCE_DIR + File.separator + PersistenceLayerTestStoreable.TABLENAME);
    tmpDirFile.mkdirs();
    File []childfiles = tmpDirFile.listFiles();
    if(childfiles != null) {
      for(File child : childfiles) {
       child.delete();
      }
    }
  }
  
  public void tearDown() {
    File tmpDirFile = new File(XynaProperty.PERSISTENCE_DIR + File.separator + PersistenceLayerTestStoreable.TABLENAME);
    File []childfiles = tmpDirFile.listFiles();
    if(childfiles != null) {
      for(File child : childfiles) {
       child.delete();
      }
    }
    tmpDirFile.delete();
  }
  
  @Override
  public PersistenceLayerConnection getConnection() {
    try {
      XynaXMLShellPersistenceLayer xmlshellPL = new XynaXMLShellPersistenceLayer();
      xmlshellPL.init(null, "", "60", "true");
      PersistenceLayerConnection connection = xmlshellPL.getConnection();
      connection.addTable(PersistenceLayerTestStoreable.class, false, null);
      return connection;
    } catch(PersistenceLayerException e) {
      e.printStackTrace();
      Assert.fail(e.getMessage());
    }
    return null;
  }

}
