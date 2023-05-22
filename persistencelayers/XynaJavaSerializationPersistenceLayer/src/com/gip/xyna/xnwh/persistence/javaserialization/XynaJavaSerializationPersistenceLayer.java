/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

package com.gip.xyna.xnwh.persistence.javaserialization;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.db.UnsupportingResultSet;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xods.configuration.XynaProperty;
import com.gip.xyna.xnwh.exceptions.XNWH_GeneralPersistenceLayerException;
import com.gip.xyna.xnwh.exceptions.XNWH_IncompatiblePreparedObjectException;
import com.gip.xyna.xnwh.exceptions.XNWH_MissingAnnotationsException;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.exceptions.XNWH_UnsupportedPersistenceLayerFeatureException;
import com.gip.xyna.xnwh.persistence.AnnotationHelper;
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
import com.gip.xyna.xnwh.persistence.javaserialization.TableIndex.Status;



public class XynaJavaSerializationPersistenceLayer implements PersistenceLayer {

  private static final Logger logger = CentralFactoryLogging.getLogger(XynaJavaSerializationPersistenceLayer.class);
  
  
  private static final String TRANSACTIONMODE = "true";
  private static final String NOTRANSACTIONMODE = "false";
  
  private volatile Map<String, TableIndex> knownTables;

  private static Pattern selectCountStarPattern = Pattern.compile("select count\\(\\*\\) from (\\w+)");
  
  private boolean fileAccessTransactionSafe = true;
  
  
  public XynaJavaSerializationPersistenceLayer() throws PersistenceLayerException {

    knownTables = new HashMap<String, TableIndex>();
    restoreKnownTables();
  }
  
  // rebuild the known tables from the stored indexes
  private void restoreKnownTables() throws PersistenceLayerException {
    File[] dirsInPersDir;
    File f = new File(XynaProperty.PERSISTENCE_DIR);
    if (!f.exists()) {
      return;
    }
    dirsInPersDir = f.listFiles();
    if (dirsInPersDir.length == 0) {
      return;
    }
    for (File dirInPersDir : dirsInPersDir) {
      if (dirInPersDir.isDirectory()) {
        File[] index = dirInPersDir.listFiles(new FilenameFilter() {

          public boolean accept(File dir, String name) {
            if (name.equals(dir.getName() + XynaProperty.INDEX_SUFFIX)) {
              return true;
            }
            return false;
          }
        });

        if (index.length > 0) {
          // restore that TableIndex
          ObjectInputStream ois = null;
          try {
            ois = new ObjectInputStream(new FileInputStream(index[0]));
            TableIndex ti = (TableIndex) ois.readObject();
            ti.init();
            // this is ugly
            knownTables.put((String) index[0].getName().subSequence(0, index[0].getName().indexOf('.')), ti);
          } catch (FileNotFoundException e) {
            throw new XNWH_GeneralPersistenceLayerException("the persistence directory could not be found", e);
          } catch (IOException e) {
            throw new XNWH_GeneralPersistenceLayerException("the persistence directory could not be read", e);
          } catch (ClassNotFoundException e) {
            throw new XNWH_GeneralPersistenceLayerException("the deserialized index turned out not to be an index", e);
          } finally {
            try {
              if (ois != null) {
                ois.close();
              }
            } catch (IOException e) {
              throw new XNWH_GeneralPersistenceLayerException("could not close ObjectStream of tableIndex");
            }
          }
        }
      }
    }
  }


  public String getTableDir(String tableName) {
    return XynaProperty.PERSISTENCE_DIR + Constants.fileSeparator + tableName;
  }


  public String getTablePath(String tableName) {
    return new StringBuilder().append(XynaProperty.PERSISTENCE_DIR + Constants.fileSeparator).append(tableName)
        .append(Constants.fileSeparator).append(tableName).toString();
  }


  public String getIndexPath(String tableName) {
    return new StringBuilder().append(XynaProperty.PERSISTENCE_DIR + Constants.fileSeparator).append(tableName)
        .append(Constants.fileSeparator).append(tableName).append(XynaProperty.INDEX_SUFFIX).toString();
  }


