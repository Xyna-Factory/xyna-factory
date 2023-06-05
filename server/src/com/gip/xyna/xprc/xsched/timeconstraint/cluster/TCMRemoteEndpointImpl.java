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
package com.gip.xyna.xprc.xsched.timeconstraint.cluster;

import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowNotFoundInDatabaseException;
import com.gip.xyna.xprc.exceptions.XPRC_TimeWindowStillUsedException;
import com.gip.xyna.xprc.xsched.timeconstraint.windows.TimeConstraintWindowDefinition;


/**
 *
 */
public class TCMRemoteEndpointImpl implements TCMInterface {

  TCMLocalImpl localImpl;
  
  public TCMRemoteEndpointImpl(TCMLocalImpl localImpl) {
    this.localImpl = localImpl;
  }
    
  public void activateTimeWindow(String name) throws PersistenceLayerException,
  XPRC_TimeWindowNotFoundInDatabaseException {
    localImpl.activateTimeWindow(name); //FIXME hier tryLock in allTimeConstraintWindows  
  }
  
  public void activateTimeWindow(TimeConstraintWindowDefinition definition) {
    localImpl.activateTimeWindow(definition); //FIXME hier tryLock in allTimeConstraintWindows  
  }

  public void deactivateTimeWindow(String name, boolean force) throws XPRC_TimeWindowStillUsedException {
    localImpl.deactivateTimeWindow(name,force);
  }
  
  public void undoDeactivateTimeWindow(String name) throws PersistenceLayerException, XPRC_TimeWindowNotFoundInDatabaseException {
    localImpl.undoDeactivateTimeWindow(name);
  }

}
