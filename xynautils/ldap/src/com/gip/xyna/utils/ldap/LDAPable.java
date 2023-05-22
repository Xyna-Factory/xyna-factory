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


package com.gip.xyna.utils.ldap;

import java.util.Hashtable;

/**
 * LDAPable: Dieses Objekt gibt es im LDAP
 */
public interface LDAPable {

  /** Liefert den Suchstring. Muss evtl. vorher intern gesetzt werden
   * @return DN-String 
   */
  public String getDN();
  
  /** Liefert einen Hashtable der im LDAP zu setzenden Attribute 
   * @return  Hashtable der im LDAP zu setzenden Attribute
   */
  public Hashtable getLDAPAttributes();

  /**
   * Damit setzt die LDAP-Klasse die gelesenen Werte
   * @param key
   * @param value
   */
  public void setValue(String key, String value);
 
  /**
   * Damit setzt die LDAP-Klasse die gelesenen Werte
   * @param key
   * @param value
   */
  public void setValue(String key, byte[] value);
  
  /**
   * Liefert eine neue Instanz des Objekts (fuer Listen-Abfrage)
   * @return
   */
  public LDAPable getInstance();
  
}


