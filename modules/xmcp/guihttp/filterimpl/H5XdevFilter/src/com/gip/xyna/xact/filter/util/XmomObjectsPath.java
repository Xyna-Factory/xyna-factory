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
package com.gip.xyna.xact.filter.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.ComparatorUtils;
import com.gip.xyna.utils.misc.JsonBuilder;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson;
import com.gip.xyna.xact.filter.json.ObjectIdentifierJson.Type;
import com.gip.xyna.xact.filter.session.FQName;
import com.gip.xyna.xact.filter.session.GenerationBaseObject;
import com.gip.xyna.xact.filter.session.XMOMLoader;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabase;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseEntryColumn;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.XMOMDatabaseType;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResult;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSearchResultEntry;
import com.gip.xyna.xfmg.xfctrl.xmomdatabase.search.XMOMDatabaseSelect;
import com.gip.xyna.xnwh.exceptions.XNWH_NoSelectGivenException;
import com.gip.xyna.xnwh.exceptions.XNWH_SelectParserException;
import com.gip.xyna.xnwh.exceptions.XNWH_WhereClauseBuildException;
import com.gip.xyna.xnwh.selection.parsing.ArchiveIdentifier;
import com.gip.xyna.xnwh.selection.parsing.SearchRequestBean;
import com.gip.xyna.xnwh.selection.parsing.SelectionParser;
import com.gip.xyna.xprc.exceptions.XPRC_OperationUnknownException;
import com.gip.xyna.xprc.xfractwfe.generation.Operation;

/**
 * Suche der ObjectIdentifierJson. 
 * 
 * Verwendet in /xmom/objects/<TypePath>, /xmom/datatypes/<TypePath>, /xmom/workflow/<TypePath>
 *
 */
public class XmomObjectsPath {

  private static final Logger logger = CentralFactoryLogging.getLogger(XmomObjectsPath.class);
  
  private final XMOMLoader xmomLoader;

  /**
   * Auftrennung: Default ist Suche direkt im Server.
   * Im Test ist beliebige andere Implementierung (beispielsweise über RMI) möglich.
   *
   */
  public interface Searcher {

    XMOMDatabaseSearchResult search(Long revision, RuntimeContext runtimeContext, List<XMOMDatabaseSelect> selects) throws XynaException;
    
  }
   
  private String listLabel;
  private Searcher searcher;
  private List<ObjectIdentifierJson> objects;
  private EnumSet<Type> filterTypes = EnumSet.allOf(Type.class) ;
  
  public XmomObjectsPath(XMOMLoader xmomLoader, String listLabel) {
    this.xmomLoader = xmomLoader;
    this.listLabel = listLabel;
    this.searcher = new DefaultSearcherImpl();
  }
  
  public XmomObjectsPath(XMOMLoader xmomLoader, String listLabel, Searcher searcher) {
    this.xmomLoader = xmomLoader;
    this.listLabel = listLabel;
    this.searcher = searcher;
  }

  public void filterTypes(Type ... values) {
    this.filterTypes = EnumSet.copyOf( Arrays.asList(values) );
  }
  
  public void search(Long revision, RuntimeContext runtimeContext, String path, SortType sortType) throws XynaException {
    search(revision, runtimeContext, path, null, sortType);
  }

