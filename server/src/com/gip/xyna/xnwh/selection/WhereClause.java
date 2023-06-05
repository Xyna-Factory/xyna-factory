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

package com.gip.xyna.xnwh.selection;

import java.io.Serializable;
import java.util.List;

import com.gip.xyna.xnwh.exceptions.XNWH_InvalidSelectStatementException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;


public abstract class WhereClause<P extends WhereClausesContainerBase<P>> implements Serializable {

  private static final long serialVersionUID = 7551562694736724815L;
  private P parent;
  private String col;
  private boolean locked = false;
  
  public WhereClause(P parent, String col) {
    this.parent = parent;
    this.col = col;
  }
  
  public WhereClause<P> copy(String col) {
    //FIXME Implementierung in allen abgeleiteten Klassen bereitstellen
    throw new UnsupportedOperationException("copy not implemented");
  }
  
  public abstract String asString() throws XNWH_InvalidSelectStatementException;
  
  public abstract void addParameter(List<Object> list);
  
  protected  void checkLocked(String usage) throws XNWH_WhereClauseBuildException {
    if (locked) {
      throw new XNWH_WhereClauseBuildException(usage);
    }
    locked = true;
  }
  
  protected P getParent() {
    return parent;
  }
  
  public String getColumn() {
    return col;
  }

  public void changeParent(P parent2) {
    this.parent = parent2;
  }

}
