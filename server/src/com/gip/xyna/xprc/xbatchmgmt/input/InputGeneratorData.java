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
package com.gip.xyna.xprc.xbatchmgmt.input;

import java.io.Serializable;

import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.xprc.exceptions.XPRC_InputGeneratorInitializationException;
import com.gip.xyna.xprc.xbatchmgmt.storables.BatchProcessRestartInformationStorable;


public class InputGeneratorData implements Serializable {

  private static final long serialVersionUID = 1L;

  private final InputGeneratorType inputGeneratorType; //Typ des InputGenerators
  private String constantInput; //XML-Darstellung des konstanten Inputss 
  private String storable; //vollqualifizierter Name des Storables für den OnTheFly-InputGenerator 
  private String sortCriteria; //Sortierkriterien (bei OnTheFly)
  private String query; //benutzerdefinierte QueryCondition in XFL (bei OnTheFly)
  private int maximumInputs; //maximale Anzahl an Inputs, die der Generator erzeugen soll
  
  public enum InputGeneratorType implements StringSerializable<InputGeneratorType>{
    Constant() {
      public InputGenerator createInputGenerator(InputGeneratorData inputGeneratorData, Long revision) throws XPRC_InputGeneratorInitializationException {
        return new ConstantInputGenerator(inputGeneratorData.constantInput,
                                          inputGeneratorData.maximumInputs,
                                          revision);
      }
    },
    
    OnTheFly() {
      public InputGenerator createInputGenerator(InputGeneratorData inputGeneratorData, Long revision) throws XPRC_InputGeneratorInitializationException {
        return new OnTheFlyInputGenerator(inputGeneratorData.storable,
                                          inputGeneratorData.query,
                                          inputGeneratorData.sortCriteria,
                                          inputGeneratorData.constantInput,
                                          inputGeneratorData.maximumInputs,
                                          revision);
      }
    };

    public InputGeneratorType deserializeFromString(String string) {
      return InputGeneratorType.valueOf(string);
    }

    public String serializeToString() {
      return toString();
    }

    public abstract InputGenerator createInputGenerator(InputGeneratorData inputGeneratorData, Long revision) throws XPRC_InputGeneratorInitializationException;
  }
  
  
  public InputGeneratorData(InputGeneratorType inputGeneratorType) {
    if (inputGeneratorType == null) {
      throw new IllegalArgumentException("inputGeneratorType must not be null");
    }
    
    this.inputGeneratorType = inputGeneratorType;
  }

  public InputGeneratorData(BatchProcessRestartInformationStorable restartInformation) {
    this.inputGeneratorType = restartInformation.getInputGeneratorType();
    this.constantInput = restartInformation.getConstantInput();
    this.storable = restartInformation.getInputStorable();
    this.sortCriteria = restartInformation.getInputSortCriteria();
    this.query = restartInformation.getInputQuery();
    this.maximumInputs = restartInformation.getTotal();
  }
  
  public InputGeneratorType getInputGeneratorType() {
    return inputGeneratorType;
  }

  public String getConstantInput() {
    return constantInput;
  }

  public void setConstantInput(String constantInput) {
    this.constantInput = constantInput;
  }

  public String getStorable() {
    return storable;
  }

  public void setStorable(String storable) {
    this.storable = storable;
  }

  public String getSortCriteria() {
    return sortCriteria;
  }

  public void setSortCriteria(String sortCriteria) {
    this.sortCriteria = sortCriteria;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }
  
  public int getMaximumInputs() {
    return maximumInputs;
  }
  
  public void setMaximumInputs(int maximumInputs) {
    this.maximumInputs = maximumInputs;
  }
  
  public InputGenerator createInputGenerator(Long revision) throws XPRC_InputGeneratorInitializationException {
    return inputGeneratorType.createInputGenerator(this, revision);
  }

}
