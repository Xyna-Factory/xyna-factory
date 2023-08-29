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



import base.File;
import base.Text;

import java.util.List;

import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import xmcp.gitintegration.FactoryContent;
import xmcp.gitintegration.FactoryContentDifferences;
import xmcp.gitintegration.FactoryObjectManagementServiceOperation;
import xmcp.gitintegration.FactoryXmlEntryType;
import xmcp.gitintegration.FactoryXmlIgnoreEntry;
import xmcp.gitintegration.Flag;
import xmcp.gitintegration.cli.generated.OverallInformationProvider;
import xmcp.gitintegration.storage.FactoryDifferenceListStorage;
import xmcp.gitintegration.storage.FactoryXmlIgnoreEntryStorage;



public class FactoryObjectManagementServiceOperationImpl implements ExtendedDeploymentTask, FactoryObjectManagementServiceOperation {

  public void onDeployment() throws XynaException {
    FactoryDifferenceListStorage.init();
    FactoryXmlIgnoreEntryStorage.init();
    OverallInformationProvider.onDeployment();
  }


  public void onUndeployment() throws XynaException {
    OverallInformationProvider.onUndeployment();
  }


  public Long getOnUnDeploymentTimeout() {
    // If null is returned, the default timeout (defined by XynaProperty xyna.xdev.xfractmod.xmdm.deploymenthandler.timeout) will be used.
    return null;
  }


  public BehaviorAfterOnUnDeploymentTimeout getBehaviorAfterOnUnDeploymentTimeout() {
    // If null is returned, the factory default <IGNORE> will be used.
    return null;
  }


  public FactoryContentDifferences compareFactoryContent(FactoryContent factoryContent9, FactoryContent factoryContent10) {
    FactoryContentComparator comparator = new FactoryContentComparator();
    FactoryContentDifferences differences = comparator.compareFactoryContent(factoryContent9, factoryContent10, true);
    return differences;
  }


  public FactoryContent createFactoryContent() {
    FactoryContentCreator creator = new FactoryContentCreator();
    FactoryContent content = creator.createFactoryContent();
    return content;
  }


  public FactoryContent createFactoryContentFromFile(File file6) {
    FactoryContentCreator creator = new FactoryContentCreator();
    FactoryContent content = creator.createFactoryContentFromFile(new java.io.File(file6.getPath()));
    return content;
  }


  public FactoryContent createFactoryContentFromText(Text text8) {
    FactoryContentCreator creator = new FactoryContentCreator();
    FactoryContent content = creator.createFactoryContentFromText(text8.getText());
    return content;
  }


  @Override
  public void addFactoryXmlIgnoreEntry(FactoryXmlIgnoreEntry entry) {
    FactoryXmlIgnoreEntryStorage storage = new FactoryXmlIgnoreEntryStorage();
    storage.addFactoryXmlIgnoreEntry(entry.getConfigType(), entry.getValue());
  }


  @Override
  public List<? extends FactoryXmlEntryType> listFactoryXmlEntryTypes() {
    // TODO Auto-generated method stub
    return null;
  }


  @Override
  public List<? extends FactoryXmlIgnoreEntry> listFactoryXmlIgnoreEntries() {
    FactoryXmlIgnoreEntryStorage storage = new FactoryXmlIgnoreEntryStorage();
    return storage.listAllFactoryXmlIgnoreEntries();
  }


  @Override
  public void removeFactoryXmlIgnoreEntry(FactoryXmlIgnoreEntry entry) {
    FactoryXmlIgnoreEntryStorage storage = new FactoryXmlIgnoreEntryStorage();
    storage.removeFactoryXmlIgnoreEntry(entry.getConfigType(), entry.getValue());
  }


  @Override
  public void validateFactoryXmlIgnoreEntries(Flag flag) {
    // TODO Auto-generated method stub

  }

}
