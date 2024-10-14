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

package com.gip.xyna.xnwh.securestorage;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.apache.log4j.Logger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.FunctionGroup;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XynaFactoryPath;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.misc.Base64;
import com.gip.xyna.utils.misc.encryption.StreamDESEncrypter;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.XynaFactoryManagement;
import com.gip.xyna.xfmg.xods.XynaFactoryManagementODS;
import com.gip.xyna.xfmg.xods.configuration.Configuration;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xnwh.exceptions.XNWH_EncryptionException;
import com.gip.xyna.xnwh.persistence.ODS;
import com.gip.xyna.xnwh.persistence.ODSConnection;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xnwh.securestorage.SecuredStorable.DataType;
import com.gip.xyna.xprc.XynaProcessing;
import com.gip.xyna.xprc.xprcods.XynaProcessingODS;


public class SecureStorage extends FunctionGroup {
  private static final Logger logger = CentralFactoryLogging.getLogger(SecureStorage.class);
  
  protected static final String PROP_CACHE = "securestorage.cache"; //true/false

  static {
    addDependencies(SecureStorage.class, new ArrayList<XynaFactoryPath>(Arrays.asList(new XynaFactoryPath[] {
                    new XynaFactoryPath(XynaProcessing.class, XynaProcessingODS.class),
                    new XynaFactoryPath(XynaFactoryManagement.class, XynaFactoryManagementODS.class,
                                        Configuration.class)})));
  }
  

  final static String DEFAULT_NAME = "Secure Storage";
  private static final String SECURE_STORAGE = DEFAULT_NAME;
  
  private static final String SEED_FILE_KEY = "securestorage.seed";
  private static final String SEED_FILE_PROPERTY = "xnwh.securestorage.seedfile"; 
  private static final String SEED_FILE_DEFAULT = "/etc/opt/xyna/environment/black_edition_001.properties";
  private static String seed;
  
  private static volatile SecureStorage instance = null;
  private volatile boolean caching = false;
  
  private Map<String, SecuredStorable> storage;
  private final ReentrantReadWriteLock storageLock = new ReentrantReadWriteLock();
  
  private ODS ods;
  
  private SecureStorage() throws XynaException {
    super();
    init();    
  }
  

  public static SecureStorage getInstance() throws XynaException {
    if (instance == null) {
      synchronized (SecureStorage.class) {
        if (instance == null)
          instance = new SecureStorage();
      }
    }
    return instance;
  }

  private static String getSeed() {
    if (seed != null) {
      return seed;
    }
    //kann keine xynaproperty sein, weil sonst zyklische abhaengigkeit beim serverstart (db-pw in securestorage!)
    String file = System.getProperty(SEED_FILE_PROPERTY);
    if (file == null) {
      file = SEED_FILE_DEFAULT;
    }
    Properties p = new Properties();
    try (BufferedReader br = new BufferedReader(new FileReader(new File(file), Charset.forName(Constants.DEFAULT_ENCODING)))) {
      p.load(br);
    } catch (IOException e) {
      throw new RuntimeException("Secure Storage seed file configured in system property " + SEED_FILE_PROPERTY
          + " could not be read (default=" + SEED_FILE_DEFAULT + ").", e);
    }
    String s = p.getProperty(SEED_FILE_KEY);
    if (s == null) {
      throw new RuntimeException("Secure Storage seed file configured in system property " + SEED_FILE_PROPERTY + " does not contain a seed value (key=" + SEED_FILE_KEY + ").");
    }
    seed = s;
    return s;
  }
  
  static void setInstance(SecureStorage newInstance) {
    instance = newInstance;
  }
  
  
  @Override
  public String getDefaultName() {
    return DEFAULT_NAME;
  }

  
  @Override
  protected void init() throws XynaException {
    //load storage
    
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().addPropertyChangeListener(new IPropertyChangeListener() {
      public ArrayList<String> getWatchedProperties() {
        return new ArrayList<String>(Arrays.asList(new String[]{PROP_CACHE}));
      }


      public void propertyChanged() {
        String val = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().getProperty(PROP_CACHE);
        if (val == null || val.equalsIgnoreCase("true")) {
          caching = true;
          try {
            refreshCache();
          } catch (XynaException e) {
            throw new RuntimeException("caching could not be activated", e);
          }
        } else if (val.equalsIgnoreCase("false")) {
          caching = false;
          storageLock.writeLock().lock();
          try {
            storage = null;
          } finally {
            storageLock.writeLock().unlock();
          }
          logger.debug("caching deactivated");
        } else {
          throw new RuntimeException("invalid value für property " + PROP_CACHE);
        }
      }
    });
    
    String val = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().getProperty(PROP_CACHE);
    if (val != null && val.equalsIgnoreCase("false")) {
      caching = false;
    } else {
      caching = true;
    }
    
    
    ods = XynaFactory.getInstance().getProcessing().getXynaProcessingODS().getODS();
    ods.registerStorable(SecuredStorable.class);

    if (cacheData()) {
      refreshCache();
    } else {
      logger.debug("caching of secure storage not active");
    }
  }


