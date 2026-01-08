/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2026 Xyna GmbH, Germany
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

package com.gip.xyna.xact.trigger.tlvencoding.database;



import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.gip.xyna.xact.trigger.tlvencoding.radius.RadiusEncoding;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;



public class RadiusOptionList {

  static List<RadiusEncoding> liste = new ArrayList<RadiusEncoding>();


  public static void main(String argh[]) throws PersistenceLayerException {
    RadiusOptionList.fillListe();
    LoadConfig anbindung = new LoadConfig();
    anbindung.setUp();
    anbindung.createListOfDHCPEncodingEntry(liste);
  }


  public static void fillListe() {
    // Optionen anlegen
    Map<String, String> valuearguments = new HashMap<String, String>();

    RadiusOptionList.liste.add(new RadiusEncoding(0, null, "User-Name", 1, null, "OctetString", valuearguments));
    valuearguments.clear();

    RadiusOptionList.liste.add(new RadiusEncoding(1, null, "User-Password", 2, null, "OctetString", valuearguments));
    valuearguments.clear();

    RadiusOptionList.liste.add(new RadiusEncoding(2, null, "CHAP-Password", 3, null, "OctetString", valuearguments));
    valuearguments.clear();
  }
}
