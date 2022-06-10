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
package com.gip.xyna.update;

import com.gip.xyna.utils.collections.CollectionUtils.Transformation;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xfmg.xfctrl.keymgmt.JavaSecurityKeyStore;
import com.gip.xyna.xfmg.xfctrl.keymgmt.JavaSecurityStoreType;
import com.gip.xyna.xfmg.xfctrl.keymgmt.KeyStoreStorable;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.update.outdatedclasses_8_2_1_16.ExternalFileKeyStoreStorable;
import com.gip.xyna.update.utils.StorableUpdater;


public class UpdateKeyStoreMgmtStorable extends UpdateJustVersion {
  

  public UpdateKeyStoreMgmtStorable(Version oldVersion, Version newVersion) {
    super(oldVersion, newVersion);
    setExecutionTime(ExecutionTime.endOfFactoryStart);
  }
  

  @Override
  protected void update() throws XynaException {
    StorableUpdater.update(ExternalFileKeyStoreStorable.class, // TODO move class to outdated
                           KeyStoreStorable.class,
                           new GeneralizeKeyStoreStorable(),
                           ODSConnectionType.HISTORY);
  }
  
  
  private static class GeneralizeKeyStoreStorable implements Transformation<ExternalFileKeyStoreStorable, KeyStoreStorable> {
    
    public KeyStoreStorable transform(ExternalFileKeyStoreStorable from) {
      KeyStoreStorable to = new KeyStoreStorable(from.getName());
      to.setType(JavaSecurityStoreType.NAME);
      to.setVersion(from.getVersion());
      to.setFilename(from.getExtfilename());
      to.getParameter().add(JavaSecurityStoreType.FILE_TYPE.toNamedParameterObject(from.getFiletype()));
      return to;
    }
  }
  

}
