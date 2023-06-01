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
package com.gip.xyna.utils.db;

import java.util.ArrayList;
import java.util.List;

import com.gip.xyna.utils.db.exception.UnexpectedParameterException;

/**
 * Parameterliste, in die Parameter mit Namen eingetragen werden können.
 * Dies ist bei längeren Insert-Statements praktisch.
 */
public class NamedParameter {

  private Parameter params;
  private List<String> names;

  /**
   * Anlegen einer leeren Parameterliste
   */
  public NamedParameter() {
    names = new ArrayList<String>();
    params = new Parameter();
  }

  /**
   * Übernahme einer bereits bestehenden Parameterliste
   */
  public NamedParameter(Parameter params) {
    names = new ArrayList<String>();
    this.params = params;
  }

  /**
   * Eintragen des Parameters name und Wert Param
   * @param name
   * @param param
   * @throws UnexpectedParameterException, weitergereicht von
   *         Parameter.addParameter(param)
   */
  public void add(String name, Object param) {
    names.add(name);
    params.addParameter(param);
  }

  /**
   * @return kommaseparierte Liste der Parameternamen
   */
  public String getNames() {
    return namesToString("");
  }

  /**
   * @return kommaseparierte Liste der Platzhalter "?"
   */
  public String getPlaceHolders() {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < names.size(); ++i) {
      sb.append("?,");
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
  }

  /**
   * @return gesammelte Parameter in der Parameter-Struktur
   */
  public Parameter getParameter() {
    return params;
  }

  /**
   * @return Rückgabe der Platzhalter für Aufruf einer StoredProcesdure in der Form
   * "name1=>?,name2=>?"
   */
  public String getNamedPlaceHoldersProcedure() {
    return namesToString("=>?");
  }

  /**
   * @return Rückgabe der Platzhalter für Aufruf eines Updates in der Form
  "name1=?,name2=?"
   */
  public String getNamedPlaceHoldersUpdate() {
    return namesToString("=?");
  }

  /**
   * Bau eines Strings, der alle Namen aus names enthält, an jeden Namen wird
   * appendix angehängt
   * @param appendix
   * @return
   */
  private String namesToString(String appendix) {
    StringBuilder sb = new StringBuilder();
    for (String name: names) {
      sb.append(name).append(appendix).append(",");
    }
    if (sb.length() > 0) {
      sb.deleteCharAt(sb.length() - 1);
    }
    return sb.toString();
  }
}
