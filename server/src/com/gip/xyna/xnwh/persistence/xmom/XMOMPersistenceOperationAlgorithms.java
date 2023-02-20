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
package com.gip.xyna.xnwh.persistence.xmom;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.idgeneration.IDGenerator;
import com.gip.xyna.utils.collections.CollectionUtils;
import com.gip.xyna.utils.collections.LruCache;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.ConcurrentReentrantReadWriteLockMap;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectVisitor;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.classloading.ClassLoaderBase;
import com.gip.xyna.xfmg.xfctrl.classloading.MDMClassLoader;
import com.gip.xyna.xfmg.xfctrl.dependencies.RuntimeContextDependencyManagement;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_XMOMPersistenceMaxLengthValidationException;
import com.gip.xyna.xnwh.exceptions.XNWH_XMOMPersistenceValidationException;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter.BackwardReferenceHandling;
import com.gip.xyna.xnwh.persistence.xmom.DeleteParameter.ForwardReferenceHandling;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceAccessControl.AccessType;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceAccessControl.PersistenceAccessContext;
import com.gip.xyna.xnwh.persistence.xmom.PersistenceExpressionVisitors.CastCondition;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.QualifiedStorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.QueryGenerator.QueryPiplineElement;
import com.gip.xyna.xnwh.persistence.xmom.UpdateGenerator.UnfinishedUpdateStatement;
import com.gip.xyna.xnwh.persistence.xmom.UpdateGenerator.UpdateGeneration;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableColumnInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureIdentifier;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureInformation;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureRecursionFilter;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableStructureVisitor;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.StorableVariableType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.VarType;
import com.gip.xyna.xnwh.persistence.xmom.XMOMStorableStructureCache.XMOMStorableStructureInformation;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoException;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutableNoResult;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor;
import com.gip.xyna.xnwh.xclusteringservices.WarehouseRetryExecutor.WarehouseRetryExecutorBuilder;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidPackageNameException;
import com.gip.xyna.xprc.exceptions.XPRC_UnknownTransaction;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage;
import com.gip.xyna.xprc.xfractwfe.base.ChildOrderStorage.ChildOrderStorageStack;
import com.gip.xyna.xprc.xfractwfe.base.FractalProcessStep;
import com.gip.xyna.xprc.xfractwfe.base.GenericInputAsContextStep;
import com.gip.xyna.xprc.xfractwfe.generation.GenerationBase;
import com.gip.xyna.xprc.xfractwfe.generation.PersistenceTypeInformation;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.MandatoryRestriction;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.MaxLengthRestriction;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.RestrictionType;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.RestrictionUtils;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.Restrictions;
import com.gip.xyna.xprc.xfractwfe.generation.restriction.Utilizations;
import com.gip.xyna.xprc.xpce.transaction.TransactionAccess;
import com.gip.xyna.xprc.xpce.transaction.TransactionManagement;
import com.gip.xyna.xprc.xpce.transaction.odsconnection.ODSConnectionTransaction;


@SuppressWarnings({"unchecked", "rawtypes"})
public class XMOMPersistenceOperationAlgorithms implements XMOMPersistenceOperations {
  
  private final static FlatteningTransformator transformator = new FlatteningTransformator();
  private final static ConcurrentReentrantReadWriteLockMap concurrencyProtectionLocks = new ConcurrentReentrantReadWriteLockMap();
  public final static ThreadLocal<Boolean> allowReResolution = new ThreadLocal<Boolean>() {protected Boolean initialValue() {return false;}; };

  private final static Logger logger = CentralFactoryLogging.getLogger(XMOMPersistenceOperationAlgorithms.class);

  private IDGenerator idGen; 
  
  public XMOMPersistenceOperationAlgorithms() {
    try {
      idGen = IDGenerator.getInstance();
    } catch (XynaException e) {
      throw new RuntimeException(e);
    }
  }

  public List<? extends XynaObject> query(XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter, final Long revision) 
                  throws PersistenceLayerException {
    return query(correlatedOrder, selectionMask, formula, queryParameter, revision, new ExtendedParameter(ODSConnectionType.DEFAULT));
  }
  
  
  public List<? extends XynaObject> query(final XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter,
                                          Long revision, ExtendedParameter extendedParameter)
                  throws PersistenceLayerException {
    allowReResolution.set(true);
    try {
      final XMOMStorableStructureInformation info = getStorableStructureInformation(selectionMask.roottype, revision);
      // TODO sinnvollere Fehlermeldung hier erzeugen wenn info == null
      XMOMStorableStructureInformation mergedInfo = info.generateMergedClone(revision);
      return query(correlatedOrder, selectionMask, formula, queryParameter, revision, extendedParameter, mergedInfo);
    } finally {
      allowReResolution.remove();  
    }
  }
  
  private static class IFormulaTransformWrapper implements IFormula {
    private final List<Accessor> values;
    private final String formula;

    public IFormulaTransformWrapper(String formula, List<Accessor> values) {
      this.values = values;
      this.formula = formula;
    }

    public List<Accessor> getValues() {
      return values;
    }

    public String getFormula() {
      return formula;
    }
  }


