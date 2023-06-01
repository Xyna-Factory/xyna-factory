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
package gip.base.common;

import java.util.Vector;


@SuppressWarnings("serial")
public class OBListDTO<O extends OBDTO> extends OBListObject<O> implements OBDTOInterface {

  // ---------------------------------------------------------------------------
  // -------- Konstruktor ----------------------------------------------------  
  // ---------------------------------------------------------------------------

  /** Standard-Konstruktor */
  public OBListDTO() {
    super();
  }

  public OBListDTO(Vector<O> newContent) {
    super(newContent);
  }
  
  public OBListDTO(O[] newContent) {
    super(newContent);
  }
  
  public OBListDTO(OBListObject<O> newContent) {
    super(newContent.getVector());
    totalLines = newContent.getTotalLines();
    firstLine = newContent.getFirstLine();
  }
  
  // ---------------------------------------------------------------------------
  // -------- Zu verwaltende Werte (der Form halber) ---------------------------  
  // ---------------------------------------------------------------------------

  private long capabilityId;
  public long getCapabilityId() { return capabilityId; }
  public void setCapabilityId(long _capabilityId) { this.capabilityId = _capabilityId;}

  /**
   * Liefert immer ein Objekt zu diesem Index. Ist die Liste kleiner, wird sie vergroessert.
   * @param index Nummer in der Liste
   * @return Element zum Index oder leeres Element
   */
  public O getNotNull(int index) {
    ensureIndex(index);
    return content.get(index);
  }
  
  /**
   * Stellt sicher, dass es den index gibt
   * @param index Nummer in der Liste
   */
  @SuppressWarnings("unchecked")
  protected void ensureIndex(int index) {
    while (index>=size()) {
      add((O) O.getInstance());
    }
  }
}
