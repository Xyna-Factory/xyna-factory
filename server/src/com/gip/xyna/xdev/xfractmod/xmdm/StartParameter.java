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
package com.gip.xyna.xdev.xfractmod.xmdm;

import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;

public interface StartParameter {

  public StartParameter build(String... args) throws XACT_InvalidStartParameterCountException, XACT_InvalidTriggerStartParameterValueException;


  /**
   * Zur Dokumentation.
   * 
   * @return Liste der Startparameterbeschreibungen, sortiert nach m�glichen Kombinationen. Falls es die
   *         Startparameterm�glichkeiten (A,B) und (A,C,D) gibt, w�rde hier zur�ckgegeben werden: new String[][]{{"A",
   *         "B"}, {"A", "C", "D"}}.
   */
  public String[][] getParameterDescriptions();

}
