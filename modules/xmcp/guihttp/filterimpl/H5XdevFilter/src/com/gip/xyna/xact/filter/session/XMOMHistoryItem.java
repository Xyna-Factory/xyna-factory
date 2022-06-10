/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2022 GIP SmartMercial GmbH, Germany
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

package com.gip.xyna.xact.filter.session;



public class XMOMHistoryItem {

  private String xml;
  private final FQName fqName;
  private boolean saveState;
  private boolean modified;


  public XMOMHistoryItem(FQName fqName, String xml, boolean saveState, boolean modified) {
    this.fqName = fqName;
    this.xml = xml;
    this.saveState = saveState;
    this.modified = modified;
  }


  public String getXml() {
    return xml;
  }


  public void setXml(String xml) {
    this.xml = xml;
  }


  public FQName getFQName() {
    return fqName;
  }


  public boolean getSaveState() {
    return saveState;
  }


  public void setSaveState(boolean saveState) {
    this.saveState = saveState;
  }


  public boolean getModified() {
    return modified;
  }


  public void setModified(boolean modified) {
    this.modified = modified;
  }

}
