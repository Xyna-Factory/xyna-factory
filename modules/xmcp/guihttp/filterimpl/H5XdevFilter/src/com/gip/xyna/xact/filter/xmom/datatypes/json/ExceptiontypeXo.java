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
package com.gip.xyna.xact.filter.xmom.datatypes.json;

import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;

import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.gb.ObjectId;
import com.gip.xyna.xact.filter.session.gb.ObjectType;
import com.gip.xyna.xact.filter.xmom.workflows.enums.Tags;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xprc.xfractwfe.generation.ExceptionGeneration;

import xmcp.processmodeller.datatypes.ExceptionType;
import xmcp.processmodeller.datatypes.TextArea;
import xmcp.processmodeller.datatypes.datatypemodeller.ExceptionMessage;
import xmcp.processmodeller.datatypes.datatypemodeller.ExceptionMessagesArea;

public class ExceptiontypeXo extends DomOrExceptionXo { 

  
  
  private final Map<String, String> exceptionMessages;
  private final ExceptionGeneration exceptionGeneration;
  private boolean readonly = false;
  
  public ExceptiontypeXo(GenerationBaseObject gbo) {
    super(gbo);
    this.exceptionGeneration = gbo.getExceptionGeneration();
    exceptionMessages = exceptionGeneration.getExceptionEntry().getMessages();
  }

  @Override
  public GeneralXynaObject getXoRepresentation() {
    ExceptionType exceptionType = new ExceptionType();
    exceptionType.setReadonly(readonly);
    try {
      exceptionType.setRtc(com.gip.xyna.xact.filter.util.Utils.getModellerRtc(exceptionGeneration.getRevision()));
    } catch (XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY e) {
      // nothing
    }
    exceptionType.setFqn(exceptionGeneration.getOriginalFqName());
    exceptionType.setDeletable(false);
    exceptionType.setLabel(exceptionGeneration.getLabel());
    exceptionType.setId("dt");
    exceptionType.setIsAbstract(exceptionGeneration.isAbstract());
    
    exceptionType.addToAreas(createDataTypeTypeLabelArea());
    exceptionType.addToAreas(createExceptionMessagesArea());
    
    TextArea documentationArea = createDocumentationArea();
    documentationArea.setText(exceptionGeneration.getDocumentation());
    exceptionType.addToAreas(documentationArea);
    
    exceptionType.addToAreas(createInheritedVariablesArea());
    exceptionType.addToAreas(createMemberVariableArea());
    
    return exceptionType;
  }
  
  private ExceptionMessagesArea createExceptionMessagesArea() {
    ExceptionMessagesArea area = new ExceptionMessagesArea();
    area.setItemTypes(Collections.emptyList());
    area.setReadonly(false);
    area.setName(Tags.DATA_TYPE_EXCEPTION_MESSAGES_AREA);
    area.setId(ObjectId.createId(ObjectType.exceptionMessageArea));

    int i = 0;
    for (Entry<String, String> set : exceptionMessages.entrySet()) {
      ExceptionMessage em = new ExceptionMessage();
      em.setId(ObjectId.createId(ObjectType.exceptionMessage, String.valueOf(i)));
      em.setLanguage(set.getKey());
      em.setMessage(set.getValue());
      area.addToItems(em);
      i++;
    }
    return area;
  }

  
  public boolean isReadonly() {
    return readonly;
  }

  
  public void setReadonly(boolean readonly) {
    this.readonly = readonly;
  }
  
}
