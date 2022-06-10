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

package com.gip.xyna.xprc;

/**
 *@deprecated
 */
@Deprecated
public interface SuccessorFinder extends Finder {
  
  /**
   * wird bis zum ersten match aufgerufen, um aus der liste aller wartenden xynaorders den passenden successor zu
   * ermitteln. es werden nur die xynaorders untersucht, die einen predecessorfinder definiert haben. das matching gilt
   * nur dann als erfolgreich, falls es in beide richtung erfolgreich ist, d.h. ein predecessorfinder von xo muss auf die
   * xynaorder matchen bei der dieser successor definiert wurde
   * 
   * @param xo
   * @return
   */
  public boolean matchesSuccessor(XynaOrder xo);
  
}
