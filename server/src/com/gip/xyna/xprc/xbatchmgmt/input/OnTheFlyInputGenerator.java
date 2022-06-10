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
package com.gip.xyna.xprc.xbatchmgmt.input;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.xmom.IFormula;
import com.gip.xyna.xnwh.persistence.xmom.QueryParameter;
import com.gip.xyna.xnwh.persistence.xmom.SelectionMask;
import com.gip.xyna.xnwh.persistence.xmom.SortCriterion;
import com.gip.xyna.xnwh.persistence.xmom.XMOMPersistenceManagement;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xprc.exceptions.XPRC_InputGeneratorInitializationException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;


public class OnTheFlyInputGenerator extends InputGenerator {

  private String storableName;
  private String idColumn;
  private String query;
  private SortCriterion[] sortCriteria;
  private volatile List<? extends XynaObject> inputs = new ArrayList<XynaObject>(); //noch nicht vergebene Inputs
  private String containerXML;
  private Container container;
  private int containerIndex;
  private int startedInputs; //Anzahl bereits erzeugter Inputs
  

  public OnTheFlyInputGenerator(String storableName, String query, String sortCriteria, String containerXML, int maximumInputs,
                                Long revision) throws XPRC_InputGeneratorInitializationException {
    super(maximumInputs, revision);
    this.startedInputs = 0;
    this.storableName = storableName;
    this.query = query;
    try {
      long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObjectOrParent(storableName, revision);
      XMOMStorableStructureInformation structuralInformation =
          XMOMStorableStructureCache.getInstance(rev).getStructuralInformation(GenerationBase.transformNameForJava(storableName));
      if (structuralInformation == null) {
        throw new XPRC_InputGeneratorInitializationException(new IllegalArgumentException("Storable <" + storableName + "> not found in revision " + revision + "."));
      }
      this.idColumn = structuralInformation.getColInfoByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.UNIQUE_IDENTIFIER).getVariableName();
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    createSortCriteria(sortCriteria);
    this.containerXML = containerXML;
    parseContainer(containerXML);
  }


  private void createSortCriteria(String sortCriteria) {
    int numberOfCriteria = 1; //auf jeden Fall nach idColumn sortieren
    
    //benutzerdefinierte Sortierkriterien
    if (sortCriteria != null && sortCriteria.length() > 0) {
      String[] criteria = sortCriteria.split(",");
      numberOfCriteria += criteria.length/2;
      this.sortCriteria = new SortCriterion[numberOfCriteria];
      for (int i=0; i<criteria.length/2; i++) {
        boolean reverse = "\"desc\"".equals(criteria[i*2+1].trim());
        SortCriterion criterion = new SortCriterion(criteria[i*2].trim(), reverse);
        this.sortCriteria[i] = criterion;
      }
    } else {
      this.sortCriteria = new SortCriterion[numberOfCriteria];
    }
    
    this.sortCriteria[numberOfCriteria-1] = new SortCriterion("%0%." + idColumn, false);
  }
  
  
  private XynaObject getInputById (String inputId) throws PersistenceLayerException {
    XMOMPersistenceManagement persistenceMgmt = XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    
    //select * from <storableName>
    SelectionMask selectionMask = new SelectionMask(storableName);
    QueryParameter queryParameter = new QueryParameter(1, false, null);
    
    //where <idColumn> = <id>
    IFormula formula = new GetInputById(inputId);
    
    List<? extends XynaObject> result = persistenceMgmt.query(null, selectionMask, formula, queryParameter, revision);
    
    return result.get(0);
  }

  
  @Override
  public boolean hasNext() throws PersistenceLayerException, InvalidObjectPathException {
    //wieviel müssen noch gestartet werden?
    int numberToBeStarted = maximumInputs == 0 ? Integer.MAX_VALUE : (maximumInputs-startedInputs);
    
    if (numberToBeStarted <= 0 ) {
      //Maximalzahl ist erreicht
      return false;
    }
    
    //es sind noch Daten vorhanden
    if (reusableInputIds.size() > 0 || inputs.size() > 0) {
      return true;
    }
    
    //versuche neue Daten zu holen
    XMOMPersistenceManagement persistenceMgmt = XynaFactory.getInstance().getXynaNetworkWarehouse().getXMOMPersistence().getXMOMPersistenceManagement();
    
    //select * from <storableName>
    SelectionMask selectionMask = new SelectionMask(storableName);

    //where <userDefinedQuery> and <idColumn> > <lastInputId>
    String lastInputQuery = createLastInputQuery();
    IFormula formula = new GetNextInputs(lastInputQuery);
    
    //maxRows: es müssen nur so viele Datensätze geholt werden, wie noch Slaves gestartet werden müssen.
    int maxRows = Math.min(XynaProperty.BATCH_INPUT_MAX_ROWS.get(), numberToBeStarted);
   
    QueryParameter queryParameter = new QueryParameter(maxRows, false, sortCriteria);
    inputs = persistenceMgmt.query(null, selectionMask, formula, queryParameter, revision);
    
    //es sind wieder neue Daten vorhanden
    if (inputs.size() > 0) {
      return true;
    }
    
    // keine Daten mehr vorhanden
    return false;
  }

