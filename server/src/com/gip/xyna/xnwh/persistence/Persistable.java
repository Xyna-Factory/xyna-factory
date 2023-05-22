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

package com.gip.xyna.xnwh.persistence;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.gip.xyna.xfmg.Constants;


/**
 * tabellen-information.
 */
@Target(value = {ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Persistable {

  public enum StorableProperty {

    /**
     * sch�tzt storables vor unn�tigen ausgaben im logfile und �ber das commandlineinterface, damit keine daten sichtbar
     * werden, die durch dieses storable gesch�tzt sein sollen. beispielsweise passw�rter.
     */
    PROTECTED, STORE_REDUNDANTLY;

    public boolean isProtected() {
      if (this == PROTECTED && Constants.PROTECTED_STORABLE_ENABLE) {
        return true;
      }
      return false;
    }


    public boolean storeRedundantly() {
      return this == STORE_REDUNDANTLY;
    }

  }


  public String tableName();
  /**
   * name der spalte die den primary key enth�lt
   * @return
   * @see com.gip.xyna.xnwh.persistence.Column
   */
  public String primaryKey();

  /**
   * 
   */
  public StorableProperty[] tableProperties() default {};
  
}
