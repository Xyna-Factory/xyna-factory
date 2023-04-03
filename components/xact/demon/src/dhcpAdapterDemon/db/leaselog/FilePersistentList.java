/*
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 GIP SmartMercial GmbH, Germany
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
package dhcpAdapterDemon.db.leaselog;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.AbstractList;
import java.util.ArrayList;

import org.apache.log4j.Logger;

/**
 * FilePersistentList ist eine Liste, die persistent im Dateisystem gespeichert wird.
 * Nach jedem Aufruf {@link #add(E entry)} wird über die {@link #StringPersistableFactory} 
 * der Entry in einen String verwandelt und in die Datei geschrieben. Nach jedem 
 * Schreibvorgang wird ein fsync audgeführt, damit die Datei dauerhaft und konsistent 
 * gespeichert ist. 
 *
 * @param <E>
 */
public class FilePersistentList<E> extends AbstractList<E> {
  
  /**
   * Factory, die die Entries in Strings verwandelt bzw. aus den Strings wiederherstellt.
   *
   * @param <E>
   */
  public static interface StringPersistableFactory<E> {
    /**
     * Ausgabe als String
     * @param entry
     * @return
     */
    public String asString(E entry);
    /**
     * Konstruktion aus dem übergeben String
     * @param string
     * @return 
     * @throws IllegalArgumentException, falls der String nicht geparst werden kann
     */
    public E fromString(String string);
  }
  
  static Logger logger = Logger.getLogger(FilePersistentList.class.getName());

  private ArrayList<E> data;
  private FileOutputStream fos;
  private OutputStreamWriter osw;
  private File file;
  private StringPersistableFactory<E> factory;

  private Status status;
  
  public static enum Status {
    NEW,
    APPEND,
    OVERWRITE,
    READONLY,
    DELETED;
  }
  
  public FilePersistentList(File file, StringPersistableFactory<E> factory) throws IOException {
    this(file, 10, factory, Status.NEW);
  }
  
  public FilePersistentList(File file, StringPersistableFactory<E> factory, Status status) throws IOException {
    this(file, 10, factory, status);
  }
 
  public FilePersistentList(File file, int capacity, StringPersistableFactory<E> factory, Status status) throws IOException {
    this.file = file;
    this.factory = factory;
    this.status = status;
    data = new ArrayList<E>(capacity);
    
    //Prüfen des Status
    if( status == Status.DELETED ) {
      throw new IllegalArgumentException("state must not be DELETED");
    }
    if( file.canRead() ) {
      if( status == Status.NEW ) {
        throw new IOException("File \""+file+"\" already exists");
      }
    } else {
      if( status != Status.NEW ) {
        throw new IOException("File \""+file+"\" does not exist");
      }
    }
    
    //evtl. bisherigen Inhalt lesen
    if( status == Status.APPEND || status == Status.READONLY ) {
      readFromFile();
    }
    
    if( status == Status.READONLY ) {
      //File nicht mehr zum Schreiben öffnen
      return;
    }
     
    //File zum Schreiben öffnen
    if( status == Status.NEW || status == Status.OVERWRITE ) {
      fos = new FileOutputStream(file);
      osw = new OutputStreamWriter(fos);
    }
    if( status == Status.APPEND ) {
      fos = new FileOutputStream(file,true);
      osw = new OutputStreamWriter(fos);
    }
  }
  
  /**
   * Lesen der bereits eingetragenen Date
   * @throws IOException
   */
  private void readFromFile() throws IOException {
    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader( file);
      br = new BufferedReader(fr);
      String line; 
      while( (line = br.readLine() ) != null ) {
        E entry = factory.fromString(line);
        data.add( entry );
      }
    }
    finally {
      if( fr != null ) { 
        try {fr.close(); } catch( IOException e ) { logger.error(e); }
      }
      if( br != null ) { 
        try {br.close(); } catch( IOException e ) { logger.error(e); }
      }
    }
  }

  @Override
  public E get(int index) {
    return data.get(index);
  }

  @Override
  public int size() {
    return data.size();
  }

  /**
   * Hinzufügen eines Objektes, kann scheitern
   * @return true, wenn Objekt eingetragen wurde
   * @see java.util.AbstractList#add(java.lang.Object)
   */
  @Override
  public boolean add(E entry) {
    if( isWriteable() ) {
      data.add(entry);
      try {
        osw.append( factory.asString(entry)).append('\n');
        osw.flush();
        fos.getFD().sync();
      } catch (IOException e) {
        logger.error( e );
        return false;
      }
      return true;
    }
    return false;
  }
  
  /**
   * Sync der Festplatte.
   * @throws IOException
   */
  public void sync() throws IOException {
    if( isWriteable() ) {
      osw.flush();
      fos.getFD().sync();
    }
  }
  
  /**
   * Schließen der Datei, Übergang in Status READONLY 
   * @throws IOException
   */
  public void close() throws IOException {
    if( isWriteable() ) {
      sync();
      osw.close();
      fos.close();
      osw = null;
      fos = null;
      status = Status.READONLY;
    }
  }
  
  private boolean isWriteable() {
    if( status == Status.READONLY || status == Status.DELETED ) {
      return false;
    }
    return true;
  }

  /**
   * Löscht die zugrundeliegende Datei, Übergang ins Status DELETED
   * Dazu muss sich FilePersistentList im Status READONLY befinden
   * Achtung: Kann nicht rückgängig gemacht werden!
   * @return true, wenn Datei gelöscht wurde
   * @throws IOException 
   */
  public boolean delete() throws IOException {
    if( status != Status.READONLY ) {
     return false;
    }
    if( file.delete() ) {
      data.clear();
      status = Status.DELETED;
      return true;
    } else {
      throw new IOException( "File \""+file+"\" could not be deleted");
    }
  }

  /**
   * @return Status
   */
  public Status getStatus() {
    return status;
  }
  
  @Override
  public String toString() {
    return "FilePersistentList("+file.getName()+","+size()+" entries)";
  }
  
} 