  private void refreshCache() throws PersistenceLayerException {

    ODSConnection con = ods.openConnection();
    try {
      Collection<SecuredStorable> loadedStorables = con.loadCollection(SecuredStorable.class);
      storage = new HashMap<String, SecuredStorable>();
      storageLock.writeLock().lock();
      try {
        for (SecuredStorable storable : loadedStorables) {
          storage.put(storable.getId(), storable);
        }
      } finally {
        storageLock.writeLock().unlock();
      }
      con.commit();
    } finally {
      con.closeConnection();
    }
    logger.debug("caching of secure storage active");
  }
  
  private boolean cacheData() {
    return caching;
  }

  
  @Override
  protected void shutdown() throws XynaException {

  }
  
  /**
   * stores a serializable object as an DES encrypted byteArray, key should be unique for the destination
   * 
   * @param destination
   * @param key
   * @param serializable
   * @return
   * @throws PersistenceLayerException 
   */
  public boolean store(String destination, String key, Serializable serializable) throws PersistenceLayerException {
    Serializable oldVal = retrieve(destination, key);
    if (oldVal != null) {
      if (oldVal.equals(serializable)) {
        return true;
      }
    }
    String identifier = generateIdentifier(destination, key);
    
    DataType type = null;
    byte[] encryptedData = null;
    try {
      if (serializable instanceof String) {
        type = DataType.STRING;
        encryptedData = encryptString(identifier, (String) serializable);
      } else if (serializable instanceof Number) {
        encryptedData = encryptString(identifier, serializable.toString());
        if (serializable instanceof Integer) {
          type = DataType.INTEGER;
        } else if (serializable instanceof Long) {
          type = DataType.LONG;
        } else if (serializable instanceof Byte) {
          type = DataType.BYTE;
        } else if (serializable instanceof Short) {
          type = DataType.SHORT;
        } else if (serializable instanceof Double) {
          type = DataType.DOUBLE;
        } else if (serializable instanceof Float) {
          type = DataType.FLOAT;
        }
      } else {
        type = DataType.JAVAOBJECT;
        encryptedData = encryptJavaObject(identifier, serializable);
      }
    } catch (Exception e) {
      return false;
    }       
    
    if (type == null) {
      return false;
    }
    
    SecuredStorable storable = new SecuredStorable(identifier, encryptedData, type);
    if (cacheData()) {
      storageLock.writeLock().lock();
      try {
        storage.put(identifier, storable);
      } finally {
        storageLock.writeLock().unlock();
      }
    }
    ODSConnection con = ods.openConnection();
    try {
      con.persistObject(storable);
      con.commit();
    } finally {
      con.closeConnection();
    }

    return true;
  }


  /**
   * Retrieves (and decrypts) an object from a destination if an object with that key is present
   * @param destination
   * @param key
   * @return
   */
  public Serializable retrieve(String destination, String key) {   
    String identifier = generateIdentifier(destination, key);
    
    SecuredStorable storable = null;
    if (cacheData()) {
      storageLock.readLock().lock();
      try {
        if (!storage.containsKey(identifier)) {
          return null;
        }      
        storable = storage.get(identifier);
      } finally {
        storageLock.readLock().unlock();
      }
    } else {
      try {
        ODSConnection con = ods.openConnection();
        try {
          storable = new SecuredStorable(identifier);
          con.queryOneRow(storable);
        } finally {
          con.closeConnection();
        }
      } catch (XynaException e) {
        return null;
      }
    }
    
    if (storable == null) {
      return null;
    }

    try {
      if (storable.getDataTypeEnum() == DataType.JAVAOBJECT) {
        return decryptJavaObject(identifier, storable);
      } else if (storable.getDataTypeEnum() == DataType.STRING) {
        return decryptString(identifier, storable);
      } else {
        String number = decryptString(identifier, storable);
        try {
        if (storable.getDataTypeEnum() == DataType.INTEGER) {
          return Integer.valueOf(number);
        } else if (storable.getDataTypeEnum() == DataType.LONG) {
          return Long.valueOf(number);
        } else if (storable.getDataTypeEnum() == DataType.BYTE) {
          return Byte.valueOf(number);
        } else if (storable.getDataTypeEnum() == DataType.SHORT) {
          return Short.valueOf(number);
        } else if (storable.getDataTypeEnum() == DataType.FLOAT) {
          return Float.valueOf(number);
        } else if (storable.getDataTypeEnum() == DataType.DOUBLE) {
          return Double.valueOf(number);
        } else {
          throw new XynaException("unsupported DataType");
        }
        } catch (NumberFormatException e2) {
          throw new XynaException("found invalid value for number: " + number, e2);
        }
      }
    } catch (Exception e) {
      return null;
    }
  }


