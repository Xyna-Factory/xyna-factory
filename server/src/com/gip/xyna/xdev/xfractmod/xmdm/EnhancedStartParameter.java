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
package com.gip.xyna.xdev.xfractmod.xmdm;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.misc.StringParameter;
import com.gip.xyna.xact.exceptions.XACT_InvalidStartParameterCountException;
import com.gip.xyna.xact.exceptions.XACT_InvalidTriggerStartParameterValueException;


/**
 * 
 */
public abstract class EnhancedStartParameter implements StartParameter {

  Logger logger = CentralFactoryLogging.getLogger(EnhancedStartParameter.class);
  
  public StartParameter build(String... args) throws XACT_InvalidStartParameterCountException,
      XACT_InvalidTriggerStartParameterValueException {
    throw new UnsupportedOperationException( "build(String... args) is not supported in EnhancedStartParameter");
  }

  public String[][] getParameterDescriptions() {
    return new String[][] { {"invalid call"} };
  }


  /**
   * Konveriert das alte Format Value-Liste in das neue Format Key-Value-Paar-Liste
   * @param params
   * @return
   * @throws XACT_InvalidStartParameterCountException
   * @throws XACT_InvalidTriggerStartParameterValueException
   */
  public abstract List<String> convertToNewParameters(List<String> params) throws XACT_InvalidStartParameterCountException,
      XACT_InvalidTriggerStartParameterValueException;

  /**
   * Liefert alle StringParameter, die für die StartParameter möglich sind
   * @return
   */
  public abstract List<StringParameter<?>> getAllStringParameters();

  /**
   * Baut die StartParameter-Instanz aus den bereits über die StringParameter validierten Daten
   * @param paramMap
   * @return
   * @throws XACT_InvalidTriggerStartParameterValueException
   */
  public abstract StartParameter build(Map<String,Object> paramMap) throws XACT_InvalidTriggerStartParameterValueException;
  
}
