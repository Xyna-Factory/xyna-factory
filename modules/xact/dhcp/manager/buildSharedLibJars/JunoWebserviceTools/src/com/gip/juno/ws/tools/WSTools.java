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

package com.gip.juno.ws.tools;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.Properties;
import java.util.TreeMap;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


import com.gip.juno.ws.enums.FailoverFlag;
import com.gip.juno.ws.exceptions.*;
import com.gip.juno.cfggen.util.PallasPasswordUtil;

import dhcpdConf.IPAddress;


/**
 * class for unspecific tools
 */
public class WSTools {

  private static String xsdPath=null;
  private static Long xsdPathLock=new Long(17121966);;

  public static String getColValue(java.sql.ResultSet rs, String col) throws SQLException {
    String ret = rs.getString(rs.findColumn(col));
    if (ret == null) { return ""; }
    return ret;
  }

  public static FailoverFlag translateFailoverFlag(String val) {
    if (val.equals("primary")) {
      return FailoverFlag.primary;
    } else if (val.equals("secondary")) {
      return FailoverFlag.secondary;
    }
    return FailoverFlag.primary;
  }

  public static boolean hasWildcard(String val) {
    //if ((val.matches("[.\\s]*\\*[.\\s]*"))) {
    if (val.indexOf("*") >=0) {
      return true;
    }
    return false;
  }

  public static String adjustWildcard(String val) {
    String ret = val.replace("*", "%");
    return ret;
  }


  /**
   * Returns map which is build by merging map1 and map2;
   * if keys appear in both maps, the value of map2 is used
   */
  public static TreeMap<String, String> mergeMaps(TreeMap<String, String> map1, TreeMap<String, String> map2) {
    TreeMap<String, String> ret = new TreeMap<String, String>();
    ret.putAll(map1);
    ret.putAll(map2);
    return ret;
  }


  /**
   * Parsen einer IP-Liste
   * @param string
   * @return
   */
  public static String parseIpList(String string) {
    if( string == null || string.trim().length() == 0 ) {
      return null;
    }
    StringBuilder sb = new StringBuilder();
    for( String s : string.split(",") ) {
      IPAddress ip = IPAddress.parse(s.trim());
      sb.append(ip).append(",");
    }
    if( sb.length() > 0 ) {
      return sb.substring(0,sb.length()-1);
    } else {
      return null;
    }
  }

  private static String getEncryptionKey(Logger logger) throws RemoteException {
    Properties prop = PropertiesHandler.getWsProperties();
    String ret = prop.getProperty("encryption.key");
    if (ret == null) {
      logger.error("Property encryption.key could not be read from file.");
      throw new DPPWebserviceUnexpectedException("Property encryption.key could not be read from file.");
    }
    return ret;
  }

  public static String encrypt(String value, Logger logger) throws RemoteException {
    String key = getEncryptionKey(logger);
    try {
      String ret = PallasPasswordUtil.encrypt(key, value);
      return ret;
    } catch (Exception e) {
      logger.error("Encryption failed.", e);
      throw new DPPWebserviceUnexpectedException("Encryption failed.", e);
    }
  }

  public static String decrypt(String value, Logger logger) throws RemoteException {
    String key = getEncryptionKey(logger);
    try {
      String ret = PallasPasswordUtil.decrypt(key, value);
      return ret;
    } catch (Exception e) {
      logger.error("Decryption failed.", e);
      throw new DPPWebserviceUnexpectedException("Decryption failed.", e);
    }
  }

  public static void HandleIntegrityConstraintViolation(Logger logger,
        com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) throws RemoteException {
    String val = parseIntegrityException(logger, e);
    if (val.trim().equals("")) {
      MessageBuilder builder = new MessageBuilder();
      builder.setDescription("Duplicate Entry.");
      builder.setErrorNumber("00002");
      builder.setCause(e.getMessage().replaceFirst("'.*?'", "'*'"));
      throw new DPPWebserviceDatabaseException(builder);
    }
    if (!val.trim().matches("\\w*")) {
      MessageBuilder builder = new MessageBuilder();
      builder.setDescription("Duplicate Entry.");
      builder.setErrorNumber("00002");
      builder.setCause(e.getMessage().replaceFirst("'.*?'", "'*'"));
      throw new DPPWebserviceDatabaseException(builder);
    }
    MessageBuilder builder = new MessageBuilder();
    builder.setErrorNumber("00002");
    builder.addParameter(val);
    DPPWebserviceDatabaseException exception = new DPPWebserviceDatabaseException(builder, e);
    throw exception;
  }

  private static String parseIntegrityException(Logger logger,
        com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException e) throws RemoteException {
    try {
      String text = e.getMessage();
      logger.info("Integrity exception message: " + text);
      text = text.substring(text.indexOf("Duplicate entry '"));
      text = text.substring(text.indexOf("'") + 1);
      text = text.substring(0, text.indexOf("'"));
      if (text == null) {
        throw e;
      }
      logger.info("Integrity exception parameter: " + text);
      return text;
    } catch (Exception exception) {
      throw new DPPWebserviceDatabaseException(e);
    }
  }


  /**
   * validiert das xml gegen das xsd "../conf/"+xsdPrefix.toLowerCase()+"schema.xsd"
   * @param xsdPrefix
   * @param xml
   * @throws DPPWebserviceException
   */
  public static void validate(String xsdPrefix, String xml, Logger logger) throws DPPWebserviceException{

    try {
      if (xml==null || xml.length()==0){
        logger.debug("not validating empty xml");
        return; //Funktion wird evtl. hier verlassen !!!
      }
      DocumentBuilder documentBuilder =DocumentBuilderFactory.newInstance().newDocumentBuilder();
      Document document = documentBuilder.parse(new InputSource(new StringReader(xml)));
      SchemaFactory schemaFactory =SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
      StreamSource schemaStreamSource = new StreamSource(new File(getXsdPath()+xsdPrefix.toLowerCase()+"schema.xsd"));
      Schema schema = schemaFactory.newSchema(schemaStreamSource);
      Validator validator = schema.newValidator();
      validator.validate(new DOMSource(document));

    } catch (SAXException e) {
      logger.info("xml is not valid: "+e.getMessage(),e);
      throw new DPPWebserviceException("xml is not valid: "+e.getMessage(),e);
    }
    catch (ParserConfigurationException e) {
      logger.info("Error validating xml: "+e.getMessage(),e);
      throw new DPPWebserviceException(e);
    }  catch (IOException e) {
      logger.info("Error validating xml: "+e.getMessage(),e);
      throw new DPPWebserviceException(e);
    }
  }


  public static File getXsdFile(String xsdPrefix) throws DPPWebserviceException {
    return new File(getXsdPath()+xsdPrefix.toLowerCase()+"schema.xsd");
  }

  private static String getXsdPath() throws DPPWebserviceException {
    synchronized(xsdPathLock){
      if(xsdPath==null){
        try {
          xsdPath=PropertiesHandler.getWsProperties().getProperty("xsd.path");
        }
        catch (RemoteException e) {
          throw new DPPWebserviceException(e);
        }
        if (!xsdPath.endsWith("/")){
          xsdPath=xsdPath+"/";
        }
      }
      return xsdPath;
    }
  }

}
