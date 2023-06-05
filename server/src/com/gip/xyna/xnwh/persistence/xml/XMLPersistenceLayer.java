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
package com.gip.xyna.xnwh.persistence.xml;



import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.sql.Blob;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.Department;
import com.gip.xyna.FileUtils;
import com.gip.xyna.exceptions.Ex_FileAccessException;
import com.gip.xyna.utils.collections.ConcurrentMapWithObjectRemovalSupport;
import com.gip.xyna.utils.collections.ObjectWithRemovalSupport;
import com.gip.xyna.utils.collections.Pair;
import com.gip.xyna.utils.concurrent.ReusableCountDownLatch;
import com.gip.xyna.utils.db.UnsupportingResultSet;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.utils.streams.ReverseLineInputStream;
import com.gip.xyna.utils.timing.Duration;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyDuration;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyLong;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_InvalidObjectForTableException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerClassIncompatibleException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerIdUnknownException;
import com.gip.xyna.xnwh.exceptions.XNWH_PersistenceLayerNotRegisteredException;
import com.gip.xyna.xnwh.exceptions.XNWH_RetryTransactionException;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.ColumnType;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.ODSConnectionType;
import com.gip.xyna.xnwh.persistence.ODSImpl;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PersistenceLayerInstanceBean;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TransactionProperty;
import com.gip.xyna.xnwh.persistence.file.TransactionFile;
import com.gip.xyna.xnwh.persistence.xml.backup.BackupConfig;
import com.gip.xyna.xnwh.persistence.xml.backup.BackupManager;
import com.gip.xyna.xnwh.persistence.xml.backup.BackupManager.FilePathBuilder;
import com.gip.xyna.xnwh.persistence.xml.backup.BackupManagerIfc;
import com.gip.xyna.xnwh.persistence.xml.backup.NoBackup;
import com.gip.xyna.xprc.exceptions.XPRC_XmlParsingException;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils.DocumentBuilderInstance;




public class XMLPersistenceLayer implements PersistenceLayer {

  private static final Logger logger = CentralFactoryLogging.getLogger(XMLPersistenceLayer.class);
  
  public static final String ENTRY_TAG = "Entry";
  public static final String KEY_TAG = "Key";
  public static final String VALUE_TAG = "Value";

  public static enum TransactionMode {
    FULL_TRANSACTION("true", true, true),
    AUTO_COMMIT("autocommit", true, false),
    NO_TRANSACTION("false", false, false);
    
    private final String startParamIdentifier;
    private final boolean useTransactionFiles;
    private final boolean wrapOriginalFile;
    
    private TransactionMode(String startParamIdentifier, boolean useTransactionFiles, boolean wrapOriginalFile) {
      this.startParamIdentifier = startParamIdentifier;
      this.useTransactionFiles = useTransactionFiles;
      this.wrapOriginalFile = wrapOriginalFile;
    }
    
    public String getStartParamIdentifier() {
      return startParamIdentifier;
    }
    
    public boolean useTransactionFiles() {
      return useTransactionFiles;
    }
    
    public boolean wrapOriginalFile() {
      return wrapOriginalFile;
    }
    
    public static TransactionMode getByStartParamIdentifier(String param) {
      for (TransactionMode mode : values()) {
        if (mode.getStartParamIdentifier().equals(param)) {
          return mode;
        }
      }
      return FULL_TRANSACTION;
    }
    
  }

  private enum TransactionPartID {
    COMMIT, ROLLBACK, STORE, DELETE, DELETEALL;
  }

  private interface TransactionPart {

    void apply(MemoryQueue memoryQueue, long transactionId);


    String asString();


    String getTableName();

  }
  
  private final static Map<String, ResultSetReader<Storable>> readerCache = new HashMap<String, ResultSetReader<Storable>>();
  
  private class TXStore implements TransactionPart {


    private final String tableName;
    private Storable storable;
    private String storableAsString;

    public TXStore(String tableName, Storable storable) {
      this.tableName = tableName;
      this.storable = storable;
    }


    public TXStore(String tableName) {
      this.tableName = tableName;
    }

    public boolean parseStorable(String currentXml) {
      ResultSetReader<Storable> reader = getReader();
      if (reader == null) {
        storableAsString = currentXml.toString();
        return false;
      }
      //TODO performance: lieber mit saxparser parsen
      Document doc;
      try {
        doc = XMLUtils.parseString(currentXml.toString());
      } catch (XPRC_XmlParsingException e) {
        throw new RuntimeException("Could not parse journal entry for table " + tableName + ".", e);
      }
      try {
        storable = reader.read(new XMLResultSet(doc.getDocumentElement()));
      } catch (SQLException e) {
        throw new RuntimeException(e);
      }
      return true;
    }

    private ResultSetReader<Storable> getReader() {
      ResultSetReader<Storable> reader = readerCache.get(tableName);
      if (reader == null) {
        String tableNameLC = tableName.toLowerCase();
        Class<? extends Storable> clazz;
        try {
          //bei mehreren registrierten storables für den tablename (applications) kann hier eine beliebige klasse genommen werden,
          //weil die klasse nur für den datentransfer von journal->xml verwendet wird
          clazz = classes.lazyCreateGet(tableNameLC).getAnyElement();
        } finally {
          classes.cleanup(tableNameLC);
        }
        
        if (clazz == null) {
          return null;
        }
        Storable tempInstance;
        try {
          tempInstance = clazz.getConstructor().newInstance();
        } catch (Exception e) {
          throw new RuntimeException("Could not create instance of " + tableName + " per reflection.", e);
        }
        reader = tempInstance.getReader();
        readerCache.put(tableName, reader);
      }
      return reader;
    }

    public void apply(MemoryQueue memoryQueue, long transactionId) {
      TableInfo tableInfo = memoryQueue.tableInfos.get(tableName);
      int retrycnt = 0;
      while (tableInfo == null || tableInfo.currentTransactionId == null) {
        if (retrycnt ++ > 10000) {
          throw new RuntimeException("storable " + tableName + " not registered yet. (table null)");
        }
        //in entwicklung gabs beim zugriff auf currentTransactionId ne NPE. Unklar, wie es dazu kommen kann
        tableInfo = memoryQueue.tableInfos.get(tableName);
        Thread.yield();
      }
      if (tableInfo.currentTransactionId.get() > transactionId) {
        return;
      }
      
      if (storable == null) {
        if (!parseStorable(storableAsString)) {
          throw new RuntimeException("storable " + tableName + " not registered yet.");
        }
      }

      synchronized (tableInfo) {
        tableInfo.getRows().put(String.valueOf(storable.getPrimaryKey()), storable);
        tableInfo.setCurrentTransactionId(transactionId);
      }
    }



    public String asString() {
      StringBuilder sb = new StringBuilder(TransactionPartID.STORE.name()).append(" ").append(tableName).append("\n");
      try {
        DocumentBuilder builder = DocumentBuilderInstance.DEFAULT.getDocumentBuilder();
        Document doc;
        try {
          doc = builder.newDocument();
        } finally {
          DocumentBuilderInstance.DEFAULT.returnBuilder(builder);
        }
        Element elem = createElement(tableName, doc, storable);
        sb.append(XMLUtils.getXMLString(elem, false));
        sb.append("\n");
      } catch (XNWH_GeneralPersistenceLayerException e) {
        throw new RuntimeException(e);
      } catch (XNWH_UnsupportedPersistenceLayerFeatureException e) {
        throw new RuntimeException(e);
      }
      return sb.toString();
    }


    public String getTableName() {
      return tableName;
    }

  }

  private static class TXDelete implements TransactionPart {

    private static final Pattern deletePattern = Pattern.compile(TransactionPartID.DELETE.name() + " ([^ ]+) (.*)");

    private final String tableName;
    private final String primaryKey;


    public TXDelete(String tableName, String primaryKey) {
      this.tableName = tableName;
      this.primaryKey = primaryKey;
    }


    public static TXDelete parse(String line) {
      Matcher m = deletePattern.matcher(line);
      if (!m.matches()) {
        throw new RuntimeException("could not parse '" + line + "'");
      }
      return new TXDelete(m.group(1), m.group(2));
    }


    public void apply(MemoryQueue memoryQueue, long transactionId) {
      TableInfo ti = memoryQueue.tableInfos.get(tableName);
      if (ti.currentTransactionId.get() > transactionId) {
        return;
      }
        
      synchronized (ti) {
        ti.getRows().remove(String.valueOf(primaryKey));
        ti.setCurrentTransactionId(transactionId);
      }
    }


    public String asString() {
      return TransactionPartID.DELETE.name() + " " + tableName + " " + primaryKey;
    }


    public String getTableName() {
      return tableName;
    }

  }

  private static class TXDeleteAll implements TransactionPart {

    private static final int substringStart = TransactionPartID.DELETEALL.name().length() + 1;
    private final String tableName;


    public TXDeleteAll(String tableName) {
      this.tableName = tableName;
    }


    public static TXDeleteAll parse(String line) {
      return new TXDeleteAll(line.substring(substringStart));
    }


    public void apply(MemoryQueue memoryQueue, long transactionId) {
      TableInfo ti = memoryQueue.tableInfos.get(tableName);
      if (ti.currentTransactionId.get() > transactionId) {
        return;
      }
      
      synchronized (ti) {
        ti.getRows().clear();
        ti.setCurrentTransactionId(transactionId);
      }
    }


    public String asString() {
      return TransactionPartID.DELETEALL.name() + " " + tableName;
    }


    public String getTableName() {
      return tableName;
    }

  }
 

  private static class TXCommit implements TransactionPart {

    private static final Pattern pattern = Pattern.compile("^" + TransactionPartID.COMMIT.name() + " (\\d+) (.*)"
        + TransactionPartID.COMMIT.name() + "$");

    private final long txId;
    private final String[] tableNames;


    public TXCommit(long transactionId, String[] tableNames) {
      this.txId = transactionId;
      this.tableNames = tableNames;
    }


    public static TXCommit parse(String line) {
      Matcher m = pattern.matcher(line);
      if (!m.matches()) {
        throw new RuntimeException("journal line invalid: " + line);
      }
      return new TXCommit(Long.valueOf(m.group(1)), m.group(2).length() == 0 ? new String[0] : m.group(2).trim().split(" "));
    }


    public long getId() {
      return txId;
    }


    public String[] getTables() {
      return tableNames;
    }


    public void apply(MemoryQueue memoryQueue, long transactionId) {
      //ntbd
    }


    public String asString() {
      StringBuilder sb = new StringBuilder();
      sb.append(TransactionPartID.COMMIT.name()).append(" ").append(txId);
      for (String tableName : tableNames) {
        sb.append(" ").append(tableName);
      }
      sb.append(" ").append(TransactionPartID.COMMIT.name());
      return sb.toString();
    }


    public String getTableName() {
      throw new RuntimeException();
    }

  }

  private static class TXRollback implements TransactionPart {

    private static final int substringStart = TransactionPartID.ROLLBACK.name().length() + 1;
    private final long transactionId;


    public TXRollback(long transactionId) {
      this.transactionId = transactionId;
    }


    public Long getId() {
      return transactionId;
    }


    public static TXRollback parse(String line) {
      return new TXRollback(Long.valueOf(line.substring(substringStart)));
    }


    public static String asString(TransactionContext transactionContext) {
      return TransactionPartID.ROLLBACK.name() + " " + transactionContext.getId();
    }


    public void apply(MemoryQueue memoryQueue, long transactionId) {
      throw new RuntimeException();
    }


    public String asString() {
      return TransactionPartID.ROLLBACK.name() + " " + transactionId;
    }


    public String getTableName() {
      throw new RuntimeException();
    }

  }

  private enum TransactionContextState {
    VALID, INVALID, UNKNOWN;
  }

  private class TransactionContext {

    private final List<TransactionPart> parts = new ArrayList<TransactionPart>(1);
    private long transactionId;
    private final Map<String, PersistenceLayerConnection> openConnections = new HashMap<String, PersistenceLayerConnection>(1);
    private List<String> partsAsString;
    private String[] tableNames;
    private volatile TransactionContextState state = TransactionContextState.UNKNOWN;


    public void commit() throws PersistenceLayerException {
      memoryQueue.add(this);
      boolean success = false;
      try {
        for (PersistenceLayerConnection plc : openConnections.values()) {
          plc.commit();
        }
        openConnections.clear();
        success = true;
      } finally {
        setValid(success);
      }
    }


    private String[] getTables() {
      if (tableNames != null) {
        return tableNames;
      }
      Set<String> tables = new HashSet<String>();
      for (TransactionPart tp : parts) {
        if (tp instanceof TXCommit) {
          continue;
        }
        tables.add(tp.getTableName());
      }
      tableNames = tables.toArray(new String[tables.size()]);
      return tableNames;
    }


    private void setValid(boolean success) {
      if (success) {
        state = TransactionContextState.VALID;
      } else {
        state = TransactionContextState.INVALID;
      }
    }


    public void rollback() throws PersistenceLayerException {
      for (PersistenceLayerConnection plc : openConnections.values()) {
        plc.rollback();
      }
      parts.add(new TXRollback(transactionId));
    }


    public void setTransactionId(long txId) {
      this.transactionId = txId;
      parts.add(new TXCommit(transactionId, getTables()));
    }


    public PersistenceLayerConnection getConnection(String tableName) throws PersistenceLayerException {
      String tableNameLC = tableName.toLowerCase();
      PersistenceLayerConnection con = openConnections.get(tableNameLC);
      if (con == null) {
        if (!XMLPersistenceLayer.this.classes.containsKey(tableNameLC)) {
          throw new XNWH_GeneralPersistenceLayerException("Table <" + tableName + "> unknown in xml persistencelayer instance " + journal.pliId);
        }
        con = cachePL.getConnection();
        openConnections.put(tableNameLC, con);
      }
      return con;
    }


