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
package xmcp.gitintegration.impl;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import com.gip.xyna.xfmg.Constants;

import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.FactoryContentItem;
import xmcp.gitintegration.impl.processing.FactoryContentProcessingPortal;
import xmcp.gitintegration.impl.xml.FactoryContentXmlConverter;

public class FactoryContentCreator {

  public static final String FACTORY_XML_FILENAME = "factory.xml";
  public static final String FACTORY_XML_SPLITNAME = "config";
  
  public FactoryContent createFactoryContent() {
    FactoryContent result = new FactoryContent();
    FactoryContentProcessingPortal portal = new FactoryContentProcessingPortal();
    List<FactoryContentItem> items = portal.createItems();
    result.unversionedSetFactoryContentItems(items);
    return result;
  }
  
  public FactoryContent readFactoryContent() {
    StringBuilder builder = new StringBuilder();
    builder.append("..").append(Constants.fileSeparator).append(Constants.REVISION_PATH);
    File f = new File(builder.toString(), FactoryContentCreator.FACTORY_XML_FILENAME);
    if(!f.exists()) {
      f = new File(builder.toString(), FactoryContentCreator.FACTORY_XML_SPLITNAME);
    }
    return createFactoryContentFromFile(f);
  }
  

  /**
   * File is either a workspace.xml or a configuration-folder containing
   * files named after workspaceContentItem subclasses  
   */
  public FactoryContent createFactoryContentFromFile(File file) {
    FactoryContent result = null;
    try {
      result = file.isFile() ? createFactoryContentFromText(Files.readString(file.toPath())) : createFactoryContentFromDirectory(file);
    } catch (IOException e) {
      throw new RuntimeException("Could not read FactoryContent from " + file.getAbsolutePath(), e);
    }
    return result;
  }
  
  public FactoryContent createFactoryContentFromText(String xml) {
    FactoryContentXmlConverter converter = new FactoryContentXmlConverter();
    FactoryContent content = converter.convertFromXml(xml);
    return content;
  }
  
  private FactoryContent createFactoryContentFromDirectory(File file) throws IOException {
    FactoryContentXmlConverter converter = new FactoryContentXmlConverter();
    FactoryContent result = new FactoryContent();
    for (File f : file.listFiles()) {
      String input = Files.readString(f.toPath());
      converter.addToFactoryContent(input, result);
    }
    return result;
  }
}