  /**
   * removes an object from the secureStorage
   * @param destination
   * @param key
   * @return
   * @throws PersistenceLayerException
   */
  public boolean remove(String destination, String key) throws PersistenceLayerException {
    String identifier = generateIdentifier(destination, key);
    
    if (cacheData()) {
      storageLock.readLock().lock();
      try {
        if (!storage.containsKey(identifier)) {
          return false;
        }
      } finally {
        storageLock.readLock().unlock();
      }

      storageLock.writeLock().lock();
      try {
        SecuredStorable storable = storage.remove(identifier);

        if (storable == null) {
          return false;
        }

        rewriteStorage(storable);
     
      } finally {
        storageLock.writeLock().unlock();
      }
    } else {
      ODSConnection con = ods.openConnection();
      try {
        con.delete(new ArrayList<SecuredStorable>(Arrays
                        .asList(new SecuredStorable[] {new SecuredStorable(identifier)})));
        con.commit();
      } finally {
        con.closeConnection();
      }
    }
    
    return true;
  }
  
  
  private static String generateIdentifier(String destination, String key) {
    return destination + "." + key;
  }
  
  
  private static String generateSecretKey(String identifier) {
    String seed = getSeed();
    String passphrase = seed + identifier + "\n";
    return (calcMD5(passphrase) + seed).substring(0, 8);
  }

  
  /**
   * berechnet md5 hash so wie er auch zb in der linux commandline berechnet wird (achtung,
   * string muss dann mit zeilenumbruch enden).
   * vergleichbare linuxtools:
   * md5sum oder md5sum.textutils
   * @param input
   * @return
   */
  private static String calcMD5(String input) {
    MessageDigest md;
    try {
      md = MessageDigest.getInstance("MD5");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException(e);
    }
    char[] charArray = input.toCharArray();

    byte[] byteArray = new byte[charArray.length];

    for (int i = 0; i < charArray.length; i++)
      byteArray[i] = (byte) charArray[i];

    byte[] md5Bytes = md.digest(byteArray);

    return byteArrayToHexString(md5Bytes);
  }
  
  public static String byteArrayToHexString(byte[] bytes) {
    StringBuffer hexValue = new StringBuffer();
    for (int i = 0; i < bytes.length; i++) {
      int val = ((int) bytes[i]) & 0xff;
      if (val < 16)
        hexValue.append("0");
      hexValue.append(Integer.toHexString(val));
    }

    return hexValue.toString();
  }
  
  public static byte[] hexStringToByteArray(String string) {
    char[] chars = string.toCharArray();
    byte[] bytes = new byte[chars.length / 2];
    for (int i = 0; i<chars.length; i = i+2) {
      String s = Character.toString(chars[i]) + Character.toString(chars[i+1]);
      int val = Integer.parseInt(s, 16);
      bytes[i/2] = (byte) val;
    }
    return bytes;
  }
  
  
  private static StreamDESEncrypter getDesStream(String identifier) throws XNWH_EncryptionException {  
    try {
      return new StreamDESEncrypter(StreamDESEncrypter.generateKey(generateSecretKey(identifier)));
    } catch (Exception e) {
      throw new XNWH_EncryptionException(e);
    }
  }
  
  private StreamDESEncrypter getDesStreamWithStoredPassPhrase(String identifier) throws XNWH_EncryptionException {  
    try {
      return new StreamDESEncrypter(StreamDESEncrypter.generateKey(getStoredPassPhrase(identifier)));
    } catch (Exception e) {
      throw new XNWH_EncryptionException(e);
    }
  }

