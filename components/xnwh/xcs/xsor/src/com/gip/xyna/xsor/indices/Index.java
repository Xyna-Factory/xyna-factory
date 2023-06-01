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
package com.gip.xyna.xsor.indices;



public interface Index<O> {
  
  /**
   * Inserts a mapping of the generic object O to an internal int id. 
   * @param obj
   * @param internalId
   * @return true if a mapping for the key was created and false if it weren't.
   *         Depending o the Index-Implementation a false can mean the insertion was denied ({@link XSORPayloadPrimaryKeyIndex} or {@link UniqueIndex}
   */
  public boolean put(O obj, int internalId);
    
  /**
   * Returns all internal ids associated with the given object
   * @param obj
   * @return an array of internal ids, empty array if no ids are to be found for that object 
   * If the mapping from objects to ids is unique {@link UniqueKeyValueMappingIndex} the returning array will always contain a single element
   */
  public int[] get(O obj);
  
  /**
   * Removes the association of a given object with an internal id. If that's the only internal id associated with the object the mapping as a whole will be removed.
   * @param obj
   * @param internalId
   */
  public void delete(O obj, int internalId);
  
  /**
   * Returns all ids known to the index in no particular order
   * @return all internal ids
   */
  public int[] values();
  

  public void clear();

}
