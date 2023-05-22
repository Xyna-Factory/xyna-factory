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

/**
 * beschreibung eines composite indexes. von {@link CompositeIndices} verwendet
 */
@Target(value={ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface CompositeIndex {

  public IndexTypeComposite type();
  
  /**
   * beschreibt die zu indizierenden spalten oder den ausdruck dar�ber. leerzeichen sind nicht notwendig.
   * 
   * syntax: spaltennamen so wie sie in {@link Column} angegeben werden
   * value := &lt;spalte&gt; [, &lt;spalte&gt;]* | MAX(&lt;spalte&gt; [, &lt;spalte&gt;]* )
   * 
   * Beispiel f�r Abfragen der art
   * "where superpoolid = ? AND binding = ? AND expirationtime &lt; ? AND reservationtime &lt; ?", wobei die
   * beiden zeiten den gleichen parameter haben m�ssen.
   * 
   * value := "superpoolid,binding,MAX(expirationtime,reservationtime)"
   * 
   * @return
   */
  public String value();

}