  public void search(Long revision, RuntimeContext runtimeContext, String path, String name, SortType sortType) throws XynaException {
    objects = new ArrayList<>();
    List<XMOMDatabaseSelect> selects = createSelects(path, name);
    
    XMOMDatabaseSearchResult result = searcher.search(revision,runtimeContext, selects);
    List<XMOMDatabaseSearchResultEntry> resultEntries = result.getResult();
    
    for (int i = 0; i < resultEntries.size(); i++) {
      XMOMDatabaseSearchResultEntry e = resultEntries.get(i);
      Type type = Type.of( e.getType() );
      if( type == null ) {
        logger.info("Skipped "+e.getFqName()+": "+e.getType());
        continue;
      }
      ObjectIdentifierJson identifierJson;
      if((i > 0 && resultEntries.get(i-1).getFqName().equals(e.getFqName())) 
              || (i < resultEntries.size()-1 && resultEntries.get(i+1).getFqName().equals(e.getFqName()))) {
        identifierJson = new ObjectIdentifierJson(runtimeContext, e, true);
      }
      else identifierJson = new ObjectIdentifierJson(runtimeContext, e, false);
      
      if(Type.codedService == identifierJson.getType()) {
        String dataTypeFqn = identifierJson.getFQName().getTypePath() + "." + identifierJson.getFQName().getTypeName();
        GenerationBaseObject gbo = xmomLoader.loadNoCacheChange(new FQName(runtimeContext, dataTypeFqn));
        identifierJson.setDataTypeLabel(gbo.getDOM().getLabel());
        try {
          Operation operation = gbo.getDOM().getOperationByName(identifierJson.getFQName().getOperation());
          if(!operation.isStatic()) {
            identifierJson.setType(Type.instanceService);
          }
        } catch(XPRC_OperationUnknownException exception) {
          if(logger.isWarnEnabled()) {
            logger.warn("XMOMDatabaseSearchResult inconsistent with DOM: " + gbo.getDOM().getOriginalFqName() + " - service: " + identifierJson.getFQName().getOperation());
          }
          continue;
        } catch (Exception ex) {
          if(logger.isWarnEnabled()) {
            logger.warn("Error occurred during receiving operations: - message: " + ex.getMessage() + " - FqName: " + gbo.getDOM().getOriginalFqName() + " - service: " + identifierJson.getFQName().getOperation());
          }
          continue;
        }
      }
      if(Type.serviceGroup == identifierJson.getType()) {
        String dataTypeFqn = identifierJson.getFQName().getTypePath() + "." + identifierJson.getFQName().getTypeName();
        GenerationBaseObject gbo = xmomLoader.loadNoCacheChange(new FQName(runtimeContext, dataTypeFqn));
        identifierJson.setDataTypeLabel(gbo.getDOM().getLabel());
        
        List<Operation> operations = gbo.getDOM().getOperations();
        boolean hasStaticOperations = false;
        for (Operation operation : operations) {
          if(operation.isStatic()) {
            hasStaticOperations = true;
            break;
          }
        }
        if(!hasStaticOperations && !gbo.getDOM().isServiceGroupOnly()) {
          continue;
        }
      }
      objects.add(identifierJson);
    }
    
    try {
      Collections.sort(objects, sortType.getComparator() );
    } catch (Exception e) {
      Utils.logError("Sorting of documents under path " + path + " failed! Falling back to returning unsorted list.", e);
    }
  }

  private List<XMOMDatabaseSelect> createSelects(String path, String name) throws XNWH_NoSelectGivenException, XNWH_WhereClauseBuildException, XNWH_SelectParserException {
    List<XMOMDatabaseSelect> selects = new ArrayList<>();
    SearchRequestBean srb = new SearchRequestBean();
    srb.setArchiveIdentifier(ArchiveIdentifier.xmomcache);
    srb.setMaxRows(330);
    srb.setSelection(XMOMDatabaseEntryColumn.CASE_SENSITIVE_LABEL.getColumnName()+","+XMOMDatabaseEntryColumn.NAME.getColumnName()+","+XMOMDatabaseEntryColumn.PATH.getColumnName()+","+XMOMDatabaseEntryColumn.REVISION.getColumnName());
    srb.addFilterEntry(XMOMDatabaseEntryColumn.PATH.getColumnName(), path);
    if (path != null) {
      srb.addFilterEntry(XMOMDatabaseEntryColumn.NAME.getColumnName(), name);
    }

    XMOMDatabaseSelect select = (XMOMDatabaseSelect) SelectionParser.generateSelectObjectFromSearchRequestBean(srb);

    for( Type t : filterTypes ) {
      switch( t ) {
      case codedService:
        select.addDesiredResultTypes(XMOMDatabaseType.OPERATION);
        select.addDesiredResultTypes(XMOMDatabaseType.SERVICEGROUP);
        break;
      case dataType:
        select.addDesiredResultTypes(XMOMDatabaseType.DATATYPE);
        break;
      case exceptionType:
        select.addDesiredResultTypes(XMOMDatabaseType.EXCEPTION);
        break;
      case workflow:
        select.addDesiredResultTypes(XMOMDatabaseType.WORKFLOW);
        break;
      default:
        break;
      }
    }
    selects.add( select );
    return selects;
  }
  
