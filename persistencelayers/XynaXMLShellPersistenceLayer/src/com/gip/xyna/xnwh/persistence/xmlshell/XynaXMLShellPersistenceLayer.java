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
package com.gip.xyna.xnwh.persistence.xmlshell;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.types.StringSerializable;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertySourceDefault;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_MissingAnnotationsException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.AnnotationHelper;
import com.gip.xyna.xnwh.persistence.Column;
import com.gip.xyna.xnwh.persistence.Command;
import com.gip.xyna.xnwh.persistence.Parameter;
import com.gip.xyna.xnwh.persistence.Persistable;
import com.gip.xyna.xnwh.persistence.PersistenceLayer;
import com.gip.xyna.xnwh.persistence.PersistenceLayerConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.persistence.PreparedCommand;
import com.gip.xyna.xnwh.persistence.PreparedQuery;
import com.gip.xyna.xnwh.persistence.Query;
import com.gip.xyna.xnwh.persistence.ResultSetReader;
import com.gip.xyna.xnwh.persistence.Storable;
import com.gip.xyna.xnwh.persistence.TransactionProperty;
import com.gip.xyna.xnwh.persistence.file.TransactionFile;
import com.gip.xyna.xprc.xfractwfe.generation.XMLUtils;


/*
 * Performance stuff we could do
 * Generate minimalKeys for regex
 * kill processes once the Query-Latch timed out
 */
public class XynaXMLShellPersistenceLayer implements PersistenceLayer {

  private static Logger logger = CentralFactoryLogging.getLogger(XynaXMLShellPersistenceLayer.class);

  private static final ConcurrentMap<String, ReentrantReadWriteLock> locks =
      new ConcurrentHashMap<String, ReentrantReadWriteLock>();

  private final static String PART_PREFIX = "part";
  private final static int DEFAUL_MAXIMUM_FILES_PER_DIRECTORY = 30000;
  
  private static final String TRANSACTIONMODE = "true";
  private static final String NOTRANSACTIONMODE = "false";
  
  private String tableSuffix = "";
  private int grepTimeoutSecs = 60;
  private boolean generateParts;  
  private int maximumFilesPerDirectory = DEFAUL_MAXIMUM_FILES_PER_DIRECTORY;    
  
  static ExecutorService grepExecutor = Executors.newCachedThreadPool();
  
  /**
   * Path to and including grep (for XynaXMLShellLayer)
   * Examples:
   * "/bin/grep"
   * "/usr/xpg4/bin/grep"
   */
  private static final String PROPERTY_PATH_TO_GREP = "xyna.xnwh.persistence.xmlshell.greppath";
  
  private boolean fileAccessTransactionSafe = true;
  
  static Comparator<String> reverseOrder = new Comparator<String>() {

    public int compare(String name1, String name2) {
      return name2.compareTo(name1);
    }

  };
  

  public PersistenceLayerConnection getConnection() throws PersistenceLayerException {
    return new XMLShellPersistenceLayerConnection();
  }
  
  
  public PersistenceLayerConnection getDedicatedConnection() throws PersistenceLayerException {
    return new XMLShellPersistenceLayerConnection();
  }


  private final String getTableName(Class<?> klass) throws PersistenceLayerException {
    Persistable persi = AnnotationHelper.getPersistable(klass);
    if (persi == null) {
      throw new XNWH_MissingAnnotationsException(klass.getName());
    }
    return persi.tableName();
  }


  private void releaseAllLocks() throws PersistenceLayerException {
    for (ReentrantReadWriteLock lock : locks.values()) {
      releaseWriteLocks(lock);
    }
  }


  private void releaseWriteLocks(ReentrantReadWriteLock lock) {
    // release writeLocks
      while (lock.isWriteLockedByCurrentThread()) {
        lock.writeLock().unlock();
      }
  }
  

