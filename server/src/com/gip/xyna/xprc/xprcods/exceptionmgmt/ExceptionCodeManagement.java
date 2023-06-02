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
package com.gip.xyna.xprc.xprcods.exceptionmgmt;

import org.w3c.dom.Document;

import com.gip.xyna.utils.exceptions.exceptioncode.CodeGroupUnknownException;
import com.gip.xyna.utils.exceptions.exceptioncode.DuplicateCodeGroupException;
import com.gip.xyna.utils.exceptions.exceptioncode.InvalidPatternException;
import com.gip.xyna.utils.exceptions.exceptioncode.NoCodeAvailableException;
import com.gip.xyna.utils.exceptions.exceptioncode.OverlappingCodePatternException;
import com.gip.xyna.utils.exceptions.exceptioncode.ExceptionCodeManagement.CodeGroup;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


public interface ExceptionCodeManagement {

  public void init() throws PersistenceLayerException;

  public void checkExceptionCode(Document doc) throws CodeGroupUnknownException, NoCodeAvailableException, PersistenceLayerException;

  public CodeGroup[] listCodeGroups() throws PersistenceLayerException;

  public void addCodeGroup(String codeGroupName) throws DuplicateCodeGroupException, PersistenceLayerException;

  public void removeCodeGroup2(String codeGroupName) throws CodeGroupUnknownException, PersistenceLayerException;

  public void addCodePattern(String codeGroupName, String pattern, int startIndex, int endIndex, int padding) throws CodeGroupUnknownException, InvalidPatternException, OverlappingCodePatternException, PersistenceLayerException;

  public void removeCodePattern2(String codeGroupName, int patternIndex) throws CodeGroupUnknownException, PersistenceLayerException;

  public void exportToXml() throws PersistenceLayerException;

  public void importFromXml() throws PersistenceLayerException;

}