  /**
   * @param identifier
   * @return
   * @throws PersistenceLayerException 
   */
  private String getStoredPassPhrase(String identifier) throws PersistenceLayerException {
    String passPhrase = null;
    while( passPhrase == null ) {
      Serializable ser = retrieve( SECURE_STORAGE, identifier );
      if( ser instanceof String ) {
        passPhrase = (String) ser;
      } else {
        logger.info("Generating passphrase for identifier \""+identifier+"\"");
        String pp = generateSecretKey(identifier);
        store(SECURE_STORAGE, identifier, pp);
      }
    }
    return passPhrase;
  }


  public static byte[] encryptString(String identifier, String s) throws XNWH_EncryptionException {
    // get a new stream with a identifier based secretKey
    StreamDESEncrypter sDESe= getDesStream(identifier);
    
    // generate an InputStream with the writen byteArray as underlying data
    ByteArrayInputStream bais;
    try {
      bais = new ByteArrayInputStream(s.getBytes("UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
    return encryptBytes( sDESe, bais );
  }
  
  /**
   * @param sDESe
   * @param bais
   * @return
   * @throws XNWH_EncryptionException 
   */
  private static byte[] encryptBytes(StreamDESEncrypter sDESe, ByteArrayInputStream bais) throws XNWH_EncryptionException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // encrypt that data by sending the undecrypted serialized data into the ByteArrayOutputStream via the generated StreamDESEncrypter
    try {
      sDESe.encrypt(bais, baos);
    } catch (Exception e) {
      throw new XNWH_EncryptionException(e);
    }
    
    if (logger.isTraceEnabled()) {
      logger.trace("Written " + baos.size() + "byte encrypted data to buffer");
    }
    
    //return the encrypted ByteArray
    return baos.toByteArray();
  }


  private static byte[] encryptJavaObject(String identifier, Serializable data) throws XNWH_EncryptionException {
    // get a new stream with a identifier based secretKey
    StreamDESEncrypter sDESe= getDesStream(identifier);

    // write the data into a ByteArrayOutputStream
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos;
    try {
      oos = new ObjectOutputStream(baos);
      oos.writeObject(data);
      oos.flush();
      oos.close();
    } catch (IOException e1) {
      throw new RuntimeException(e1); //da sollte nichts passieren können
    }
    
    if (logger.isTraceEnabled()) {
      logger.trace("Written " + baos.size() + "byte unencrypted data to buffer");
    }
    
    // encrypt that data by sending the undecrypted serialized data via the generated StreamDESEncrypter
    return encryptBytes( sDESe, new ByteArrayInputStream(baos.toByteArray() ) );
  }
  
  private static String decryptString(String identifier, SecuredStorable storable) throws XNWH_EncryptionException {
    // get a new stream with a identifier based secretKey
    StreamDESEncrypter sDESe= getDesStream(identifier);
    
    // create an ByteArrayInputStream for the encryptedData of the Storable and create an empty ByteArrayOutputStream
    ByteArrayInputStream bais = new ByteArrayInputStream(storable.getEncryptedData());
    if (logger.isTraceEnabled()) {
      logger.trace("Got " + bais.available() + "byte from storable");
    }
    byte[] data = decryptBytes( sDESe, bais );
    
    try {
      return new String(data, "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
  
  /**
   * @param sDESe
   * @param bais
   * @return
   * @throws XNWH_EncryptionException 
   */
  private static byte[] decryptBytes(StreamDESEncrypter sDESe, ByteArrayInputStream bais) throws XNWH_EncryptionException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    // decrypt the data into the empty ByteArrayOutputStream
    try {
      sDESe.decrypt(bais, baos);
    } catch (Exception e) {
      throw new XNWH_EncryptionException(e);
    }

    if (logger.isTraceEnabled()) {
      logger.trace("Written " + baos.size() + "byte unencrypted data to buffer");
    }
    return baos.toByteArray();
  }


  private Serializable decryptJavaObject(String identifier, SecuredStorable storable) throws XNWH_EncryptionException {
    //logger.debug("Starting Decryption for: "+ storable.getEncryptedData().toString());
    
    // get a new stream with a identifier based secretKey
    StreamDESEncrypter sDESe= getDesStream(identifier);
    
    // create an ByteArrayInputStream for the encryptedData of the Storable and create an empty ByteArrayOutputStream
    ByteArrayInputStream bais = new ByteArrayInputStream(storable.getEncryptedData());
    if (logger.isTraceEnabled()) {
      logger.trace("Got " + bais.available() + "byte from storable");
    }
    
    byte[] data = decryptBytes( sDESe, bais );
    
    // restore the Serializable from the decrypted Data
    bais = new ByteArrayInputStream(data);

    ObjectInputStream ois;
    try {
      ois = new ObjectInputStream(bais);
      //return the restored object
      return (Serializable) ois.readObject();
    } catch (IOException e) {
      throw new RuntimeException(e);
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
    
  }   
  
  private void rewriteStorage(SecuredStorable deletedStorable) throws PersistenceLayerException {
    ODSConnection con = ods.openConnection();
    try {
      Collection<SecuredStorable> toDelete = new ArrayList<SecuredStorable>();
      toDelete.add(deletedStorable);
      con.delete(toDelete);
      con.commit();
    } finally {
      con.closeConnection();
    }
  }

  /**
   * Verschlüsselt den übergebenen String mittels DES und Base64.
   * Identifier dient zum Suchen der PassPhrase im SecureStorage.
   * @param identifier
   * @param string
   * @return
   * @throws XNWH_EncryptionException
   */
  public String encrypt(String identifier, String string) throws XNWH_EncryptionException {
    // get a new stream with a identifier based secretKey
    return encrypt(getDesStreamWithStoredPassPhrase(identifier), string);
  }

  /**
   * Verschlüsselt den übergebenen String mittels DES und Base64.
   * Identifier dient zum Erzeugen der PassPhrase.
   * @param identifier
   * @param string
   * @return
   * @throws XNWH_EncryptionException
   */
  public static String staticEncrypt(String identifier, String string) throws XNWH_EncryptionException {
    // get a new stream with a identifier based secretKey
    return encrypt(getDesStream(identifier), string);
  }
  
  private static String encrypt(StreamDESEncrypter sDESe, String string) throws XNWH_EncryptionException {
    // generate an InputStream with the writen byteArray as underlying data
    ByteArrayInputStream bais;
    try {
      bais = new ByteArrayInputStream(string.getBytes( Constants.DEFAULT_ENCODING ));
    } catch (UnsupportedEncodingException e) {
      throw new XNWH_EncryptionException(e);
    }
    byte[] data = encryptBytes( sDESe, bais );
    
    return Base64.encode(data);
  }

  
  /**
   * Entschlüsselt den übergebenen String mittels Base64 und DES.
   * Identifier dient zum Suchen der PassPhrase im SecureStorage.
   * @param identifier
   * @param string
   * @return
   * @throws XNWH_EncryptionException
   */
  public String decrypt(String identifier, String string) throws XNWH_EncryptionException {
    // get a new stream with a identifier based secretKey
    return decrypt(getDesStreamWithStoredPassPhrase(identifier), string);
  }
 
  /**
   * Entschlüsselt den übergebenen String mittels Base64 und DES.
   * Identifier dient zum Erzeugen der PassPhrase.
   * @param identifier
   * @param string
   * @return
   * @throws XNWH_EncryptionException
   */
  public static String staticDecrypt(String identifier, String string) throws XNWH_EncryptionException {
    // get a new stream with a identifier based secretKey
    return decrypt(getDesStream(identifier), string);
  }

  private static String decrypt(StreamDESEncrypter sDESe, String string) throws XNWH_EncryptionException {
    ByteArrayInputStream bais;
    try {
      bais = new ByteArrayInputStream( Base64.decode(string) );
    } catch (IOException e) {
      throw new XNWH_EncryptionException(e);
    }
    
    byte[] bytes = decryptBytes( sDESe, bais );
    try {
      return new String( bytes, Constants.DEFAULT_ENCODING );
    } catch (UnsupportedEncodingException e) {
      throw new XNWH_EncryptionException(e);
    }
  }


  /**
   * Creates a random String of given length. If length is negative, the empty String is returned.
   * @param length
   * @return
   */
  public static String createPadding(int length) {
    if (length <= 0) {
      return "";
    }

    int leftLimit = 21; // '!'
    int rightLimit = 126; // '~'
    Random random = new Random();

    String result = random.ints(leftLimit, rightLimit + 1).limit(length)
        .filter(i -> i != 94 && i != 96) // 94 = '^', 96 = '`'
        .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();

    return result;
  }

}
