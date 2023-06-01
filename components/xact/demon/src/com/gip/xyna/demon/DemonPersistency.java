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
package com.gip.xyna.demon;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import com.gip.xyna.demon.persistency.Persistable;

public class DemonPersistency {

  private File file;
  private Map<String,String> map = new TreeMap<String,String>();
  private boolean changed = false;
  private Map<String, Persistable> persistables = new HashMap<String, Persistable>();
  
  static Logger logger = Logger.getLogger(DemonPersistency.class.getName());
  
  private static DemonPersistency INSTANCE = null;
  private DemonPersistency(String filename) {
    file = new File( filename );
    if( file.exists() ) {
      if( ! file.canRead() || ! file.canWrite() ) {
        throw new IllegalArgumentException( "DemonPersistency File \""+filename+"\" is not readable or writeable");
      }
    } else {
      logger.warn( "DemonPersistency File \""+filename+"\" does not exist" );
      //kein Fehler, da beim ersten Aufruf erlaubt
    }
  }
  public static DemonPersistency getInstance() {
    return INSTANCE;
  }
  public static void createInstance(String filename ) {
    DemonPersistency dp = new DemonPersistency(filename);
    dp.open();
    INSTANCE = dp;
  }
 
  

  public static class DemonPersistencyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public static enum Type {
      ALREADY_EXISTS,
      DOES_NOT_EXIST,
      FILE
    }

    private Type type;
    
    public DemonPersistencyException(Type type) {
      this.type = type;
    }

    public Type getType() {
      return type;
    }
    
  }
  
  private void checkKey(String key) {
    if( key == null ) {
      throw new IllegalArgumentException( "key is not allowed to be null");
    }
  }
   
  public synchronized void commit() throws DemonPersistencyException {
    for( Persistable p : persistables.values() ) {
      String key = p.getUniqueName();
      String value = p.getPersistentValue();
      if( value != null ) {
        String old = map.put( key, value );
        if(! value.equals(old) ) {
          changed = true;
        }
      }
    }
    
    if( !changed ) {
      //keine Änderungen, daher ist nichts zu tun
      return;
    }
    FileWriter fw  = null;
    try {
      fw = new FileWriter(file);
      for( Map.Entry<String,String> e : map.entrySet() ) {
        fw.write( e.getKey()+" = "+e.getValue()+"\n");
      }
      fw.close();
      changed = false; //File ist auf aktuellem Stand
    }
    catch (IOException e) {
      logger.error( "Error while writing file " + file, e );
    }
    finally {
      try {
        if( fw != null ) fw.close();
      }
      catch (Exception e) {
        logger.error( e ); //ignore
      }
    }
  }
 
  public synchronized void open() throws DemonPersistencyException{
    FileReader fr = null;
    BufferedReader br = null;
    try {
      fr = new FileReader(file);
      br = new BufferedReader(fr);
      String line = null;
      do {
        line = br.readLine();
        if( line == null ) break;
        int pos = line.indexOf("=");
        if( pos < 0 ) {
          logger.warn( "Unparseable row in persistency file \""+file.getName()+"\": \""+line+"\"");
          continue;
        }
        String key = line.substring(0,pos).trim();
        String value = line.substring(pos+1).trim();
        logger.trace("#"+key+"#"+value+"#");
        map.put(key,value);
      } while( true ); 
    }
    catch (IOException e) {
      if( e instanceof FileNotFoundException ) {
        //Default bei erstem Start FIXME sinnvoll?
      } else {
        logger.error( "Error while reading file " + file, e );
      }
    }
    finally {
      try {
        if( fr != null ) fr.close();
        if( br != null ) br.close();
      }
      catch (Exception e) {
        logger.error( e ); //ignore
      }
    }
  }

  /**
   * Registrieren des {@link Persistable}, der bereits der 
   * {@link DemonPersistency} bekannte Wert wird sofort im Persistable eingetragen.
   * @param persistable
   */
  public void registerPersistable(Persistable persistable) {
    String key = persistable.getUniqueName();
    checkKey(key);
    Persistable old = persistables.put( key, persistable );
    if( old != null ) {
      persistables.put( key, old ); //rollback
      throw new IllegalArgumentException( "Persistable is already registered");
    }
    persistable.setPersistentValue( map.get( key ) );
  }
}