    public <T extends Storable> void store(String tableName, T storable) {
      T clone;
      try {
        clone = (T) storable.getClass().getConstructor().newInstance();
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      clone.setAllFieldsFromData(storable);
      parts.add(new TXStore(tableName, clone));
    }


    public void delete(String tableName, Object primaryKey) {
      parts.add(new TXDelete(tableName, String.valueOf(primaryKey)));
    }


    public void deleteAll(String tableName) {
      parts.add(new TXDeleteAll(tableName));
    }


    /**
     * in umgekehrter reihenfolge
     */
    public void addLinesForReplay(List<String> lines) {
      partsAsString = lines;
    }


    public void addTableNamesForReplay(String[] tables) {
      tableNames = tables;
    }
    

    public void restoreFromLines() {
      if (partsAsString == null) {
        return;
      }
      StringBuilder currentXml = new StringBuilder();
      TXStore currentStore = null;

      for (String line : partsAsString) {
        if (line.startsWith(TransactionPartID.DELETEALL.name())) {
          if (currentStore != null) {
            currentStore.parseStorable(currentXml.toString());
            currentXml.setLength(0);
          }
          TXDeleteAll delete = TXDeleteAll.parse(line);
          parts.add(delete);
        } else if (line.startsWith(TransactionPartID.DELETE.name())) {
          if (currentStore != null) {
            currentStore.parseStorable(currentXml.toString());
            currentXml.setLength(0);
          }
          TXDelete delete = TXDelete.parse(line);
          parts.add(delete);
        } else if (line.startsWith(TransactionPartID.STORE.name())) {
          if (currentStore != null) {
            currentStore.parseStorable(currentXml.toString());
            currentXml.setLength(0);
          }
          currentStore = new TXStore(line.substring(TransactionPartID.STORE.name().length() + 1));
          parts.add(currentStore);
        } else {
          //gehört zum store-xml
          currentXml.append(line);
        }
      }
      if (currentStore != null) {
        currentStore.parseStorable(currentXml.toString());
      }
      partsAsString = null;
    }


    public long getId() {
      return transactionId;
    }


    public StringBuilder asString() {
      StringBuilder sb = new StringBuilder();
      for (TransactionPart part : parts) {
        sb.append(part.asString()).append("\n");
      }
      return sb;
    }


    public void setXmlsReadOnly() {
      
      XMLPersistenceLayerConnection con;
      try {
        con = new XMLPersistenceLayerConnection();
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
      for (String tableName : getTables()) {
        TableInfo ti = memoryQueue.getOrCreateTableInfo(tableName, true);
        if (ti.needsToBeSetToReadOnly()) {
          File file = con.getFile(dir, tableName, true);
          if (file.lastModified() == ti.lastModified.get() && file.canWrite()) {
            if (logger.isTraceEnabled()) {
              logger.trace("setting file " + file.getName() + " to readonly");
            }
            file.setReadOnly();
          }
        }
      }
    }


    public int size() {
      return parts.size();
    }

  }


  private class JournalCheckThread implements Runnable {

    public static final long JOURNAL_EMPTY = -2;
    private long currentTransactionId = -1;
    private final Set<Long> ignoreIds = new HashSet<Long>();
    private final List<ReplayTransaction> transactionsToReplay = new ArrayList<>();
    private boolean transactionsAreComplete = false;
    private int waitingForClass = 0;
    private final Object askThreadWaits = new Object();
    private Throwable error;

    public void run() {
      boolean first = true;
      ReplayTransaction currentContext = null;
      File journalFile = null;
      try {
        File[] journalFiles = journal.getJournalFiles();

        outer : for (int i = journalFiles.length - 1; i >= 0; i--) {
          journalFile = journalFiles[i];

          //suche darin von hinten solange, bis eine transaktion gefunden ist, die vollständig abgearbeitet wurde
          //alle neueren transaktionen merken und später in umgekehrter reihenfolge (älteste zuerst)
          //der memory queue zum abarbeiten geben.

          //falls man einer tabelle begegnet, die unbekannt ist, das parsing unterbrechen und später fortsetzen, 
          //wenn diese methode erneut aufgerufen wird

          if (logger.isDebugEnabled()) {
            logger.debug("checking " + journalFile.getName());
          }
          BufferedReader r = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(journalFile, 1024*1024), Constants.DEFAULT_ENCODING), 1024*1024);
          try {
            boolean skipTransaction = false;
            while (true) {
              String line = r.readLine();
              if (line == null) {
                if (currentTransactionId < 0) {
                  //ohne die aktuelle transaktionsid müsste man alle zugehörigen tabellen-xmls nach der entsprechenden id durchsuchen
                  //die weiß man aber hier nicht.
                  //FIXME
                  logger.warn("Found empty journal file " + journalFile.getAbsolutePath());
                  currentTransactionId = JOURNAL_EMPTY;
                  //wenn man nun im nächstälteren file eine transactionsid findet, ist es ok. usecase: im aktuellen journalfile steht nur eine IGNORE.
                }
                break; //nächstes file
              }

              if (line.startsWith(TransactionPartID.COMMIT.name())) {
                TXCommit commit = TXCommit.parse(line);
                if (currentTransactionId < 0) {
                  currentTransactionId = commit.getId();
                  if (logger.isTraceEnabled()) {
                    logger.trace("found txid " + currentTransactionId);
                  }
                }

                if (ignoreIds.contains(commit.getId())) {
                  skipTransaction = true;
                  continue;
                }
                skipTransaction = false;

                boolean transactionIsComplete = true;
                for (String tableName : commit.getTables()) {
                  TableInfo tableInfo = memoryQueue.getTableInfoForJournal(tableName);
                  if (tableInfo.needsToBeReplayed(commit.getId())) {
                    if (logger.isTraceEnabled()) {
                      logger.trace("table " + tableName + ": " + tableInfo.transactionLastSavedToFile.get()
                          + " <-> commit=" + commit.getId());
                    }
                    transactionIsComplete = false;
                    break;
                  }
                }

                if (transactionIsComplete) {
                  /*
                   * TODO ganz sauber ist das so nicht. es könnte evtl passieren,
                   * dass vor einem crash ein file nicht geschrieben werden könnte, danach aber
                   * eine transaktion auf anderen files erfolgreich durchgeführt werden konnte.
                   * dann genügt es nicht, nur eine erfolgreiche transaktion zu finden, sondern
                   * man müsste für jede tabelle checken, ob die letzte transaktion enthalten ist
                   */
                  if (logger.isDebugEnabled()) {
                    logger.debug("found complete transaction " + commit.getId());
                  }
                  break outer;
                }
                if (first) {
                  if (logger.isInfoEnabled()) {
                    logger.info("found incomplete transaction: " + commit.getId() + ". replaying all missed transactions.");
                  }
                  first = false;
                }

                //nicht complete, also aktuelle transaction merken und nächste checken
                currentContext = new ReplayTransaction(journalFile.getAbsolutePath(), commit.getTables(), commit.getId());
                transactionsToReplay.add(currentContext);
              } else if (line.startsWith(TransactionPartID.ROLLBACK.name())) { //FIXME achtung, wenn zeilen in xml content mit ROLLBACK beginnen, gibt es probleme
                skipTransaction = false;
                TXRollback rollback = TXRollback.parse(line);
                ignoreIds.add(rollback.getId());
              } else if (line.trim().length() == 0) {
                continue; //ntbd
              } else if (line.startsWith("IGNORE")) {
                skipTransaction = true;
                continue;
              } else {
                if (skipTransaction) {
                  continue;
                }
                
                if (currentContext == null) {
                  /*
                   * kein commit/rollback am ende des journals -> journal wurde entweder manuell editiert oder ein kill 
                   * des servers während des schreibens des commits hat es nicht beendet.
                   * -> verwerfen des eintrags und als "IGNORE" markieren, damit nicht danach weitere transaktionen angehängt werden, die dann parsingprobleme bereiten.
                   */
                  logger.warn("Missing COMMIT at end of journal " + journalFile.getAbsolutePath() + ", marking as IGNORE.");
                  BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(journalFile, true)));
                  try {
                    bw.write("\nIGNORE\n");
                    bw.flush();
                  } finally {
                    bw.close();
                  }
                  skipTransaction = true;
                  continue;
                }
              }
            }
          } finally {
            r.close();
          }

        }