  public List<? extends XynaObject> query(final XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter,
                                          Long revision, ExtendedParameter extendedParameter, final XMOMStorableStructureInformation info)
                  throws PersistenceLayerException {
    if (queryParameter.getMaxObjects() == 0) {
      return Collections.emptyList();
    }
    if (selectionMask.roottype == null || selectionMask.roottype.length() == 0) {
      throw new RuntimeException("Selection mask does not contain a root type.");
    }
    final PersistenceAccessContext context = PersistenceAccessControl.getAccessContext(correlatedOrder);
    if (info == null) {
      throw new RuntimeException("No registered type information for: " + selectionMask.roottype + ", either not a storable or not deployed.");
    }
    
    final QueryParameter queryParameterT = transformator.transformQueryParameter(queryParameter, info, revision);
    final SelectionMask selectionMaskT = transformator.transformSelectionMask(selectionMask, info, revision);
    IFormula formulaT = transformator.transformFormula(formula, info, revision);
    
    final IFormula adjustedFormula = adjustFormulaForRootType(selectionMask.roottype, revision, formulaT, queryParameterT);
    
    WarehouseRetryExecutorBuilder builder = 
      WarehouseRetryExecutor.buildExecutor(XMOMPersistenceManagement.DEADLOCK_RETRIES.get(),
                                           Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION)
                            .storable(info.getStorableClass());
    
    appendConnection(builder, correlatedOrder, Optional.of(extendedParameter.getOdsConnectionType()));
    
    final PersistenceConcurrencyContext perConCon = determineConcurrencyContext(builder);
    
    return executeProtected(Collections.emptySortedSet(), () -> builder.execute(new WarehouseRetryExecutableNoException<List<? extends XynaObject>>() {
    
      public List<? extends XynaObject> executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        try {
          return query(con, info, selectionMaskT.columns, adjustedFormula, false, queryParameterT, context);
        } catch (XNWH_RetryTransactionException e) {
          perConCon.handle(e);
          throw e;
        }
      }
      
    }), perConCon, AccessType.READ);
    
  }
  
  private static interface ConcurrencyProtectedCall<R> {
    R invoke() throws PersistenceLayerException;
  }
  
  private static <R> R executeProtected(SortedSet<StorableIdentifier> affected, ConcurrencyProtectedCall<R> call, PersistenceConcurrencyContext perConCon, AccessType type) throws PersistenceLayerException {
    perConCon.protect(affected, type);
    try {
      return call.invoke();
    } finally {
      perConCon.finish(affected, type);
    }
  }
  
  
  public class StorableIdentifier implements Comparable<StorableIdentifier> {

    private final String table;
    private final Object pk;
    
    StorableIdentifier(String table, Object pk) {
      this.table = table;
      this.pk = pk;
    }
    
    @Override
    public int hashCode() {
      return Objects.hash(table, pk);
    }
    
    @Override
    public boolean equals(Object obj) {
      if (obj == null ||
          !(obj instanceof StorableIdentifier)) {
        return false;
      }
      StorableIdentifier otherId = (StorableIdentifier) obj;
      return Objects.equals(table, otherId.table) && 
             Objects.equals(pk, otherId.pk);
    }

    @Override
    public int compareTo(StorableIdentifier o) {
      int comp = table.compareTo(o.table);
      if (comp == 0) {
        return String.valueOf(pk).compareTo(String.valueOf(o.pk));
      } else {
        return comp;        
      }
    }
    
  }
  

  private PersistenceConcurrencyContext determineConcurrencyContext(WarehouseRetryExecutorBuilder builder) {
    if (builder.hasConnection()) {
      return new ExternalConnectionContext();
    } else {
      return new LocalConnectionContext();
    }
  }


  private static void appendConnection(final WarehouseRetryExecutorBuilder builder, final XynaOrderServerExtension correlatedOrder,
                                       final Optional<ODSConnectionType> conType) {
    if (correlatedOrder == null) {
      builder.connection(conType.orElse(ODSConnectionType.DEFAULT));
    } else {
      TransactionManagement txMgmt = XynaFactory.getInstance().getProcessing().getXynaProcessCtrlExecution().getTransactionManagement();
      ChildOrderStorageStack childOrderStorageStack = ChildOrderStorage.childOrderStorageStack.get();
      if (txMgmt.numberOfOpenTransactions(childOrderStorageStack.getCorrelatedXynaOrder().getRootOrder().getId()) == 0) {
        builder.connection(conType.orElse(ODSConnectionType.DEFAULT));
      } else {
        checkForConnection(txMgmt, childOrderStorageStack.getCurrentlyExecutingStep()).ifPresentOrElse(c -> {
          builder.connection(c);
          if (conType.isPresent() && !conType.get().equals(c.getConnectionType())) {
            logger.warn("Context connection is opened for " + c.getConnectionType() + " but the operation parameter specified "
                + conType.get());
          }
        }, () -> builder.connection(conType.orElse(ODSConnectionType.DEFAULT)));
      }
    }
  }
  
  
  private static Optional<ODSConnection> checkForConnection(TransactionManagement txMgmt, final FractalProcessStep<?> executingStep) {
    Optional<GeneralXynaObject> contextValue =
      GenericInputAsContextStep.retrieveFromContext(executingStep, TransactionManagement.TRANSACTION_GENERIC_CONTEXT_IDENTIFIER); 
    
    if (contextValue.isEmpty()) {
      return Optional.empty();
    } else {
      TransactionAccess txAxs;
      try {
        txAxs = txMgmt.access((Long) contextValue.get().get("transactionId"));
      } catch (XPRC_UnknownTransaction | InvalidObjectPathException e) {
        logger.debug("checkForConnection error",e);
        return Optional.empty();
      }
      ODSConnectionTransaction tx = txAxs.getTypedTransaction(ODSConnectionTransaction.class);
      return Optional.of(tx.getConnection());
    }
  }
  
  
  private IFormula adjustFormulaForRootType(String roottype, Long revision, IFormula formulaT, QueryParameter queryParameterT) {
    XMOMStorableStructureInformation rootType = getStorableStructureInformation(roottype, revision);
    
    IFormula adjustedFormula;
    if (rootType.getSuperEntry() != null) {
      String formulaString = formulaT.getFormula();
      if (formulaT.getFormula() != null &&
          !formulaT.getFormula().isEmpty()) {
        formulaString = "(" + formulaString + ")";
        formulaString += " && (";  
      } else {
        formulaString = "";
      }
      formulaString += "typeof(%0%, \""+rootType.getFqClassNameForDatatype()+"\")";
      if (formulaT.getFormula() != null &&
          !formulaT.getFormula().isEmpty()) {
        formulaString += ")";
      }
      adjustedFormula = new IFormulaTransformWrapper(formulaString, formulaT.getValues());
      
      if (queryParameterT.getSortCriterions() != null &&
          queryParameterT.getSortCriterions().length > 0) {
        for (int i = 0; i < queryParameterT.getSortCriterions().length; i++) {
          queryParameterT.getSortCriterions()[i] = new SortCriterion(
            queryParameterT.getSortCriterions()[i].getCriterion().replaceAll("%0%", "%0%#cast(\""+rootType.getFqClassNameForDatatype()+"\")"),
            queryParameterT.getSortCriterions()[i].isReverse());
        }
      }
    } else {
      adjustedFormula = formulaT;
    }
    return adjustedFormula;
  }


  public int count(final XynaOrderServerExtension correlatedOrder, SelectionMask selectionMask, IFormula formula, QueryParameter queryParameter,
                   Long revision, ExtendedParameter extendedParameter) throws PersistenceLayerException {
    final PersistenceAccessContext context = PersistenceAccessControl.getAccessContext(correlatedOrder);
    final XMOMStorableStructureInformation info = getStorableStructureInformation(selectionMask.roottype, revision);
    if (info == null) {
      throw new RuntimeException("No registered type information for: " + selectionMask.roottype + ", either not a storable or not deployed.");
    }
    final XMOMStorableStructureInformation mergedInfo = info.generateMergedClone(revision);
    final QueryParameter queryParameterT = transformator.transformQueryParameter(queryParameter, mergedInfo, revision);
    IFormula formulaT = transformator.transformFormula(formula, mergedInfo, revision);
    
    final IFormula adjustedFormula = adjustFormulaForRootType(selectionMask.roottype, revision, formulaT, queryParameterT);
    
    WarehouseRetryExecutorBuilder builder = 
      WarehouseRetryExecutor.buildExecutor(XMOMPersistenceManagement.DEADLOCK_RETRIES.get(),
                                           Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION)
                            .storable(info.getStorableClass());
    
    appendConnection(builder, correlatedOrder, Optional.of(extendedParameter.getOdsConnectionType()));
    
    final PersistenceConcurrencyContext perConCon = determineConcurrencyContext(builder);
    
    return executeProtected(Collections.emptySortedSet(), () -> builder.execute(new WarehouseRetryExecutableNoException<Integer>() {
                                   
       public Integer executeAndCommit(ODSConnection con) throws PersistenceLayerException {
         QueryGenerator generator = getQueryGenerator();
         QueryPiplineElement qpe = generator.count(mergedInfo, adjustedFormula, queryParameterT, new Parameter(), context);
         try {
           List<Integer> countResult = executeQueryPipeline(con, qpe);
           return countResult.get(0);
         } catch (XNWH_RetryTransactionException e) {
           perConCon.handle(e);
           throw e;
         }
       }
       
     }), perConCon, AccessType.READ);
  }

  private List<? extends XynaObject> query(ODSConnection con, XMOMStorableStructureInformation info, List<String> columns,
                                           IFormula formula, boolean forUpdate, QueryParameter queryParameter, PersistenceAccessContext context)
      throws PersistenceLayerException {
    QueryGenerator generator = getQueryGenerator();
    QueryPiplineElement qpe = generator.parse(info, columns, formula, queryParameter, forUpdate, new Parameter(), context);
    List<? extends XynaObject> result = executeQueryPipeline(con, qpe);
    return restoreOrder(qpe, result);
  }
  
  
  private List executeQueryPipeline(ODSConnection con, QueryPiplineElement qpe) throws PersistenceLayerException {
    List result = Collections.emptyList();
    QueryPiplineElement currentQueryElement = qpe;
    while (currentQueryElement != null) {
      PreparedQuery pq = con.prepareQuery(new Query<>(currentQueryElement.getSqlString(), currentQueryElement.getReader(), currentQueryElement.getRootTableName()));
      result = con.query(pq, currentQueryElement.getParams(), currentQueryElement.getMaxObjects(), currentQueryElement.getReader());
      if (result == null || result.size() == 0) {
        return Collections.emptyList();
      }
      currentQueryElement = currentQueryElement.getNext(result);
    }
    return result;
  }
  
  
  public void store(XynaOrderServerExtension correlatedOrder, final XynaObject storable, final StoreParameter storeParameter) throws PersistenceLayerException {
    store(correlatedOrder, storable, storeParameter, new ExtendedParameter(ODSConnectionType.DEFAULT));
  }
  
  
  public void store(final XynaOrderServerExtension correlatedOrder, final XynaObject storable, final StoreParameter storeParameter,
                    ExtendedParameter extendedParameter) throws PersistenceLayerException {
    final PersistenceAccessContext context = PersistenceAccessControl.getAccessContext(correlatedOrder);
    final XMOMStorableStructureInformation info = getStorableStructureInformation(storable);
    final XMOMStorableStructureInformation correspondingInfo = info;
    
    WarehouseRetryExecutorBuilder builder = 
      WarehouseRetryExecutor.buildExecutor(XMOMPersistenceManagement.DEADLOCK_RETRIES.get(),
                                           Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION)
                            .storable(info.getStorableClass());
    
    appendConnection(builder, correlatedOrder, Optional.of(extendedParameter.getOdsConnectionType()));
    
    final PersistenceConcurrencyContext perConCon = determineConcurrencyContext(builder);

    WarehouseRetryExecutableNoResult wrenr =
      new WarehouseRetryExecutableNoResult() {
      
      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        try {
          while (true) {
            try {
              if (!storeParameter.keepMetaFieldData()) {
                adjustMetaFieldData(storable, correlatedOrder, correspondingInfo);
              } else {
                //will man hier validieren, dass alle objekte in der hierarchie identische historizationtimestamp und currentversion-flag besitzen?
                //oder darf der user sich hier auch objekt-abhängigkeiten speichern, die es ohne "keepMetaData" nicht geben könnte?
                // => da es keine folgefehler gibt, sondern eine solche inkonsistenz keine anderen features kaputt macht,
                //    ist das also erlaubt.
              }
              HistorizationInformation historizationInfo = EMPTY_HISTORIZATION_INFORMATION;
              if (storeParameter.doHistorizePreviousObject() && 
                  usesHistorizationAcrossHierarchy(correspondingInfo)) {
                historizationInfo = historize(con, storable, correspondingInfo, storeParameter.doForceRecursiveStore(), storeParameter.keepMetaFieldData(), perConCon);
              }
              PersistenceStoreContext storeContext = new PersistenceStoreContext(correspondingInfo);
              Storable<?> odsStorable = PersistenceAccessDelegator.transformDatatypeToStorable(storable, correspondingInfo);
              store(con, storable, odsStorable, odsStorable, correspondingInfo, historizationInfo, storeParameter.doForceRecursiveStore(), true, storeParameter.doHistorizePreviousObject(), storeContext, context);
              storeContext.store(con);
              perConCon.commit(con);
              return;
            } catch (HistorizationTimeStampCollision e) {
              perConCon.handle(con, e);
            }
          }
        } catch (XNWH_RetryTransactionException e) {
          perConCon.handle(e);
        }
      }
    };
    
    SortedSet<StorableIdentifier> affected = determineAffected(info, storable);
    
    executeProtected(affected, () -> {builder.execute(wrenr); return (Void)null;}, perConCon, AccessType.INSERT);
  }
  
  
  private boolean store(ODSConnection con, XynaObject storable, Storable<?> odsStorable, Storable<?> rootOdsStorable, StorableStructureInformation info,
                     HistorizationInformation historizationMap, boolean forceRecursive, boolean override, boolean historizePrevious,
                     PersistenceStoreContext storeContext, PersistenceAccessContext context) throws PersistenceLayerException {
    Storable<?> nextRootOdsStorable = rootOdsStorable;
    AccessType type = AccessType.INSERT;
    if (override) {
      if (info instanceof XMOMStorableStructureInformation) {
        /*
         * man muss aufpassen, dass man nicht ein objekt löscht, was von einem anderen thread geschrieben wurde, wenn wir vorher keine historisierung
         * machen konnten (objekt existierte zum zeitpunkt der historisierung noch nicht).
         */
        XMOMStorableStructureInformation xmomInfo = (XMOMStorableStructureInformation) info;
        if (!xmomInfo.usesHistorization() || !historizePrevious || historizationMap.presentInHistorization( xmomInfo, storable, false)) {
          //lösche die aktuelle version (currentversion oder history, je nachdem worauf man store sagt)
          XMOMStorableStructureInformation mergedClone = ((XMOMStorableStructureInformation) info).generateMergedClone(); 
          if (deletePreviousVersion(con, storable, mergedClone) != null) {
            type = AccessType.UPDATE;
          }
          //else objekt existiert nicht
        }
        /*
         * else:
         * es gibt historization-spalten UND doHistorize wurde angewiesen UND es gabs nichts zu historisieren
         */
      } 
      // else:
      // in einem vorherigen schritt der store-rekursion ist das delete bereits passiert, welches das aktuelle storable schon mitgelöscht hat
    } else if (alreadyExists(con, storable, info, historizationMap)) {
      return true;
    }
    if (info instanceof XMOMStorableStructureInformation) {
      nextRootOdsStorable = odsStorable;
      validateInfoAgainstCallingContext(context, type, (XMOMStorableStructureInformation) info);
      validate(storable);
    }
    Map<StorableColumnInformation, Object/*either List or Storable*/> transformedChildren = new HashMap<StorableColumnInformation, Object>();
    for (StorableColumnInformation column : info.getColumnInfoAcrossHierarchy()) {
      if (column.isStorableVariable()) {
        Object transformation = null;
        if (column.isList()) { // there are no referenced lists
          transformation = PersistenceAccessDelegator.transformDatatypeListToExpansionStorableList(getStorableVariableList(storable, column), odsStorable, nextRootOdsStorable, column.getStorableVariableInformation(), column.getPrimitiveType() != null);
        } else if (column.getStorableVariableType() == StorableVariableType.EXPANSION) {
          transformation = PersistenceAccessDelegator.transformDatatypeToExpansionStorable(getStorableVariable(storable, column), odsStorable, nextRootOdsStorable, column.getStorableVariableInformation());
        } else if (column.getStorableVariableType() == StorableVariableType.REFERENCE) {
          if (info.isSyntheticStorable()) {
            transformation = PersistenceAccessDelegator.transformDatatypeToReferencedStorable(storable, odsStorable, rootOdsStorable, column);
          } else {
            transformation = PersistenceAccessDelegator.transformDatatypeToReferencedStorable(getStorableVariable(storable, column), odsStorable, rootOdsStorable, column);
          }
        }
        if (transformation != null) {
          transformedChildren.put(column, transformation);
        }
      }
    }

    boolean alreadyContained = storeContext.add(info, odsStorable, storable);
    if (!alreadyContained) {
      for (Entry<StorableColumnInformation, Object> entry : transformedChildren.entrySet()) {
        if (entry.getKey().isList()) {
          List<? extends Storable> list = (List<? extends Storable>) entry.getValue();
          if (entry.getKey().getPrimitiveType() != null) {
            for (Storable primitiveStorable : list) {
              con.persistObject(primitiveStorable); // primitives won't satisfy the interfaces 'XynaObject storable' hence no recursive call
            }
          } else {
            List<? extends Object> xynaList = getStorableVariableList(storable, entry.getKey());
            int j=0;
            for (int i=0; i < xynaList.size(); i++) {
              if (xynaList.get(i) != null) {
                XynaObject childStorable = (XynaObject) xynaList.get(i);
                Storable<?> childOdsStorable = list.get(j);
                if (childOdsStorable != null) {
                  StorableStructureInformation ssi = resolveToTypeRecursivly(entry.getKey().getStorableVariableInformation(), childOdsStorable);
                  store(con, childStorable, childOdsStorable, nextRootOdsStorable, ssi, historizationMap, forceRecursive, forceRecursive, historizePrevious, storeContext, context);
                }
                j++;
              }
            }
          }
        } else {
          if (info.isSyntheticStorable()) {
            store(con, storable, (Storable<?>)entry.getValue(), nextRootOdsStorable, entry.getKey().getStorableVariableInformation(), historizationMap, forceRecursive, forceRecursive, historizePrevious, storeContext, context);
          } else {
            StorableStructureInformation ssi = resolveToTypeRecursivly(entry.getKey().getStorableVariableInformation(), (Storable<?>)entry.getValue());
            store(con, (XynaObject)getStorableVariable(storable, entry.getKey()), (Storable<?>)entry.getValue(), nextRootOdsStorable, ssi, historizationMap, forceRecursive, forceRecursive, historizePrevious, storeContext, context);
          }
        }
      }
    }
    return type == AccessType.UPDATE;
  }
  
  
  static StorableStructureInformation resolveToTypeRecursivly(StorableStructureInformation info, Storable<?> storable) {
    if (info.getFqClassNameForStorable().equals(storable.getClass().getName())) {
      return info;
    }
    if (info.getSubEntries() != null) {
      for (StorableStructureIdentifier ssi : info.getSubEntries()) {
        StorableStructureInformation ssInfo = resolveToTypeRecursivly(ssi.getInfo(), storable);
        if (ssInfo != null) {
          return ssInfo;
        }
      }
    }
    return null;
  }

  public void update(XynaOrderServerExtension correlatedOrder, XynaObject storable, List<String> updatePaths, UpdateParameter updateParameter) throws PersistenceLayerException {
    update(correlatedOrder, storable, updatePaths, updateParameter, new ExtendedParameter(ODSConnectionType.DEFAULT));
  }
  
  public void update(final XynaOrderServerExtension correlatedOrder, final XynaObject storable, List<String> untransformedUpdatePaths, final UpdateParameter updateParameter, ExtendedParameter extendedParameter) 
                  throws PersistenceLayerException {
    PersistenceAccessContext context = PersistenceAccessControl.getAccessContext(correlatedOrder);
    final XMOMStorableStructureInformation info = getStorableStructureInformation(storable);
    long revision;
    if (correlatedOrder == null) {
      revision = determineHighestRevisionFromXynaObject(storable);
    } else {
      revision = correlatedOrder.getRootOrder().getRevision();
    }
    final XMOMStorableStructureInformation mergedInfo = info.generateMergedClone(revision);
    
    List<String> updatePaths = transformator.transformPaths(untransformedUpdatePaths, mergedInfo, revision);
    
    final List<UpdateGeneration> updates = UpdateGenerator.parse(mergedInfo, adjustUpdatePaths(updatePaths));
    
    //wenn update historisierte objekte ändert, historizationtimestamp anpassen. -> entsprechende spalten mit in die updates aufnehmen
    final boolean usesHistorizationAcrossHierarchy = usesHistorizationAcrossHierarchy(mergedInfo);
    if (usesHistorizationAcrossHierarchy) {
      final Long timestamp = System.currentTimeMillis();
      final List<UpdateGeneration> historizationUpdates = new ArrayList<UpdateGeneration>();
      
      for (UpdateGeneration update : updates) {
        final UpdateGeneration preparedUpdate = update;
        mergedInfo.traverse(new QualifiedStorableDataVisitor(storable, preparedUpdate.getListIndizesForRootObject()) {

          @Override
          public void executeOnEnter(QualifiedStorableColumnInformation column, StorableStructureInformation current, XynaObject obj) {
            if (current instanceof XMOMStorableStructureInformation && ((XMOMStorableStructureInformation)current).usesHistorization() && obj != null) {
              StorableColumnInformation currentVersionColumn = current.getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG);
              StorableColumnInformation historizationColumn = current.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP);
              Boolean currentVersion = (Boolean) currentVersionColumn.getFromDatatype(obj);
              boolean updateHistorizationTimeStamp = false;
              if (currentVersion != null && !currentVersion) {
                //ntbd
              } else if (!updateParameter.keepMetaFieldData()) {
                //currentVersion == null || true. -> also true setzen
                if (currentVersion == null) {
                  currentVersionColumn.setInDatatype(obj, Boolean.TRUE);
                }
                updateHistorizationTimeStamp = true;
              }
              //falls keepmetafielddata=true, dann müssen die metafeldspalten explizit angegeben sein
              
              if (updateHistorizationTimeStamp) {
                boolean isXMOMStorableChangedByUpdates = false;
                List<Integer> listIdxs = new ArrayList<Integer>();
                if (column == null) {
                  isXMOMStorableChangedByUpdates = true;
                } else {
                  //falls referenzierte objekte existieren, muss man diese nur dann updaten, wenn auch in ihnen etwas anderes geupdated wird!
                  //FIXME referenzierte objekte können in der hierarchie mehrfach vorkommen, dann nur ein update durchführen
                  List<StorableColumnInformation> accessPathOfUpdate =
                      new ArrayList<StorableColumnInformation>(preparedUpdate.getUnfinishedUpdateStatement().getQualifiedColumn().getAccessPath());
                  List<StorableColumnInformation> accessPathOfColumn = new ArrayList<StorableColumnInformation>(column.getAccessPath());
                  accessPathOfUpdate.add(preparedUpdate.getUnfinishedUpdateStatement().getQualifiedColumn().getColumn());
                  accessPathOfColumn.add(column.getColumn());
                  if (QueryGenerator.covers(accessPathOfUpdate, accessPathOfColumn)) {
                    isXMOMStorableChangedByUpdates = true;
                    listIdxs = preparedUpdate.getListIndizesForRootObject();
                  }
                }
                if (isXMOMStorableChangedByUpdates) {
                  List<StorableColumnInformation> accessPath = new ArrayList<StorableColumnInformation>();
                  if (column != null) {
                    accessPath = new ArrayList<StorableColumnInformation>(column.getAccessPath());
                    accessPath.add(column.getColumn());
                  }
                   
                  historizationColumn.setInDatatype(obj, timestamp);
                  UnfinishedUpdateStatement updateStatement = UpdateGenerator.buildUpdate("", new QualifiedStorableColumnInformation(historizationColumn, accessPath), false);
                  UpdateGeneration update = new UpdateGeneration(updateStatement, listIdxs);
                  historizationUpdates.add(update);
                }
              }
            }
          }
        });
      }
      
      updates.addAll(UpdateGenerator.pruneDuplicatedUpdates(historizationUpdates));
    }
    
    for (UpdateGeneration update : updates) {
      context.checkAccess(AccessType.UPDATE, update.getUnfinishedUpdateStatement().getQualifiedColumn().getColumn());
    }
    
    WarehouseRetryExecutorBuilder builder =
    WarehouseRetryExecutor.buildExecutor(XMOMPersistenceManagement.DEADLOCK_RETRIES.get(),
                                         Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION)
                          .storable(info.getStorableClass());
  
  appendConnection(builder, correlatedOrder, Optional.of(extendedParameter.getOdsConnectionType()));
    
    final PersistenceConcurrencyContext perConCon = determineConcurrencyContext(builder);
    
    WarehouseRetryExecutableNoResult wrenr =
      new WarehouseRetryExecutableNoResult() {

        public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
          try {
            if (updateParameter.doHistorizePreviousObject() && usesHistorizationAcrossHierarchy) {
              historize(con, storable, info, false, updateParameter.keepMetaFieldData(), perConCon);
            }
            
            for (UpdateGeneration update : updates) {
              QualifiedStorableColumnInformation columnToUpdate = update.getUnfinishedUpdateStatement().getQualifiedColumn();
              String primaryKeyOfPossessingXMOMStorable = getPrimaryKeyOfFirstPossessingXMOMStorable(storable, info, columnToUpdate, update.getListIndizesForRootObject());
              Object value = getColumnFromDatatype(columnToUpdate, storable, update.getListIndizesForRootObject());
              Pair<String, Parameter> finishedStatement = update.getUnfinishedUpdateStatement().finish(primaryKeyOfPossessingXMOMStorable, value);
              PreparedCommand cmd = con.prepareCommand(new Command(finishedStatement.getFirst()));
              int modifiedRows = con.executeDML(cmd, finishedStatement.getSecond());
              if (modifiedRows <= 0) {
                ResultSetReader<Long> reader = new ResultSetReader<Long>() {
                  public Long read(ResultSet rs) throws SQLException {
                    return rs.getLong(1);
                  }
                };
                Pair<String, Parameter> existenceVerification = update.getUnfinishedUpdateStatement().getExistenceVerificationRequest(primaryKeyOfPossessingXMOMStorable);
                PreparedQuery<Long> query = con.prepareQuery(new Query<Long>(existenceVerification.getFirst(), reader));
                Long count = con.queryOneRow(query, existenceVerification.getSecond());
                if (count <= 0) {
                  throw new RuntimeException(new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(existenceVerification.getSecond().get(0)),
                                                                                       columnToUpdate.getColumn().getParentStorableInfo().getTableName()));
                }
              }
            }
            perConCon.commit(con);
          } catch (XNWH_RetryTransactionException e) {
            perConCon.handle(e);
          }
        }
      
    };
    
    executeProtected(Collections.emptySortedSet(), () -> {builder.execute(wrenr); return (Void)null;}, perConCon, AccessType.UPDATE);
  }
  
  private static Pattern VARIABLE_INDEX = Pattern.compile("%[0-9]+%");
  
  private List<String> adjustUpdatePaths(List<String> updatePaths) {
    List<String> paths = new ArrayList<>();
    for (String path : updatePaths) {
      StringBuilder pathBuilder = new StringBuilder();
      Matcher matcher = VARIABLE_INDEX.matcher(path);
      if (matcher.find()) {
        matcher.appendReplacement(pathBuilder, "%0%");
      }
      matcher.appendTail(pathBuilder);
      paths.add(pathBuilder.toString());
    }
    return paths;
  }

  private static Object getColumnFromDatatype(QualifiedStorableColumnInformation qualifiedColumn, XynaObject datatype, List<Integer> listIdx) {
    Iterator<Integer> listIdxIterator = listIdx.iterator();
    XynaObject current = datatype;
    for (int i = 0; i < qualifiedColumn.getAccessPath().size(); i++) {
      StorableColumnInformation column = qualifiedColumn.getAccessPath().get(i);
      if (!column.isStorableVariable()) {
        throw new RuntimeException("primitive child in accesspath " + column.getColumnName());
      }
      if (column.isList()) {
        // TODO check size
        current = (XynaObject) ((List) column.getFromDatatype(current)).get(listIdxIterator.next());
      } else {
        current = (XynaObject) column.getFromDatatype(current);
      }
    }
    Object value = qualifiedColumn.getColumn().getFromDatatype(current);
    if (qualifiedColumn.getColumn().isList() && value instanceof List && listIdxIterator.hasNext()) {
      return ((List)value).get(listIdxIterator.next());
    } else {
      return value;
    }
  }
  

  private static boolean equalUids(XynaObject storable, XynaObject versionToOverride, XMOMStorableStructureInformation info) {
    StorableColumnInformation uidColumn = info.getColInfoByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
    return uidColumn.getFromDatatype(storable).equals(uidColumn.getFromDatatype(versionToOverride));
  }
  
  
  private static boolean equalTimeStamps(XynaObject storable, XynaObject versionToOverride, XMOMStorableStructureInformation info) {
    if (info.usesHistorization()) {
      StorableColumnInformation histoColumn = info.getColInfoByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP);
      return histoColumn.getFromDatatype(storable).equals(histoColumn.getFromDatatype(versionToOverride));
    } else {
      return true;
    }
  }
  
  
  private static XynaObject getStorableVariable(XynaObject storable, StorableColumnInformation storableVariableColumn) {
    try {
      return (XynaObject) storableVariableColumn.getFromDatatype(storable);
    } catch (NullPointerException e) {
      //ntbd: das ist erwartet, wenn der pfad bei geflatteten variablen nicht auf dem contextroot get-bar ist, weil entsprechende
      //kinder null sind.
      logger.trace(null, e);
      return null;
    }
  }


  private static List<? extends Object> getStorableVariableList(XynaObject storable, StorableColumnInformation storableVariableColumn) {
    try {
      return (List<? extends Object>) storableVariableColumn.getFromDatatype(storable);
    } catch (NullPointerException e) {
      //ntbd: das ist erwartet, wenn der pfad bei geflatteten variablen nicht auf dem contextroot get-bar ist, weil entsprechende
      //kinder null sind.
      logger.trace(null, e);
      return null;
    }
  }

  
  private boolean alreadyExists(ODSConnection con, XynaObject storable, StorableStructureInformation info, HistorizationInformation historization) throws PersistenceLayerException {
    if (info instanceof XMOMStorableStructureInformation) {
      XMOMStorableStructureInformation xmomInfo = (XMOMStorableStructureInformation) info;
      if (historization.presentInHistorization(xmomInfo, storable, false)) {
        return true;
      } else {
        return contains(con, storable, xmomInfo);
      }
    } else {
      // assume we were deleted in a previous recursion on the parent
      return false;
    }
  }
  
  
  public void delete(final XynaOrderServerExtension correlatedOrder, final XynaObject storable, final DeleteParameter deleteParameter, ExtendedParameter extendedParameter)
                  throws PersistenceLayerException {
    final XMOMStorableStructureInformation info = getStorableStructureInformation(storable);
    long revision;
    if (correlatedOrder == null) {
      revision = determineHighestRevisionFromXynaObject(storable);
    } else {
      revision = correlatedOrder.getRootOrder().getRevision();
    }
    final XMOMStorableStructureInformation mergedInfo = info.generateMergedClone(revision);
    
    WarehouseRetryExecutorBuilder builder = 
      WarehouseRetryExecutor.buildExecutor(XMOMPersistenceManagement.DEADLOCK_RETRIES.get(),
                                           Constants.DEFAULT_NO_CONNECTION_AVAILABLE_RETRIES__USERINTERACTION)
                            .storable(info.getStorableClass());
    
    appendConnection(builder, correlatedOrder, Optional.of(extendedParameter.getOdsConnectionType()));
    
    final PersistenceConcurrencyContext perConCon = determineConcurrencyContext(builder);
    
    SortedSet<StorableIdentifier> affected = determineAffected(info, storable);
    
    executeProtected(affected, () -> {builder.execute(new WarehouseRetryExecutableNoResult() {
      
      public void executeAndCommit(ODSConnection con) throws PersistenceLayerException {
        try {
            delete(correlatedOrder, con, storable, mergedInfo, deleteParameter);
            perConCon.commit(con);
          } catch (XNWH_RetryTransactionException e) {
            perConCon.handle(e);
          }
        }
      }); return (Void)null;}, perConCon, AccessType.DELETE);
  }


  private SortedSet<StorableIdentifier> determineAffected(XMOMStorableStructureInformation info, XynaObject storable) {
    SortedSet<StorableIdentifier> affected = new TreeSet<>();
    info.traverse(new StorableDataVisitor(storable) {
      @Override
      public void executeOnEnter(StorableColumnInformation column, StorableStructureInformation current, XynaObject obj) {
        if (obj != null &&
            current instanceof XMOMStorableStructureInformation) {
          try {
            StorableColumnInformation pkCol = current.getSuperRootStorableInformation().getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
            if (pkCol != null) {
              affected.add(new StorableIdentifier(current.getTableName(), obj.get(pkCol.getVariableName())));
            }
          } catch (InvalidObjectPathException e) {
            throw new RuntimeException(e);
          }
        }
      }
    });
    return affected;
  }


  private long determineHighestRevisionFromXynaObject(XynaObject storable) {
    RuntimeContextDependencyManagement rcdm = 
      XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement();
    RevisionCollector revCol = new RevisionCollector();
    XynaObjectVisitor.visit(storable, revCol);
    Set<Long> allEncounteredRevisions = revCol.revisions;
    Set<Long> allPossibleRevisions = new HashSet<>();
    for (Long anEncounteredRevision : allEncounteredRevisions) {
      rcdm.getParentRevisionsRecursivly(anEncounteredRevision, allPossibleRevisions);
    }
    for (Long aPossibleRevision : allPossibleRevisions) {
      Set<Long> allCoveredRevisions = new HashSet<>();
      rcdm.getDependenciesRecursivly(aPossibleRevision, allCoveredRevisions);
      if (allCoveredRevisions.containsAll(allEncounteredRevisions)) {
        // we could collect those in case we encounter another even higher
        return aPossibleRevision;
      }
    }
    return ((ClassLoaderBase) storable.getClass().getClassLoader()).getRevision();
  }
  
  private static class RevisionCollector extends XynaObjectVisitor {

    private Set<Long> revisions = new HashSet<>();
    
    public void visitXynaObject(GeneralXynaObject xo) {
      ClassLoaderBase cl = (ClassLoaderBase) xo.getClass().getClassLoader();
      revisions.add(cl.getRevision());
    }
    
  }
  
  public void delete(XynaOrderServerExtension correlatedOrder, XynaObject storable, DeleteParameter deleteParameter) throws PersistenceLayerException {
    delete(correlatedOrder, storable, deleteParameter, new ExtendedParameter(ODSConnectionType.DEFAULT));
  }
  
  
  private HistorizationInformation historize(ODSConnection con, XynaObject storable, XMOMStorableStructureInformation info, boolean forceRecursive, boolean userGeneratedHistoStamp, PersistenceConcurrencyContext perConCon) throws PersistenceLayerException {
    HistorizationInformation historizationInfo = new HistorizationInformation();
    while (true) {
      try {
        XMOMStorableStructureInformation mergedClone = info.generateMergedClone();
        PersistenceStoreContext storeContext = new PersistenceStoreContext(mergedClone);
        historizeRecursivly(con, storable, mergedClone, historizationInfo, forceRecursive, userGeneratedHistoStamp, storeContext);
        storeContext.store(con);
        return historizationInfo;
      } catch (HistorizationTimeStampCollision e) {
        perConCon.handle(con, historizationInfo, e);
      }
    }
  }

  private void historizeRecursivly(ODSConnection con, XynaObject storable, final StorableStructureInformation info, HistorizationInformation historization,
                                   boolean forceRecursive, boolean userGeneratedHistoStamp, PersistenceStoreContext storeContext) throws PersistenceLayerException, HistorizationTimeStampCollision {
    StorableStructureInformation relevantInfo = info;
    if (info instanceof XMOMStorableStructureInformation) {
      XMOMStorableStructureInformation xmomInfo = (XMOMStorableStructureInformation) info;
      if (isCurrentVersion(storable, xmomInfo, userGeneratedHistoStamp)) {
        XynaObject outdatedVersion = queryOneRow(con, storable, xmomInfo, true);
        if (outdatedVersion != null &&
            !historization.presentInHistorization(xmomInfo, outdatedVersion, true)) {
          final Long newTimestamp = HistorizationInformation.discoverHighestTimeStamp(storable, xmomInfo);
          final Long outdatedTimeStamp = historization.discoverTimeStamp(outdatedVersion, xmomInfo);
          if (!userGeneratedHistoStamp && 
              newTimestamp.equals(outdatedTimeStamp)) {
            throw new HistorizationTimeStampCollision(xmomInfo, outdatedVersion);
          }
          XynaObject historizationOfOutdated = outdatedVersion.clone();
          xmomInfo.traverse(new StorableDataVisitor(historizationOfOutdated) {
            @Override
            public void executeOnEnter(StorableColumnInformation column, StorableStructureInformation current, XynaObject obj) {
              if (obj != null &&
                  current instanceof XMOMStorableStructureInformation && 
                  ((XMOMStorableStructureInformation) current).usesHistorization()) {
                current.getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG).setInDatatype(obj, Boolean.FALSE);
                if (outdatedTimeStamp != null && outdatedTimeStamp >= 0) {
                  current.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP).setInDatatype(obj, outdatedTimeStamp);
                }
              }
            }
          });
          Storable<?> odsStorable = PersistenceAccessDelegator.transformDatatypeToStorable(historizationOfOutdated, xmomInfo);
          XMOMStorableStructureInformation reResolved = XMOMStorableStructureCache.getInstance(xmomInfo.getDefiningRevision()).getStructuralInformation(xmomInfo.getFqXmlName());
          if (store(con, historizationOfOutdated, odsStorable, odsStorable, reResolved, historization, false, false, false, storeContext, PersistenceAccessControl.allAccess()) &&
              !userGeneratedHistoStamp) {
            throw new HistorizationTimeStampCollision(xmomInfo, historizationOfOutdated);
          }
          historization.addToHistorization(xmomInfo, historizationOfOutdated);
        }
      }
    }
    columns: for (StorableColumnInformation column : relevantInfo.getColumnInfo(false)) {
      if (column.isStorableVariable()) {
        if (column.isList()) {
          List<? extends Object> list = getStorableVariableList(storable, column);
          if (list != null) {
            for (int i=0; i < list.size(); i++) {
              StorableStructureInformation ssi = column.getStorableVariableInformation();
              if (ssi.isSynthetic) {
                StorableColumnInformation isReferenceCheck = ssi.getColInfoByVarType(VarType.REFERENCE_FORWARD_FK);
                if (isReferenceCheck != null) {
                  ssi = isReferenceCheck.getStorableVariableInformation();  
                } else {
                  continue columns;
                }
              }
              XynaObject childStorable = (XynaObject) getStorableVariableList(storable, column).get(i);
              if (doesQualifyForHistorization(con, childStorable, ssi, historization, forceRecursive)) {
                historizeRecursivly(con, childStorable, ssi, historization, forceRecursive, userGeneratedHistoStamp, storeContext);
              }
            }
          }
        } else {
          XynaObject childStorable = (XynaObject) getStorableVariable(storable, column);
          if (doesQualifyForHistorization(con, childStorable, column.getStorableVariableInformation(), historization, forceRecursive)) {
            historizeRecursivly(con, childStorable, column.getStorableVariableInformation(), historization, forceRecursive, userGeneratedHistoStamp, storeContext);
          }
        }
      }
    }
  }
  
  
  private boolean usesHistorizationAcrossHierarchy(XMOMStorableStructureInformation info) {
    final AtomicBoolean result = new AtomicBoolean(false);
    info.traverse(new StorableStructureVisitor() {
      
      public StorableStructureRecursionFilter getRecursionFilter() {
        return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
      }
    
      public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) { }
      
      public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
        if (current instanceof XMOMStorableStructureInformation && ((XMOMStorableStructureInformation)current).usesHistorization()) {
          result.compareAndSet(false, true);
        }
      }
    });
    return result.get();
  }
  
  
  private boolean isCurrentVersion(XynaObject obj, XMOMStorableStructureInformation info, boolean throwExceptionIfInHistory) {
    if (info.usesHistorization()) {
      Boolean b = (Boolean) info.getColInfoByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.CURRENTVERSION_FLAG).getFromDatatype(obj);
      if (Boolean.TRUE.equals(b)) {
        return true;
      } else if (throwExceptionIfInHistory) {
        //false und null nicht erlauben
        throw new RuntimeException("Historization in history not allowed.");
      } else {
        return false;
      }
    } else {
      return false;
    }
  }
  
  
  private boolean doesQualifyForHistorization(ODSConnection con, XynaObject storable, StorableStructureInformation info, HistorizationInformation historization, boolean forceRecursive) throws PersistenceLayerException {
    if (storable == null) {
      return false;
    }
    if (forceRecursive) {
      if (historization.presentInHistorization(info, storable, false)) {
        return false;
      } else {
        return true;
      }
    } else if (!alreadyExists(con, storable, info, historization)) {
      return true;
    } else {
      return false;
    }
  }
  
  
  private void delete(final XynaOrderServerExtension correlatedOrder, ODSConnection con, XynaObject storable, XMOMStorableStructureInformation info, DeleteParameter deleteParameter) throws PersistenceLayerException {
    PersistenceAccessContext context = PersistenceAccessControl.getAccessContext(correlatedOrder);
    delete(con, storable, info, deleteParameter, context);
  }
    
  private void delete(ODSConnection con, XynaObject storable, XMOMStorableStructureInformation info, DeleteParameter deleteParameter, PersistenceAccessContext context) throws PersistenceLayerException {
    // if !info.usesHistorization -> just query by uid
    // if info.usesHistorization:
    //   a) it's a request with currentVersion = true, delete current version and history if deleteParams say so
    //   b) it's a request with currentVersion = false and deleteParams say purge, delete complete history
    //   c) it's a request with currentVersion = false and deleteParams say !purge, historizationStamp has to be set and we'll delete only that version
    Parameter parameter = new Parameter();
    QueryGenerator generator = getQueryGenerator();
    List<String> columns = generateSelectionForDelete(info, deleteParameter);
    String formula = buildFormulaConditionForDelete(info, storable, deleteParameter);
    QueryPiplineElement currentQueryElement = generator.parse(info, columns, new ArgumentlessFormula(formula),
                                                              new QueryParameter(-1, deleteParameter.doIncludeHistory(), null),
                                                              false, parameter, PersistenceAccessControl.allAccess());
    List result = null;
    PrimaryKeyHierarchyReader finalReader = null;
    while (currentQueryElement != null) {
      ResultSetReader reader;
      if (currentQueryElement.hasNext()) {
        reader = currentQueryElement.getReader();
      } else {
        finalReader = new PrimaryKeyHierarchyReader(currentQueryElement.getSelectedColumns(), currentQueryElement.getAliasDictionary());
        reader = finalReader;
      }
      PreparedQuery pq = con.prepareQuery(new Query(currentQueryElement.getSqlString(), reader));
      result = con.query(pq, currentQueryElement.getParams(), currentQueryElement.getMaxObjects(), reader);
      if (result == null || result.size() == 0) {
        result = Collections.emptyList();
        break;
      }
      currentQueryElement = currentQueryElement.getNext(result);
    }
    
    Pair<Map<String, Set<Object>>, Map<String, Set<String>>> pkReaderResult;
    Map<String, Set<Object>> pkResult = Collections.emptyMap();
    Map<String, Set<String>> typeResult = Collections.emptyMap();
    if (finalReader != null) {
      pkReaderResult = finalReader.getResult();
      pkResult = pkReaderResult.getFirst();
      typeResult = pkReaderResult.getSecond();
    }
      
    
    // As the traversal is depth- and not breadth-first there might still be some 'collisions'
    // but as the structures get more complex those occurrences can't be prevented anyway
    //   collisions: backwardReferenceHandling retrieves an object that would already be a target of the current
    //               deletion iteration therefore performing the deletion twice
    List<StorableStructureInformation> deletionOrder = getStorableStructureHierarchyOrder(info);
    
    for (StorableStructureInformation structure : deletionOrder) {
      if (structure instanceof XMOMStorableStructureInformation &&
          (pkResult.get(structure.getTableName()) != null && pkResult.get(structure.getTableName()).size() > 0)) {
        Long revision = structure.getDefiningRevision();
        for (String typename : typeResult.get(structure.getTableName())) {
          if (allowReResolution.get()) {
            XMOMStorableStructureInformation reResolved = XMOMStorableStructureCache.getInstance(revision).getStructuralInformation(typename);
            validateInfoAgainstCallingContext(context, AccessType.DELETE, reResolved);
          } else {
            validateInfoAgainstCallingContext(context, AccessType.DELETE, (XMOMStorableStructureInformation) structure);
          }
        }
      }
    }
    deleteHierarchy(con, pkResult, deletionOrder, deleteParameter, context);
  }
  
  
  private XynaObject deleteOneRow(ODSConnection con, XynaObject storable, XMOMStorableStructureInformation info, DeleteParameter deleteParameter) throws PersistenceLayerException {
    List<? extends XynaObject> results = query(con, info, (List<String>)null, 
                                               new ArgumentlessFormula(buildFormulaConditionForDelete(info, storable, deleteParameter)),
                                               true, new QueryParameter(1, false, null), PersistenceAccessControl.allAccess()); // TODO can we use queryOneRow?
    switch (results.size()) {
      case 0 :
        return null;
      case 1 :
        break;
      default :
        throw new RuntimeException("Invalid size");
    }
    
    XynaObject toDelete = results.get(0);
    delete(con, toDelete, info, deleteParameter, PersistenceAccessControl.allAccess());
    return toDelete;
  }
  
  
  private XynaObject queryOneRow(ODSConnection con, XynaObject storable, XMOMStorableStructureInformation info, boolean forUpdate) throws PersistenceLayerException {
    List<? extends XynaObject> results = query(con, info, (List<String>)null,
                                               new ArgumentlessFormula(buildFormulaConditionByIdentity(info, storable)),
                                               forUpdate, new QueryParameter(1, false, null), PersistenceAccessControl.allAccess());
    if (results.size() <= 0) {
      return null; 
    } else {
      return results.get(0);
    }
  }
  
  
  private boolean contains(ODSConnection con, XynaObject xoStorable, XMOMStorableStructureInformation info) throws PersistenceLayerException {
    Storable<?> storable = PersistenceAccessDelegator.transformDatatypeToStorable(xoStorable, info);
    return con.containsObject(storable);
  }
  

  private void adjustMetaFieldData(final XynaObject storable, final XynaOrderServerExtension correlatedOrder,
                                   StorableStructureInformation info) {
    final Long timestamp = System.currentTimeMillis();
    info.traverse(new StorableDataVisitor(storable) {

      @Override
      public void executeOnEnter(StorableColumnInformation column, StorableStructureInformation current, XynaObject obj) {
        if (obj != null &&
            isWithinObjecHierarchy(obj, current)) {
          if (current instanceof XMOMStorableStructureInformation) { // && current instanceof obj
            StorableColumnInformation uidCol = current.getColInfoByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
            if (uidMustBeAutoGenerated(uidCol.getFromDatatype(obj))) {
              //TODO ids nicht global eindeutig sondern nur lokal eindeutig für diese tabelle erzeugen
              uidCol.setInDatatype(obj, transformType(String.valueOf(idGen.getUniqueId()), uidCol));
            }

            if (((XMOMStorableStructureInformation) current).usesHistorization()) {
              current.getColInfoByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP).setInDatatype(obj, timestamp);
              current.getColInfoByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.CURRENTVERSION_FLAG).setInDatatype(obj, Boolean.TRUE);
            }
          }
          List<StorableColumnInformation> custom0Vars = current.getColInfosByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.CUSTOMFIELD_0);
          for (StorableColumnInformation custom0Var : custom0Vars) {
            custom0Var.setInDatatype(obj, transformType(correlatedOrder.getCustom0(), custom0Var));
          }
          List<StorableColumnInformation> custom1Vars = current.getColInfosByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.CUSTOMFIELD_1);
          for (StorableColumnInformation custom1Var : custom1Vars) {
            custom1Var.setInDatatype(obj, transformType(correlatedOrder.getCustom1(), custom1Var));
          }
          List<StorableColumnInformation> custom2Vars = current.getColInfosByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.CUSTOMFIELD_2);
          for (StorableColumnInformation custom2Var : custom2Vars) {
            custom2Var.setInDatatype(obj, transformType(correlatedOrder.getCustom2(), custom2Var));
          }
          List<StorableColumnInformation> custom3Vars = current.getColInfosByPersistenceTypeAcrossHierachy(PersistenceTypeInformation.CUSTOMFIELD_3);
          for (StorableColumnInformation custom3Var : custom3Vars) {
            custom3Var.setInDatatype(obj, transformType(correlatedOrder.getCustom3(), custom3Var));
          }
          
        }
      }

      private boolean isWithinObjecHierarchy(XynaObject obj, StorableStructureInformation structure) {
        Set<String> xoTypes = new HashSet<>();
        Class<?> currentXoClass = obj.getClass();
        while (currentXoClass != null &&
               !XMOMPersistenceManagement.STORABLE_BASE_CLASS.equals(currentXoClass.getName())) { 
          xoTypes.add(currentXoClass.getName());
          currentXoClass = currentXoClass.getSuperclass();
        }
        return xoTypes.contains(structure.getFqClassNameForDatatype());
      }

      private boolean uidMustBeAutoGenerated(Object uidValue) {
        if (uidValue == null) {
          return true;
        }
        if (uidValue instanceof Number && ((Number)uidValue).longValue() == 0) {
          return true;
        }
        if (uidValue instanceof String && ((String) uidValue).length() == 0) {
          return true;
        }
        return false;
      }
    });
  }
  
  
  private void validate(final XynaObject storable) throws XNWH_XMOMPersistenceValidationException {
    final List<XNWH_XMOMPersistenceValidationException> validationFailures = new ArrayList<XNWH_XMOMPersistenceValidationException>();
    
    validateRecursivly(storable, false, validationFailures);
    
    if (validationFailures.size() > 0) {
      throw validationFailures.get(0);
    }
  }
  
  
  private static void validateRecursivly(GeneralXynaObject xo, boolean breakOnStorable, List<XNWH_XMOMPersistenceValidationException> validationFailures) {
    if (xo == null) {
      return;
    }
    if (breakOnStorable &&
        isStorable(xo)) {
      return;
    }
    Map<String, Restrictions>  restrictionsMap = RestrictionUtils.parseClass(xo.getClass());
    try {
      Method getVariableNamesMethod = xo.getClass().getDeclaredMethod("getVariableNames");
      Set<String> varNames = (Set<String>) getVariableNamesMethod.invoke(xo);
      for (String varName : varNames) {
        Object value = xo.get(varName);
        validateMandatoryRestriction(xo.getClass().getName(), varName, value, restrictionsMap, validationFailures);
        validateMaxLengthRestriction(xo.getClass().getName(), varName, value, restrictionsMap, validationFailures);
        if (value instanceof List) {
          for (Object listValue : (List)value) {
            if (listValue instanceof GeneralXynaObject) {
              validateRecursivly((GeneralXynaObject) listValue, true, validationFailures);
            }
          }
        } else if (value instanceof GeneralXynaObject) {
          validateRecursivly((GeneralXynaObject) value, true, validationFailures);
        } else {
          // ntbd
        }
      }
    } catch (NoSuchMethodException | SecurityException | IllegalAccessException |
             IllegalArgumentException | InvocationTargetException | InvalidObjectPathException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  private static void validateMaxLengthRestriction(String className, String varName, Object value,
                                                   Map<String, Restrictions> restrictionsMap,
                                                   List<XNWH_XMOMPersistenceValidationException> validationFailures) {
    if (value != null &&
        value instanceof String) {
      int maxLength;
      if (restrictionsMap.containsKey(varName)) {
        if (restrictionsMap.get(varName).hasApplicableRestriction(RestrictionType.MAX_LENGTH, Utilizations.XMOM_PERSISTENCE_STORE.getName())) {
          maxLength = restrictionsMap.get(varName).<MaxLengthRestriction>getRestriction(RestrictionType.MAX_LENGTH, Utilizations.XMOM_PERSISTENCE_STORE.getName()).getLimit();
        } else {
          return;
        }
      } else {
        maxLength = XynaProperty.DEFAULT_SIZE_COLUMN_TYPE.get();
      }
      if (((String)value).length() > maxLength) {
        validationFailures.add(new XNWH_XMOMPersistenceMaxLengthValidationException(className, varName, "MaxLength:" + maxLength, ((String)value).length()));
      }
    }
  }

  private static void validateMandatoryRestriction(String className, String varName, Object value, Map<String, Restrictions>  restrictionsMap,
                                                   List<XNWH_XMOMPersistenceValidationException> validationFailures) {
    if (value == null &&
        restrictionsMap.containsKey(varName)) {
      Restrictions restrictions = restrictionsMap.get(varName);
      if (restrictions.hasApplicableRestriction(RestrictionType.MANDATORY, Utilizations.XMOM_PERSISTENCE_STORE.getName())) {
        MandatoryRestriction restriction = restrictions.getRestriction(RestrictionType.MANDATORY, Utilizations.XMOM_PERSISTENCE_STORE.getName());
        validationFailures.add(new XNWH_XMOMPersistenceValidationException(className, varName, restriction.toString()));
      }
    }
  }

  private static boolean isStorable(GeneralXynaObject obj) {
    Class<?> currentXoClass = obj.getClass();
    while (currentXoClass != null) {
       if (currentXoClass.getName().equals(XMOMPersistenceManagement.STORABLE_BASE_CLASS)) {
         return true;
       }
       currentXoClass = currentXoClass.getSuperclass();
    }
    return false;
  }
  
  
  /**
   * beispiel: falls spalte von typ integer ist, wird stringwert in integer umgewandelt
   */
  static Object transformType(String value, StorableColumnInformation customXCol) {
    if (customXCol.getPrimitiveType() == null) {
      //exception membervar?? sollte bereits beim deployment verhindert werden
      throw new RuntimeException();
    }
    try {
      return customXCol.getPrimitiveType().fromString(value);
    } catch (NumberFormatException e) {
      logger.warn("could not transform custom value from order to store in membervar " + customXCol.getVariableName(), e);
      //FIXME
      return 0;
    }
  }


  private void deleteHierarchy(ODSConnection con, Map<String, Set<Object>> pkMap, List<StorableStructureInformation> deletionOrder,
                               DeleteParameter deleteParameter, PersistenceAccessContext context) throws PersistenceLayerException {
    for (StorableStructureInformation structure : deletionOrder) {
      if (pkMap.containsKey(structure.getTableName()) && pkMap.get(structure.getTableName()) != null) {
        Set<Object> pks = pkMap.get(structure.getTableName());
        StringBuilder deleteBuilder = new StringBuilder();
        deleteBuilder.append("DELETE FROM ").append(structure.getTableName())
                     .append(" WHERE ").append(structure.getPrimaryKeyName()).append(" IN (");
        Parameter params = new Parameter(pks.toArray());
        deleteBuilder.append(nQuestionMarks(pks.size()));
        deleteBuilder.append(")");
        
        Command command = new Command(deleteBuilder.toString());
        con.executeDML(con.prepareCommand(command), params);
      }
    }
    
    /*
     * erst löschen, dann fehler werfen, falls noch anderswo referenziert, damit mehrfache referenzen auf das gleiche storable innerhalb einer
     * hierarchie nicht beim "handleBackwardReferences" zu fehlern führt
     */

    for (StorableStructureInformation structure : deletionOrder) {
      if (pkMap.containsKey(structure.getTableName()) && pkMap.get(structure.getTableName()) != null) {
        Set<Object> pks = pkMap.get(structure.getTableName());
        for (Object value : pks) {
          if (value != null) {
            if (structure instanceof XMOMStorableStructureInformation) {
              handleBackwardReferences(con, (XMOMStorableStructureInformation) structure, value, deleteParameter, pkMap, context);
            }
          }
        }
      }
    }
  }
  
  
  private void handleBackwardReferences(ODSConnection con, XMOMStorableStructureInformation info, Object pk, DeleteParameter deleteParameter,
                                        Map<String, Set<Object>> pkMap, PersistenceAccessContext context) throws PersistenceLayerException {
    if (info.getPossibleReferences().size() > 0) {
      switch (deleteParameter.getBackwardReferenceHandling()) {
        case ERROR :
          throwOnReference(con, info, pk, pkMap);
          break;
        case CASCADE :
          deleteBackwardReferences(con, info, pk, deleteParameter, context);
          break;
        case DELETE :
          invalidateReferences(con, info, pk, context);
          break;
        case IGNORE :
          // ntbd
          break;
      }
    }
  }
  
  
  private void throwOnReference(ODSConnection con, XMOMStorableStructureInformation info, Object pk, Map<String, Set<Object>> deletionMap) throws PersistenceLayerException {
    for (StorableColumnInformation columnReferencedBy : info.getPossibleReferences()) {
      StringBuilder queryBuilder = new StringBuilder();
      StorableStructureInformation parent = columnReferencedBy.getParentStorableInfo();
      queryBuilder.append("SELECT count(*) FROM ")
                  .append(parent.getTableName())
                  .append(" WHERE ").append(columnReferencedBy.getCorrespondingReferenceIdColumn().getColumnName()).append(" = ?");
      Collection<Object> keysFromDeletionMap = deletionMap.get(parent.getTableName());
      Object[] paramsArr;
      if (keysFromDeletionMap != null && keysFromDeletionMap.size() > 0) {
        queryBuilder.append(" AND ").append("NOT ").append(parent.getPrimaryKeyName()).append(" IN (");
        queryBuilder.append(nQuestionMarks(keysFromDeletionMap.size()));
        queryBuilder.append(")");
        paramsArr = new Object[keysFromDeletionMap.size() + 1];
        paramsArr[0] = pk;
        System.arraycopy(keysFromDeletionMap.toArray(), 0, paramsArr, 1, keysFromDeletionMap.size());
      } else {
        paramsArr = new Object[] {pk};
      }
      Parameter params = new Parameter(paramsArr);
      
      ResultSetReader<Long> reader = new ResultSetReader<Long>() {
        public Long read(ResultSet rs) throws SQLException {
          return rs.getLong(1);
        }
      };
      
      Long count = null;
      PreparedQuery<Long> countQuery = con.prepareQuery(new Query<>(queryBuilder.toString(), reader));
      count = con.queryOneRow(countQuery, params);
      if (count > 0) {
        throw new RuntimeException("Object ist still referenced from " + columnReferencedBy.getParentStorableInfo().getTableName() + "." + columnReferencedBy.getColumnName());
      }
    }
  }


  private static final String[] nQuestionMarks;
  private static final int nQuestionMarksCount = 20;
  static {
    nQuestionMarks = new String[nQuestionMarksCount];
    for (int i = 0; i < nQuestionMarks.length; i++) {
      nQuestionMarks[i] = createNQuestionMarksNotCached(i);
    }
  }

  private final LruCache<Integer, String> nQuestionMarksCache = new LruCache<>(200);


  private String nQuestionMarks(int count) {
    if (count < nQuestionMarksCount) {
      return nQuestionMarks[count];
    }
    synchronized (nQuestionMarksCache) {
      String nq = nQuestionMarksCache.get(count);
      if (nq == null) {
        nq = createNQuestionMarksNotCached(count);
        nQuestionMarksCache.put(count, nq);
      }
      return nq;
    }
  }


  private static String createNQuestionMarksNotCached(int count) {
    StringBuilder sb = new StringBuilder();
    for (int k = 0; k < count - 1; k++) {
      sb.append("?,");
    }
    if (count > 0) {
      sb.append("?");
    }
    return sb.toString();
  }

  private void invalidateReferences(ODSConnection con, XMOMStorableStructureInformation info, Object pk, PersistenceAccessContext context) throws PersistenceLayerException {
    for (StorableColumnInformation reference : info.getPossibleReferences()) {      
      validateColumnAgainstCallingContext(context, AccessType.UPDATE, reference);
      StorableColumnInformation columnReferencedBy = reference;
      StringBuilder updateBuilder = new StringBuilder();
      updateBuilder.append("UPDATE ")
                  .append(columnReferencedBy.getParentStorableInfo().getTableName())
                  .append(" SET ").append(columnReferencedBy.getCorrespondingReferenceIdColumn().getColumnName()).append(" = NULL")
                  .append(" WHERE ").append(columnReferencedBy.getCorrespondingReferenceIdColumn().getColumnName()).append(" = ?");
      Command cmd = new Command(updateBuilder.toString());
      con.executeDML(con.prepareCommand(cmd), new Parameter(pk));
    }
  }
  
  
  private void deleteBackwardReferences(ODSConnection con, XMOMStorableStructureInformation info, Object pk, DeleteParameter deleteParameter, PersistenceAccessContext context) throws PersistenceLayerException {
    for (StorableColumnInformation reference : info.getPossibleReferences()) {
      XMOMStorableStructureInformation root = reference.getParentXMOMStorableInformation();
      root = root.generateMergedClone();
      StorableColumnInformation referencedByColumn = reference.getCorrespondingReferenceIdColumn();
      StorableColumnInformation currentPath = referencedByColumn;
      List<StorableColumnInformation> path = new ArrayList<>();
      do {
        currentPath = currentPath.getParentStorableInfo().getPossessingColumn();
        if (currentPath != null) {
          path.add(currentPath);
        }
      } while (currentPath != null);
      PersistenceExpressionVisitors.UnfinishedWhereClause uwc = new PersistenceExpressionVisitors.UnfinishedWhereClause();
      uwc.append(" WHERE ")
         .append(path, referencedByColumn)
         .append(" = ?");
      PersistenceExpressionVisitors.FormulaParsingResult fpr = new PersistenceExpressionVisitors.FormulaParsingResult(uwc, Collections.<CastCondition>emptyList());
      QueryPiplineElement qpe = getQueryGenerator().build(root, null, fpr, new QueryParameter(-1, true, null), true, 
                                                          new Parameter(pk), PersistenceAccessControl.allAccess());
      List<? extends XynaObject> referenceHolderToDelete = executeQueryPipeline(con, qpe);
      for (XynaObject xynaObject : referenceHolderToDelete) {
        delete(con, xynaObject, root, deleteParameter, context);
      }
    }
  }
  
  
  
  private XynaObject deletePreviousVersion(ODSConnection con, XynaObject storable, XMOMStorableStructureInformation info) throws PersistenceLayerException {
    ForwardReferenceHandling referenceHandling = ForwardReferenceHandling.KEEP;
    BackwardReferenceHandling backReferenceHandling = BackwardReferenceHandling.IGNORE;
    return deleteOneRow(con, storable, info, new DeleteParameter(false, referenceHandling, backReferenceHandling));
  }
  
  
  
  private List<? extends XynaObject> restoreOrder(QueryPiplineElement qpe, List<? extends XynaObject> unsortedResult) {
    if (unsortedResult.size() <= 1) {
      return unsortedResult; // empty or one element collections are sorted
    }
    if (qpe.hasNext()) {
      List<Object> sortedPrimaryKeys = qpe.getInterimResult();
      List<XynaObject> sortedResult = new ArrayList<XynaObject>(unsortedResult.size());
      Map<Object, XynaObject> map = new HashMap<>(unsortedResult.size());
      for (XynaObject xynaObject : unsortedResult) {
        if (xynaObject != null) {
          //TODO kann man den PK nicht einfacher herausbekommen? der storable-structure cache weiß doch, welche spalte der PK ist.
          Storable<?> storable = PersistenceAccessDelegator.transformDatatypeToStorable(xynaObject);
          map.put(storable.getPrimaryKey(), xynaObject);
        }
      }
      for (int i=0; i <sortedPrimaryKeys.size(); i++) {
        XynaObject xo = map.get(sortedPrimaryKeys.get(i));
        if (xo != null) {
          sortedResult.add(xo);
        }
      }
      return sortedResult;
    } else {
      return unsortedResult;
    }
  }
  
  
  // TODO as visitor
  private static List<String> generateSelectionForDelete(XMOMStorableStructureInformation rootInfo, DeleteParameter deleteParameter) {
    List<String> columns = new ArrayList<String>(); 
    appendSelectionForDelete(rootInfo, deleteParameter, "%0%", columns);
    return columns;
  }
  
  
  private static void appendSelectionForDelete(StorableStructureInformation rootInfo, DeleteParameter deleteParameter, String currentPrefix, List<String> columns) {
    columns.add(currentPrefix + "." + rootInfo.getPrimaryKeyName());
    StorableColumnInformation typenameCol = rootInfo.getSuperRootStorableInformation().getColInfoByVarType(VarType.TYPENAME);
    if (typenameCol != null) {
      columns.add(currentPrefix + "." + typenameCol.getColumnName());  
    }
    // TODO isnt this to little now with inheritance? Test if we are missing deletes for hierarchy branches below us
    for (StorableColumnInformation column : rootInfo.getColumnInfo(false)) {
      if (column.isStorableVariable()) {
        if (column.getStorableVariableType() == StorableVariableType.EXPANSION || 
            (column.getStorableVariableType() == StorableVariableType.REFERENCE && 
             deleteParameter.getForwardReferenceHandling() == ForwardReferenceHandling.RECURSIVE_DELETE)) {
          appendSelectionForDelete(column.getStorableVariableInformation(), deleteParameter, currentPrefix + "." +  column.getColumnName(), columns);
        }
      }
    }
  }
  
  
  private static String buildFormulaConditionForDelete(XMOMStorableStructureInformation info, XynaObject storable, DeleteParameter deleteParameter) {
    StringBuilder formulaBuilder = new StringBuilder(buildUidFormulaCondition(info, storable));
    if (info.usesHistorization()) {
      StorableColumnInformation colCV = info.getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG);
      Boolean currentVersionFlag = (Boolean) colCV.getFromDatatype(storable);
      if (!currentVersionFlag) {
        formulaBuilder.append(" && %0%.").append(colCV.getColumnName()).append(" == \"").append(Boolean.FALSE.toString()).append("\"");
        if (!deleteParameter.doIncludeHistory()) {
          StorableColumnInformation colHT = info.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP);
          Long historizationTimeStamp = (Long) colHT.getFromDatatype(storable);
          formulaBuilder.append(" && %0%.").append(colHT.getColumnName()).append(" == \"").append(historizationTimeStamp).append("\"");
        }
      } else if (!deleteParameter.doIncludeHistory()) {
        formulaBuilder.append(" && %0%.").append(colCV.getColumnName()).append(" == \"").append(Boolean.TRUE.toString()).append("\"");
      }  // else delete complete history by not appending currentVersion
    }
    return formulaBuilder.toString();
  }
  
  
  private static String buildFormulaConditionByIdentity(XMOMStorableStructureInformation info, XynaObject storable) {
    StringBuilder formulaBuilder = new StringBuilder(buildUidFormulaCondition(info, storable));
    if (info.usesHistorization()) {
      StorableColumnInformation colCV = info.getColInfoByPersistenceType(PersistenceTypeInformation.CURRENTVERSION_FLAG);
      Boolean currentVersionFlag = (Boolean) colCV.getFromDatatype(storable);
      formulaBuilder.append(" && %0%.").append(colCV.getColumnName()).append(" == \"");
      if (currentVersionFlag) {
        formulaBuilder.append(Boolean.TRUE.toString()).append("\"");
      } else {
        formulaBuilder.append(Boolean.FALSE.toString()).append("\"");
        StorableColumnInformation colHT = info.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP);
        Long historizationTimeStamp = (Long) colHT.getFromDatatype(storable);
        formulaBuilder.append(" && %0%.").append(colHT.getColumnName()).append(" == \"").append(historizationTimeStamp).append("\"");
      }
    }
    return formulaBuilder.toString();
  }
  
  
  private static String buildUidFormulaCondition(XMOMStorableStructureInformation info, XynaObject storable) {
    StringBuilder formulaBuilder = new StringBuilder();
    StorableColumnInformation colUid = info.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
    Object uid = colUid.getFromDatatype(storable);
    formulaBuilder.append("%0%.").append(colUid.getColumnName()).append(" == \"").append(String.valueOf(uid)).append("\"");
    return formulaBuilder.toString();
  }
  
  
  private static XMOMStorableStructureCache getStructureCache(XynaObject storable) {
    return getStructureCache(getRevision(storable));
  }
  
  private static XMOMStorableStructureCache getStructureCache(Long revision) {
    return XMOMStorableStructureCache.getInstance(revision);
  }
  
  
  private static Long getRevision(XynaObject storable) {
    return ((MDMClassLoader) storable.getClass().getClassLoader()).getRevision();
  }
  
  
  private static XMOMStorableStructureInformation getStorableStructureInformation(XynaObject storable) {
    return getStructureCache(storable).getStructuralInformation(storable.getClass().getName());
  }
  
  
  private XMOMStorableStructureInformation getStorableStructureInformation(String roottype, Long revision) {
    try {
      long rev = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRuntimeContextDependencyManagement().getRevisionDefiningXMOMObjectOrParent(roottype, revision);
      return getStructureCache(rev).getStructuralInformation(GenerationBase.transformNameForJava(roottype));
    } catch (XPRC_InvalidPackageNameException e) {
      throw new RuntimeException(e);
    }
  }
  
  
  protected static abstract class StorableDataVisitor extends QualifiedStorableDataVisitor {
    
    protected StorableDataVisitor(XynaObject rootObject) {
      super(rootObject);
      
    }

    public abstract void executeOnEnter(StorableColumnInformation column, StorableStructureInformation current, XynaObject obj);
    
    public void executeOnEnter(QualifiedStorableColumnInformation column, StorableStructureInformation current, XynaObject obj) {
      executeOnEnter(column == null ? null : column.getColumn(), current, obj);
    }
    
    
  }
  
  
  protected static abstract class QualifiedStorableDataVisitor implements StorableStructureVisitor {
    
    private final Stack<XynaObject> contextObjectStack = new Stack<XynaObject>();
    private final Stack<StorableColumnInformation> accessStack = new Stack<StorableColumnInformation>();
    private final StorableStructureRecursionFilter filterInstance;
    private final HashSet<StorableStructureInformation> visited;
    
    
    protected QualifiedStorableDataVisitor(XynaObject rootObject) {
      contextObjectStack.push(rootObject);
      visited = new HashSet<>();
      filterInstance = new ListTraversingRecursionFilter(visited);
    }
    
    protected QualifiedStorableDataVisitor(XynaObject rootObject, List<Integer> listIdxs) {
      contextObjectStack.push(rootObject);
      visited = new HashSet<>();
      filterInstance = new SelectiveListTraversingRecursionFilter(listIdxs, visited);
    }


    public void enter(StorableColumnInformation column, StorableStructureInformation current) {
      if (column != null && !column.isList()) {
        XynaObject contextRoot = contextObjectStack.peek();
        if (contextRoot == null || column.getStorableVariableInformation().isSyntheticStorable()) {
          contextObjectStack.push(null);
        } else {
          contextObjectStack.push((XynaObject) getStorableVariable(contextRoot, column));
        }
      }
      executeOnEnter(column == null ? null : new QualifiedStorableColumnInformation(column, accessStack),
                     current, contextObjectStack.peek());
      if (column != null && column.isStorableVariable()) {
        accessStack.push(column);
      }
    }
    
    
    public void exit(StorableColumnInformation column, StorableStructureInformation current) {
      if (column != null) {
        if (!column.isList()) {
        contextObjectStack.pop();
        }
        if (column.isStorableVariable()) {
          accessStack.pop();
        }
      }
    }

    
    
    public StorableStructureRecursionFilter getRecursionFilter() {
      return filterInstance;
    }
    
    
    public abstract void executeOnEnter(QualifiedStorableColumnInformation column, StorableStructureInformation current, XynaObject obj);
    
    private abstract class AbstractListTraversingRecursionFilter implements StorableStructureRecursionFilter {
      
      public boolean accept(final StorableColumnInformation column) {
        if (column.isList()) {
          XynaObject contextRoot = contextObjectStack.peek();
          if (contextRoot == null) {
            column.getStorableVariableInformation().traverse(column, QualifiedStorableDataVisitor.this, visited);
          } else {
            if (column.getPrimitiveType() == null) {
              //komplexwertige liste
              StorableStructureInformation infoForTraversal = null;
              if (column.getStorableVariableInformation().isSyntheticStorable()) {
                for (StorableColumnInformation subColumn : column.getStorableVariableInformation().getColumnInfo(false)) {
                  if (subColumn.isStorableVariable()) {
                    infoForTraversal = subColumn.getStorableVariableInformation();
                    break;
                  }
                }
              } else {
                infoForTraversal = column.getStorableVariableInformation();
              }
              if (infoForTraversal == null) {
                throw new RuntimeException("no traversal info found");
              }
              List<? extends XynaObject> list = (List<? extends XynaObject>) getStorableVariableList(contextRoot, column);
              if (list != null) {
                traverseList(infoForTraversal, column, list);
              }
            }
          }
          return false;
        } else {
          return true;
        }
      }

      protected abstract void traverseList(StorableStructureInformation infoForTraversal, StorableColumnInformation column, List<? extends XynaObject> list);
      
      @Override
      public boolean acceptHierarchy(StorableStructureInformation declaredType) {
        return false;
      }
    }

    
    private class ListTraversingRecursionFilter extends AbstractListTraversingRecursionFilter {
      
      private HashSet<StorableStructureInformation> visited;
      
      private ListTraversingRecursionFilter(HashSet<StorableStructureInformation> visited) {
        this.visited = visited;
      }
      
      protected void traverseList(StorableStructureInformation infoForTraversal, StorableColumnInformation column, List<? extends XynaObject> list) {
        for (XynaObject xynaObject : list) {
          contextObjectStack.push(xynaObject);
          infoForTraversal.traverse(column, QualifiedStorableDataVisitor.this, visited);
          contextObjectStack.pop();
        }
      }
      
    }
    
    
    private class SelectiveListTraversingRecursionFilter extends AbstractListTraversingRecursionFilter {
      
      private HashSet<StorableStructureInformation> visited;
      private final ListIterator<Integer> listIdxIterator;
      
      private SelectiveListTraversingRecursionFilter(List<Integer> listIdxs, HashSet<StorableStructureInformation> visited) {
        this.listIdxIterator = listIdxs.listIterator();
        this.visited = visited;
      }
      
      @Override
      protected void traverseList(StorableStructureInformation infoForTraversal, StorableColumnInformation column,
                                  List<? extends XynaObject> list) {
        if (listIdxIterator.hasNext()) { // TODO throw ?
          Integer listIndex = listIdxIterator.next();
          if (listIndex >= list.size()) {
            throw new RuntimeException("Update object did not contain a list entry at list index " + listIndex);
          }
          XynaObject xynaObject = list.get(listIndex);
          contextObjectStack.push(xynaObject);
          infoForTraversal.traverse(column, QualifiedStorableDataVisitor.this, visited);
          contextObjectStack.pop();
          listIdxIterator.previous();
        }
      }

    }
    
  }
  
  public static class TypeCollectionVisitor implements StorableStructureVisitor {

    private Map<String, Collection<StorableStructureInformation>> types = new HashMap<>();
    
    public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
      Collection<StorableStructureInformation> subtypes = types.get(current.getFqXmlName());
      if (subtypes == null) {
        subtypes = new HashSet<>();
        types.put(current.getFqXmlName(), subtypes);
      }
      subtypes.add(current);
    }

    public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
    }

    public StorableStructureRecursionFilter getRecursionFilter() {
      return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
    }
    
    public Map<String, Collection<StorableStructureInformation>> getTypes() {
      return types;
    }
    
  }
  
  public static class HierachyTypeCollectionVisitor implements StorableStructureVisitor {

    private Collection<StorableStructureInformation> types = new ArrayList<>();
    
    public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
      types.add(current);
    }

    public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
    }

    public StorableStructureRecursionFilter getRecursionFilter() {
      return new StorableStructureRecursionFilter() {
        
        public boolean acceptHierarchy(StorableStructureInformation declaredType) {
          return true;
        }
        
        public boolean accept(StorableColumnInformation columnLink) {
          return false;
        }
      };
    }
    
    public Collection<StorableStructureInformation> getTypes() {
      return types;
    }
    
  }
  
  public static class TypeLinkCollectionVisitor implements StorableStructureVisitor {

    private Map<String, Collection<StorableColumnInformation>> types = new HashMap<>();
    
    public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
      Collection<StorableColumnInformation> subtypes = types.get(current.getFqXmlName());
      if (subtypes == null) {
        subtypes = new HashSet<>();
        types.put(current.getFqXmlName(), subtypes);
      }
      if (columnLink == null) {
        StorableColumnInformation fakeColumn = new StorableColumnInformation(null, null, Collections.<PersistenceTypeInformation>emptySet(), null);
        fakeColumn.setStorableVariableInformation(new XMOMStorableStructureCache.DirectStorableStructureIdentifier(current));
        subtypes.add(fakeColumn);
      } else {
        subtypes.add(columnLink);
      }
    }

    public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) {
    }

    public StorableStructureRecursionFilter getRecursionFilter() {
      return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
    }
    
    public Map<String, Collection<StorableColumnInformation>> getTypes() {
      return types;
    }
    
  }
  
  private final static QueryGenerator qg = new QueryGenerator();
  
  private static QueryGenerator getQueryGenerator() {
    return qg;
  }
  
  
  private static class ArgumentlessFormula implements IFormula {

    private final String formula; 
    
    private ArgumentlessFormula(String formula) {
      this.formula = formula;
    }
    
    public List<Accessor> getValues() {
      return null;
    }

    public String getFormula() {
      return formula;
    }
    
  }
  
  
  private static final HistorizationInformation EMPTY_HISTORIZATION_INFORMATION = new HistorizationInformation();
  
  private static class HistorizationInformation {
    
    private Map<String, List<XynaObject>> historized = new HashMap<>();
    private Map<String, Map<Object, Long>> historizationTimestamps = new HashMap<>();
    
    public void addToHistorization(StorableStructureInformation info, XynaObject historizedVersion) {
      if (historizedVersion == null) {
        return;
      }
      if (info instanceof XMOMStorableStructureInformation) {
        List<XynaObject> list = historized.get(info.getTableName());
        if (list == null) {
          list = new ArrayList<XynaObject>();
          historized.put(info.getTableName(), list);
        }
        list.add(historizedVersion);
      }
      for (StorableColumnInformation column : info.getColumnInfo(false)) {
        if (column.isStorableVariable() && column.getPrimitiveType() == null) {
          if (column.isList()) {
            List<? extends Object> list = getStorableVariableList(historizedVersion, column);
            if (list != null) {
              for (Object object : list) {
                addToHistorization(column.getStorableVariableInformation(), (XynaObject) object);
              }
            }
          } else {
            addToHistorization(column.getStorableVariableInformation(), getStorableVariable(historizedVersion, column));
          }
        }
      }
    }
    
    
    public boolean presentInHistorization(StorableStructureInformation info, XynaObject otherVersion, boolean timestampAware) {
      if (info instanceof XMOMStorableStructureInformation) {
        List<XynaObject> historizedList = historized.get(info.getTableName());
        if (historizedList == null) {
          return false;
        }
        for (XynaObject historizedObject : historizedList) {
          if (equalUids(otherVersion, historizedObject, (XMOMStorableStructureInformation) info)) {
            if (timestampAware) {
              if (equalTimeStamps(otherVersion, historizedObject, (XMOMStorableStructureInformation) info)) {
                return true;
              }
            } else {
              return true;
            }
          }
        }
      }
      return false;
    }
    
    
    public long discoverTimeStamp(XynaObject outdatedVersion, StorableStructureInformation info) {
      Map<Object, Long> stampsForTable = historizationTimestamps.get(info.getTableName());
      if (stampsForTable == null) {
        stampsForTable = new HashMap<Object, Long>();
        historizationTimestamps.put(info.getTableName(), stampsForTable);
      }
      Object uid = info.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
      Long stamp = stampsForTable.get(uid);
      if (stamp == null) {
        stamp = discoverHighestTimeStamp(outdatedVersion, info);
        stampsForTable.put(uid, stamp);
      }
      return stamp;
    }
    
    
    public void incrementStamp(XynaObject outdatedVersion, StorableStructureInformation info) {
      Map<Object, Long> stampsForTable = historizationTimestamps.get(info.getTableName());
      Object uid = info.getColInfoByPersistenceType(PersistenceTypeInformation.UNIQUE_IDENTIFIER);
      Long stamp = stampsForTable.get(uid);
      stamp++;
      stampsForTable.put(uid, stamp);
    }
    
    
    public static Long discoverHighestTimeStamp(final XynaObject outdatedVersion, StorableStructureInformation info) {
      final AtomicLong highestTimeStampReference = new AtomicLong(-1);
      info.traverse(new StorableDataVisitor(outdatedVersion) {
        @Override
        public void executeOnEnter(StorableColumnInformation column, StorableStructureInformation current, XynaObject obj) {
          if (current instanceof XMOMStorableStructureInformation &&
              ((XMOMStorableStructureInformation) current).usesHistorization() &&
              obj != null) {
            Long timestamp = (Long) current.getColInfoByPersistenceType(PersistenceTypeInformation.HISTORIZATION_TIMESTAMP).getFromDatatype(obj);
            if (timestamp > highestTimeStampReference.get()) {
              highestTimeStampReference.set(timestamp);
            }
          }
        }
      });
      return highestTimeStampReference.get();
    }
    
    public void clear() {
      historized.clear();
      // historizationTimestamps not cleared on purpose as those are important to track already used stamps
    }
    
  }


  private static String getPrimaryKeyOfFirstPossessingXMOMStorable(XynaObject rootDatatype, XMOMStorableStructureInformation info,
                                                                   QualifiedStorableColumnInformation column, List<Integer> listIdxs) {
    List<StorableColumnInformation> completePath = new ArrayList<StorableColumnInformation>(column.getAccessPath());
    completePath.add(column.getColumn());
    XynaObject primaryKeyHolder = rootDatatype; //TODO immer oberstes objekt removen
    for (int i = completePath.size() - 1; i >= 0; i--) {
      StorableColumnInformation currentCol = completePath.get(i);
      boolean isReference = currentCol.isStorableVariable() && currentCol.getStorableVariableType() == StorableVariableType.REFERENCE;
      boolean isReferencedList = currentCol.isStorableVariable() && currentCol.isList() && currentCol.getStorableVariableInformation().isReferencedList();
      if (isReference || isReferencedList) {
        //xmomstorable gefunden, das eigenen PK haben sollte
        List<StorableColumnInformation> newAccessPath = new ArrayList<StorableColumnInformation>();
        for (int j = 0; j < i; j++) {
          newAccessPath.add(completePath.get(j));
        }
        XynaObject newPrimaryKeyHolder =
            (XynaObject) getColumnFromDatatype(new QualifiedStorableColumnInformation(currentCol, newAccessPath), rootDatatype, listIdxs);
        if (newPrimaryKeyHolder == null) {
          continue;
        } else {
          if (isReference) {
            info = (XMOMStorableStructureInformation) currentCol.getStorableVariableInformation();
          } else {
            info = (XMOMStorableStructureInformation) currentCol.getStorableVariableInformation()
                           .getColInfoByVarType(VarType.REFERENCE_FORWARD_FK).getStorableVariableInformation();
          }
          primaryKeyHolder = newPrimaryKeyHolder;
        }
        break;
      }
    }

    return String.valueOf(PersistenceAccessDelegator.transformDatatypeToStorable(primaryKeyHolder, info).getPrimaryKey());
  }


  // TODO we need to ensure same ordering for access of a storable from different revisions that access the same table 
  private static List<StorableStructureInformation> getStorableStructureHierarchyOrder(XMOMStorableStructureInformation info) {
    final List<StorableStructureInformation> orderedInfo = new ArrayList<StorableStructureInformation>();
    info.traverse(new StorableStructureVisitor() {
      
      public StorableStructureRecursionFilter getRecursionFilter() {
        return XMOMStorableStructureCache.ALL_RECURSIONS_AND_FULL_HIERARCHY;
      }
      
      public void exit(StorableColumnInformation columnLink, StorableStructureInformation current) { }
    
      public void enter(StorableColumnInformation columnLink, StorableStructureInformation current) {
        if (!orderedInfo.contains(current)) {
          orderedInfo.add(current);
        }
      }
    });
    return orderedInfo;
  }
  
  
  static void validateSelectionAgainstCallingContext(PersistenceAccessContext context, List<QualifiedStorableColumnInformation> qualifiedColumns) {
    context.checkAccess(AccessType.READ, CollectionUtils.transform(qualifiedColumns, qc -> qc.getColumn()));
  }
  
  static void validateInfoAgainstCallingContext(PersistenceAccessContext context, AccessType type, XMOMStorableStructureInformation info) {
    context.checkAccess(type, info);
  }
  
  
  static void validateColumnAgainstCallingContext(PersistenceAccessContext context, AccessType type, StorableColumnInformation column) {
    context.checkAccess(type, column);
  }
  
  
  private static class HistorizationTimeStampCollision extends Exception {

    private static final long serialVersionUID = 1L;
    
    StorableStructureInformation collidingStructure;
    XynaObject collidingObject;
    
    public HistorizationTimeStampCollision(StorableStructureInformation collidingStructur, XynaObject collidingObject) {
      this.collidingStructure = collidingStructur;
      this.collidingObject = collidingObject;
    }
    
  }
  
  
  
  private static interface PersistenceConcurrencyContext {
    
    public void handle(XNWH_RetryTransactionException e) throws  XNWH_RetryTransactionException;
    
    public void commit(ODSConnection con) throws PersistenceLayerException;

    public void handle(ODSConnection con, HistorizationInformation historizationInfo, HistorizationTimeStampCollision e) throws PersistenceLayerException;

    public void handle(ODSConnection con, HistorizationTimeStampCollision e) throws  PersistenceLayerException;
    
    public void protect(Set<StorableIdentifier> affected, AccessType operation);
    
    public void finish(Set<StorableIdentifier> affected, AccessType operation);
    
  }
  
  private static class LocalConnectionContext implements PersistenceConcurrencyContext {
    
    private Long startStamp= null;
    private int retries = 0;
    
    public void handle(XNWH_RetryTransactionException e) throws  XNWH_RetryTransactionException {
      if (startStamp == null) {
        startStamp = System.currentTimeMillis();
      } else {
        long timeSpendInRetries = System.currentTimeMillis() - startStamp;
        Duration timeout = XMOMPersistenceManagement.DEADLOCK_RETRY_TIMEOUT.get();
        if (timeSpendInRetries > timeout.getDurationInMillis()) {
          throw new RuntimeException("Spend " + timeSpendInRetries + "ms executing " + retries + "/" + XMOMPersistenceManagement.DEADLOCK_RETRIES.get() + " retries.", e);
        }
      }
      retries++;
      Thread.yield(); //or a longer random backof?
      throw e;
    }

    public void handle(ODSConnection con, HistorizationTimeStampCollision e) throws PersistenceLayerException {
      logger.trace("Retry for HistorizationTimeStampCollision", e);
      con.rollback();
    }

    public void handle(ODSConnection con, HistorizationInformation historizationInfo, HistorizationTimeStampCollision e) throws PersistenceLayerException {
      con.rollback();
      historizationInfo.clear();
      historizationInfo.incrementStamp(e.collidingObject, e.collidingStructure);
    }

    public void commit(ODSConnection con) throws PersistenceLayerException {
      con.commit();
    }

    @Override
    public void protect(Set<StorableIdentifier> affected, AccessType operation) {
      switch (operation) {
        case DELETE :
        case INSERT :
          logger.trace("LocalConnectionContext.protect -> readLock()");
          for (StorableIdentifier storableIdentifier : affected) {
            concurrencyProtectionLocks.readLock(storableIdentifier);
          }
        case UPDATE :
        case READ :
        case ALL : /* unexpected */
        default :
          logger.trace("LocalConnectionContext.protect -> ntbd");
      }
    }

    @Override
    public void finish(Set<StorableIdentifier> affected, AccessType operation) {
      switch (operation) {
        case DELETE :
        case INSERT :
          logger.trace("LocalConnectionContext.protect -> readUnlock()");
          for (StorableIdentifier storableIdentifier : affected) {
            concurrencyProtectionLocks.readUnlock(storableIdentifier);
          }
        case UPDATE :
        case READ :
        case ALL : /* unexpected */
        default :
          logger.trace("LocalConnectionContext.protect -> ntbd");
      }
    }
    
  }
  
  
  private static class ExternalConnectionContext implements PersistenceConcurrencyContext {
    
    
    public void handle(XNWH_RetryTransactionException e) throws  XNWH_RetryTransactionException {
      throw e;
    }

    public void handle(ODSConnection con, HistorizationTimeStampCollision e) throws PersistenceLayerException {
      throw new RuntimeException("Collision should have been prevented from global lock",e);
    }
    
    public void handle(ODSConnection con, HistorizationInformation historizationInfo, HistorizationTimeStampCollision e) throws PersistenceLayerException {
      throw new RuntimeException("Collision should have been prevented from global lock",e);
    }

    public void commit(ODSConnection con) throws PersistenceLayerException {
      // no commit
    }

    public void protect(Set<StorableIdentifier> affected, AccessType operation) {
      switch (operation) {
        case DELETE :
        case INSERT :
          logger.trace("ExternalConnectionContext.protect -> writeLock");
          for (StorableIdentifier storableIdentifier : affected) {
            concurrencyProtectionLocks.writeLock(storableIdentifier);            
          }
        case UPDATE :
        case READ :
        case ALL : /* unexpected */
        default :
          logger.trace("ExternalConnectionContext.protect -> ntbd");
      }
    }

    public void finish(Set<StorableIdentifier> affected, AccessType operation) {
      switch (operation) {
        case DELETE :
        case INSERT :
          logger.trace("ExternalConnectionContext.protect -> writeUnlock");
          for (StorableIdentifier storableIdentifier : affected) {
            concurrencyProtectionLocks.writeUnlock(storableIdentifier);            
          }
        case UPDATE :
        case READ :
        case ALL : /* unexpected */
        default :
          logger.trace("ExternalConnectionContext.protect -> ntbd");
      } 
    }

  }

  
  private static class PersistenceStoreContext {

    private Map<String, HashMap<String, Storable<?>>> objectsToStorePerTable = new HashMap<>();
    private Map<Storable<?>, XynaObject> collisionInformation = new HashMap<>();
    private final List<StorableStructureInformation> order;

    private PersistenceStoreContext(XMOMStorableStructureInformation rootInfo) {
      order = getStorableStructureHierarchyOrder(rootInfo);
    }


    public boolean add(StorableStructureInformation info, Storable<?> storable, XynaObject xoStorable) {
      if (storable == null) {
         return false;
      }

      HashMap<String, Storable<?>> objectsToStoreForTable = objectsToStorePerTable.get(info.getTableName());
      if (objectsToStoreForTable == null) {
        objectsToStoreForTable = new HashMap<String, Storable<?>>();
        objectsToStorePerTable.put(info.getTableName(), objectsToStoreForTable);
      }

      String pk = String.valueOf(storable.getPrimaryKey());
      boolean contained = objectsToStoreForTable.containsKey(pk);

      if (!contained) {
        objectsToStoreForTable.put(pk, storable);
        if (info instanceof XMOMStorableStructureInformation) {
          collisionInformation.put(storable, xoStorable);
        }
      } else {
        logger.debug("Multiple instances for table " + info.getTableName() + " with same primaryKey " + pk + " contained in hierarchy, skipping additional Data!");
      }

      return contained;
    }

    public void store(ODSConnection con) throws HistorizationTimeStampCollision, PersistenceLayerException {
      try {
        for (StorableStructureInformation structureInfo : order) {
          HashMap<String, Storable<?>> objectsToStoreForTable = objectsToStorePerTable.remove(structureInfo.getTableName());
          if (objectsToStoreForTable != null && !objectsToStoreForTable.isEmpty()) {
            if (structureInfo instanceof XMOMStorableStructureInformation) {
              for (Storable<?> rootOrReferenceStorable : objectsToStoreForTable.values()) {
                if (con.persistObject(rootOrReferenceStorable)) {
                  // update-fall
                  // can happen if the entry did not exist during historization but does now exist
                  throw new HistorizationTimeStampCollision((XMOMStorableStructureInformation)structureInfo, collisionInformation.get(rootOrReferenceStorable));
                }
              }
            } else {
              if (!objectsToStoreForTable.isEmpty()) {
                for (Storable<?> objectToStoreForTable : objectsToStoreForTable.values()) {
                  if (objectToStoreForTable != null) {
                    con.persistObject(objectToStoreForTable);
                  }
                }
              }
            }
          }
        }
      } finally {
        // there might be a pending retry
        objectsToStorePerTable.clear();
        collisionInformation.clear();
      }
    }
    
  }
  
}