  public String toJson() {
    JsonBuilder jb = new JsonBuilder();
    jb.startObject();{
      jb.addObjectListAttribute(listLabel, objects);
    } jb.endObject();
    
    return jb.toString();
  }
  
  public static class DefaultSearcherImpl implements Searcher {

    public XMOMDatabaseSearchResult search(Long revision,
        RuntimeContext runtimeContext, List<XMOMDatabaseSelect> selects) throws XynaException {
      XMOMDatabase xmomDB = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getXMOMDatabase();
      return xmomDB.searchXMOMDatabase(selects, -1, revision);
    }
    
    
  }

  
  public List<ObjectIdentifierJson> getObjects() {
    return objects;
  }

  
  public enum SortType {
    typeAware {
      @Override
      public Comparator<ObjectIdentifierJson> getComparator() {
        return new Comparator<ObjectIdentifierJson>() {
          
          @Override
          public int compare(ObjectIdentifierJson o1, ObjectIdentifierJson o2) {
            return getSortString(o1).compareToIgnoreCase(getSortString(o2));
          }
          
          private String getSortString(ObjectIdentifierJson o) {
            if(o == null) {
              return "";
            }
            StringBuilder sb = new StringBuilder();
            sb.append(getTypeSortString(o.getType())).append("-");
            switch(o.getType()) {
              case instanceService:
              case codedService:
                sb.append(o.getDataTypeLabel())
                  .append("-")
                  .append(o.getLabel());
                break;
              default:
                sb.append(o.getLabel());
                break;
            }
            return sb.toString();
          }
          
          private String getTypeSortString(Type t) {
            switch(t) {
              case dataType:
              case instanceService:
                return "A";
              case exceptionType:
                return "B";
              case serviceGroup:
              case codedService:
                return "C";
              case workflow:
                return "D";
              default :
                return "ZZZ";
            }
          }
        };
      }
    },
    alphabetical {
      @Override
      public Comparator<ObjectIdentifierJson> getComparator() {
        return (o1, o2) -> {
          String c1 = o1.getLabel();
          String c2 = o2.getLabel();
          if(c1 != null && c2 != null) {
            return c1.compareToIgnoreCase(c2);
          } else {
            return ComparatorUtils.compareNullAware(c1, c2, false);
          }
        };
      }
    },
    old {
      @Override
      public Comparator<ObjectIdentifierJson> getComparator() {
        return (o1, o2) -> {
          if( o1.getType() == Type.codedService || o2.getType() == Type.codedService ) {
            if( o1.getType() == Type.codedService && o2.getType() == Type.codedService) {
              return ComparatorUtils.chain().
                compareNullAware(o1.getLabel(), o2.getLabel(), false).
                compareNullAware(o1.getFQName().getOperation(), o2.getFQName().getOperation(), true).
                result();
            } else {
              int cmp = ComparatorUtils.compareNullAware(o1.getLabel(), o2.getLabel(), false);
              if( cmp == 0 ) {
                cmp = o1.getType() == Type.codedService ? 1 : -1;
              }
              return cmp;
            }
          } else {
            return ComparatorUtils.compareNullAware(o1.getLabel(), o2.getLabel(), false);
          }
        };
      }
    };
    
    public abstract Comparator<ObjectIdentifierJson> getComparator();
  }

}