        //alle gefundenen transaktionen replayen
        if (transactionsToReplay.size() > 0) {
          if (logger.isInfoEnabled()) {
            logger.info("preparing " + transactionsToReplay.size() + " transactions for replay ...");
          }
          ReplayContext rc = new ReplayContext();
          try {
            for (int i = transactionsToReplay.size() - 1; i >= 0; i--) {
              currentContext = transactionsToReplay.get(i);
              memoryQueue.addForReplay(currentContext, rc);
            }
          } finally {
            rc.cleanup();
          }
        }


      } catch (Throwable t) {
        error = new RuntimeException("Exception processing journal file " + journalFile.getAbsolutePath() + ".", t);
      } finally {
        logger.info("Old journals have been read (" + journal.pliId + ").");
        synchronized (askThreadWaits) {
          transactionsAreComplete = true;
          askThreadWaits.notifyAll();
        }
        transactionsToReplay.clear();
        ignoreIds.clear();
      }
    }


    /**
     * wartet darauf, ob check entweder ein unbekanntes storable findet (returns false)
     * oder fertig wird (returns true)
     * @return
     * @throws fehler, den der journalthread hatte (falls vorhanden)
     */
    public boolean getCheckResult() {
      checkError();
      final int waitingForClassBefore;
      synchronized (askThreadWaits) {
        if (transactionsAreComplete) {
          checkError();
          return true;
        }
        waitingForClassBefore = waitingForClass;
      }
      while (true) {
        synchronized (askThreadWaits) {
          if (transactionsAreComplete) {
            checkError();
            return true;
          }

          if (waitingForClass > waitingForClassBefore) {
            checkError();
            return false;
          } else {
            logger.debug("Waiting for journal check thread ... ");
            try {
              askThreadWaits.wait();
            } catch (InterruptedException e) {
            }
          }
        }
      }
    }


    private void checkError() {
      if (error != null) {
        throw new RuntimeException("XML Persistence Layer Journal could not be read successfully (" + XMLPersistenceLayer.this.cachePLName + ")", error);
      }
    }


    public long getCurrentTransactionId() {
      synchronized (askThreadWaits) {
        return currentTransactionId;
      }
    }

  }

  private class Journal {

    private static final long CNT_NOT_SET = -2;

    private final Pattern fileNamePattern = Pattern.compile("(\\d+)_(\\d+)\\.journal");
    private final long pliId;

    private final AtomicLong transactionCnt = new AtomicLong(CNT_NOT_SET);
    private final TreeSet<Long> openTransactions = new TreeSet<Long>();
    private final ReentrantLock openTransactionsLock = new ReentrantLock();
    private final Condition condition = openTransactionsLock.newCondition();
    private JournalCheckThread checkThread;
    private BufferedWriter currentJournal_Writer;
    private volatile File currentJournal_File;


    public Journal(long pliID) {
      this.pliId = pliID;
    }


    /**
     * initialisiert auch {@link #transactionCnt}
     * @return true falls check abgeschlossen/transaktionen vollständig nachgeholt
     */
    public boolean checkLastTransactions(MemoryQueue memoryQueue) {

      synchronized (this) {
        if (checkThread == null) {
          checkThread = new JournalCheckThread();
          new Thread(checkThread, "Journal Check Thread").start();
        }
      }

      boolean b = checkThread.getCheckResult();
      if (b) {
        if (checkThread.getCurrentTransactionId() == JournalCheckThread.JOURNAL_EMPTY) {
          throw new RuntimeException("XML Persistence Layer " + pliId + " can not be initialized. Last journal file is empty."
              + " It must contain at least one line \n   COMMIT <txid> <tablename> COMMIT\ncontaining the last transaction id.");
        }
        if (transactionCnt.compareAndSet(CNT_NOT_SET, checkThread.getCurrentTransactionId())) {
          logger.info("Initialized transaction cnt to " + checkThread.getCurrentTransactionId() + " for pl " + pliId + ".");
        }
      }
      if (logger.isDebugEnabled()) {
        logger.debug("Check Thread is finished: " + b);
      }
      return b;
    }


    public void setTransactionId(TransactionContext transactionContext) {
      int i = 0;
      while (transactionCnt.get() == CNT_NOT_SET) {
        if (i++ % 100 == 0) {
          logger.debug("waiting for initialization ...");
        }
        try {
          Thread.sleep(100);
        } catch (InterruptedException e) {
        }
      }
      long l;
      openTransactionsLock.lock();
      try {
        l = transactionCnt.incrementAndGet();
        openTransactions.add(l);
      } finally {
        openTransactionsLock.unlock();
      }
      transactionContext.setTransactionId(l);
    }


    public void write(TransactionContext transactionContext, String lines) throws IOException {
      boolean success = false;
      openTransactionsLock.lock();
      try {
        while (openTransactions.first() != transactionContext.getId()) {
          //TODO die verantwortung zum schreiben der anderen id übergeben. dann kann der andere gebatched ins journal schreiben. 
          try {
            condition.await();
          } catch (InterruptedException e) {
          }
        }

        synchronized (this) {
          openTransactions.remove(transactionContext.getId());
          condition.signalAll();
          openTransactionsLock.unlock();
          success = true;

          BufferedWriter bw = getOrCreateCurrentJournal();
          try {
            bw.write(lines);
            bw.newLine();
            flush(bw);
          } catch (IOException e) {
            reopenWriter(e);
            throw e;
          }
          transactionContext.setXmlsReadOnly();
        }
        if (logger.isTraceEnabled()) {
          logger.trace("Wrote transaction " + transactionContext.getId() + " to journal " + currentJournal_File.getName());
        }
      } finally {
        if (!success) {
          openTransactionsLock.unlock();
        }
      }
    }


    private void reopenWriter(IOException orig) throws IOException {
      logger.warn("reopening journal writer after write error", orig);
      if (currentJournal_Writer != null) {
        try {
          currentJournal_Writer.close();
        } catch (IOException e) {
          logger.trace("could not close writer", e);
        }
      }
      openCurrentFile();
    }


    private void flush(BufferedWriter bw) throws IOException {
      /*
       * AFS z.b. überträgt die änderungen bei einem flush nicht an den server, sondern hält die änderung nur clientseitig. dann muss 
       * ein close aufgerufen werden, damit bei einem crash keine daten verloren gehen.
       */
      if (closeToFlush) {
        bw.flush();
        bw.close();
        openCurrentFile();
      } else {
        bw.flush();
      }
    }

    private void openCurrentFile() throws UnsupportedEncodingException, FileNotFoundException {
      currentJournal_Writer =
          new BufferedWriter(new OutputStreamWriter(new FileOutputStream(currentJournal_File, true), Constants.DEFAULT_ENCODING));
    }
    
    private BufferedWriter getOrCreateCurrentJournal() throws IOException {
      try {
        BufferedWriter bw = getOrCreateCurrentJournalInternal();
        return bw;
      } catch( Exception e ) {
        logger.warn("Failed to getOrCreateCurrentJournal", e);
        try {
          currentJournal_File = null;
          currentJournal_Writer.close();
        } catch( Throwable t ) {
          //im finally ignorieren...
        }
        if( e instanceof RuntimeException ) {
          throw (RuntimeException)e;
        } else {
          throw new RuntimeException(e);
        }
      }
    }
    
    private BufferedWriter getOrCreateCurrentJournalInternal() throws IOException {
      if (currentJournal_File == null) {
        File[] existingFiles = getJournalFiles();
        if (existingFiles.length > 0) {
          currentJournal_File = existingFiles[existingFiles.length - 1];
          if (logger.isDebugEnabled()) {
            logger.debug("reusing journal file " + currentJournal_File.getName());
          }
        } else {
          File path = new File(getBasePath());
          if (!path.exists()) {
            if (!path.mkdirs()) {
              throw new RuntimeException("could not create path" + path.getAbsolutePath());
            }
          }
          File f = new File(path, pliId + "_0.journal");           
          if (!f.createNewFile()) {
            throw new RuntimeException("could not create journal file " + f.getAbsolutePath());
          }
          if (logger.isInfoEnabled()) {
            logger.info("created new journal file " + f.getName());
          }
          currentJournal_File = f;
        }
        openCurrentFile();
      } else {
        long len = currentJournal_File.length();
        if (len >= journalMaxLengthPerFile.get()) {
          //maximale journal file size erreicht. neues öffnen
          Matcher m = fileNamePattern.matcher(currentJournal_File.getName());
          if (!m.matches()) {
            throw new RuntimeException();
          }
          long nextJournalId = Long.valueOf(m.group(2)) + 1;
          currentJournal_Writer.close();
          currentJournal_File = new File(getBasePath(), pliId + "_" + nextJournalId + ".journal");
          if (!currentJournal_File.createNewFile()) {
            throw new RuntimeException("could not create journal file " + currentJournal_File.getAbsolutePath());
          }
          if (logger.isInfoEnabled()) {
            logger.info("created new journal file " + currentJournal_File.getName());
          }
          openCurrentFile();
        } else if (len == 0) {
          if (!currentJournal_File.exists()) {
            logger.error("Journal file " + currentJournal_File.getName() + " not found unexpectedly. Creating new one...");
            File old = currentJournal_File;
            currentJournal_File = null;
            //gelöscht? -> neu erstellen
            boolean success = false;
            try {
              BufferedWriter bw = getOrCreateCurrentJournal();
              success = true;
              return bw;
            } finally {
              if (!success) {
                logger.warn("Could not create new Journal.");
                currentJournal_File = old; //falls netzwerk-filesystem o.ä. (nfs, afs), vielleicht gibts das file nach retry wieder?
              }
            }
          }
        }
      }

      return currentJournal_Writer;
    }


    public void writeRollback(TransactionContext transactionContext) throws IOException {
      String line = TXRollback.asString(transactionContext);

      synchronized (this) {
        BufferedWriter bw = getOrCreateCurrentJournal();
        try {
          bw.write(line);
          bw.newLine();
          flush(bw);
        } catch (IOException e) {
          reopenWriter(e);
          throw e;
        }
      }
    }


    public void invalidateTransactionId(TransactionContext transactionContext) {
      openTransactionsLock.lock();
      try {
        openTransactions.remove(transactionContext.getId());
        condition.signalAll();
      } finally {
        openTransactionsLock.unlock();
      }
    }


    private String getBasePath() {
      String filePath = new StringBuffer(Constants.STORAGE_PATH).append(File.separator).append(dir).append(File.separator).toString();
      return filePath.toString();
    }


    /**
     * in reihenfolge von alt nach neu
     */
    public File[] getJournalFiles() {
      File[] journals = new File(getBasePath()).listFiles(new FilenameFilter() {

        public boolean accept(File dir, String name) {
          if (name.endsWith(".journal")) {
            if (name.startsWith(pliId + "_")) {
              return true;
            }
          }
          return false;
        }

      });
      if (journals == null) {
        logger.info("could not find journal files in " + getBasePath());
        return new File[0];
      }
      Arrays.sort(journals, new Comparator<File>() {

        public int compare(File o1, File o2) {
          long l1 = get(o1);
          long l2 = get(o2);
          if (l1 < l2) {
            return -1;
          } else if (l1 > l2) {
            return 1;
          }
          return 0;
        }


        private long get(File o1) {
          Matcher m = fileNamePattern.matcher(o1.getName());
          if (m.matches()) {
            return Long.valueOf(m.group(2));
          }
          return -1;
        }

      });
      return journals;
    }


    public void shutdown() {
      try {
        synchronized (this) {
          if (currentJournal_Writer != null) {
            currentJournal_Writer.close();
            currentJournal_Writer = null;
          }
        }
      } catch (IOException e) {
        String fileName = "unknown";
        if (currentJournal_File != null) {
          fileName = currentJournal_File.getName();
          currentJournal_File = null;
        }
        logger.warn("Could not close " + fileName, e);
      }
    }


    public void updateTxIdIfNecessary(long txId) {
      while (true) {
        long current = transactionCnt.get();
        if (txId > current) {
          if (transactionCnt.compareAndSet(current, txId)) {
            break;
          }
        } else {
          break;
        }
      }
    }
  }
  
  private static class ModificationRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    
  }
  
  private static class ReplayTransaction {
    
    private final String filename;
    private final String[] tableNames;
    private final long transactionid;
    
    public ReplayTransaction(String filename, String[] tableNames, long transactionId) {
      this.filename = filename;
      this.tableNames = tableNames;
      this.transactionid = transactionId;
    }

    public TransactionContext restore(TransactionContext tc, ReplayContext rc) {
      tc.setTransactionId(transactionid);
      tc.addTableNamesForReplay(tableNames);
      rc.setActiveFile(filename);
      List<String> txLines = rc.readLines(transactionid);
      tc.addLinesForReplay(txLines);
      tc.restoreFromLines();
      return tc;
    }

    public String[] getTables() {
      return tableNames;
    }
    
  }
  
  
  private static class ReplayContext {
    
    private String activeFile = null;
    
    private BufferedReader journalFileReader;
    

    public void setActiveFile(String filename) {
      if (activeFile == null || !activeFile.equals(filename)) {
        //close
        cleanup();
        //open
        try {
          journalFileReader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), Constants.DEFAULT_ENCODING));
        } catch (UnsupportedEncodingException e) {
          throw new RuntimeException(e);
        } catch (FileNotFoundException e) {
          throw new RuntimeException("journal file " + filename + " went missing unexpectedly", e);
        }
        this.activeFile = filename;
      }
    }

    public List<String> readLines(long transactionid) {
      List<String> lines = new ArrayList<>();
      String line;
      try {
        while (null != (line = journalFileReader.readLine())) {
          if (line.startsWith(TransactionPartID.COMMIT.name())) {
            TXCommit commit = TXCommit.parse(line);
            if (commit.getId() == transactionid) {
              break;
            } else if (commit.getId() < transactionid) {
              //diese transaktion ist nicht die gewünschte - überspringen
              lines.clear();
            } else {
              throw new RuntimeException("transactionid " + transactionid + " not found in " + activeFile);
            }
          } else {
            lines.add(line);
          }
        }
      } catch (IOException e) {
        throw new RuntimeException("Could not read from journal " + activeFile, e);
      }
      return lines;
    }

    public void cleanup() {
      if (journalFileReader != null) {
        try {
          journalFileReader.close();
        } catch (IOException e) {
          logger.info("could not close journal file " + activeFile, e);
        }
      }
    }
    
  }

  private static class TableInfo {

    public static final String ATT_TRANSACTION_ID = "transaction";
    public static final long TXID_NOT_INITIALIZED = -16L; //loadfromfile wurde noch nicht aufgerufen
    public static final long TXID_OVERRIDE = -17L;
    public static final long TXID_NOT_SET_IN_XML = -18L;
    private final AtomicLong transactionLastSavedToFile = new AtomicLong(TXID_NOT_INITIALIZED);
    private final AtomicLong currentTransactionId = new AtomicLong(TXID_NOT_INITIALIZED);
    //PK toString -> storable
    private final Map<String, Storable<?>> rows = new ConcurrentHashMap<String, Storable<?>>();
    public final List<ReplayTransaction> toReplay = new ArrayList<ReplayTransaction>();
    private final AtomicLong lastModified = new AtomicLong(-1L);
    private final AtomicBoolean needsToBeSetToReadOnly = new AtomicBoolean(true);
    private final String tableName;
    private boolean initializedFromJournal = false;
    private boolean initializedFromAddTable = false;
    
    public TableInfo(String tableName) {
      if (logger.isTraceEnabled()) {
        logger.trace("created tableinfo for " + tableName);
      }
      this.tableName = tableName;
    }

    public String toString() {
      StringBuilder sb = new StringBuilder();
      sb.append("cnt=").append(rows.size()).append(",lastsaved=");
      sb.append(printIdPretty(transactionLastSavedToFile.get()));
      sb.append(",current=");
      sb.append(printIdPretty(currentTransactionId.get()));
      if (toReplay.size() > 0) {
        sb.append(",replay=" + toReplay.size());
      }
      return sb.toString();
    }

    public boolean needsToBeReplayed(long txId) {
      long currentId = transactionLastSavedToFile.get();
      if (currentId == TXID_NOT_SET_IN_XML) {
        return true;
      }
      if (currentId == TXID_NOT_INITIALIZED) {
        return true; //file existiert nicht
      }
      if (currentId == TXID_OVERRIDE) {
        return false;
      }
      if (currentId < txId) {
        return true;
      }
      return false;
    }

    private String printIdPretty(long l) {
      if (l == TXID_NOT_INITIALIZED) {
        return "new";
      } else if (l == TXID_NOT_SET_IN_XML) {
        return "empty";
      } else if (l == TXID_OVERRIDE) {
        return "override";
      }
      return String.valueOf(l);
    }

    public boolean needsToBeSetToReadOnly() {
      while (needsToBeSetToReadOnly.get()) {
        if (needsToBeSetToReadOnly.compareAndSet(true, false)) {
          return true;
        }
      }
      return false;
    }

    private void setSavedToFile(long txId) {
      needsToBeSetToReadOnly.set(true);
      transactionLastSavedToFile.set(txId);
    }
    
    public Map<String, Storable<?>> getRows() {
      return rows;
    }


    public boolean isCompletelySavedToFile() {
      long c = currentTransactionId.get();
      if (c == TXID_NOT_INITIALIZED && rows.size() == 0) {
        return true;
      }
      return transactionLastSavedToFile.get() == c && c != TXID_NOT_SET_IN_XML;
    }


    public void addForReplay(ReplayTransaction rt) {
      toReplay.add(rt);      
    }


    public static long readTransactionId(Element root, File file) {
      long txId = TXID_NOT_SET_IN_XML;
      String txIdString = root.getAttribute(TableInfo.ATT_TRANSACTION_ID);
      if (txIdString != null && txIdString.length() > 0) {
        if (txIdString.equalsIgnoreCase("override")) {
          txId = TableInfo.TXID_OVERRIDE;
        } else {
          try {
            txId = Long.valueOf(txIdString);
            if (txId < 0) {
              logger.warn("invalid value for transaction id in file " + file.getAbsolutePath());
              txId = TXID_NOT_SET_IN_XML;
            }
          } catch (NumberFormatException e) {
            logger.warn("invalid value for transaction id in file " + file.getAbsolutePath());
          }
        }
      }
      return txId;
    }
    
    public boolean isInitializedFromJournal() {
      return this.initializedFromJournal;
    }
    public boolean isInitializedFromAddTable() {
      return this.initializedFromAddTable;
    }
    public void setInitializedFromJournal() {
      this.initializedFromJournal = true;
    }
    public void setInitializedFromAddTable() {
      this.initializedFromAddTable = true;
    }

    public void setCurrentTransactionId(long txId) {
      //StackTraceElement[] cs = Thread.currentThread().getStackTrace();
      if (currentTransactionId.get() > txId && txId >= 0) {
        logger.warn("set currTxId = " + txId, new Exception());
      }
      currentTransactionId.set(txId);
    }

    public boolean isNotInitialized() {
      return currentTransactionId.get() == TXID_NOT_INITIALIZED;
    }
   
  }

  private class MemoryQueue implements Runnable {

    private volatile boolean running = true; //soll-zustand
    private volatile boolean isRunning = false; //thread-zustand
    private final ConcurrentMap<String, TableInfo> tableInfos = new ConcurrentHashMap<String, TableInfo>();
    private final ConcurrentLinkedQueue<TransactionContext> transactionQueue = new ConcurrentLinkedQueue<TransactionContext>();
    private final XMLPersistenceLayer pl; //für das asynchrone schreiben ins xml
    private final ReusableCountDownLatch rcdl = new ReusableCountDownLatch(1);
    private boolean doSleep = true;
    private final BackupManagerIfc backupManager;
    
    public MemoryQueue() throws PersistenceLayerException {
      //ohne cache initialisieren
      pl = new XMLPersistenceLayer();
      pl.init(-4L, dir, TransactionMode.FULL_TRANSACTION.name(), "false");
      
      boolean backupEnabled = "true".equals(System.getProperties().get(BackupConfig.
                                                                       SYSTEM_PROPERTY_NAME_FOR_ENABLED));
      logger.debug("XML persistence backups enabled: " + backupEnabled);
      if (backupEnabled) {
        backupManager = new BackupManager(pl.getConnection());
      } else {
        backupManager = new NoBackup();
      }
    }

    private void awake() {
      synchronized (this) {
        doSleep = false;
        notifyAll();
      }
    }

    private void sleep(long sleep) throws InterruptedException {
      synchronized (this) {
        if( doSleep ) {
          wait(sleep);
        }
        doSleep = true;
      }
    }
    
    
    private TableInfo getOrCreateTableInfo(String tableName, boolean tableMustBeRegistered) {
      TableInfo ti = null;
      synchronized (this) {
        ti = tableInfos.get(tableName);
        if (ti != null) {
          return ti;
        }
        if (tableMustBeRegistered && !tableNames.containsValue(tableName)) {
          throw new RuntimeException();
        }
        ti = new TableInfo(tableName);
        TableInfo ti2 = tableInfos.putIfAbsent(tableName, ti);
        if (ti2 != null) {
          logger.error("Unexpected synchronization problem: Table info created twice.");
        }
      }
      synchronized (ti) {
        handleBackupFiles(tableName);
      }
      return ti;
    }
    
    
    private void handleBackupFiles(String tableName) {      
      try {
        XMLPersistenceLayerConnection con = new XMLPersistenceLayerConnection();        
        discoverBackupFiles(con, tableName);
        try {
          File file = con.getFile(dir, tableName, true);
          con.lockFile(file);
          try {
            if (!con.existsNonEmpty(file)) {
              backupManager.tryRestoreFromBackup(tableName, file);
            }          
          } finally {
            con.unlockFile(file);
          }
        } finally {
          con.closeConnection();
        }
      } catch (PersistenceLayerException e) {
        logger.warn("Error handling xml backup files for table " + tableName + ": " + e.getMessage(), e);
      }
    }
    
    
    public TableInfo getTableInfoForJournal(String tableName) {
      final TableInfo ti = getOrCreateTableInfo(tableName, false);
      synchronized (ti) {
        if (ti.isInitializedFromJournal() || ti.isInitializedFromAddTable()) {
          return ti;
        }
        //txid direkt aus file lesen
        try {
          XMLPersistenceLayerConnection con = new XMLPersistenceLayerConnection();
          try {
            File file = con.getFile(dir, tableName, true);
            con.lockFile(file);
            try {
              if (con.existsNonEmpty(file)) {
                Document doc = con.parseFile(file);
                Element root = doc.getDocumentElement();
                long txId = TableInfo.readTransactionId(root, file);
                if (logger.isTraceEnabled()) {
                  logger.trace("setting transaction last saved to " + txId + " for table " + tableName);
                }
                ti.setSavedToFile(txId);
                journal.updateTxIdIfNecessary(txId);
              }
            } finally {
              con.unlockFile(file);
            }
          } finally {
            con.closeConnection();
          }        
          
        } catch (PersistenceLayerException e) {
          logger.warn("could not read " + tableName + " xml", e);
        }
        ti.setInitializedFromJournal();        
      }
      return ti;
    }

    
    private void discoverBackupFiles(XMLPersistenceLayerConnection con, String tableName) {
      logger.trace("Entering discoverBackupFiles for " + tableName);
      File[] files = backupManager.searchBackupFilesOfTable(tableName);
      for (File file : files) {
        logger.trace("Found backup file " + file.getPath());
        try {
          Document doc = XMLUtils.parse(file);
          Element root = doc.getDocumentElement();
          long txId = TableInfo.readTransactionId(root, file);
          backupManager.registerDiscoveredBackupFile(tableName, txId, file);
        } catch (Exception e) {
          logger.warn("Error parsing discovered backup file " + file.getPath() + ": " + e.getMessage(), e);
        }
      }
    }

    
    public boolean awaitStore(long timeout) {
      //TODO solange noch etwas geschrieben werden kann, weiter warten - nicht abbrechen. usecase: sehr viel nachzuholen.
      long maxEndTime = System.currentTimeMillis() + timeout;
      int cnt = 0;
      while (isRunning) {
        if (System.currentTimeMillis() > maxEndTime) {
          return false;
        }
        awake();
        if (transactionQueue.size() > 0) {
          try {
            Thread.sleep(10);
          } catch (InterruptedException e) {
          }
          continue;
        }
        boolean finished = true;
        for (Entry<String, TableInfo> tiEntry : tableInfos.entrySet()) {
          TableInfo ti = tiEntry.getValue();
          if (!ti.isCompletelySavedToFile()) {
            finished = false;
            if (logger.isDebugEnabled()) {
              if (cnt++ % 200 == 0) {
                logger.debug("tableInfo: " + tiEntry.getKey() + "->" + ti);
              }
            }
            try {
              Thread.sleep(10);
            } catch (InterruptedException e) {
            }
            break;
          }
        }
        if (finished) {
          return true;
        }
      }
      return true;
    }

    public boolean awaitReload(long timeout) {
      //Damit sicher ist, dass der Reload wirklich durchgeführt wurde, muss eine vollständiger 
      //Durchlauf abgewartet werden. Es reicht nicht, dass ein Durchlauf fertig wurde, da er
      //zu Beginn die gewünschte Tabelle übersprungen haben kann.
      if( ! isRunning ) {
        return false;
      }
      long start = System.currentTimeMillis();
      try {
        CountDownLatch cdl = rcdl.prepareLatch();
        //Thread wecken
        awake();
        if( ! cdl.await(timeout, TimeUnit.MILLISECONDS) ) {
          return false;
        }
      } catch (InterruptedException e) {
        //dann halt nicht länger warten
        return false;
      }
      
      long remainingTimeout = start+timeout - System.currentTimeMillis();
      
      //Einen vollständigen Durchlauf abwarten
      try {
        CountDownLatch cdl = rcdl.prepareLatch();
        //Thread wecken
        awake();
        return cdl.await(remainingTimeout, TimeUnit.MILLISECONDS);
      } catch (InterruptedException e) {
        //dann halt nicht länger warten
        return false;
      }
    }


    public void addForReplay(ReplayTransaction replayTransaction, ReplayContext rc) {
      for (String tableName : replayTransaction.getTables()) {
        TableInfo ti = getTableInfoForJournal(tableName);
        
        synchronized (ti) {
          if (ti.isNotInitialized()) {
            //loadFromFile wurde noch nicht aufgerufen
            ti.addForReplay(replayTransaction);
            continue;
          }

          //transaktion direkt nachholen, loadfrom file wurde bereits aufgerufen.
          if (ti.toReplay.size() > 0) {
            throw new RuntimeException();
          }
          synchronized (this) {
            TransactionContext currentContext = new TransactionContext(); 
            replayTransaction.restore(currentContext, rc);
            long txId = TableInfo.TXID_NOT_SET_IN_XML;
            for (TransactionPart tp : currentContext.parts) {
              if (tp instanceof TXCommit) {
                continue;
              }
              if (tp.getTableName().equals(tableName)) {
                tp.apply(memoryQueue, currentContext.getId());
                txId = currentContext.transactionId;
              }
            }
            if (txId == TableInfo.TXID_NOT_SET_IN_XML) {
              continue; //nichts getan
            }
            if (logger.isInfoEnabled()) {
              logger.info("Replayed transaction " + currentContext.getId() + " for " + tableName + ".");
            }
            ti.setCurrentTransactionId(txId);
          }
          ti.toReplay.clear();
        }
      }
    }


    public void add(TransactionContext transactionContext) {
      transactionQueue.add(transactionContext);
    }


    public void startThread() {
      Thread t = new Thread(this, "XML PL Memory Queue Worker (" + journal.pliId + ")");
      t.start();
    }


    public void run() {
      int notInitializedCnt = 0;
      isRunning = true;
      while (running) {
        try {
          TransactionContext transaction = transactionQueue.peek();
          if (transaction == null || transaction.state == TransactionContextState.UNKNOWN) {
            long sleep = memoryQueueWorkerSleepTime.getMillis() + random.nextInt(1000) + 1;
            boolean initialized = saveModifiedDOMsToAndDeleteOldJournals();
            rcdl.countDown();
            if (!initialized) {
              notInitializedCnt++;
              if (notInitializedCnt > Integer.MAX_VALUE / 2) {
                notInitializedCnt = 100;
              }
              sleep = (long) (sleep * Math.min(notInitializedCnt, 50) / 10.0);
            } else {
              notInitializedCnt = 0;
            }
            if (logger.isTraceEnabled()) {
              logger.trace("sleeping until " + Constants.defaultUTCSimpleDateFormat().format(new Date(System.currentTimeMillis() + sleep)));
            }
            try {
              sleep(sleep); 
            } catch (InterruptedException e) {
            }
            continue;
          }

          transactionQueue.poll(); //entnehmen

          if (transaction.state == TransactionContextState.VALID) {
            saveTransactionToDOMs(transaction);
          } else {
            //invalid verwerfen
            continue;
          }
        } catch (Throwable t) {
          Department.handleThrowable(t);
          logger.error("Memory Queue Thread had unexpected error. ignoring error and continuing anyway.", t);
          long sleep = memoryQueueWorkerSleepTime.getMillis() + random.nextInt(1000) + 1;
          
          try {
            sleep(sleep);
          } catch (InterruptedException e) {
          }
        }
      }
      isRunning = false;
    }


    private void saveTransactionToDOMs(TransactionContext transaction) {
      long id = transaction.getId();
      for (TransactionPart tp : transaction.parts) {
        tp.apply(this, id);
      }
    }


    private boolean saveModifiedDOMsToAndDeleteOldJournals() {
      long oldestOpenTransaction = Long.MAX_VALUE;
      long highestTransactionDone = -2;
      forloop: for (Entry<String, TableInfo> tiEntry : tableInfos.entrySet()) {
        final String tableName = tiEntry.getKey();
        final TableInfo ti = tiEntry.getValue();
        try {
          while (true) {
            if (ti.toReplay.size() > 0 || (ti.isNotInitialized() && ti.rows.size() == 0)) {
              continue forloop;
            }
            if (ti.isCompletelySavedToFile()) {
              try {
                File f = pl.getConnection().getFile(dir, tableName, true);
                if (ti.lastModified.get() == -1L || f.lastModified() == ti.lastModified.get()) {
                  if (logger.isTraceEnabled()) {
                    logger.trace("skipping " + tableName + ".");
                  }
                  continue forloop;
                }
                logger.debug("XML " + f.getName() + " seems to have been changed.");
                //else: unten nochmal auf den konflikt stossen und cache updaten.
              } catch (PersistenceLayerException e) {
                logger.warn("could not check file for storable " + tableName, e);
              }
            }
            //storables über "alten" pl ins xml speichern
            Collection<Storable<?>> storables;
            long txId;
            synchronized (ti) {
              storables = ti.rows.values();
              txId = ti.currentTransactionId.get();
              if (txId == TableInfo.TXID_NOT_SET_IN_XML) {
                txId = journal.transactionCnt.incrementAndGet();
                ti.setCurrentTransactionId(txId);
              }
            }
            
            String tableNameLC = tableName.toLowerCase();
            final Class<? extends Storable> clazz;
            try {
              //bei mehreren registrierten storables für den tablename (applications) kann hier eine beliebige klasse genommen werden,
              //weil die klasse nur für den datentransfer von journal->xml verwendet wird
              clazz = classes.lazyCreateGet(tableNameLC).getAnyElement();
            } finally {
              classes.cleanup(tableNameLC);
            }
            if (clazz == null) {
              continue forloop; //wird vermutlich später noch registriert
            }

            XMLPersistenceLayerConnection con;
            try {
              con = pl.getConnection();
            } catch (PersistenceLayerException e) {
              throw new RuntimeException(e);
            }
            try {
              //falls backupManager das file moved, muss trotzdem noch das file auf modification gecheckt werden
              final long lastModified = new File(con.getFilePathByTableName(tableName)).lastModified();
              backupManager.backupCurrentFile(tableName, txId);
              con.lastModificationChangeHandler = new LastModificationChangeHandler() {

                public long getStoredLastModified(File originalFile) {
                  return ti.lastModified.get();
                }

                public void afterRename(File originalFile) {
                  ti.lastModified.set(originalFile.lastModified());
                }

                public boolean checkModified(TransactionFile newFile) {
                  if (backupManager.newBackupDoesClearTable()) {
                    //vergleich gegen das lastmodified vor der backup-erstellung
                    return getStoredLastModified(null) != lastModified;
                  } else {
                    return !newFile.checkNotModified();
                  }
                }

                public void handleFileIsModified(File originalFile) {
                  /*
                   * 1. file lesen und in cache übernehmen
                   * 2. tableinfo aktualisieren
                   * 3. aktuellen stand trotzdem schreiben, damit die txid im xml auf dem neusten stand steht.
                   */
                  logger.warn("File " + originalFile.getAbsolutePath() + " has been changed externally.");
                  try {
                    Pair<Long, Collection<? extends Storable>> storables;
                    XMLPersistenceLayerConnection con = pl.getConnection();
                    try {
                      storables = con.loadCollectionAndTxId((Class) clazz);
                    } finally {
                      con.closeConnection();
                    }
                    if (storables.getFirst() == TableInfo.TXID_OVERRIDE) {
                      logger.warn("File is flagged to override all pending changes. Refreshing cache from file "
                          + originalFile.getAbsolutePath() + " forcefully ... # entries: " + storables.getSecond().size());
                    } else if (storables.getFirst() == ti.currentTransactionId.get()) {
                      logger.warn("There are no pending changes. Refreshing cache from file " + originalFile.getAbsolutePath() + " ...");
                    } else {
                      logger.warn("There are pending changes in memory (current transaction=" + ti.currentTransactionId + ", file="
                          + storables.getFirst() + " entries=" + storables.getSecond().size()
                          + "). External changes will be overridden! (Use transaction=\"override\" if pending changes are to be ignored)");
                      return;
                    }
                    PersistenceLayerConnection cacheCon = cachePL.getConnection();
                    try {
                      cacheCon.deleteAll(clazz);
                      cacheCon.persistCollection(storables.getSecond());
                      synchronized (ti) {
                        cacheCon.commit();
                        long id = journal.transactionCnt.incrementAndGet();
                        ti.setCurrentTransactionId(id); 
                        ti.lastModified.set(originalFile.lastModified());
                        ti.rows.clear();
                        for (Storable s : storables.getSecond()) {
                          ti.rows.put(String.valueOf(s.getPrimaryKey()), s);
                        }
                      }
                    } finally {
                      cacheCon.closeConnection();
                    }
                  } catch (PersistenceLayerException e) {
                    logger.warn("Could not handle external file modification of " + tableName + ". External changes will be overridden.", e);
                  }
                  throw new ModificationRuntimeException();
                }

              };
              if (!backupManager.newBackupDoesClearTable()) {
                con.deleteAll(clazz);
              }
              con.persistCollection(storables, txId, clazz);
              con.commit();
              ti.setSavedToFile(txId);
              journal.updateTxIdIfNecessary(txId);
              if (logger.isDebugEnabled()) {
                logger.debug("Successfully saved xml for table " + tableName + ", txId = " + txId + "");
              }
              backupManager.deleteObsoleteBackupFilesOfTable(tableName);
              break; //while true
            } catch (ModificationRuntimeException e) {
              //kein break, nochmal versuchen
            } catch (PersistenceLayerException e) {
              logger.warn("Could not store xml for table " + tableName + ". Retrying in " + memoryQueueWorkerSleepTime + "ms.", e);
              break; //while true
            } finally {
              try {
                con.closeConnection();
              } catch (PersistenceLayerException e) {
                logger.warn("Could not close connection successfully", e);
              }
            }
          }
        } finally {
          if (ti.isCompletelySavedToFile()) {
            if (ti.currentTransactionId.get() > highestTransactionDone) {
              highestTransactionDone = ti.currentTransactionId.get();
            }
          } else {
            if (ti.transactionLastSavedToFile.get() < oldestOpenTransaction) {
              oldestOpenTransaction = ti.transactionLastSavedToFile.get() + 1;
            }
          }
          if (logger.isTraceEnabled() && ti.currentTransactionId.get() != TableInfo.TXID_NOT_INITIALIZED
              && ti.currentTransactionId.get() != TableInfo.TXID_NOT_SET_IN_XML) {
            logger.trace(tableName + ": current=" + ti.currentTransactionId.get() + ", saved=" + ti.transactionLastSavedToFile.get() +" lastModified " +new Date(ti.lastModified.get()) +" "+ ti.lastModified.get()%1000L  );
          }
        }
      }

      if (oldestOpenTransaction == Long.MAX_VALUE) {
        oldestOpenTransaction = highestTransactionDone + 1;
      }

      if (journal.currentJournal_File == null) {
        //erst mit löschen anfangen, wenn das aktuelle journal auch verwendet wird, damit es keine raceconditions gibt
        if (logger.isTraceEnabled()) {
          logger.trace("Not deleting anything, because journal is not initialized properly yet.");
        }
        return false;
      }
      
      if (logger.isTraceEnabled()) {
        logger.trace("Deleting journals up to txid = " + oldestOpenTransaction);
      }

      List<File> delete = new ArrayList<File>();
      File[] journalFiles = journal.getJournalFiles();
      outer : for (int i = 0; i < journalFiles.length; i++) {
        File journalFile = journalFiles[i];
        if (journalFile.equals(journal.currentJournal_File)) {
          if (journal.currentJournal_File.length() == 0) {
            //nichts löschen, wenn das aktuelle journal file noch leer ist
            delete.clear();
          }
          break;
        }
        if (i == journalFiles.length - 1) {
          //letztes file nie löschen
          break;
        }
        
        //wird es noch für replay benötigt?
        for (TableInfo ti : tableInfos.values()) {
          for (ReplayTransaction rt : ti.toReplay) {
            if (rt.filename.equals(journalFile.getAbsolutePath())) {
              continue outer;
            }
          }
        }
        
        //
        try {
         BufferedReader r = new BufferedReader(new InputStreamReader(new ReverseLineInputStream(journalFile, 1024*1024), Constants.DEFAULT_ENCODING), 1024*1024);
          try {
            while (true) {
              String line = r.readLine();
              if (line == null) {
                break outer;
              }

              if (line.startsWith(TransactionPartID.COMMIT.name())) {
                TXCommit commit = TXCommit.parse(line);
                if (logger.isTraceEnabled()) {
                  logger.trace("journal " + journalFile.getAbsolutePath() + " has transactions up to " + commit.getId());
                }
                if (commit.getId() < oldestOpenTransaction) {
                  if (backupManager.isDeletionOfJournalFileAllowed(commit.getId())) {
                    delete.add(journalFile);
                  }
                }
                break; //nächstes file
              } else if (line.trim().length() == 0) {
                //ignore
              } else {
                //unerwartet
                logger.warn("last line in old journal " + journalFile.getAbsolutePath() + " is not a commit: " + line);
                break outer;
              }
            }
          } finally {
            r.close();
          }
        } catch (IOException e) {
          logger.warn("could not access " + journalFile.getAbsolutePath(), e);
        }
      }

      for (File f : delete) {
        if (logger.isDebugEnabled()) {
          logger.debug("deleting " + f.getAbsolutePath());
        }
        if (!FileUtils.deleteFileWithRetries(f)) {
          logger.warn("could not delete journal " + f.getAbsolutePath());
        }
      }
      return true;
    }


    public <T extends Storable> void loadFromFile(Class<T> klass, final String tableName) 
                                     throws PersistenceLayerException {
      final TableInfo ti = getOrCreateTableInfo(tableName, true);
      
      XMLPersistenceLayerConnection con;
      try {
        con = pl.getConnection();
        con.lastModificationChangeHandler = new LastModificationChangeHandler() {

          public void handleFileIsModified(File originalFile) {
            logger.warn("file modification not supported at this time.");
          }

          public long getStoredLastModified(File originalFile) {
            long lm = originalFile.lastModified();
            ti.lastModified.set(lm);
            return lm;
          }

          public void afterRename(File originalFile) {
          }

          @Override
          public boolean checkModified(TransactionFile newFile) {
            return !newFile.checkNotModified();
          }
        };
      } catch (PersistenceLayerException e) {
        throw new RuntimeException(e);
      }
      
      synchronized (ti) {
        try {
          Pair<Long, Collection<T>> loadCollectionAndTxId = con.loadCollectionAndTxId(klass);
          if (!ti.isInitializedFromJournal()) {
            if (logger.isTraceEnabled()) {
              logger.trace("filling tableinfo from xml");
            }
            for (T s : loadCollectionAndTxId.getSecond()) {
              ti.rows.put(String.valueOf(s.getPrimaryKey()), s);
            }
            long txId = loadCollectionAndTxId.getFirst();
            if (txId != TableInfo.TXID_NOT_INITIALIZED) { //file existiert
              ti.setCurrentTransactionId(txId);
              ti.setSavedToFile(txId);
              journal.updateTxIdIfNecessary(txId);
            }
          } else {
            if (logger.isTraceEnabled()) {
              logger.trace("filling tableinfo from xml with replay data.");
            }
            
            //es gibt vermutlich noch transaktionen die replayed werden müssen -> in memoryqueue schreiben            
            long txId = loadCollectionAndTxId.getFirst();
            for (T s : loadCollectionAndTxId.getSecond()) {
              ti.rows.put(String.valueOf(s.getPrimaryKey()), s);
            }
            if (txId == TableInfo.TXID_OVERRIDE) {
              if (logger.isInfoEnabled()) {
                logger.info("Replay ignored, because file transaction is set to override.");
              }
            } else {
              synchronized (this) {
                ReplayContext rc = new ReplayContext();
                try {
                  for (ReplayTransaction replayTransaction : ti.toReplay) {
                    TransactionContext tc = new TransactionContext();
                    replayTransaction.restore(tc, rc);
                    for (TransactionPart tp : tc.parts) {
                      if (tp instanceof TXCommit) {
                        continue;
                      }
                      if (tp.getTableName().equals(tableName)) {
                        tp.apply(memoryQueue, tc.getId());
                        txId = tc.transactionId;
                      }
                    }
                    if (logger.isInfoEnabled()) {
                      logger.info("Replayed transaction " + tc.getId() + " for table " + tableName + ".");
                    }
                  }
                  ti.toReplay.clear();
                } finally {
                  rc.cleanup();
                }
              }             
            }
            ti.setCurrentTransactionId(txId);
            ti.setSavedToFile(loadCollectionAndTxId.getFirst());
            ti.setInitializedFromAddTable();
            journal.updateTxIdIfNecessary(loadCollectionAndTxId.getFirst());          
          }
        } finally {
          if (logger.isTraceEnabled()) {
            logger.trace("loadfromfile complete for " + tableName + ": currentTxId=" + ti.currentTransactionId.get() + ", lastSaved="
                + ti.transactionLastSavedToFile.get() + ", size=" + ti.rows.size());
          }
          try {
            con.closeConnection();
          } catch (PersistenceLayerException e) {
            logger.warn("Could not close connection successfully", e);
          }
        }
      }
    }

  }


  /**
   * regelt zugriff auf files, damit nichts durcheinander kommt
   */
  private static ConcurrentMapWithObjectRemovalSupport<File, ReentrantLockWrapper> fileLocks =
    new ConcurrentMapWithObjectRemovalSupport<File, XMLPersistenceLayer.ReentrantLockWrapper>() {

      private static final long serialVersionUID = 6380974811804083526L;

    @Override
    public ReentrantLockWrapper createValue(File key) {
      return new ReentrantLockWrapper();
    }
  };

  private static final XynaPropertyString defaultCachePL = new XynaPropertyString("xnwh.persistence.xml.cache.default", null);
  private static final XynaPropertyDuration memoryQueueWorkerSleepTime = new XynaPropertyDuration("xyna.persistence.xml.cache.writeInterval", new Duration(30000));
  private static final XynaPropertyLong journalMaxLengthPerFile = new XynaPropertyLong("xnwh.persistence.xml.journal.maxlength", 10L*1024*1024);
  static {
    defaultCachePL
        .setDefaultDocumentation(DocumentationLanguage.EN,
                                 "Persistence layer instance name. Default value for the persistence layer to be used as cache for xml persistence layers. It not set, an anonymous persistence layer will be used.");
    memoryQueueWorkerSleepTime.setDefaultDocumentation(DocumentationLanguage.EN, "Time interval in which the cache of xml persistencelayers is written to the xml file.");
    journalMaxLengthPerFile.setDefaultDocumentation(DocumentationLanguage.EN, "Maximum length of journal files in bytes.");
  }
  
  private String dir;
  private TransactionMode modeDefault = TransactionMode.FULL_TRANSACTION;
  
  //bei mehreren registrierten storables für den tablename (applications) werden hier alle klasse eingetragen
  private final ConcurrentMapWithObjectRemovalSupport<String, SetWrapper<Class<? extends Storable>>> classes = new ConcurrentMapWithObjectRemovalSupport<String, SetWrapper<Class<? extends Storable>>>() {

    private static final long serialVersionUID = 1L;

    @Override
    public SetWrapper<Class<? extends Storable>> createValue(String key) {
      return new SetWrapper<Class<? extends Storable>>();
    }
  };
  
  private final ConcurrentMap<Class<? extends Storable>, String> tableNames = new ConcurrentHashMap<Class<? extends Storable>, String>();
  private final ConcurrentMap<String, Boolean> uninitialized = new ConcurrentHashMap<String, Boolean>();
  private Journal journal;
  private MemoryQueue memoryQueue;
  private boolean useCache;
  private volatile PersistenceLayer cachePL;
  private String cachePLName;
  private boolean closeToFlush;
  
  
  private static class SetWrapper<E> extends ObjectWithRemovalSupport{
    private final Set<E> set = new HashSet<E>(); 
    
    /**
     * Fügt ein Element zur Liste hinzu
     * @param e
     * @return true, falls die Liste vorher leer war
     */
    public boolean add(E e) {
      boolean first = set.isEmpty();
      set.add(e);
      
      return first;
    }

    public boolean remove(E e) {
      return set.remove(e);
    }
    
    /**
     * Liefert ein beliebiges Element aus der Liste, oder null falls leer
     * @return
     */
    public E getAnyElement() {
      if (set.isEmpty()) {
        return null;
      }
      
      return set.iterator().next();
    }
    
    public boolean shouldBeDeleted() {
      return set.isEmpty();
    }
  }

  public XMLPersistenceLayer() {
  }

  public String[] getParameterInformation() {
    return new String[] {
        "subdirectory name where XMLs are stored under server/" + Constants.STORAGE_PATH,
        "transaction mode:\n    " + 
           TransactionMode.FULL_TRANSACTION.getStartParamIdentifier() + " (default) - Full transaction support.\n    " +
           TransactionMode.AUTO_COMMIT.getStartParamIdentifier() + " - Transactions only for single operations, every change is commited to main archive.\n    " + 
           TransactionMode.NO_TRANSACTION.getStartParamIdentifier() + " - No transaction support.",
        "useCache: true/false, default=true", 
        "persistenceLayerInstance name of persistence layer to be used as cache (default=value of xyna property " + defaultCachePL.getPropertyName() + ")",
        "closeToFlush: true/false, default=false. true, if each journal write should close and reopen the file."};
  }

  public void init(Long pliID, String... args) throws PersistenceLayerException {
    if (args == null || args.length < 1) {
      throw new XNWH_GeneralPersistenceLayerException("expected parameter for storage directory (under server/"
          + Constants.STORAGE_PATH + ")");
    }
    dir = args[0];
    if (args.length > 1) {
      modeDefault = TransactionMode.getByStartParamIdentifier(args[1]);
    }
    if (args.length > 2) {
      useCache = Boolean.valueOf(args[2]);
    } else {
      useCache = true;
    }
    if (useCache) {
      closeToFlush = false;
      journal = new Journal(pliID);
      if (memoryQueue != null) {
        throw new RuntimeException();
      }
      memoryQueue = new MemoryQueue();
      memoryQueue.startThread();
      cachePLName = defaultCachePL.get();
      if (args.length > 3) {
        cachePLName = args[3];
      }
      if (args.length > 4) {
        closeToFlush = Boolean.valueOf(args[4]);
      }
      logger.info("XML PersistenceLayer " + pliID + " initialized. uses cache (" + cachePLName + "), closeToFlush=" + closeToFlush + ".");
    } else {
      logger.info("XML PersistenceLayer " + pliID + " initialized without cache. transactionmode=" + modeDefault.name());
    }
  }


  public XMLPersistenceLayerConnection getConnection() throws PersistenceLayerException {
    if (dir == null) {
      throw new XNWH_GeneralPersistenceLayerException("PersistenceLayer is not initialized properly.");
    }
    return new XMLPersistenceLayerConnection();
  }
  
  
  public PersistenceLayerConnection getDedicatedConnection() throws PersistenceLayerException {
    return getConnection();
  }


  private static Element createElement(String tableName, Document doc, Storable storable) throws XNWH_GeneralPersistenceLayerException,
      XNWH_UnsupportedPersistenceLayerFeatureException {
    Element elem = doc.createElement(tableName);
    Column[] cols = storable.getColumns();
    if (cols.length == 0) {
      throw new RuntimeException("Storable class '" + storable.getTableName() + "' not loaded properly (no data columns found).");
    }
    for (Column col : cols) {
      if (col.type() == ColumnType.INHERIT_FROM_JAVA) {
        Object columnValue = storable.getValueByColName(col);
        if (columnValue != null) {
          Element colEl = doc.createElement(col.name());
          String stringValue = null;
          if (columnValue instanceof StringSerializable) {
            stringValue = ((StringSerializable<?>) columnValue).serializeToString();
          } else {
            stringValue = String.valueOf(columnValue);
          }
          Text text = doc.createTextNode(stringValue);
          colEl.appendChild(text);
          elem.appendChild(colEl);
        }
      } else if (col.type() == ColumnType.BLOBBED_JAVAOBJECT) {
        Element colEl = doc.createElement(col.name());
        Serializable o = storable.getValueByColName(col);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos;
        try {
          oos = new ObjectOutputStream(baos);
          // wrap the object into a serializable classloaded object in case xmom objects are stored
          oos.writeObject(new SerializableClassloadedObject(o));
          oos.close();
        } catch (IOException e) {
          throw new XNWH_GeneralPersistenceLayerException("unexpected problem serializing value of column " + col.name(), e);
        }
        byte[] bytes = baos.toByteArray();
        String encoded = encodeBytes(bytes);
        Text text = doc.createTextNode(encoded);
        colEl.appendChild(text);
        elem.appendChild(colEl);
      } else if (col.type() == ColumnType.BYTEARRAY) {
        Element colEl = doc.createElement(col.name());
        byte[] bytes = (byte[]) storable.getValueByColName(col);
        String encoded = encodeBytes(bytes);
        Text text = doc.createTextNode(encoded);
        colEl.appendChild(text);
        elem.appendChild(colEl);
      } else {
        throw new XNWH_UnsupportedPersistenceLayerFeatureException("columns of type " + col.type().toString());
      }
    }
    return elem;
  }


  private static <T extends Storable> Document createDocument(String tableName, Collection<T> storableCollection, long transactionId)
      throws ParserConfigurationException, XNWH_GeneralPersistenceLayerException, XNWH_UnsupportedPersistenceLayerFeatureException {
    String rootElementName = tableName + "Table";

    DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
    Document doc = builder.newDocument();
    Element root = doc.createElement(rootElementName);
    if (transactionId >= 0) {
      root.setAttribute(TableInfo.ATT_TRANSACTION_ID, String.valueOf(transactionId));
    }
    doc.appendChild(root);
    Iterator<T> iter = storableCollection.iterator();
    Element elem = null;
    T storable = null;

    while (iter.hasNext()) {
      storable = iter.next();

      if (storable == null) {
        continue;
      } else {
        elem = createElement(tableName, doc, storable);
        root.appendChild(elem);
      }
    }
    return doc;
  }

  public interface LastModificationChangeHandler {

    long getStoredLastModified(File originalFile);
    public void afterRename(File originalFile);
    public void handleFileIsModified(File originalFile);
    boolean checkModified(TransactionFile newFile);
  }

  public class XMLPersistenceLayerConnection implements PersistenceLayerConnection, FilePathBuilder {
    
    public LastModificationChangeHandler lastModificationChangeHandler;
    private final ConcurrentMap<String, TransactionFile> openFiles;
    private TransactionContext transactionContext;
    private TransactionMode mode;

    public XMLPersistenceLayerConnection() throws PersistenceLayerException {
      openFiles = new ConcurrentHashMap<String, TransactionFile>();
      transactionContext = new TransactionContext();
      mode = modeDefault;
    }


    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props) throws PersistenceLayerException {
      final String tableName;
      Storable storable;
      try {
        storable = klass.getConstructor().newInstance();
      } catch (Exception e) {
        throw new XNWH_GeneralPersistenceLayerException("could not create instance of storable", e);
      }
      tableName = storable.getTableName();
      try {
        File dirfile = new File(Constants.STORAGE_PATH + File.separator + dir);
        if(dirfile.exists()) {
          File []childfiles = dirfile.listFiles(new FilenameFilter() {
            
            public boolean accept(File dir, String name) {
              if(name.startsWith(tableName) && name.endsWith(TransactionFile.FILE_SUFFIX)) {
                return true;
              }
              return false;
            }
          });
          for(File child : childfiles) {
            if(logger.isDebugEnabled()) {
              logger.debug("Delete temporary file " + child.getAbsolutePath());
            }
            child.delete();
          }
        }
      } catch(Exception e) {
        // Wenn hier irgendwas passiert, können wir das eigentlich ignorieren. Worst case wäre, dass die temporären Dateien nicht gelöscht werden konnten und
        // die Platte voll müllen.
        logger.warn("could not delete temporary xml persistence layer files.", e);
      }

      if (useCache) {
        String tableNameLC = tableName.toLowerCase();
        tableNames.put(klass, tableName);
        try {
          if (!classes.lazyCreateGet(tableNameLC).add(klass)) {
            //andere application -> nichts mehr zu tun
            if (logger.isDebugEnabled()) {
              logger.debug("a class is already registered for " + tableName + " in pl " + journal.pliId);
            }
            return;
          }
        } finally {
          classes.cleanup(tableNameLC);
        }
        if (logger.isDebugEnabled()) {
          logger.debug("registering " + tableName + " in pl " + journal.pliId);
        }
        getCache();
        boolean success = false;
        try {
          memoryQueue.loadFromFile(klass, tableName); //initialisiert tableInfo
          success = true;
        } finally {
          if (!success) {
            //zb deserialisierungsfehler
            removeTable(klass, props, false);
          }
        }
        uninitialized.putIfAbsent(tableName, Boolean.TRUE);
        if (journal.checkLastTransactions(memoryQueue)) {
          if (logger.isDebugEnabled()) {
            logger.debug("filling xml persistencelayer caches for pl=" + journal.pliId + " ... ");
          }
          synchronized (uninitialized) { //nacheinander, damit nicht das persist zu häufig passiert
            Iterator<String> it = uninitialized.keySet().iterator();
            while (it.hasNext()) {
              String t = it.next();
              TableInfo ti = memoryQueue.tableInfos.get(t);
              if (ti == null) {
                continue;
              }
              Collection<Storable<?>> objects = ti.rows.values();
              PersistenceLayerConnection c = getCache().getConnection();
              try {
                Class<? extends Storable> tClass = classes.lazyCreateGet(t.toLowerCase()).getAnyElement();
                classes.cleanup(t.toLowerCase());
                try {
                  c.addTable(tClass, false, null); //forcewidening für memory-pl egal
                  c.persistCollection(objects);
                  c.commit();
                  if (logger.isDebugEnabled()) {
                    logger.debug("stored " + objects.size() + " storables in cache for table " + tableName + ".");
                  }
                } catch (RuntimeException | PersistenceLayerException | LinkageError e) {
                  //memorypl hat ein problem, das ist ja nicht die schuld von dem aktuellen thread, deshalb nur loggen.
                  logger.warn("Table " + t + " could not be registered in memory persistence layer cache.", e);
                }
              } finally {
                c.closeConnection();
              }
              it.remove();
            }
          }
        } else {
          //beim nächsten addTable wieder versuchen
          if (logger.isDebugEnabled()) {
            logger.debug("table " + tableName + " will be registered at a later time.");
          }
        }
      }
    }
    
    public <T extends Storable> void removeTable(Class<T> klass, Properties properties)
        throws PersistenceLayerException {
      removeTable(klass, properties, true);
    }


    public <T extends Storable<?>> void removeTable(Class<T> klass, Properties properties, boolean addedToCachePL)
        throws PersistenceLayerException {
      if (useCache) {
        String tableName = Storable.getPersistable(klass).tableName();

        if (addedToCachePL) {
          //darauf warten, dass alles ins file geschrieben wird.
          //es könnte nach dem removeTable ein erneutes addTable mit dem gleichen storable geben, welches dann die daten sehen soll
          int retryCnt = 0;
          memoryQueue.awake();
          while (!memoryQueue.awaitStore(2000)) {
            if (retryCnt++ > 600) {
              logger.warn("memory queue did not finish transferring open transactions for table " + tableName);
              break;
            }
            memoryQueue.awake();
          }
        }

        tableNames.remove(klass);

        String tableNameLC = tableName.toLowerCase();
        try {
          classes.lazyCreateGet(tableNameLC).remove(klass);
        } finally {
          classes.cleanup(tableNameLC);
        }
        synchronized (uninitialized) {
          uninitialized.remove(tableName);
        }
        memoryQueue.tableInfos.remove(tableName);

        if (addedToCachePL && cachePL != null) {
          PersistenceLayerConnection con = getCache().getConnection();
          try {
            con.removeTable(klass, null);
          } finally {
            con.closeConnection();
          }
        }
        if (logger.isDebugEnabled()) {
          logger.debug("unregistered storable " + klass.getName() + "(" + tableName + ") from " + journal.pliId);
        }
      }
    }


    private PersistenceLayer getCache() throws PersistenceLayerException {
      if (cachePL == null) {
        synchronized (this) {
          if (cachePL == null) {
            ODSImpl ods = ODSImpl.getInstance();
            if (cachePLName != null && cachePLName.length() > 0) {
              for (PersistenceLayerInstanceBean plib : ods.getPersistenceLayerInstances()) {
                if (plib.getPersistenceLayerInstanceName().equals(cachePLName)) {
                  cachePL = plib.getPersistenceLayerInstance();
                }
              }
            }
            cachePLName = "CacheForXMLPL" + journal.pliId;
            if (cachePL == null) {
              for (PersistenceLayerInstanceBean plib : ods.getPersistenceLayerInstances()) {
                if (plib.getPersistenceLayerInstanceName().equals(cachePLName)) {
                  cachePL = plib.getPersistenceLayerInstance();
                }
              }
              if (cachePL == null) {
                //neue memory PL instanz

                long plid;
                try {
                  plid = ods.getMemoryPersistenceLayerID();
                  long mem_pliid =
                      ods.instantiatePersistenceLayerInstance(cachePLName, plid, "xnwh", ODSConnectionType.INTERNALLY_USED, new String[] {});
                  for (PersistenceLayerInstanceBean plib : ods.getPersistenceLayerInstances()) {
                    if (plib.getPersistenceLayerInstanceID() == mem_pliid) {
                      cachePL = plib.getPersistenceLayerInstance();
                    }
                  }
                } catch (XNWH_PersistenceLayerNotRegisteredException e) {
                  throw new RuntimeException(e);
                } catch (XNWH_PersistenceLayerIdUnknownException e) {
                  throw new RuntimeException(e);
                } catch (XNWH_PersistenceLayerClassIncompatibleException e) {
                  throw new RuntimeException(e);
                }
              }
            }
          }
        }
      }
      return cachePL;
    }
    

    public void closeConnection() throws PersistenceLayerException {
      rollback();
    }


    public void commit() throws PersistenceLayerException {
      if (useCache) {
        if (transactionContext.size() == 0) {
          //nichts zu tun
          transactionContext = new TransactionContext();
          return;
        }
        StringBuilder lines = transactionContext.asString();
        //bei großen transaktionen kann die umwandlung in den string etwas dauern. deshalb tun, bevor man transaktionsid ermittelt, 
        //weil sich das schreiben ins journal dann von der reihenfolge nach den ids richtet
        
        journal.setTransactionId(transactionContext);

        //allerdings muss in die letzte zeile dann das COMMIT mit der transaktionsid - das muss dann nachträglich erzeugt werden
        lines.append(transactionContext.parts.get(transactionContext.parts.size() - 1).asString()).append("\n");

        boolean needToInvalidateTransactionId = true;
        try {
          journal.write(transactionContext, lines.toString());
          needToInvalidateTransactionId = false;
          boolean success = false;
          try {
            transactionContext.commit();
            success = true;
          } finally {
            if (!success) {
              journal.writeRollback(transactionContext);
            }
          }
        } catch (IOException e) { //TODO woran erkennt man IOExceptions, wo ein retry nichts bringt 
          throw new XNWH_RetryTransactionException(e);
        } finally {
          if (needToInvalidateTransactionId) {
            journal.invalidateTransactionId(transactionContext);
          }
        }
        transactionContext = new TransactionContext();
      } else {
        try {
          for (TransactionFile file : openFiles.values()) {
            boolean success = false;
            try {
              if (lastModificationChangeHandler != null) {
                if (lastModificationChangeHandler.checkModified(file)) {
                  lastModificationChangeHandler.handleFileIsModified(file.getOriginalFile());
                }
              } else if (!file.checkNotModified()) {
                logger.warn("File " + file.getOriginalFile().getAbsolutePath() + " has been modified ("
                    + Constants.defaultUTCSimpleDateFormat().format(new Date(file.getOriginalFile().lastModified()))
                    + ") after it has been set to readonly. User changes will be overwritten.");
              }
              file.commit();
              success = true;
              if (lastModificationChangeHandler != null) {
                lastModificationChangeHandler.afterRename(file.getOriginalFile());
              }
            } finally {
              if (!success) {
                file.rollback();
              }
            }
          }
        } finally {
          openFiles.clear();
        }
      }
    }


    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
      if (useCache) {
        return transactionContext.getConnection(storable.getTableName()).containsObject(storable);
      } else {

        if (storable == null) {
          return false;
        }

        Persistable persistable = Storable.getPersistable(storable.getClass());
        String tableName = persistable.tableName();
        String primaryKey = persistable.primaryKey();

        File f = getFile(dir, tableName, true);
        if (!existsNonEmpty(f)) {
          return false;
        }
        lockFile(f);
        try {

          Document doc = parseFile(f);

          List<Element> existingEntries = XMLUtils.getChildElements(doc.getDocumentElement());
          for (Element next : existingEntries) {
            Element nextPkElement = XMLUtils.getChildElementByName(next, primaryKey);
            if (nextPkElement != null) {
              if (storable.getPrimaryKey().equals(XMLUtils.getTextContent(nextPkElement))) {
                return true;
              }
            }
          }
          return false;
        } catch (XynaException e) {
          throw new XNWH_GeneralPersistenceLayerException("problem writing to file", e);
        } finally {
          unlockFile(f);
        }
      }
    }


    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
      if (storableCollection == null) {
        throw new IllegalArgumentException("Collection to be removed may not be null");
      }
      if (storableCollection.size() == 0) {
        return;
      }
      String tableName = storableCollection.iterator().next().getTableName();
      
      if (useCache) {
        PersistenceLayerConnection connection = transactionContext.getConnection(tableName);
        connection.delete(storableCollection);
        for (T s : storableCollection) {
          transactionContext.delete(tableName, s.getPrimaryKey());
        }
        
      } else {

        String primaryKey = Storable.getPersistable(storableCollection.iterator().next().getClass()).primaryKey();

        File f = getFile(dir, tableName, false);
        if (existsNonEmpty(f)) {
          lockFile(f);
          try {
            Document doc = parseFile(f);
            List<Element> existingEntries = XMLUtils.getChildElements(doc.getDocumentElement());
            Set<Element> elementsToBeRemoved = new HashSet<Element>();
            Iterator<Element> iter = existingEntries.iterator();
            while (iter.hasNext()) {
              Element next = iter.next();
              for (T entryToBeRemoved : storableCollection) {
                Element nextPkElement = XMLUtils.getChildElementByName(next, primaryKey);
                if (nextPkElement != null) {
                  if ((String.valueOf(entryToBeRemoved.getPrimaryKey())).equals(XMLUtils.getTextContent(nextPkElement))) {
                    elementsToBeRemoved.add(next);
                  }
                }
              }
            }
            for (Element e : elementsToBeRemoved) {
              doc.getDocumentElement().removeChild(e);
            }

            writeToFile(doc, f);
          } catch (XynaException e) {
            throw new XNWH_GeneralPersistenceLayerException("problem writing to file", e);
          } finally {
            unlockFile(f);
          }
        }
      }
    }


    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
      String tableName = Storable.getPersistable(klass).tableName();
      if (useCache) {
        transactionContext.getConnection(tableName).deleteAll(klass);
        transactionContext.deleteAll(tableName);
        
      } else {

        File f = getFile(dir, tableName, false);
        lockFile(f);
        try {

          if (existsNonEmpty(f)) {
            Document doc = parseFile(f);
            XMLUtils.removeChildNodes(doc.getDocumentElement());
            writeToFile(doc, f);
          }
        } catch (XynaException e) {
          throw new XNWH_GeneralPersistenceLayerException("problem writing to file", e);
        } finally {
          unlockFile(f, true);
        }
      }
    }


    public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("execute DML");
    }
    
    
    public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
      return loadCollectionAndTxId(klass).getSecond();
    }
    
    
    public <T extends Storable> Pair<Long, Collection<T>> loadCollectionAndTxId(Class<T> klass) throws PersistenceLayerException {
      T tempInstance;
      try {
        tempInstance = klass.getConstructor().newInstance();
      } catch (Exception e) {
        throw new XNWH_GeneralPersistenceLayerException("Could not create instance of " + klass.getName() + " per reflection.", e);
      }
      String tableName = tempInstance.getTableName();
      if (useCache) {
        Collection<T> list = transactionContext.getConnection(tableName).loadCollection(klass);
        //TODO clone
        return new Pair<Long, Collection<T>>(-1L, list);

      } else {
        ResultSetReader<? extends T> reader = tempInstance.getReader();
        
        List<T> list = new ArrayList<T>();

        File file = null;
        long txId = TableInfo.TXID_NOT_INITIALIZED;

        file = getFile(dir, tableName, true);
        lockFile(file);
        try {
          if (existsNonEmpty(file)) {

            Document doc = parseFile(file);
            Element root = doc.getDocumentElement();
            txId = TableInfo.readTransactionId(root, file);
            List<Element> entries = XMLUtils.getChildElementsByName(root, tableName);
            for (Element entry : entries) {
              try {
                T storable = reader.read(new XMLResultSet(entry));
                list.add(storable);
              } catch (SQLException e) {
                throw new XNWH_GeneralPersistenceLayerException("could not read from xml result set", e);
              }
            }
          }
        } finally {
          unlockFile(file);
        }

        return new Pair<Long, Collection<T>>(txId, list);
      }
    }


    public <T extends Storable> void persistCollection(Collection<T> storableCollection) throws PersistenceLayerException {
      if (storableCollection.size() == 0) {
        return;
      }

      T firstElement = storableCollection.iterator().next();
      persistCollection(storableCollection, -1L, firstElement.getClass());
    }

    public <T extends Storable> void persistCollection(Collection<T> storableCollection, long txId, Class<? extends Storable> storableClass)
                    throws PersistenceLayerException {      
      String tableName = Storable.getPersistable(storableClass).tableName();    
      
      if (useCache) {
        if (storableCollection.size() == 0) {
          return;
        }
        transactionContext.getConnection(tableName).persistCollection(storableCollection);
        for (T s : storableCollection) {
          transactionContext.store(tableName, s);
        }
        
      } else {
        //update alter werte => alte collection laden
        
        //TODO performance: in vielen fällen weiss man als aufrufer dieser methode, dass man vorher deleteall aufgerufen hat
        //     dann kann man sich das laden der alten collection sparen.
        Collection<T> oldCollection = (Collection<T>) loadCollection(storableClass);
        HashMap<Object, T> pkMap = new HashMap<Object, T>();
        for (T t : oldCollection) {
          pkMap.put(t.getPrimaryKey(), t);
        }
        for (T t : storableCollection) {
          pkMap.put(t.getPrimaryKey(), t);
        }
        if (oldCollection.size() + storableCollection.size() > pkMap.size() * 3) {
          //mehr pk-duplikate als erwartet. da ist was faul!
          throw new RuntimeException("Primary keys of table " + tableName + " seem to be duplicated: old=" + oldCollection.size() + ", new=" + storableCollection.size() + ", pks=" + pkMap.size());
        }
        storableCollection = pkMap.values();
        
        //nun xml erstellen
        try {
          Document doc = createDocument(tableName, storableCollection, txId);
          File f = getFile(dir, tableName, false);
          lockFile(f);
          try {
            writeToFile(doc, f);
          } catch (XynaException e) {
            throw new XNWH_GeneralPersistenceLayerException("problem writing to file", e);
          } finally {
            unlockFile(f);
          }
        } catch (ParserConfigurationException e) {
          throw new XNWH_GeneralPersistenceLayerException(e.getMessage(), e);
        }
      }
    }
    
    
    public <T extends Storable> String getFilePath(Class<T> klass) {
      String tableName = Storable.getPersistable(klass).tableName();
      return getFilePath(dir, tableName);      
    }
    
    public String getFilePathByTableName(String tableName) {
      return getFilePath(dir, tableName);      
    }
    
    public String getFilePath(String subdir, String filename) {
      return new StringBuffer(Constants.STORAGE_PATH).append(File.separator).append(subdir).append(File.separator)
                                                     .append(filename).append(".xml").toString();
    }
    
    private File getFile(String subdir, String filename, boolean readAccess) {
      // base path + department + class name
      if (openFiles.containsKey(filename)) {
        return openFiles.get(filename);
      } else {
        String filePath = getFilePath(subdir, filename);
        File originalFile = new File(filePath);
        long lastModified;
        if (lastModificationChangeHandler != null) {
          lastModified = lastModificationChangeHandler.getStoredLastModified(originalFile);
        } else {
          lastModified = originalFile.lastModified();
        }
        if (mode.wrapOriginalFile() && !readAccess) {
          TransactionFile newFile = new TransactionFile(originalFile, lastModified);
          openFiles.putIfAbsent(filename, newFile);
          if (lastModificationChangeHandler != null) {
            if (lastModificationChangeHandler.checkModified(newFile)) {
              lastModificationChangeHandler.handleFileIsModified(originalFile);
            }
          }
          return newFile;
        } else {
          return originalFile;
        }
      }
    }

    
    private Document parseFile(File file) throws XNWH_GeneralPersistenceLayerException {
      File fileToRead = file;
      if (file instanceof TransactionFile && !((TransactionFile)file).hasLocalChanges()) {
        fileToRead = ((TransactionFile)file).getOriginalFile();
      }
      try {
        return XMLUtils.parse(fileToRead.getAbsolutePath());
      } catch (XynaException e) {
        throw new XNWH_GeneralPersistenceLayerException("Could not parse file " + file.getPath(), e);
      }
    }
     
    
    private void writeToFile(Document doc, File file) throws Ex_FileAccessException {
      if(mode.useTransactionFiles()) {
        TransactionFile tfile = new TransactionFile(file);
        XMLUtils.saveDom(tfile, doc);
        tfile.commit();
      } else {
        XMLUtils.saveDom(file, doc);
      }
    }
    
    
    private boolean existsNonEmpty(File file) {
      if (file instanceof TransactionFile) {
        // TransactionFiles do always exist
        TransactionFile tFile = (TransactionFile)file;
        if (tFile.hasLocalChanges()) {
          return tFile.exists() && tFile.length() > 0;
        } else {
          return tFile.getOriginalFile().exists() && tFile.getOriginalFile().length() > 0;
        }
      } else {
        return file.exists() && file.length() > 0;
      }
    }


    private void lockFile(File f) {
      if (f != null) {
        File fileToLock = f;
        if (f instanceof TransactionFile) {
          fileToLock = ((TransactionFile)f).getOriginalFile();
        }
        fileLocks.lazyCreateGet(fileToLock).lock();
        fileLocks.cleanup(fileToLock);
      }
    }
    
    
    private void unlockFile(File f) {
      unlockFile(f, false);
    }
    
    
    private void unlockFile(File f, boolean andRemove) {
      if (f != null) {
        File fileToLock = f;
        if (f instanceof TransactionFile) {
          fileToLock = ((TransactionFile)f).getOriginalFile();
        }
        ReentrantLockWrapper lock = fileLocks.lazyCreateGet(fileToLock);
        if (andRemove) {
          lock.markForDelete();
        }
        lock.unlock();
        fileLocks.cleanup(fileToLock);
      }
    }
    
    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
      if (storable == null) {
        return false;
      }
      
      String tableName = storable.getTableName();

      if (useCache) {
        boolean ret = transactionContext.getConnection(tableName).persistObject(storable);
        transactionContext.store(tableName, storable);
        return ret;

      } else {

        File f = getFile(dir, tableName, false);
        lockFile(f);
        try {

          boolean existedBefore = false;
          // FIXME performance: it would be faster not to load all objects
          ArrayList<Storable> existingEntries = new ArrayList<Storable>(loadCollection(storable.getClass()));
          if (existingEntries.size() > 0) {

            Iterator<? extends Storable> iter = existingEntries.iterator();

            Storable<?> next = existingEntries.iterator().next();
            if (!next.getClass().equals(storable.getClass())) {
              throw new XNWH_InvalidObjectForTableException(next.getTableName(), storable.getClass().getName(), next.getClass().getName());
            }

            iter = existingEntries.iterator();
            while (iter.hasNext()) {
              next = iter.next();
              if (next.getPrimaryKey().equals(storable.getPrimaryKey())) {
                iter.remove();
                existedBefore = true;
                break;
              }
            }

          }

          existingEntries.add(storable);
          persistCollection(existingEntries);

          return existedBefore;

        } finally {
          unlockFile(f);
        }
      }
    }


    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("prepare command");
    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
      if (useCache) {
        return transactionContext.getConnection(query.getTable()).prepareQuery(query);
      } else {
        throw new XNWH_UnsupportedPersistenceLayerFeatureException("prepare query");
      }
    }


    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException {
      if (useCache) {
        return transactionContext.getConnection(query.getTable()).query(query, parameter, maxRows);
      } else {
        throw new XNWH_UnsupportedPersistenceLayerFeatureException("query");
      }
    }

    
    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader) throws PersistenceLayerException {
      if (useCache) {
        return transactionContext.getConnection(query.getTable()).query(query, parameter, maxRows, reader);
      } else {
        throw new XNWH_UnsupportedPersistenceLayerFeatureException("query");
      }
    }
    

    public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
    XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      if (useCache) {
        transactionContext.getConnection(storable.getTableName()).queryOneRow(storable);
      } else {
        throw new XNWH_UnsupportedPersistenceLayerFeatureException("query for update");
      }
    }


    public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      if (useCache) {
        transactionContext.getConnection(storable.getTableName()).queryOneRow(storable);
        
      } else {

        if (storable == null) {
          return;
        }

        String tableName = storable.getTableName();

        File f = getFile(dir, tableName, true);
        lockFile(f);
        try {

          // FIXME performance: it would be faster not to load all objects
          ArrayList<Storable> existingEntries = new ArrayList<Storable>(loadCollection(storable.getClass()));
          if (existingEntries.size() > 0) {

            Storable<?> next = existingEntries.iterator().next();
            if (!existingEntries.iterator().next().getClass().equals(storable.getClass())) {
              throw new XNWH_InvalidObjectForTableException(next.getTableName(), storable.getClass().getName(), next.getClass().getName());
            }

            Iterator<? extends Storable> iter = existingEntries.iterator();
            while (iter.hasNext()) {
              next = iter.next();
              if (next.getPrimaryKey() != null) {
                if (next.getPrimaryKey().equals(storable.getPrimaryKey())) {
                  storable.setAllFieldsFromData(next);
                  return;
                }
              }
            }

          }

          throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()), tableName);

        } finally {
          unlockFile(f);
        }
      }
    }


    public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
      if (useCache) {
        return transactionContext.getConnection(query.getTable()).queryOneRow(query, parameter);
      } else {
        throw new XNWH_UnsupportedPersistenceLayerFeatureException("query");
      }
    }


    public void rollback() throws PersistenceLayerException {
      if (useCache) {
        transactionContext.rollback();
        transactionContext = new TransactionContext();
      } else {
        try {
          for (TransactionFile file : openFiles.values()) {
            file.rollback();
          }
        } finally {
          openFiles.clear();
        }
      }
    }


    public void setTransactionProperty(TransactionProperty property) {
      //nicht unterstützt
    }


    public <T extends Storable> void deleteOneRow(T toBeDeleted) throws PersistenceLayerException {
      if (useCache) {
        String tableName = toBeDeleted.getTableName();
        transactionContext.getConnection(tableName).deleteOneRow(toBeDeleted);
        transactionContext.delete(tableName, toBeDeleted.getPrimaryKey());
        
      } else {

        Collection<T> toBeDeletedList = new ArrayList<T>();
        toBeDeletedList.add(toBeDeleted);
        delete(toBeDeletedList); // TODO performance
      }
    }


    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> storableClazz)
                    throws PersistenceLayerException {
      // TODO what could we do? Check if the file is parseable?      
    }


    public boolean isOpen() {
      return true;
    }

  }

  public boolean describesSamePhysicalTables(PersistenceLayer plc) {
    if (plc instanceof XMLPersistenceLayer) {
      XMLPersistenceLayer xplc = (XMLPersistenceLayer)plc;
      return xplc.dir.equals(dir);
    } else {
      return false;
    }
  }
  
  //TODO encoding/decoding extrahieren
  
  //huffman encoding: schneller als BASE64. nicht fertig.
  
  private static String[] codebook; //index ist byte + 128
  private static byte[] inverseCodeBook; //index von array berechnet sich aus chars. siehe calcIndex methoden
  private static Random random = new Random();
  static {
    String[] prefixes = new String[]{"", ".", ",", ";"};
    String chars = ""; //kleinbuchstaben, großbuchstaben, ziffern, -_
    char a = 'a';
    for (int i = 0; i<26; i++) {
      chars += (char)(a + i); 
    }
    a = 'A';
    for (int i = 0; i<26; i++) {
      chars += (char)(a + i); 
    }
    for (int i = 0; i<10; i++) {
      chars += i;
    }
    chars += "-_"; //64 zeichen
    codebook = new String[256];
    inverseCodeBook = new byte[1024];
    for (int i = 0; i<codebook.length; i++) {
      String prefix = prefixes[i / 64];
      codebook[i] = prefix + String.valueOf(chars.charAt(i % 64));
      int index;
      if (codebook[i].length() == 1) {
        index = calcIndex(codebook[i].charAt(0));
      } else {
        index = calcIndex2((i/64)*256, codebook[i].charAt(1));
      }
      inverseCodeBook[index] = (byte) (i - 128);
    }
  }
  
  private static int calcIndex(char c) {
    return c;
  }
  
  private static int calcIndex2(int plus, char c2) {
    return plus + c2;
  }

  /**
   * decoded je nachdem, was der erste character ist mit huffman oder mit base64.
   * @param s
   * @return
   */
  private static byte[] decodeBytes(String s) {
    char[] chars = new char[s.length()];
    s.getChars(0, s.length(), chars, 0);
    char firstChar = chars[0];
    byte[] newbytes = null;    
    if (firstChar == 'l') {
      //TODO siehe unten
      if (true) throw new RuntimeException("unsupported");
      int length = chars.length;
      int index = 1;
      byte[] bytes = new byte[length]; //wahrscheinlihc zuviel, aber macht nichts
      int bytesIndex = 0;

      int inverseIndex; 
      while (index < length) {
        
        char c = chars[index];
        if (c == '.') {
          char cn = chars[index+1];
          inverseIndex = calcIndex2(256, cn);
          index += 2;
        } else if (c == ',') {  
          char cn = chars[index+1];
          inverseIndex = calcIndex2(512, cn);
          index += 2;
        } else if (c == ';') {
          char cn = chars[index+1];
          inverseIndex = calcIndex2(768, cn);
          index += 2;
        } else if (c == '\n') {
          index ++;
          continue;
        } else {        
          inverseIndex = calcIndex(c);
          index ++;
        }
        bytes[bytesIndex++] = inverseCodeBook[inverseIndex];
      }
      newbytes = new byte[bytesIndex-1];
      System.arraycopy(bytes, 0, newbytes, 0, bytesIndex-1);
      newbytes = new byte[bytesIndex-1];
    } else if (firstChar == 'b') {
      try {
        newbytes = Base64.decode(s.substring(1));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else {
      throw new RuntimeException("unsupported Encoding");
    }
    //unzip
    ByteArrayOutputStream baos = new ByteArrayOutputStream(newbytes.length);
    int read = 0;
    byte[] buffer = new byte[2048];
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(newbytes))) {
      zis.getNextEntry();
      while (read > -1) {
        read = zis.read(buffer);
        if (read > -1) {
          baos.write(buffer, 0, read);
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    byte[] newbytes2 = baos.toByteArray();
    return newbytes2;
  }
  
  /**
   * zipped und encoded mit base64.
   * @param bytes
   * @return
   */
  public static String encodeBytes(byte[] bytes) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream(bytes.length);
    long t = System.currentTimeMillis();
        
    try (ZipOutputStream zos = new ZipOutputStream(baos)) {
      zos.setLevel(Deflater.BEST_SPEED);
      zos.putNextEntry(new ZipEntry("t"));
      zos.write(bytes);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    bytes = baos.toByteArray();
    if (logger.isTraceEnabled()) {
      logger.trace("zip took " + (System.currentTimeMillis() - t) + "ms. size zipped = " + bytes.length);
    }

    int mode = 0;
    int minLengthForStatistics = 1000000;
    if (bytes.length >= minLengthForStatistics) {
      t = System.currentTimeMillis();
      int[] statistics = new int[256];
      for (int i = 0; i<minLengthForStatistics; i++) {
        statistics[bytes[random.nextInt(bytes.length)] + 128]++;
      }
      Arrays.sort(statistics);
      
      int sum = 0;
      for (int i = 255; i>=192; i--) {
        sum += statistics[i];
      }
      if (logger.isTraceEnabled()) {
        logger.trace("sum=" + sum + " " + (System.currentTimeMillis()-t) + "ms");
      }

      if (sum > 2*minLengthForStatistics/3) {
        //in diesem fall ist BASE64 schlechter als huffman. siehe http://www.javaworld.com/javaworld/javatips/jw-javatip117.html?page=3
        
        //BASE64 vergrößtert die daten durchschnittlich um faktor 4/3. 
        //huffman abhängig von der verteilung der bytes in den daten um einen faktor zwischen 1 und 1,75.
        //falls die häufigsten 64 bytes insgesamt mehr als 2/3 aller bytes ausmachen, ist huffman effizienter.
        //durch das zippen passiert das fast nie. sollte das zippen hier nicht mehr automatisch geschehen, passiert es allerdings sehr oft
        
        //TODO huffman impl
        mode = 1;
      } else {
        mode = 1;
      }
    } else {
      mode = 1;
    }
    
    StringBuilder sb = new StringBuilder(bytes.length * 2);
    if (mode == 1) {
      return "b" + Base64.encode(bytes);
    } else if (mode == 2) {
      if (true) throw new RuntimeException("unsupported");
      //TODO nicht in benutzung. das ist huffman mit festem codebook.
      sb.append("l");
      int l = bytes.length;
      for (int i = 0; i<l; i++) {
        if (i % 100 == 99) {
          sb.append("\n");
        }
        sb.append(codebook[128 + bytes[i]]);
      }
    }
    return sb.toString();
  }
  
  public static void main(String[] args) throws IOException {
    int l = 9999999;
    Random r = new Random();
    byte[] bytes = new byte[l];
    for (int i = 0; i<bytes.length; i++) {
      if (r.nextInt(15) < 1) {
        bytes[i] = (byte) (r.nextInt(256) - 128);
      } else {
        bytes[i] = (byte) 310;
      }
    }
    long t= System.currentTimeMillis();    
    String s = encodeBytes(bytes);
    long t2= System.currentTimeMillis();
    byte[] b2 = decodeBytes(s);
    long t3= System.currentTimeMillis();
    if (b2.length != bytes.length) {
      System.out.println("wrong length");
    }
    for (int i = 0; i<b2.length; i++) {
      if (bytes[i] != b2[i]) {
        System.out.println("failed");
        break;
      }
    }
    System.out.println("size = " + s.length());
    System.out.println("took " + (t2-t) + "ms for encode, and " + (t3-t2) + "ms for decode");
    t= System.currentTimeMillis();
    s = Base64.encode(bytes);
    t2= System.currentTimeMillis();
    b2 = Base64.decode(s);
    t3= System.currentTimeMillis();
    if (b2.length != bytes.length) {
      System.out.println("wrong length");
    }
    for (int i = 0; i<bytes.length; i++) {
      if (bytes[i] != b2[i]) {
        System.out.println("failed");
        break;
      }
    }
    System.out.println("size = " + s.length());
    System.out.println("took " + (t2-t) + "ms for encode, and " + (t3-t2) + "ms for decode");
  }


  public String getInformation() {
    String s =
        "XML Persistence (" + Constants.STORAGE_PATH + File.separator + dir + ", transaction mode = " + modeDefault.toString()
            + ", cache = " + useCache;
    if (useCache) {
      s += ", cachePL = " + cachePLName + ", closeToFlush = " + closeToFlush;
    }
    s += ")";
    return s;
  }


  private static class XMLResultSet extends UnsupportingResultSet {

    private Element entry;
    private boolean wasNull = false;
    
    public XMLResultSet(Element entry) {
      this.entry = entry;
    }
    
    private String getTextContentOfColumn(String columnName) throws SQLException {
      Element child = XMLUtils.getChildElementByName(entry, columnName);
      if (child == null) {
        wasNull = true;
        return "";
      } else {
        wasNull = false;
      }
      String content = XMLUtils.getTextContent(child);
      return content;
    }

    @Override
    public int getInt(String columnName) throws SQLException {
      try {
        return Integer.parseInt(getTextContentOfColumn(columnName));
      } catch (NumberFormatException e) {
        if (wasNull) return 0;
        throw new SQLException("value is no number: " + getTextContentOfColumn(columnName));
      }
    }

    @Override
    public long getLong(String columnName) throws SQLException {
      try {
        return Long.parseLong(getTextContentOfColumn(columnName));
      } catch (NumberFormatException e) {
        if (wasNull) return 0;
        throw new SQLException("value is no number: " + getTextContentOfColumn(columnName));
      }
    }


    @Override
    public boolean wasNull() throws SQLException {
      return wasNull;
    }

    @Override
    public String getString(String columnName) throws SQLException {
      String tmpString = getTextContentOfColumn(columnName);
      if (wasNull) {
        return null;
      } else {
        return tmpString;
      }
    }


    @Override
    public boolean getBoolean(String columnName) throws SQLException {
      String s = getTextContentOfColumn(columnName);
      return s != null && s.equalsIgnoreCase("true");
    }


    @Override
    public Blob getBlob(String columnName) throws SQLException {
      final String s = getTextContentOfColumn(columnName);
      if (wasNull) {
        return null;
      }
      return new Blob() {

        public InputStream getBinaryStream() throws SQLException {
          byte[] bytes = decodeBytes(s);
          return new ByteArrayInputStream(bytes);
        }

        public byte[] getBytes(long pos, int length) throws SQLException {
          throw new SQLException("unsupported operation");
        }

        public long length() throws SQLException {
          throw new SQLException("unsupported operation");
        }

        public long position(byte[] pattern, long start) throws SQLException {
          throw new SQLException("unsupported operation");
        }

        public long position(Blob pattern, long start) throws SQLException {
          throw new SQLException("unsupported operation");
        }

        public OutputStream setBinaryStream(long pos) throws SQLException {
          throw new SQLException("unsupported operation");
        }

        public int setBytes(long pos, byte[] bytes) throws SQLException {
          throw new SQLException("unsupported operation");
        }

        public int setBytes(long pos, byte[] bytes, int offset, int len) throws SQLException {
          throw new SQLException("unsupported operation");
        }

        public void truncate(long len) throws SQLException {
          throw new SQLException("unsupported operation");
        }

        //---------------------- java 1.6 methoden ---------------------------
        public void free() throws SQLException {
        }

        public InputStream getBinaryStream(long pos, long length) throws SQLException {
          throw new SQLException("unsupported operation");
        }
        
      };
    }

    @Override
    public double getDouble(String columnName) throws SQLException {
      String tmpString = getTextContentOfColumn(columnName);
      if (wasNull) {
        return 0;
      } else {
        return Double.valueOf(tmpString);
      }
    }
    

  }


  public void shutdown() throws PersistenceLayerException {
    if (useCache) {
      if (!memoryQueue.awaitStore(5000)) {
        logger.warn("Memory queue of pl " + journal.pliId + " was not cleared before timeout.");
      }
      memoryQueue.running = false;
      memoryQueue.awake();
      //falls thread noch läuft, dann noch warten - ansonsten kommt die methode zurück
      if (!memoryQueue.awaitStore(5000)) {
        logger.warn("Memory queue of pl " + journal.pliId + " was not cleared before timeout.");
      }
      journal.shutdown();
      if (cachePL != null) {
        cachePL.shutdown();
      }
    }
  }


  public Reader getExtendedInformation(String[] args) {
    //TODO reader impl
    if (args != null && args.length > 0) {
      if (args[0].equals("reload")) {
        memoryQueue.awaitReload(5000);
      } else {
        logger.warn("unknown command " + args[0]);
      }
    } else {
      logger.warn("no command provided");
    }
    return null;
  }


  private static class ReentrantLockWrapper extends ObjectWithRemovalSupport {

    private volatile boolean shouldBeDeleted = false;
    private ReentrantLock lock = new ReentrantLock();
    
    @Override
    protected boolean shouldBeDeleted() {
      boolean lockableByOwnThread = lock.tryLock();
      try {
        return shouldBeDeleted && lockableByOwnThread && lock.getHoldCount() <= 2;
                                                       // previously locked once by own thread or free
      } finally {
        if (lockableByOwnThread) {
          lock.unlock();
        }
      }
    }
    
    
    public void markForDelete() {
      shouldBeDeleted = true;
    }
    
    
    public void lock() {
      lock.lock();
      shouldBeDeleted = false;
    }
    
    
    public void unlock() {
      lock.unlock();
    }
    
  }

  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    return getConnection();
  }

  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    if (!(plc instanceof XMLPersistenceLayer)) {
      return false;
    }
    return true;
  }
  
  /**
   * Direktes Lesen eines XML-Files und Ausgabe der gelesenen Storables
   * @param file
   * @param klass
   * @return
   * @throws XNWH_GeneralPersistenceLayerException
   */
  public static <T extends Storable> List<T> parseFileToStorables( File file, Class<T> klass) throws XNWH_GeneralPersistenceLayerException {
    ResultSetReader<? extends T> reader = Storable.getResultSetReader(klass);
    Document doc;
    try {
      doc = XMLUtils.parse(file.getAbsolutePath());
    } catch (Exception e) { // Ex_FileAccessException XPRC_XmlParsingException
      throw new XNWH_GeneralPersistenceLayerException("could not parse file", e);
    }
    Element root = doc.getDocumentElement();
    long txId = TableInfo.readTransactionId(root, file);
    List<Element> entries = XMLUtils.getChildElementsByName(root, Storable.getTableNameLowerCase(klass));
    List<T> list = new ArrayList<T>();
    for (Element entry : entries) {
      try {
        T storable = reader.read(new XMLResultSet(entry));
        list.add(storable);
      } catch (SQLException e) {
        throw new XNWH_GeneralPersistenceLayerException("could not read from xml result set", e);
      }
    }
    return list;
  }

  
}