  private void readLockTable(String tableName) throws PersistenceLayerException { 
    ReentrantReadWriteLock lock = locks.get(tableName);
    lock.readLock().lock();
  }

  
  private void readUnlockTable(String tableName) throws PersistenceLayerException { 
    ReentrantReadWriteLock lock = locks.get(tableName);
    lock.readLock().unlock();
  }
  
  
  private void writeLockTable(String tableName) throws PersistenceLayerException { 
    ReentrantReadWriteLock lock = locks.get(tableName);
    lock.writeLock().lock();
  }
  
  private static final XynaPropertyString pathToGrep = new XynaPropertyString(PROPERTY_PATH_TO_GREP, "grep"); 

  public static String getPathToGrep() {
    String path = null;
    try {
      if (XynaPropertyUtils.getXynaPropertySource() instanceof XynaPropertySourceDefault) {
        //properties not linked to configuration
        path = Configuration.getConfigurationPreInit().getProperty(PROPERTY_PATH_TO_GREP);
      } else {
        path = pathToGrep.get();
      }
    } catch (XynaException e) {
      logger.warn("Failed to load property.", e);
    }
    if (path == null) {
      return "grep";
    } else {
      return path;
    }
  }
  
  
  private class XMLShellPersistenceLayerConnection implements PersistenceLayerConnection {

    private DocumentBuilderFactory documentFactory;


