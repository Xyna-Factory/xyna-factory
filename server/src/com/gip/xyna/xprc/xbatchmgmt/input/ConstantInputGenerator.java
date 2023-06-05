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

import java.util.NoSuchElementException;

import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xprc.exceptions.XPRC_InputGeneratorInitializationException;


/**
 * Input-Generator, der immer den gleichen Input erzeugt.
 *
 */
public class ConstantInputGenerator extends InputGenerator {
  
  private String inputAsXml;
  private GeneralXynaObject input;
  private int startedInputs; //Anzahl bereits erzeugter Inputs
  
  public ConstantInputGenerator(String inputAsXml, int maximumInputs, Long revision) throws XPRC_InputGeneratorInitializationException {
    super(maximumInputs, revision);
    this.inputAsXml = inputAsXml;
    this.lastInputId = "0";
    this.startedInputs = 0;
    createInputFromXml();
  }
  
  
  @Override
  public boolean hasNext() {
    if( maximumInputs == 0 ) {
      //unbegrenzt viele startbar
      return true;
    } else {
      if (reusableInputIds.size() > 0) {
        //es sind noch wieder verwendbare Inputs vorhanden
        return true;
      }
      if( maximumInputs > startedInputs ) {
        //es dürfen noch neue Inputs ausgegeben werden
        return true;
      } else {
        //es gibt keine Inputs mehr
        return false;
      }
    }
  }

  @Override
  public Pair<String,GeneralXynaObject> next() {
    if (hasNext()) {
      //schon einmal ausgegebene, aber noch nicht verbrauchte Inputs noch einmal verwenden
      if (reusableInputIds.size() > 0) {
        String inputId = reusableInputIds.remove(0);
        return Pair.of(inputId, input);
      }
      
      lastInputId = String.valueOf(startedInputs);
      ++startedInputs;
      
      return Pair.of(lastInputId, input);
    } else {
      throw new NoSuchElementException();
    }
  }

  @Override
  public void changeRevision(Long revision) {
    this.revision = revision;
    //cache leeren
    input = null;
    try {
      createInputFromXml();
    } catch (XPRC_InputGeneratorInitializationException e) {
      throw new RuntimeException(e);
    }
  }

  private void createInputFromXml() throws XPRC_InputGeneratorInitializationException {
    if (inputAsXml == null || inputAsXml.length() == 0) {
      input = new Container();
    } else {
      try {
        input = XynaObject.generalFromXml(inputAsXml, revision);
      } catch (XynaException e) {
        //XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException
        throw new XPRC_InputGeneratorInitializationException(e);
      }
    }
  }

  public int getRemainingInputs () {
    if( maximumInputs == 0 ) {
      return -1;
    } else {
      return maximumInputs - startedInputs + reusableInputIds.size();
    }
  }
  
  public void setAlreadyStarted(int started) {
    startedInputs = started + reusableInputIds.size();
  }

}
