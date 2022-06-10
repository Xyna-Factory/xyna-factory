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
package com.gip.xyna.xfmg.xfctrl.datamodelmgmt.selectdatamodel;

import com.gip.xyna.xnwh.selection.WhereClauseString;
import com.gip.xyna.xnwh.selection.WhereClausesContainerBase;

public interface DataModelWhereClausesContainer<I extends WhereClausesContainerBase<I>> extends WhereClausesContainerBase<I> {

  public WhereClauseString<I> whereFqName();
  
  public WhereClauseString<I> whereLabel();
  
  public WhereClauseString<I> whereDataModelType();
  
  public WhereClauseString<I> whereBaseFqName();
  
  public WhereClauseString<I> whereBaseLabel();
  
  public WhereClauseString<I> whereVersion();
  
  public WhereClauseString<I> whereDocumentation();
  
}