  @Override
  public Pair<String, GeneralXynaObject> next() throws PersistenceLayerException, InvalidObjectPathException {
    if (hasNext()) {
      //schon einmal ausgegebene, aber noch nicht verbrauchte Inputs, noch einmal verwenden
      if (reusableInputIds.size() > 0) {
        String inputId = reusableInputIds.remove(0);
        return buildNext(inputId, getInputById(inputId) );
      }
      
      //nächsten neuen Input ausgeben
      XynaObject nextInput = inputs.remove(0);
      lastInputId = String.valueOf(nextInput.get(idColumn));

      ++startedInputs;
      
      return buildNext(lastInputId, nextInput );
    } else {
      throw new NoSuchElementException();
    }
  }
  
  private Pair<String, GeneralXynaObject> buildNext(String inputId, XynaObject input) {
    if( containerIndex >= 0 ) {
      Container c = container.clone();
      c.set(containerIndex, input);
      return new Pair<String, GeneralXynaObject>(inputId, c);
    }
    return new Pair<String, GeneralXynaObject>(inputId, input);
  }

  @Override
  public void changeRevision(Long revision) throws XPRC_InputGeneratorInitializationException {
    this.revision = revision;
    //cache leeren
    inputs = new ArrayList<XynaObject>();
    Logger.getLogger(OnTheFlyInputGenerator.class).debug("changeRevision emptied inputs");
    try {
      long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObjectOrParent(storableName, revision);
      XMOMStorableStructureInformation structuralInformation =
          XMOMStorableStructureCache.getInstance(rev).getStructuralInformation(GenerationBase.transformNameForJava(storableName));
      if (structuralInformation == null) {
        throw new XPRC_InputGeneratorInitializationException(new IllegalArgumentException("Storable <" + storableName + "> not found in revision " + revision + "."));
      }
      this.idColumn = structuralInformation.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER).getVariableName();
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
    parseContainer(containerXML);
  }

  private void parseContainer(String containerXML) throws XPRC_InputGeneratorInitializationException {
    containerIndex = -1;
    if( containerXML == null ) {
      return; //nichts zu tun
    }
    try {
      GeneralXynaObject xo = XynaObject.generalFromXml(containerXML, revision);
      container = (Container)xo;
      if (container == null) {
        return;
      }
      
    } catch( Exception e) {
      //XPRC_XmlParsingException, XPRC_InvalidXMLForObjectCreationException, XPRC_MDMObjectCreationException
      //ClassCastException
      throw new XPRC_InputGeneratorInitializationException(e);
    }
    for( int i =0; i<container.size(); ++i) {
      if( container.get(i) == null ) {
        containerIndex = i;
        break;
      }
    }
    if( containerIndex < 0 ) {
      //Keine freie Position, daher einfach hintenanhängen
      containerIndex = container.size();
      container.add(null);    
    }
  }

  /**
   * IFormula, um die nächsten Inputs zu selektieren.
   */
  public class GetNextInputs implements IFormula {

    private String lastInputQuery;
    
    public GetNextInputs(String lastInputQuery) {
      this.lastInputQuery = lastInputQuery;
    }

    public List<Accessor> getValues() {
      return Collections.emptyList();
    }

    public String getFormula() {
      StringBuilder sb = new StringBuilder();
      
      //benutzerdefinierte Query
      if (query != null && query.length() > 0) {
        sb.append("(").append(query).append(")");
      }
      //nur Werte größer als der letzte Input selektieren
      if (lastInputId != null) {
        if (sb.length() > 0) {
          sb.append(" && ");
        }
        
        sb.append("(").append(lastInputQuery).append(")");
      }
      return sb.toString();
    }
  }
  
  


  private String createLastInputQuery() throws PersistenceLayerException, InvalidObjectPathException {
    StringBuilder sb = new StringBuilder();
    
    if (lastInputId != null) {
      XynaObject lastinput = getInputById(lastInputId);
      
      for (int i=0; i<sortCriteria.length; i++) {
        sb.append("(");
        for (int j=0; j<=i; j++) {
          String column = sortCriteria[j].getCriterion();
          if (j < i) {
            sb.append(column).append(" == \"").append(lastinput.get(column.replace("%0%.", ""))).append("\" && ");
          } else {
            String op = sortCriteria[j].isReverse() ? " < " : " > "; //Sortierung beachten
            sb.append(column).append(op).append("\"").append(lastinput.get(column.replace("%0%.", ""))).append("\"");
          }
        }
        sb.append(i<sortCriteria.length-1 ? ") || " : ")");
      }
    }
    
    return sb.toString();
  }
  
  @Override
  public int getRemainingInputs() {
    //FIXME hier müsste ein select count(*) ausgeführt werden, geht derzeit nicht.... BUG 16141
    //deswegen wie im ConstantInputGenerator
    if( maximumInputs == 0 ) {
      return -1;
    } else {
      return maximumInputs - startedInputs + reusableInputIds.size();
    }
  }
  
  public void setAlreadyStarted(int started) {
    startedInputs = started + reusableInputIds.size();
  }
  
  
  /**
   * IFormula, um einen Input anhand der Id zu selektieren.
   */
  public class GetInputById implements IFormula {
    
    private String id;
    
    public GetInputById(String id) {
      super();
      this.id = id;
    }

    public List<Accessor> getValues() {
      return Collections.emptyList();
    }
    
    public String getFormula() {
      return "%0%."+ idColumn +" == \"" + id + "\"";
    }
  }
}
