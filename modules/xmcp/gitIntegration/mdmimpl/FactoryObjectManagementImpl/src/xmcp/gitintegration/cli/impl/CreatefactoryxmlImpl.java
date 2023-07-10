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
package xmcp.gitintegration.cli.impl;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import com.gip.xyna.FileUtils;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xmcp.xfcli.XynaCommandImplementation;

import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.cli.generated.Createfactoryxml;
import xmcp.gitintegration.impl.FactoryContentCreator;
import xmcp.gitintegration.impl.xml.FactoryContentXmlConverter;



public class CreatefactoryxmlImpl extends XynaCommandImplementation<Createfactoryxml> {

  public void execute(OutputStream statusOutputStream, Createfactoryxml payload) throws XynaException {
    FactoryContentCreator creator = new FactoryContentCreator();
    FactoryContent content = creator.createFactoryContent();
    FactoryContentXmlConverter converter = new FactoryContentXmlConverter();
    
    String xml = converter.convertToXml(content);

    if (payload.getPrintResult()) {
      writeLineToCommandLine(statusOutputStream, xml);
      return;
    }
    

    StringBuilder builder = new StringBuilder();
    builder.append("..").append(Constants.fileSeparator).append(Constants.REVISION_PATH);
    String path = builder.toString();
    if (!payload.getSplitResult()) {
      removeExistingFiles(path);
      File factoryXmlFile = new File(path, FactoryContentCreator.FACTORY_XML_FILENAME);
      FileUtils.writeStringToFile(xml, factoryXmlFile);
    } else {
      writeSplit(content, path);
    }
  }
  
  private void removeExistingFiles(String path) {
    FileUtils.deleteFileWithRetries(new File(path, FactoryContentCreator.FACTORY_XML_FILENAME));
    if (Files.exists(Path.of(path, FactoryContentCreator.FACTORY_XML_SPLITNAME))) {
      try (Stream<Path> files = Files.list(Path.of(path, FactoryContentCreator.FACTORY_XML_SPLITNAME))) {
        files.forEach(x -> FileUtils.deleteFileWithRetries(x.toFile()));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private void writeSplit(FactoryContent content, String path) {
    FactoryContentXmlConverter converter = new FactoryContentXmlConverter();
    File configFolder = new File(path, FactoryContentCreator.FACTORY_XML_SPLITNAME);
    List<Pair<String, String>> data = converter.split(content);

    removeExistingFiles(path);
    
    try {
      if(!Files.exists(configFolder.toPath())) {
        Files.createDirectories(configFolder.toPath());
      }
      //write new files
      for (Pair<String, String> entry : data) {
        File fi = new File(configFolder, entry.getFirst());
        FileUtils.writeStringToFile(entry.getSecond(), fi);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
