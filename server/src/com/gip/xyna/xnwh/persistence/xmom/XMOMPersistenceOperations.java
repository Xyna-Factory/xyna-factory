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
package com.gip.xyna.xnwh.persistence.xmom;

import java.util.List;

import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.XynaOrderServerExtension;


public interface XMOMPersistenceOperations {
  
  /**
   * @param selectionMask welche spalten sollen selektiert werden?
   * @param formula XFL ausdruck, welcher mit %0% den in selectionmask definierten resulttyp referenziert und mit %i% auf accessors referenziert.
   * @param queryParameter optionen
   * @param revision
   * @return
   * @throws PersistenceLayerException
   */
  public List<? extends XynaObject> query(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter, Long revision)
                  throws PersistenceLayerException;
  
  public List<? extends XynaObject> query(final XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter,
                                          Long revision, ExtendedParameter extendedParameter, final XMOMStorableStructureInformation info)
                  throws PersistenceLayerException;
  
  public int count(final XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter,
                   Long revision, ExtendedParameter extendedParameter) throws PersistenceLayerException;
    
  public void store(XynaOrderServerExtension correlatedOrder, XynaObject storable, StoreParameter storeParameter)
                  throws PersistenceLayerException;
  
  public void delete(XynaOrderServerExtension correlatedOrder, XynaObject storable, DeleteParameter deleteParameter)
                  throws PersistenceLayerException;
  
  public List<? extends XynaObject> query(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter, Long revision, ExtendedParameter extendedParameter)
                  throws PersistenceLayerException;
  
  public void store(XynaOrderServerExtension correlatedOrder, XynaObject storable, StoreParameter storeParameter, ExtendedParameter extendedParameter)
                  throws PersistenceLayerException;
  
  public void delete(XynaOrderServerExtension correlatedOrder, XynaObject storable, DeleteParameter deleteParameter, ExtendedParameter extendedParameter)
                  throws PersistenceLayerException;
  
  public void update(XynaOrderServerExtension correlatedOrder, XynaObject storable, List<String> updatePaths, UpdateParameter updateParameter)
                  throws PersistenceLayerException;
  
  public void update(XynaOrderServerExtension correlatedOrder, XynaObject storable, List<String> updatePaths, UpdateParameter updateParameter, ExtendedParameter extendedParameter)
                  throws PersistenceLayerException;
}