  public PersistenceLayerConnection getConnection() {
    return new SerializationPersistenceLayerConnection();
  }
  
  
  public PersistenceLayerConnection getDedicatedConnection() {
    return new SerializationPersistenceLayerConnection();
  }

  
  private class SerializationPersistenceLayerConnection implements PersistenceLayerConnection {
    
    public SerializationPersistenceLayerConnection() {
    }

    public <T extends Storable> void addTable(Class<T> klass, boolean forceWidening, Properties props) throws PersistenceLayerException {
      File f = new File(XynaProperty.PERSISTENCE_DIR);
      if (!f.exists()) {
        f.mkdir();
      }
      
      logger.debug("Add table called for: " + klass.getSimpleName());
      
      // create a new file and add to knownTables
      final String tableName = getTableName(klass);

      f = new File(getTableDir(tableName));
      if (!f.exists()) {
        f.mkdirs();
      }

      File tableIndex = new File(getIndexPath(tableName));
      if (!tableIndex.exists()) {
        TableIndex ti = new TableIndex();
        knownTables.put(tableName, ti);
        writeIndex(ti, tableName, null);
      } else {
        // restore that TableIndex
        ObjectInputStream ois = null;
        try {
          ois = new ObjectInputStream(new FileInputStream(tableIndex));
          TableIndex ti = (TableIndex) ois.readObject();
          ti.init();
          // this is ugly
          knownTables.put((String) tableIndex.getName().subSequence(0, tableIndex.getName().indexOf('.')), ti);
        } catch (FileNotFoundException e) {
          throw new XNWH_GeneralPersistenceLayerException("the persistence directory could not be found", e);
        } catch (IOException e) {
          throw new XNWH_GeneralPersistenceLayerException("the persistence directory could not be read", e);
        } catch (ClassNotFoundException e) {
          throw new XNWH_GeneralPersistenceLayerException("the deserialized index turned out not to be an index", e);
        } finally {
          try {
            if (ois != null) {
              ois.close();
            }
          } catch (IOException e) {
            throw new XNWH_GeneralPersistenceLayerException("could not close ObjectStream of tableIndex");
          }
        }
      }
      
      try {
        File []childfiles = f.listFiles(new FilenameFilter() {
            
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
      } catch(Exception e) {
        // Wenn hier irgendwas passiert, k�nnen wir das eigentlich ignorieren. Worst case w�re, dass die tempor�ren Dateien nicht gel�scht werden konnten und
        // die Platte voll m�llen.
        logger.warn("Beim L�schen der tempor�ren Persistencedaten kam es zu einem Fehler.", e);
      }
      
    }


    private void releaseAllLocks() {
      Collection<TableIndex> allTables = knownTables.values();
      for (TableIndex ti : allTables) {
        ti.releaseLock();
      }
    }


    public void closeConnection() throws PersistenceLayerException {
      releaseAllLocks();
    }


    public void commit() throws PersistenceLayerException {
      releaseAllLocks();
    }


    private TableIndex checkTable(String tableName) throws PersistenceLayerException {
      TableIndex t = knownTables.get(tableName);
      if (t == null) {
        // TODO exception type?
        throw new XNWH_GeneralPersistenceLayerException("table " + tableName + " unknown");
      }
      t.getLock();
      return t;
    }


    public <T extends Storable> boolean containsObject(T storable) throws PersistenceLayerException {
      String tableName = storable.getTableName();
      TableIndex ti = checkTable(tableName);
      return ti.contains(storable.getPrimaryKey());
    }


    public int executeDML(PreparedCommand cmd, Parameter paras) throws PersistenceLayerException {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("execute DML");
    }
    
    
    private void writeIndex(TableIndex index, String tableName, TransactionFile archiveFile) throws PersistenceLayerException {
      logger.debug("writeIndex called");

      File indexFile = new File (getIndexPath(tableName));
      TransactionFile tindexFile = null;
      if(fileAccessTransactionSafe) {
        tindexFile = new TransactionFile(indexFile);
      }
      if (indexFile.exists() && !fileAccessTransactionSafe) {
        indexFile.delete();
        try {
          indexFile.createNewFile();         
        } catch (IOException e) {
          logger.error("could not create the index for table " + tableName, e);
          throw new XNWH_GeneralPersistenceLayerException("could not create the index for table " + tableName, e);
        }
      }
      
      FileOutputStream indexStream = null;
      try {
        if(fileAccessTransactionSafe) {
          indexStream = new FileOutputStream(tindexFile);
        } else {
          indexStream = new FileOutputStream(indexFile);
        }
      } catch (FileNotFoundException e) {
        logger.error("Table file could not be found under " + indexFile.getAbsolutePath(),e);
        throw new XNWH_GeneralPersistenceLayerException(e.getMessage(),e);
      }

      ObjectOutputStream out = null;
      try {    
        out = new ObjectOutputStream(indexStream);
        out.writeObject(index);
        out.flush();
        if(fileAccessTransactionSafe) {
          if (archiveFile != null) {
            archiveFile.commit();
          }
          tindexFile.commit();
        }
      } catch (IOException e) {
        logger.error("could not write the index for table " + tableName, e);
        throw new XNWH_GeneralPersistenceLayerException("could not write the index for table " + tableName, e);
      } finally {
        try {
          out.close();
        } catch (IOException e) {
          logger.error("could not close ObjectOutputStream for " + tableName, e);
          throw new XNWH_GeneralPersistenceLayerException("could not close ObjectOutputStream for "+ tableName, e);
        }
      }
    }

    

    private <T extends Storable> void updateObject(T storable, String tableName) throws PersistenceLayerException {
      logger.debug("updateObject called");
      delete(new ArrayList(Arrays.asList(new Storable[]{storable})));
      persistObject(storable);
      
      /*
       * probleme durch konstruktor von objectoutputstream, der schon etwas schreibt, auch wenn das update danach
       * abgebrochen wird.
       * 
      TableIndex ti = checkTable(tableName);

      for (RowObject row : ti) {
        if (row.getPrimaryKey().equals(storable.getPrimaryKey())) {
          try {
            File f = new File(getTablePath(tableName));
            FileOutputStream fos = new FileOutputStream(f);
            PersistenceObjectOutputStream poos = new PersistenceObjectOutputStream(fos);
            try {
              poos.skipTo(ti.calculateOffset(row) + PersistenceObjectOutputStream.STREAMHEADER_SIZE);
              RowObject createdRow = poos.writeStorable(storable, row);
              // we only have 1 ColumnObject for now, else we should replace all the columns with the new one or set the
              // entire row to the newly created one
              row.get(0).setSize(createdRow.get(0).getSize());
              // object is added to the table, now we need to save the new tableIndex
              writeIndex(ti, tableName);
              
            } finally {
              // ti.add(createdRow);
              poos.flush();
              poos.close();
              
            }
          } catch (FileNotFoundException e) {
            throw new PersistenceLayerException("the archive directory did not exist", e);
          } catch (IOException e) {
            throw new PersistenceLayerException("the archive could not be written", e);
          }
        }
      }*/
    }
    
    private <T extends Storable> void insertObject(T storable, String tableName) throws PersistenceLayerException {
      logger.debug("insertObject called");
      if (storable.getPrimaryKey() == null) {
        throw new XNWH_GeneralPersistenceLayerException("primary key must not be null.");
      }
      TableIndex ti = checkTable(tableName);
      if (ti.getStatus() == Status.BLOB) {
        //TODO ist das performant?
        loadCollection(storable.getClass());
      }
      PersistenceObjectOutputStream poos = null;
      try {        
        File f = new File(getTablePath(tableName));
        TransactionFile tf = null;
        if(fileAccessTransactionSafe) {
          tf = new TransactionFile(f);
          tf.copyFileContent();
        }
        //we open to append because the file isn't contained, so we add it to the end
        FileOutputStream fos = null;
        if(fileAccessTransactionSafe) {
          fos = new FileOutputStream(tf, true);
        } else {
          fos = new FileOutputStream(f, true);
        }
        poos = new PersistenceObjectOutputStream(fos);
        RowObject createdRow = poos.writeStorable(storable, null);
        ti.add(createdRow);
        poos.flush();
        writeIndex(ti, tableName, tf);
      } catch (FileNotFoundException e) {
        throw new XNWH_GeneralPersistenceLayerException("the archive directory did not exist", e);
      } catch (IOException e) {
        throw new XNWH_GeneralPersistenceLayerException("the archive could not be written", e);
      } finally {
        try {
          if (poos != null) {
            poos.close();
          }
        } catch (IOException e) {
          throw new XNWH_GeneralPersistenceLayerException("could not close ObjectOutputStream for " + tableName, e);
        }
      }
    }


    public <T extends Storable> boolean persistObject(T storable) throws PersistenceLayerException {
      logger.debug("persistObject called");
      String tableName = storable.getTableName();
      if (containsObject(storable)) {
        updateObject(storable, tableName);      
        return true;
      } else {
        insertObject(storable, tableName);
        return false;
      }
    }


    public PreparedCommand prepareCommand(Command cmd) throws PersistenceLayerException {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("prepareCommand");
    }


    public <E> PreparedQuery<E> prepareQuery(Query<E> query) throws PersistenceLayerException {
      if (!query.getSqlString().matches("select count\\(\\*\\) from \\w+")) {
        throw new XNWH_UnsupportedPersistenceLayerFeatureException("count statements");
      }
      return new JavaSerializationPreparedQuery<E>(query);
    }


    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows) throws PersistenceLayerException {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("query");
    }
    
    
    public <E> List<E> query(PreparedQuery<E> query, Parameter parameter, int maxRows, ResultSetReader<? extends E> reader) throws PersistenceLayerException {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("query");
    }


    public <T extends Storable> void queryOneRowForUpdate(T storable) throws PersistenceLayerException,
        XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      // the queryOneRow currently locks the whole table anyway. once this is no longer the case,
      // this has to get a row lock.
      queryOneRow(storable);
    }