    public XMLShellPersistenceLayerConnection() {
      documentFactory = DocumentBuilderFactory.newInstance();
    }


    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props) throws PersistenceLayerException {
      File f = new File(XynaProperty.PERSISTENCE_DIR);
      if (!f.exists()) {
        f.mkdir();
      }

      f = new File(getTableDir(klass));
      if (!f.exists()) {
        f.mkdir();
      }

      //create lock for Table
      locks.put(getTableNameWithSuffix(klass), new ReentrantReadWriteLock());
      
      try {
        File []childfiles = f.listFiles(new FilenameFilter() {
            
          public boolean accept(File dir, String name) {
            if(name.endsWith(TransactionFile.FILE_SUFFIX)) {
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
      } catch(Exception e) {
        // Wenn hier irgendwas passiert, k�nnen wir das eigentlich ignorieren. Worst case w�re, dass die tempor�ren Dateien nicht gel�scht werden konnten und
        // die Platte voll m�llen.
        logger.warn("Beim L�schen der tempor�ren Persistencedaten kam es zu einem Fehler.", e);
      }
      
    }


    public void closeConnection() throws PersistenceLayerException {
      releaseAllLocks();
    }


    public void commit() throws PersistenceLayerException {
      releaseAllLocks();
    }


    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
      //readLockTable(storable.getClass());
      File rowDocument = getRowFile(storable);
      if (rowDocument == null) {
        return false;
      }
      return rowDocument.exists();
    }


    public <T extends Storable> void deleteOneRow(T storable) throws PersistenceLayerException {
      writeLockTable(getTableNameWithSuffix(storable.getTableName()));
      if (containsObject(storable)) {
        singleDelete(storable);
      }
    }


    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
      writeLockTable(getTableNameWithSuffix(storableCollection.iterator().next().getTableName()));
      for (T storable : storableCollection) {
        if (containsObject(storable)) {
          singleDelete(storable);
        }
      }
    }


    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
      writeLockTable(getTableNameWithSuffix(klass));
      File archiveDir = new File(getTableDir(klass));
      if (generateParts) {
        File[] archiveDirs = getTableDirs(klass);
        for (int i = 0; i < archiveDirs.length; i++) {
          File[] rows = archiveDirs[i].listFiles();
          if (rows != null && rows.length > 0) {
            for (File file : rows) {
              file.delete();
            }
            archiveDirs[i].delete();
          }
        }
      } else {
        File[] rows = archiveDir.listFiles();
        for (File file : rows) {
          file.delete();
        }
      }
    }


    public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException {
      writeLockTable(getTableNameWithSuffix(cmd.getTable()));

      Set<String> files;
      try {
        files = ((PreparedCommandForXML) cmd).execute(paras, generateAllSet(cmd.getTable()), grepExecutor);
      } catch (GrepException e) {
        logger.debug("Grep did not succed, deleting nothing");
        return 0;
      }

      //Since we only know how to delete...let's do that
      File file = null;
      int count = 0;
      for (String fileName : files) {
        file = new File(getRowFile(cmd.getTable(), fileName));
        if (file.exists()) {
          file.delete();
          count++;
        }
      }

      return count;
    }


    public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
      readLockTable(getTableNameWithSuffix(klass));
      try {
        List<File> allFiles = new ArrayList<File>();
        if (generateParts) {
          File[] tableDirs = getTableDirs(klass);
          for (File file : tableDirs) {
            allFiles.addAll(Arrays.asList(file.listFiles()));
          }
        } else {
          File archive = new File(getTableDir(klass));
          allFiles.addAll(Arrays.asList(archive.listFiles()));
        }

        Collection<T> storableCollection = new ArrayList<T>();
        for (File file : allFiles) {
          if (file.isDirectory()) {
            continue;
          }

          Document doc = null;
          try {
            doc = XMLUtils.parse(file);
          } catch (XynaException e) {
            logger.error("Error parsing file: " + file.getAbsolutePath(), e);
            continue;
          }

          Element root = doc.getDocumentElement();
          if (root != null && root.getNodeName().equals(klass.getSimpleName())) {
            XMLShellResultSet result = new XMLShellResultSet();
  
            NodeList nodes = root.getChildNodes();
            for (int i = 0; i < nodes.getLength(); i++) {
              Node node = nodes.item(i);
              if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
              }
              result.addData(node.getNodeName(), node.getTextContent());
            }
  
            ResultSetReader<T> reader = retrieveReader(klass);
  
            try {
              storableCollection.add((T) reader.read(result));
            } catch (SQLException e) {
              logger.error("Error while reading storable" + e);
              //throw new PersistenceLayerException("Error while reading storable",e);
              continue;
            }
          } else {
            System.out.println("Foreign XML!!!");
          }
        }
        return storableCollection;
      } finally {
        readUnlockTable(getTableNameWithSuffix(klass));
      }
    }


    public <T extends Storable> void persistCollection(Collection<T> storableCollection)
        throws PersistenceLayerException {
      if (storableCollection == null || storableCollection.size() == 0) {
        return;
      }
      writeLockTable(getTableNameWithSuffix(storableCollection.iterator().next().getTableName()));
      for (T storable : storableCollection) {
        persistObjectInternally(storable);
      }
    }


    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
      writeLockTable(getTableNameWithSuffix(storable.getTableName()));
      return persistObjectInternally(storable);
    }


    public <T extends Storable> boolean persistObjectInternally(T storable) throws PersistenceLayerException {

      boolean existedBefore;
      if (containsObject(storable)) {
        singleDelete(storable);
        existedBefore = true;
      } else {
        existedBefore = false;
      }

      ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();

      Column[] cols = storable.getColumns();
      DocumentBuilder builder = null;
      try {
        builder = documentFactory.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
        logger.error("Invalid Configuration for DocumentBuilder", e);
        throw new XNWH_GeneralPersistenceLayerException("Invalid Configuration for DocumentBuilder", e);
      }
      Document doc = builder.newDocument();
      Element root = doc.createElement(storable.getClass().getSimpleName());
      doc.appendChild(root);

      Element colEl = null;
      Text text = null;
      Object obj = null;
      for (Column column : cols) {
        obj = storable.getValueByColName(column);
        if (obj == null) {
          continue;
        }
        switch (column.type()) {
          case INHERIT_FROM_JAVA :
            colEl = doc.createElement(column.name());
            if( obj instanceof StringSerializable ) {
              text = doc.createTextNode(((StringSerializable<?>) obj).serializeToString());
            } else {
              text = doc.createTextNode(obj.toString());
            }
            colEl.appendChild(text);
            root.appendChild(colEl);
            break;
          case BYTEARRAY : //will be treated like Blobbed
          case BLOBBED_JAVAOBJECT :
            byteBuffer.reset();
            ObjectOutputStream objectStream = null;
            try {
              objectStream = new ObjectOutputStream(byteBuffer);
            } catch (IOException e) {
              logger.error("Error while initializing stream for storable: " + storable.getPrimaryKey(), e);
              throw new XNWH_GeneralPersistenceLayerException("Error while initializing stream for storable: "
                  + storable.getPrimaryKey());
            }
            colEl = doc.createElement(column.name());

            if (!(obj instanceof SerializableClassloadedObject)) {
              obj = new SerializableClassloadedObject((Serializable) obj);
            }
            try {
              objectStream.writeObject(obj);
            } catch (IOException e) {
              logger.error("Error while writing blob '" + column.toString() + "' from storable: "
                               + storable.getPrimaryKey() + ", continuing write process", e);
              continue;
            }
            text = doc.createTextNode(Base64.encode(byteBuffer.toByteArray()));
            colEl.appendChild(text);
            root.appendChild(colEl);
            break;
          default :
            throw new XNWH_UnsupportedPersistenceLayerFeatureException("encoding column.type: "
                + column.type().toString());
        }
      }

      try {
        if (generateParts) {
          File f = new File(nextFreeArchive(storable.getTableName()).getPath() + Constants.fileSeparator
              + storable.getClass().getSimpleName() + storable.getPrimaryKey() + ".xml");
          if(fileAccessTransactionSafe) {
            TransactionFile tf = new TransactionFile(f);
            XMLUtils.saveDom(tf, doc);
            tf.commit();
          } else {
            XMLUtils.saveDom(f, doc);
          }
        } else {
          File f = new File(getRowFileName(storable));
          if(fileAccessTransactionSafe) {
            TransactionFile tf = new TransactionFile(f);
            XMLUtils.saveDom(tf, doc);
            tf.commit();
          } else {
            XMLUtils.saveDom(f, doc);
          }
        }
      } catch (XynaException e) {
        logger.error("Error while saving xml to archive", e);
      }

      return existedBefore;

    }


    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
      return new PreparedCommandForXML(cmd, tableSuffix, grepTimeoutSecs);
    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
      try {
        return new PreparedQueryForXML<E>(query, tableSuffix, grepTimeoutSecs);
      } catch (SQLException e) {
        logger.error("Could not prepare Query", e);
        throw new XNWH_GeneralPersistenceLayerException("Could not prepare Query: " + e.getMessage(), e);
      }
    }


    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException {
      return this.query(query, parameter, maxRows, query.getReader());
    }
    
    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader) throws PersistenceLayerException {
      readLockTable(getTableNameWithSuffix(query.getTable()));
      logger.debug("Query started");

      try {
        Set<String> set = null;
        try {
          set = ((PreparedQueryForXML) query).execute(parameter, generateAllSet(query.getTable()));
        } catch (SQLException e) {
          logger.error("Could not execute Query", e);
          throw new XNWH_GeneralPersistenceLayerException("Could not execute query: " + e.getMessage(), e);
        } catch (GrepException e) {
          logger.error("Grep returned an error, returning empty result", e);
          return new ArrayList<E>();
          //throw new PersistenceLayerException("Error while executing query",e);
        }
        
        SortedSet<String> sortedResults = new TreeSet<String>(new Comparator<String>() {
          public int compare(String o1, String o2) {
            int lengthDiff = o1.length() - o2.length();
            if (lengthDiff == 0) {
              return -o1.compareTo(o2);
            } else {
              return -lengthDiff;
            }
          }
        });
        sortedResults.addAll(set);

        List<E> ret = new ArrayList<E>();
        //logger.debug("got a set from grep:\n" + set);        
        for (String rowName : sortedResults) {
          if (!rowName.endsWith(".xml")) {
            continue;
          }
          Document doc = null;
          try {
            doc = XMLUtils.parse(getRowFile(query.getTable(), rowName));
          } catch (XynaException e) {
            logger.error("Could not parse XML, continuing with next element", e);
            //throw new PersistenceLayerException("Could not parse XML",e);
            continue;
          }

          Element root = doc.getDocumentElement();
          XMLShellResultSet result = new XMLShellResultSet();

          NodeList nodes = root.getChildNodes();
          for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() != Node.ELEMENT_NODE) {
              continue;
            }
            result.addData(node.getNodeName(), node.getTextContent());
          }

          E element = null;
          try {
            element = reader.read(result);
          } catch (SQLException e) {
            logger.error("Error while reading storable, continuing with next element", e);
            //throw new PersistenceLayerException("Error while reading storable",e);
            continue;
          }
          ret.add(element);
          if (maxRows != -1 && ret.size() >= maxRows) {
            break;
          }
        }
        logger.debug("Returning " + ret.size() + " instances");
        return ret;
      } finally {
        readUnlockTable(getTableNameWithSuffix(query.getTable()));
      }
    }


    public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      queryOneRowInternally(storable, true);
    }


    public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      queryOneRowInternally(storable, false);
    }


    private <T extends Storable> void queryOneRowInternally(T storable, final boolean forUpdate)
        throws PersistenceLayerException, XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {

      if (forUpdate) {
        writeLockTable(getTableNameWithSuffix(storable.getTableName()));
      } else {
        readLockTable(getTableNameWithSuffix(storable.getTableName()));
      }

      try {
        Document doc = null;
        try {
          doc = XMLUtils.parse(getRowFile(storable));
        } catch (XynaException e1) {
          //no need to log an error, 1 ObjectNotFound is expected with calls from the orderArchive
          throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()),
                                                          storable.getTableName());
        }

        if (doc == null) {
          //no need to log an error, 1 ObjectNotFound is expected with calls from the orderArchive
          throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()),
                                                          storable.getTableName());
        }

        Element root = doc.getDocumentElement();
        XMLShellResultSet result = new XMLShellResultSet();

        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
          Node node = nodes.item(i);
          if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }
          result.addData(node.getNodeName(), node.getTextContent());
        }

        ResultSetReader<T> reader = storable.getReader();
        T data = null;
        try {
          data = reader.read(result);
        } catch (SQLException e) {
          logger.error("Error while reading storable", e);
          throw new XNWH_GeneralPersistenceLayerException("Error while reading storable", e);
        }
        storable.setAllFieldsFromData(data);
      } finally {
        if (!forUpdate) {
          readUnlockTable(getTableNameWithSuffix(storable.getTableName()));
        }
      }
    }


    public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
      readLockTable(getTableNameWithSuffix(query.getTable()));
      try {
        Set<String> set = null;
        try {
          set = ((PreparedQueryForXML) query).execute(parameter, generateAllSet(query.getTable()));
          //logger.debug("Query one row resolved: " + set);
        } catch (SQLException e) {
          logger.error("Could not execute query", e);
          throw new XNWH_GeneralPersistenceLayerException("Could not execute query", e);
        } catch (GrepException e) {
          throw new XNWH_GeneralPersistenceLayerException("Error while executing query", e);
        }

        //logger.debug(((PreparedQueryForXML)query).getSelection());
        if (((PreparedQueryForXML) query).getSelection().get(0).toString().toLowerCase().startsWith("count")) {
          XMLShellResultSet rs = new XMLShellResultSet();
          rs.addData("count", Integer.toString(set.size()));
          try {
            return query.getReader().read(rs);
          } catch (SQLException e) {
            logger.error("CountObj could not be created.", e);
            throw new XNWH_GeneralPersistenceLayerException("CountObj could not be created.", e);
          }
          //return (E)new OrderCount(set.size());
        }

        //we'll only want one
        if (set == null || set.size() == 0) {
          return null;
        }
        String rowName = set.iterator().next();

        if (!rowName.endsWith(".xml")) {
          return null;
        }
        Document doc = null;
        try {
          doc = XMLUtils.parse(getRowFileName(query.getTable(), rowName));
        } catch (XynaException e) {
          logger.error("Error parsing XML", e);
          throw new XNWH_GeneralPersistenceLayerException("Error while parsing XML", e);
        }

        Element root = doc.getDocumentElement();
        XMLShellResultSet result = new XMLShellResultSet();

        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
          Node node = nodes.item(i);
          if (node.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }
          result.addData(node.getNodeName(), node.getTextContent());
        }

        ResultSetReader<? extends E> reader = query.getReader();

        E element = null;
        try {
          element = reader.read(result);
        } catch (SQLException e) {
          logger.error("Error reading storable", e);
          throw new XNWH_GeneralPersistenceLayerException("Error reading storable", e);
        }
        return element;
      } finally {
        readUnlockTable(getTableNameWithSuffix(query.getTable()));
      }
    }


    public void rollback() throws PersistenceLayerException {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("rollback");
    }


    private <T extends Storable> void singleDelete(T storable) throws PersistenceLayerException {
      File rowDocument = getRowFile(storable);
      rowDocument.delete();
    }


    private <T extends Storable> ResultSetReader<T> retrieveReader(Class<T> klass) throws PersistenceLayerException {
      try {
        return klass.newInstance().getReader();
      } catch (InstantiationException e) {
        logger.error("Storable " + klass.getName() + " must have a valid no arguments constructor.", e);
        throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
            + " must have a valid no arguments constructor.", e);
      } catch (IllegalAccessException e) { //we could circumvent this by setting it accesseable
        logger.error("Storable " + klass.getName() + " must have a valid no arguments constructor.", e);
        throw new XNWH_GeneralPersistenceLayerException("Storable " + klass.getName()
            + " must have a valid no arguments constructor.", e);
      }
    }


    private final String getTableNameWithSuffix(Class<?> klass) throws PersistenceLayerException {
      StringBuilder sb = new StringBuilder();
      sb.append(getTableName(klass));
      sb.append(tableSuffix);
      return sb.toString();
    }


    private final String getTableNameWithSuffix(String tableName) throws PersistenceLayerException {
      StringBuilder sb = new StringBuilder();
      sb.append(tableName);
      sb.append(tableSuffix);
      return sb.toString();
    }


    private final String getTableDir(Class<?> klass) throws PersistenceLayerException {
      StringBuilder sb = new StringBuilder();
      sb.append(XynaProperty.PERSISTENCE_DIR);
      sb.append(Constants.fileSeparator);
      sb.append(getTableName(klass));
      sb.append(tableSuffix);
      return sb.toString();
    }


    private final String getTableDir(String tableName) throws PersistenceLayerException {
      StringBuilder sb = new StringBuilder();
      sb.append(XynaProperty.PERSISTENCE_DIR);
      sb.append(Constants.fileSeparator);
      sb.append(tableName);
      sb.append(tableSuffix);
      return sb.toString();
    }


    private final File[] getTableDirs(Class<?> klass) throws PersistenceLayerException {
      File mainArchiveDir = new File(getTableDir(klass));
      return mainArchiveDir.listFiles(new FileFilter() {

        public boolean accept(File pathname) {
          return pathname.isDirectory();
        }
      });
    }


    private final File[] getTableDirs(String tableName) throws PersistenceLayerException {
      File mainArchiveDir = new File(getTableDir(tableName));
      return mainArchiveDir.listFiles(new FileFilter() {

        public boolean accept(File pathname) {
          return pathname.isDirectory();
        }
      });
    }


    private final <T extends Storable> String getRowFileName(T storable, String archivePart)
        throws PersistenceLayerException {
      Class<?> klass = storable.getClass();
      StringBuilder sb = new StringBuilder();
      sb.append(getTableDir(klass));
      sb.append(Constants.fileSeparator);
      sb.append(archivePart);
      sb.append(Constants.fileSeparator);
      sb.append(klass.getSimpleName());
      sb.append(storable.getPrimaryKey());
      sb.append(".xml");
      return sb.toString();
    }


    private final <T extends Storable> String getRowFileName(T storable) throws PersistenceLayerException {
      Class<?> klass = storable.getClass();
      StringBuilder sb = new StringBuilder();
      sb.append(getTableDir(klass));
      sb.append(Constants.fileSeparator);
      sb.append(klass.getSimpleName());
      sb.append(storable.getPrimaryKey());
      sb.append(".xml");
      return sb.toString();
    }


    private final <T extends Storable> String getRowFileName(String tableName, String entryName)
        throws PersistenceLayerException {
      StringBuilder sb = new StringBuilder();
      sb.append(getTableDir(tableName));
      sb.append(Constants.fileSeparator);
      sb.append(entryName);
      //sb.append(".xml");
      //logger.debug("returning from getRowFileName(String tableName, String entryName): "+ sb.toString());
      return sb.toString();
    }


    private final <T extends Storable> File getRowFile(T storable) throws PersistenceLayerException {
      File rowFile = null;
      if (generateParts) {
        for (int i = 1; i <= getArchivePartCount(storable); i++) {
          rowFile = new File(getRowFileName(storable, PART_PREFIX + i));
          if (rowFile.exists()) {
            break;
          }
        }
      } else {
        rowFile = new File(getRowFileName(storable));
      }

      return rowFile;
    }


    private final <T extends Storable> int getArchivePartCount(T storable) throws PersistenceLayerException {
      File mainArchiveDir = new File(getTableDir(storable.getTableName()));
      File []files = mainArchiveDir.listFiles(new FileFilter() {

        public boolean accept(File pathname) {
          return pathname.isDirectory();
        }
      });
      if(files != null) {
        return files.length;
      } else {
        return 0;
      }
    }


    private final File nextFreeArchive(String tableName) throws PersistenceLayerException {
      int i = 1;
      while (i <= maximumFilesPerDirectory) {
        //logger.debug("will be creating new File on: "+getTableDir(tableName)+Constants.fileSeparator+PART_PREFIX+i);
        File f = new File(getTableDir(tableName) + Constants.fileSeparator + PART_PREFIX + i);
        if (!f.exists()) {
          f.mkdir();
          return f;
        }
        if (f.list().length < maximumFilesPerDirectory) {
          return f;
        }
        i++;
      }
      throw new XNWH_GeneralPersistenceLayerException("No free archive");
    }


    private final String getRowFile(String tableName, String retrievedRow) throws PersistenceLayerException {
      StringBuilder sb = new StringBuilder();
      sb.append(XynaProperty.PERSISTENCE_DIR);
      sb.append(Constants.fileSeparator);
      sb.append(tableName);
      sb.append(tableSuffix);
      sb.append(Constants.fileSeparator);
      sb.append(retrievedRow);
      //logger.debug("returning from getRowFile(String tableName, String retrievedRow): "+sb.toString());
      return sb.toString();
    }


    private final Set<String> generateAllSet(String tableName) throws PersistenceLayerException {
      //logger.debug("Generating allSet for: " + getTableDir(tableName));    
      //Set<String> allSet = new HashSet<String>();
      Set<String> allSet = Collections.synchronizedSortedSet(new TreeSet<String>(reverseOrder));
      if (generateParts) {
        File[] archives = getTableDirs(tableName);
        File[] filesInArchive;
        for (int i = 0; i < archives.length; i++) {
          filesInArchive = archives[i].listFiles();
          if (filesInArchive != null && filesInArchive.length > 0) {
            for (int j = 0; j < filesInArchive.length; j++) {
              StringBuilder sb = new StringBuilder(64);
              sb.append(".").append(Constants.fileSeparator).append(archives[i].getName())
                  .append(Constants.fileSeparator).append(filesInArchive[j].getName());
              allSet.add(sb.toString());
            }
          }
        }
      } else {
        File archive = new File(getTableDir(tableName));
        File[] filesInArchive = archive.listFiles();
        if (filesInArchive != null && filesInArchive.length > 0) {
          for (int i=0; i<filesInArchive.length; i++) {
            StringBuilder sb = new StringBuilder(64);
            sb.append(".").append(Constants.fileSeparator).append(filesInArchive[i].getName());
            allSet.add(sb.toString());
          }
        }
      }
      return allSet;
    }


    public void setTransactionProperty(TransactionProperty arg0) {
    }


    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> arg0) throws PersistenceLayerException {
      // TODO something?
    }


    public boolean isOpen() {
      return true;
    }


    public <T extends Storable> void removeTable(Class<T> klass, Properties props) throws PersistenceLayerException {
      locks.remove(getTableNameWithSuffix(klass)); 
    }

  }


  public String getInformation() {

    StringBuilder sb = new StringBuilder();
    sb.append("XML persistence with shell queries. ");
    sb.append("Files are written to directory '" + new File(XynaProperty.PERSISTENCE_DIR).getAbsolutePath() + "' ");
    if (!tableSuffix.equals("")) {
      sb.append("Saving files into a directory with suffix '").append(tableSuffix).append("'");
      if (generateParts) {
        sb.append(" split into chunks of ").append(maximumFilesPerDirectory).append(". ");
      } else {
        sb.append(". ");
      }
    } else {
      if (generateParts) {
        sb.append("Saving files split into chunks of ").append(maximumFilesPerDirectory).append(".");
      }
    }

    sb.append("Queries timeout after ");
    sb.append(grepTimeoutSecs);
    sb.append(" secs.");

    sb.append(" transaction mode = ").append((fileAccessTransactionSafe ? TRANSACTIONMODE : NOTRANSACTIONMODE));
    return sb.toString();

  }


  public boolean describesSamePhysicalTables(PersistenceLayer pl) {
    if (pl instanceof XynaXMLShellPersistenceLayer) {
      XynaXMLShellPersistenceLayer xpl = (XynaXMLShellPersistenceLayer)pl;
      return tableSuffix.equals(xpl.tableSuffix);
    }
    return false;
  }


  public void init(Long pliID, String... args) throws PersistenceLayerException {
    if (args == null || args.length == 0 || args[0] == null) {
      tableSuffix = "";
      return;
    } else {
      tableSuffix = args[0];
    }

    if (args.length > 1) {
      try {
        grepTimeoutSecs = Integer.parseInt(args[1]);
      } catch (NumberFormatException e) {
        logger.warn("Invalid value supplied for timeoutForSearchInSecs '" + args[1]
            + "', Integer expected. Using default: 60secs");
      }
      if (args.length > 2) {
        generateParts = Boolean.parseBoolean(args[2]);
        if (args.length > 3) {
          try {
            maximumFilesPerDirectory = Integer.parseInt(args[3]);
          } catch (NumberFormatException e) {
            logger.warn("Invalid value supplied for maximumFilesPerDirectory '" + args[3]
                + "', Integer expected. Using default: " + DEFAUL_MAXIMUM_FILES_PER_DIRECTORY);
          }
          if (args.length > 4) {
            if(TRANSACTIONMODE.equals(args[4])) {
              fileAccessTransactionSafe = true; 
            } else if(NOTRANSACTIONMODE.equals(args[4])) {
              fileAccessTransactionSafe = false;
            }
          }
        }
      }
    }
  }


  public String[] getParameterInformation() {
    return new String[] {"tableSuffix", "timeoutForQueriesInSecs", "generateParts", "maximumFilesPerDirectory", "transaction mode: " + TRANSACTIONMODE + " (default), " + NOTRANSACTIONMODE};
  }


  public void shutdown() throws PersistenceLayerException {
  }


  public Reader getExtendedInformation(String[] arg0) {
    return null;
  }


  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    return getConnection();
  }


  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    return plc instanceof XynaXMLShellPersistenceLayer;
  }

}
