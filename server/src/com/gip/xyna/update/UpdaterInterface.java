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

package com.gip.xyna.update;

import com.gip.xyna.update.Update.ExecutionTime;
import com.gip.xyna.update.Updater.ApplicationUpdate;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.appmgmt.ApplicationXmlEntry;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_GENERAL_UPDATE_ERROR;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_DETECTION_PROBLEM;
import com.gip.xyna.xprc.exceptions.XPRC_VERSION_VALIDATION_ERROR;



public interface UpdaterInterface {

  public void checkUpdate() throws XPRC_GENERAL_UPDATE_ERROR, XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException;

  public void checkUpdateMdm() throws XPRC_GENERAL_UPDATE_ERROR, XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException;

  public void updateMdm(long revision) throws XPRC_GENERAL_UPDATE_ERROR;

  public void validateMDMVersion(String attribute) throws XPRC_VERSION_VALIDATION_ERROR, XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException;

  public Version getVersionOfLastSuccessfulUpdate() throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException;
  
  public Version getFactoryVersion() throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException; //throws exception nur aus abwärtskompatibilitätsgründen

  public Version getXMOMVersion() throws XPRC_VERSION_DETECTION_PROBLEM, PersistenceLayerException;

  public void updateApplicationAtImport(Long revision, ApplicationXmlEntry applicationXml) throws XynaException;
  
  public void addApplicationUpdate(ApplicationUpdate au);

  public long getUpdateTime(Version v, ExecutionTime time) throws PersistenceLayerException;

  public boolean isInitialInstallation();

  public void refreshPLCache();
}