    public <T extends Storable> void queryOneRow(T storable) throws PersistenceLayerException,
                    XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
      logger.debug("queryOneRow called");
      String tableName = storable.getTableName();

      TableIndex ti = checkTable(tableName);
      if (ti.getStatus() == Status.BLOB) {
        // TODO use returned collection to answer the request?
        loadCollection(storable.getClass());
        ti = checkTable(tableName);
        if (ti.getStatus() == Status.BLOB) {
          throw new XNWH_UnsupportedPersistenceLayerFeatureException("queryOneRow with status == BLOB");
        }
      }

      if (!containsObject(storable)) {
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()), tableName);
      }
      File f = new File(getTablePath(tableName));
      FileInputStream fis;
      ObjectInputStream ois = null;
      try {
        fis = new FileInputStream(f);
      } catch (FileNotFoundException e) {
        logger.error("Table file could not be found under " + f.getAbsolutePath(),e);
        throw new XNWH_GeneralPersistenceLayerException(e.getMessage(),e);
      }
      
      boolean found = false;
      for (RowObject row : ti) {
        if (row.getPrimaryKey().equals(storable.getPrimaryKey())) {          
          try {
            
            // we can do this encapsulated once we overwrite the ObjectInputStream            
            fis.getChannel().position(ti.calculateOffset(row));
            //ois = new ObjectInputStream(fis);
            ois = storable.getObjectInputStreamForStorable(fis); 

            Object o = readObject(ois);
            storable.setAllFieldsFromData((T) o);
            found = true;
            break;
          } catch (FileNotFoundException e) {
            throw new XNWH_GeneralPersistenceLayerException("the archive directory did not exist", e);
          } catch (IOException e) {
            throw new XNWH_GeneralPersistenceLayerException("the archive could not be read", e);
          } catch (ClassNotFoundException e) {
            throw new XNWH_GeneralPersistenceLayerException("storable class could not be found", e);
          }/* finally { don't close, or we'll lose the FileStream
            try {
              if (ois != null) {
                //ois.close();
              }
            } catch (IOException e) {
              throw new PersistenceLayerException("could not close ObjectOutputStream for "+ tableName, e);
            }
          }*/
        }
      }
      try {
        ois.close();
      } catch (IOException e) {
        logger.error("Couldn't close stream.",e);
        throw new XNWH_GeneralPersistenceLayerException(e.getMessage(),e);
      }
      if(!found) {
        // falls Indexdatei nicht mehr up-to-date ...
        throw new XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY(String.valueOf(storable.getPrimaryKey()), tableName);
      }
    }


    private Object readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
      Object o = ois.readObject();
      if (o instanceof SerializableClassloadedObject) {
        o = ((SerializableClassloadedObject) o).getObject();
      }
      return o;
    }


    public <E> E queryOneRow(PreparedQuery<E> query, Parameter parameter) throws PersistenceLayerException {
      logger.debug("queryOneRow with query called called");
      if (query instanceof JavaSerializationPreparedQuery) {
        JavaSerializationPreparedQuery<E> q = (JavaSerializationPreparedQuery<E>) query;
        Matcher matcher = selectCountStarPattern.matcher(q.getQuery().getSqlString());
        if (matcher.matches()) {
          String tableName = matcher.group(1);
          TableIndex ti = checkTable(tableName);
          UnsupportingResultSetForCount rs = new UnsupportingResultSetForCount(ti.size());
          try {
            return q.getReader().read(rs);
          } catch (SQLException e) {
            throw new XNWH_GeneralPersistenceLayerException("error reading from resultset: " + e.getMessage(), e);
          }
        } else {
          throw new XNWH_UnsupportedPersistenceLayerFeatureException("query: " + q.getQuery().getSqlString());
        }
      } else {
        throw new XNWH_IncompatiblePreparedObjectException(PreparedQuery.class.getSimpleName());
      }
    }


    public void rollback() throws PersistenceLayerException {
      throw new XNWH_UnsupportedPersistenceLayerFeatureException("rollback");
    }
    
    public <T extends Storable> void persistCollection(Collection<T> storableCollection) throws PersistenceLayerException {
      persistCollection(storableCollection, false);
    }


    private <T extends Storable> void persistCollection(Collection<T> storableCollection, boolean deleteBeforeWrite)
                    throws PersistenceLayerException {
      logger.debug("persistCollection called");
      if (storableCollection.size() == 0) {
        return;
      }
      ArrayList<T> loadedObjects = new ArrayList<T>();

      String tableName = storableCollection.iterator().next().getTableName();

      TableIndex ti = checkTable(tableName);
 
      File f = new File(getTablePath(tableName));
      
      //alte collection laden
      if (ti.size() > 0 && !deleteBeforeWrite && f.exists()) {
        if (ti.getStatus() == Status.SINGLE) {
          ObjectInputStream ois = null;
          FileInputStream fis = null;
          try {
            fis = new FileInputStream(f);
          } catch (FileNotFoundException e1) {
            logger.error("Table file could not be found under " + f.getAbsolutePath());
          }
          try {            
            for (RowObject row : ti) {
              logger.trace("calculated offset " + ti.calculateOffset(row));
              fis.getChannel().position(ti.calculateOffset(row));

              //ois = new ObjectInputStream(fis);
              ois = storableCollection.iterator().next().getObjectInputStreamForStorable(fis);
              //try {
                Object o = readObject(ois);
                loadedObjects.add((T) o);
              /*} finally {
                //don't close or we lose our FileStream
                //ois.close();
              }*/
            }

          } catch (FileNotFoundException e) {
            throw new XNWH_GeneralPersistenceLayerException("archive directory did not exist", e);
          } catch (IOException e) {
            throw new XNWH_GeneralPersistenceLayerException("archive could not be read", e);
          } catch (ClassNotFoundException e) {
            throw new XNWH_GeneralPersistenceLayerException("storable class could not be found", e);
          } finally {
            try {
              ois.close();
            } catch (IOException e) {
              logger.error("Couldn't close stream.",e);
              throw new XNWH_GeneralPersistenceLayerException(e.getMessage(),e);
            }
          }
        } else {
          Collection<?> col = loadCollection(storableCollection.iterator().next().getClass());
          for (Object object : col) {
            loadedObjects.add((T) object);
          }
        }
      }
      // will be refilled once collection gets written
      ti.clear();

      // replace old instances with new ones
      HashMap<Object, T> map = new HashMap<Object, T>();
      for (T t: loadedObjects) {
        map.put(t.getPrimaryKey(), t);
      }
      for (T t : storableCollection) {
        map.put(t.getPrimaryKey(), t);
      }
      
      storableCollection = map.values();

      // store the whole collection in a new archive
      // update the TableIndex according in the process
      f = new File(getTablePath(tableName));
      if (f.exists() && !fileAccessTransactionSafe) {
        f.delete();
      }
      PersistenceObjectOutputStream poos = null;
      try {
        if(!fileAccessTransactionSafe) {
          f.createNewFile();
        }
        FileOutputStream fos = null;
        TransactionFile tf = null;
        if(fileAccessTransactionSafe) {
          tf = new TransactionFile(f);
          fos = new FileOutputStream(tf);
        } else {
          fos = new FileOutputStream(f);
        }
        poos = new PersistenceObjectOutputStream(fos);
        for (T t : storableCollection) {
          RowObject createdRow = poos.writeStorable(t, null);

          ti.add(createdRow);
        }
        poos.flush();
        ti.setStatus(Status.BLOB);
        writeIndex(ti, tableName, tf);
      } catch (FileNotFoundException e) {
        throw new XNWH_GeneralPersistenceLayerException("archive directory did not exist", e);
      } catch (IOException e) {
        throw new XNWH_GeneralPersistenceLayerException("archive could not be written", e);
      } finally {
        try {
          poos.close();
        } catch (IOException e) {
          throw new XNWH_GeneralPersistenceLayerException("could not close ObjectOutputStream for "+ tableName, e);
        }
      }

    }

    
    public <T extends Storable> Collection<T> loadCollection(Class<T> klass) throws PersistenceLayerException {
      logger.debug("loadCollection called");
      String tableName = getTableName(klass);
      Collection<T> ret = new ArrayList<T>();

      TableIndex ti = checkTable(tableName);

      if (ti.size() == 0) {
        return ret;
      }
      
      File f = new File(getTablePath(tableName));
      if (ti.getStatus().equals(Status.SINGLE)) {
        FileInputStream fis;
        try {
          fis = new FileInputStream(f);
        } catch (FileNotFoundException e) {
          throw new XNWH_GeneralPersistenceLayerException("archive directory did not exist", e);
        }
        try {
          for (RowObject row : ti) {         
            try {
              fis.getChannel().position(ti.calculateOffset(row));
              try { // has to be opened after positioning
                ObjectInputStream ois = klass.newInstance().getObjectInputStreamForStorable(fis);
                Object o = readObject(ois);
                ret.add((T) o);
              } catch (InstantiationException e) {
                throw new XNWH_GeneralPersistenceLayerException("Error while accessing storable",e);
              } catch (IllegalAccessException e) {
                throw new XNWH_GeneralPersistenceLayerException("Error while accessing storable",e);
              }
            } catch (FileNotFoundException e) {
              throw new XNWH_GeneralPersistenceLayerException("archive directory did not exist", e);
            } catch (ClassNotFoundException e) {
              throw new XNWH_GeneralPersistenceLayerException("storable class could not be found", e);
            } catch (IOException e) {
              throw new XNWH_GeneralPersistenceLayerException("archive could not be found", e);
            }
          }
        } finally {
          try {
            fis.close();
          } catch (IOException e) {
            throw new XNWH_GeneralPersistenceLayerException("could not close stream", e);
          }
        }
      } else {
        try {
          FileInputStream fis = new FileInputStream(f);
          ObjectInputStream ois = klass.newInstance().getObjectInputStreamForStorable(fis);
          try {
            for (RowObject rowObject : ti) {
              Object o = readObject(ois);
              ret.add((T) o);
            }
          } catch (ClassNotFoundException e) {
            throw new XNWH_GeneralPersistenceLayerException("storable class could not be found", e);
          } finally {
            // if 
            ois.close();
          }
        } catch (IOException e) {
          throw new XNWH_GeneralPersistenceLayerException("archive could not be found", e);
        } catch (InstantiationException e) {
          throw new XNWH_GeneralPersistenceLayerException("Error while accessing storable",e);
        } catch (IllegalAccessException e) {
          throw new XNWH_GeneralPersistenceLayerException("Error while accessing storable",e);
        }
        
        // Write Collection as singles to make them accessible
        ti.clear();
        if(fileAccessTransactionSafe) {
          f = new TransactionFile(f);
        }
        f.delete();        
        try {
          f.createNewFile();
        } catch (IOException e) {
          logger.error("Table file could not be created as " + f.getAbsolutePath(),e);
          throw new XNWH_GeneralPersistenceLayerException(e.getMessage(),e);
        }
        
        PersistenceObjectOutputStream poos = null;
        try {
          FileOutputStream fos = new FileOutputStream(f);
        
          for (T t : ret) {
            poos = new PersistenceObjectOutputStream(fos);

            RowObject createdRow = poos.writeStorable(t, null);

            ti.add(createdRow);
          }
          poos.flush();
        } catch (IOException e) {
          logger.error(e.getMessage(), e);
          throw new XNWH_GeneralPersistenceLayerException("could not generate new index", e);
        } finally {
          try {
            poos.close();
          } catch (IOException e) {
            throw new XNWH_GeneralPersistenceLayerException("could not close stream", e);
          }
        }
        ti.setStatus(Status.SINGLE);
        writeIndex(ti, tableName, (f instanceof TransactionFile ? (TransactionFile)f : null));
      }
        
      return ret;
    }


    private String getTableName(Class<?> klass) throws PersistenceLayerException {
      Persistable persi = AnnotationHelper.getPersistable(klass);
      if (persi == null) {
        throw new XNWH_MissingAnnotationsException(klass.getName());
      }
      return persi.tableName();
    }


    public <T extends Storable> void deleteAll(Class<T> klass) throws PersistenceLayerException {
      logger.debug("deleteAll called");
      String tableName = getTableName(klass);

      TableIndex ti = checkTable(tableName);
      ti.clear();

      File f = new File(getIndexPath(tableName));
      f.delete();
      f = new File(getTablePath(tableName));
      f.delete();
      f = new File(getTableDir(tableName));
    }


    public <T extends Storable> void delete(Collection<T> storableCollection) throws PersistenceLayerException {
      logger.debug("delete called");
      Class<? extends Storable> klass = storableCollection.iterator().next().getClass();
      String tableName = getTableName(klass);

      TableIndex ti = checkTable(tableName);
      
      if (ti.size() == storableCollection.size() && ti.containsAll(storableCollection)) {
        // fast way, every file on the disk should be deleted -> delete the file & clear the index and write it
        File f = new File(getTablePath(tableName));
        ti.clear();
        writeIndex(ti, tableName, null);
        f.delete();
      } else {
        Collection<? extends Storable> oldStorables = loadCollection(klass);
        Iterator<? extends Storable> it = oldStorables.iterator();
        while (it.hasNext()) {
          Storable s = it.next();
          Iterator<T> iteratorToDelete = storableCollection.iterator();
          while (iteratorToDelete.hasNext()) {
            T t = iteratorToDelete.next();
            if (s.getPrimaryKey().equals(t.getPrimaryKey())) {
              it.remove();
              break;
            }
          }
        }
        persistCollection(oldStorables, true);
      }
    }

    public <T extends Storable> void deleteOneRow(T toBeDeleted) throws PersistenceLayerException {
      // TODO performance
      Collection<T> toBeDeletedList = new ArrayList<T>();
      toBeDeletedList.add(toBeDeleted);
      delete(toBeDeletedList);
    }

    public void setTransactionProperty(TransactionProperty arg0) {
      //nicht unterst�tzt
    }

    public <T extends Storable> void ensurePersistenceLayerConnectivity(Class<T> arg0) throws PersistenceLayerException {
      // TODO check index to file?
    }

    public boolean isOpen() {
      return true;
    }

    public <T extends Storable> void removeTable(Class<T> klass, Properties props) throws PersistenceLayerException {
      knownTables.remove(getTableName(klass));
    }
    
    //unused helper functions
    /*public <T extends Storable> void convertBlobToSingle(TableIndex ti, Class<T> klass)
                    throws PersistenceLayerException {
      if (ti.getStatus() == Status.SINGLE) {
        return;
      }

      Collection<T> stored = null;
      try {
        stored = loadCollection(klass);
      } catch (PersistenceLayerException e) {
        e.printStackTrace();
      }

      // Write Collection as singles to make them accessible
      ti.clear();

      try {
        File f = new File(getTablePath(getTableName(klass)));
        f.delete();
        f.createNewFile();
        FileOutputStream fos = new FileOutputStream(f, true);
        PersistenceObjectOutputStream poos = null;
        for (T t : stored) {
          fos = new FileOutputStream(f, true);
          poos = new PersistenceObjectOutputStream(fos);

          RowObject createdRow = poos.writeStorable(t, null);

          ti.add(createdRow);
        }
        poos.flush();
        poos.close();
        ti.setStatus(Status.SINGLE);
        writeIndex(ti, getTableName(klass));
      } catch (IOException e) {
        throw new PersistenceLayerException("could not generate new index", e);
      } catch (PersistenceLayerException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
    }


    private <T extends Storable> void convertSingleToBlob(TableIndex ti, Class<T> klass) throws PersistenceLayerException {
      if (ti.getStatus() == Status.BLOB) {
        return;
      }
      
      Collection<T> stored = new ArrayList();
      File f = new File(getTablePath(getTableName(klass)));
      
      for (RowObject row : ti) {
        try {         
          FileInputStream fileStream = new FileInputStream(f);
          fileStream.getChannel().position(ti.calculateOffset(row));
          ObjectInputStream ois = new ObjectInputStream(fileStream);

          stored.add((T) ois.readObject());
        } catch (FileNotFoundException e) {
          throw new PersistenceLayerException("archive directory did not exist", e);
        } catch (IOException e) {
          throw new PersistenceLayerException("archive could not be found", e);
        } catch (ClassNotFoundException e) {
          throw new PersistenceLayerException("the deserialized object does not inherit from storable", e);
        }
      }
      
      ti.clear();
      f.delete();
      try {
        f.createNewFile();
      } catch (IOException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }
      persistCollection(stored);
    }*/
  }
  
  private static class JavaSerializationPreparedQuery<E> implements PreparedQuery<E> {

    private String table;
    private ResultSetReader<? extends E> reader;
    private Query<E> query;


    public JavaSerializationPreparedQuery(Query<E> q) {
      this.table = q.getTable();
      this.reader = q.getReader();
      this.query = q;
    }


    public ResultSetReader<? extends E> getReader() {
      return reader;
    }


    public String getTable() {
      return table;
    }


    public Query<E> getQuery() {
      return query;
    }

  }
  
  private static class UnsupportingResultSetForCount extends UnsupportingResultSet {

    private int cnt;
    
    public UnsupportingResultSetForCount(int cnt) {
      this.cnt = cnt;
    }

    @Override
    public int getInt(int columnIndex) throws SQLException {
      if (columnIndex != 1) {
        throw new SQLException("no data available in column with index " + columnIndex);
      }
      return cnt;
    }

  }


  public String getInformation() {
    return "Java Serialization ( transaction mode = " + (fileAccessTransactionSafe ? TRANSACTIONMODE : NOTRANSACTIONMODE) + ")";
  }


  public boolean describesSamePhysicalTables(PersistenceLayer pl) {
    return pl instanceof XynaJavaSerializationPersistenceLayer;
  }


  public void init(Long pliID, String... args) throws PersistenceLayerException {
    if(args.length > 0) {
      if(TRANSACTIONMODE.equals(args[0])) {
        fileAccessTransactionSafe = true; 
      } else if(NOTRANSACTIONMODE.equals(args[0])) {
        fileAccessTransactionSafe = false;
      }
    }
  }

  public String[] getParameterInformation() {
    return new String[] {"transaction mode: " + TRANSACTIONMODE + " (default), " + NOTRANSACTIONMODE};
  }

  public void shutdown() throws PersistenceLayerException {
  }

  public Reader getExtendedInformation(String[] arg0) {
    return null;
  }

  public PersistenceLayerConnection getConnection(PersistenceLayerConnection shareConnectionPool) throws PersistenceLayerException {
    return new SerializationPersistenceLayerConnection();
  }

  public boolean usesSameConnectionPool(PersistenceLayer plc) {
    if (plc instanceof XynaJavaSerializationPersistenceLayer) {
      return true;
    }
    return false;
  }
}
