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
package xact.snmp.commands;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.snmp4j.smi.OctetString;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.xfmg.xfctrl.netconfmgmt.InternetAddressBean;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Application;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RevisionManagement;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.RuntimeContext;
import com.gip.xyna.xfmg.xfctrl.revisionmgmt.Workspace;
import com.gip.xyna.xfmg.xods.configuration.DocumentationLanguage;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyEnum;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xnwh.exceptions.XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;


/**
 * Klasse übernimmt die Generierung der EngineId und die Verwaltung der dazu nötigen XynaProperties.
 *
 * Die EngineId kann zwischen 5 und 32 Bytes lang sein und wird wie folgt generiert (siehe auch RFC 3411 Abschnitt 5 Punkt 3 (http://tools.ietf.org/html/rfc3411)):<br>
 * Byte 1-4: enterpriseId der GIP (wobei das aller erste Bit auf 1 gesetzt wird)<br>
 * Byte 5: gibt das Format der nachfolgenden Bytes an, dies ist von der engineIdVersion abhängig:<br>
 *  - set_globally: 5<br>
 *  - set_per_application: 128<br>
 *  - generate_per_revision: 129<br>
 * Byte 6-(max)32: sind abhängig von der engineIdVersion<br>
 *  - set_globally: Wert der Property xact.snmp.service.engineId.constant<br>
 *  - set_per_application: <br>
 *    4-16 Bytes für die IP-Adresse der IP aus der Property xact.snmp.service.engineId.ip bzw. aus lokaler IP generiert, falls Property nicht gesetzt<br>
 *    5 Bytes für die Buchstaben ":serv"<br>
 *    2 Bytes mit dem Wert der Property xact.snmp.service.engineId.application.<applicationName> bzw. xact.snmp.service.engineId.workspace.<workspaceName><br> 
 *  - generate_per_revision:<br>
 *    4-16 Bytes für die IP-Adresse der IP aus der Property xact.snmp.service.engineId.ip bzw. aus lokaler IP generiert, falls Property nicht gesetzt<br>
 *    5 Bytes für die Buchstaben ":serv"<br>
 *    2 Bytes für die revision
 *
 */
public class EngineIdGeneration implements IPropertyChangeListener{
  
  private static final Logger logger = CentralFactoryLogging.getLogger(EngineIdGeneration.class);
  
  private long revision;
  private String user;
  private OctetString currentEngineId = new OctetString();
  
  private XynaPropertyEnum<EngineIdVersion> version;
  private XynaPropertyBuilds<OctetString> constant;
  private XynaPropertyString ip;
  private XynaPropertyBuilds<OctetString> application;
  private XynaPropertyBuilds<OctetString> currentValue;
  
  private static final int ENTERPRISE_ID = 28747;
  
  public EngineIdGeneration(long revision, String user) throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY, PersistenceLayerException {
    this.revision = revision;
    this.user = user;
    registerProperties();
    generateEngineId();
  }

  private static enum EngineIdVersion {
    set_globally((byte)5),
    set_per_application((byte)128),
    generate_per_revision((byte)129);
    
    private byte engineIdPart;
    
    private EngineIdVersion(byte engineIdPart) {
      this.engineIdPart = engineIdPart;
    }

    public static String documentation(DocumentationLanguage lang) {
      switch( lang ) {
        case DE:
          return "'generate_per_revision': Für jede Revision wird eine eigene EngineId generiert; "
          +"'set_per_application': Die EngineId wird pro Application bzw. Workspace erzeugt. Dazu wird der Wert der XynaProperty 'xact.snmp.service.engineid.application.<applicationName>' bzw. 'xact.snmp.service.engineid.workspace.<workspaceName>' in die EngineId einbezogen; "
          +"'set_globally': Die letzten (max. 27) Bytes der EngineId werden auf den Wert der XynaProperty 'xact.snmp.service.engineid.constant' gesetzt";
        case EN:
        default:
          return "'generate_per_revision': Generates an unique engine id for each revision; "
          +"'set_per_application': The engine id is created per application or workspace by including the XynaProperty 'xact.snmp.service.engineid.application.<applicationName>' or 'xact.snmp.service.engineid.workspace.<workspaceName>'; "
          +"'set_globally': The last bytes of the engine id will be set to the value of the XynaProperty 'xact.snmp.service.engineid.constant'";
      }
    }
  }
  
  private static class OctetStringBuilder implements XynaPropertyBuilds.Builder<OctetString> {

    private int minLength;
    private int maxLength;
    
    private OctetStringBuilder(int length) {
      this(length, length);
    }
    
    private OctetStringBuilder(int minLength, int maxLength) {
      super();
      this.minLength = minLength;
      this.maxLength = maxLength;
    }

    public OctetString fromString(String string)
                    throws com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyBuilds.Builder.ParsingException {
      OctetString os = OctetString.fromHexString(string);
      
      if (os.length() != 0 && (os.length() < minLength || os.length() > maxLength)) {
        String length;
        if (minLength == maxLength) {
          length = String.valueOf(minLength);
        } else if (minLength == 0) {
          length = "max. " + maxLength;
        } else {
          length = minLength + " - " + maxLength;
        }
        throw new IllegalArgumentException("\"" + string + "\" must be " + length + " bytes in hexadecimal representation.");
      }
      return os;
    }

    public String toString(OctetString value) {
      return value.toHexString();
    }
    
  }
  
  private void registerProperties() throws XNWH_OBJECT_NOT_FOUND_FOR_PRIMARY_KEY {
    registerVersion();
    registerConstant();
    registerIp();
    
    RuntimeContext runtimeContext = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getRevisionManagement().getRuntimeContext(revision);
    registerApplication(runtimeContext);
    if (revision == RevisionManagement.REVISION_DEFAULT_WORKSPACE) {
      //wegen Abwärtskompatibilität workingset für default workspace verwenden
      registerCurrentValue("workingset", null);
    } else {
      String versionName = (runtimeContext instanceof Application) ? ((Application) runtimeContext).getVersionName() : null;
      registerCurrentValue(runtimeContext.getName(), versionName);
    }
    
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().addPropertyChangeListener(this);
  }

  
  public void unregisterProperties() {
    version.unregister();
    constant.unregister();
    ip.unregister();
    application.unregister();
    currentValue.unregister();
    
    XynaFactory.getInstance().getFactoryManagement().getXynaFactoryManagementODS().getConfiguration().removePropertyChangeListener(this);
  }
  
  public byte[] getEngineId() {
    synchronized (currentEngineId) {
      return currentEngineId.getValue();
    }
  }
  
  
  /**
   * Generiert die engineId.
   * @throws PersistenceLayerException
   */
  private void generateEngineId() throws PersistenceLayerException {
    synchronized (currentEngineId) {
      currentEngineId = new OctetString(getEnterpriseId()); //enterpriseId (Byte 1-4)
      switch (version.get()) {
        case set_globally:
          if (constant.get().length() > 0) {
            currentEngineId.append(EngineIdVersion.set_globally.engineIdPart); //Byte 5 = 5
            currentEngineId.append(constant.get()); //restliche Bytes aus xact.snmp.service.engineId.constant
            break;
          } else {
            logger.warn("xynaProperty " + constant.getPropertyName() + " not (correctly) set -> engineId will be generated");
          }
        case set_per_application:
          XynaPropertyBuilds<OctetString> applicationProp = application;
          if (application.get().length() == 0 && revision == RevisionManagement.REVISION_DEFAULT_WORKSPACE) {
            //frühere Version der Property versuchen
            applicationProp = new XynaPropertyBuilds<OctetString>("xact.snmp.service.engineid.application.workingset", new OctetStringBuilder(2), new OctetString());
          }
          if (applicationProp.get().length() > 0) {
            currentEngineId.append(EngineIdVersion.set_per_application.engineIdPart); //Byte 5 = 128
            //restliche Bytes mit IP Adress und Application generieren
            byte[] addressPart = getIp();
            byte[] applicationPart = applicationProp.get().getValue();
            currentEngineId.append(getLocalEngineId(addressPart, applicationPart));
            break;
          } else {
            logger.warn("xynaProperty " + application.getPropertyName() + " not (correctly) set -> engineId will be generated per revision");
        }
        case generate_per_revision:
          currentEngineId.append(EngineIdVersion.generate_per_revision.engineIdPart); //Byte 5 = 129
          //restliche Bytes mit IP Adress und Revision generieren
          byte[] addressPart = getIp();
          byte[] applicationPart = getIntAsByteArray((int) revision);
          currentEngineId.append(getLocalEngineId(addressPart, applicationPart));
      }
      
      //engineId in XynaProperty eintragen
      currentValue.set(currentEngineId);
    }
  }
  
  
  private static byte[] getEnterpriseId() {
    byte[] enterpriseId = new byte[4];
    enterpriseId[0] = (byte)(0x80 | ((ENTERPRISE_ID >> 24) & 0xFF)); //das allererste Bit wird auf 1 gesetzt
    enterpriseId[1] = (byte)((ENTERPRISE_ID >> 16) & 0xFF);
    enterpriseId[2] = (byte)((ENTERPRISE_ID >> 8) & 0xFF);
    enterpriseId[3] = (byte)(ENTERPRISE_ID & 0xFF);
    return enterpriseId;
  }
  
  
  private byte[] getIp() {
    if (ip.get() != null && ip.get().length() > 0) {
      //für den ipName aus der XynaProperty 'xact.snmp.service.engineid.ip', die zugehörige IP-Adresse suchen
      InternetAddressBean iab = XynaFactory.getInstance().getFactoryManagement().getXynaFactoryControl().getNetworkConfigurationManagement()
                                .getInternetAddress(ip.get(), null);
      if (iab != null) {
        return iab.getInetAddress().getAddress();
      } else {
        logger.warn("Could not find ip adress for " + ip.get() + " -> get local ip for engine id");
      }
    }
    
    //Die XynaProperty ist nicht gesetzt oder die IP ist nicht konfiguriert,
    //also die lokale Addresse bestimmen
    try {
      return InetAddress.getLocalHost().getAddress();
    } catch (UnknownHostException e) {
      logger.warn("could not get local ip. engine id from snmp service will be based on bogus ip 1.2.3.4", e);
      return new byte[]{1, 2, 3, 4};
    }
  }
  

  private OctetString getLocalEngineId(byte[] address, byte[] application) {
    OctetString os = new OctetString(address); //address
    os.append(":".getBytes());
    os.append("serv".getBytes()); //service

    os.append(application); //application
    
    //darf maximal 27 bytes lang sein. 4-16 für address, 5 service-id, revision 2, macht zusammen 11 - 23
    //für zukünftige änderungen auf nummer sicher gehen:
    if (os.length() > 27) {
      byte[] b = os.getValue();
      byte[] b2 = new byte[27];
      System.arraycopy(b, 0, b2, 0, 27);
      os = new OctetString(b2);
    }
    return os;
    //FIXME xynainstanz id, damit eindeutigkeit auch bei mehreren instanzen gewährleistet werden kann?
  }
  
  private static byte[] getIntAsByteArray(int i) {
    return new byte[] {(byte) (i / 256 - 128), (byte) (i % 256 - 128)};
  }
  
  private void registerVersion() {
    version = XynaPropertyEnum.construct("xact.snmp.service.engineid.version",
                                             EngineIdVersion.generate_per_revision).
                    setDefaultDocumentation(DocumentationLanguage.EN, 
                                            "Version of engine id generation of the last (max 27) bytes (the first five bytes are constant): "
                                                +EngineIdVersion.documentation(DocumentationLanguage.EN)).
                    setDefaultDocumentation(DocumentationLanguage.DE,
                                            "Version der EngineId-Generierung der letzten (max. 27) Bytes (die ersten fünf Bytes sind konstant): "
                                                +EngineIdVersion.documentation(DocumentationLanguage.DE)); 
    version.registerDependency(user);
  }
  
  
  private void registerConstant() {
    constant = new XynaPropertyBuilds<OctetString>("xact.snmp.service.engineid.constant", new OctetStringBuilder(0, 27), new OctetString()).
                    setDefaultDocumentation(DocumentationLanguage.EN, "Constant that is used for the last (max 27) bytes of the engine id if 'xact.snmp.service.engineid.version' = 'set_globally'."
                                            +" (max. 27 bytes in hexadecimal representation with ':' as delimiter)").
                    setDefaultDocumentation(DocumentationLanguage.DE, "Konstante, die für die letzten (max 27) Bytes der EngineId verwendet wird, falls 'xact.snmp.service.engineid.version' = 'set_globally'." 
                                            +" (max. 27 Bytes in Hexadezimal-Darstellung mit ':' als Trennzeichen)");
    constant.registerDependency(user);
  }

  private void registerApplication(RuntimeContext runtimeContext) {
    String rc = runtimeContext instanceof Workspace ? "workspace" : "application";
    application =  new XynaPropertyBuilds<OctetString>("xact.snmp.service.engineid." + rc +"." + runtimeContext.getName(), new OctetStringBuilder(2), new OctetString()).
                    setDefaultDocumentation(DocumentationLanguage.EN, "For " + rc + " '" + runtimeContext.getName() + "' this value will be integrated into the engine id if 'xact.snmp.service.engineid.version' = 'set_per_application'." 
                                    + " (2 bytes in hexadecimal representation with ':' as delimiter)").
                                    setDefaultDocumentation(DocumentationLanguage.DE, "Für " + rc + " '" + runtimeContext.getName() + "' wird dieser Wert in die EngineId integriert, falls 'xact.snmp.service.engineid.version' = 'set_per_application'."
                                                    + " (2 Bytes in Hexadezimal-Darstellung mit ':' als Trennzeichen)");
    application.registerDependency(user);
  }

  private void registerCurrentValue(String applicationName, String versionName) {
    String applicationEN;
    String applicationDE;
    if (versionName == null) {
      applicationEN = "workspace '" + applicationName + "'";
      applicationDE = "Workspace '" + applicationName + "'";
    } else {
      applicationEN = "application '" + applicationName + "' version '" + versionName + "'";
      applicationDE = "Applikation '" + applicationName + "' Version '" + versionName + "'";
    }
    currentValue =  new XynaPropertyBuilds<OctetString>("xact.snmp.service.engineid.currentvalue.<" + applicationName + ">." + (versionName == null ? "" : "<" + versionName + ">." ) +"readonly", new OctetStringBuilder(5, 32), new OctetString()).
                    setDefaultDocumentation(DocumentationLanguage.EN, "The currently used engineId for " + applicationEN + ". Please do not edit this property.").
                                    setDefaultDocumentation(DocumentationLanguage.DE, "Aktuell verwendete engine Id für "  + applicationDE +". Bitte diesen Wert nicht ändern.");
    currentValue.registerDependency(user);
  }
  
  private void registerIp() {
    ip = new XynaPropertyString("xact.snmp.service.engineid.ip", "").
                    setDefaultDocumentation(DocumentationLanguage.EN, "Name of an registered ip address that is used to generate the engine id.").
                    setDefaultDocumentation(DocumentationLanguage.DE, "Name einer im IP-Management registrierten IP-Adresse. Diese wird zur Erstellung der EngineId verwendet.");
    ip.registerDependency(user);
  }
  
  public ArrayList<String> getWatchedProperties() {
    ArrayList<String> props = new ArrayList<String>();
    props.add(version.getPropertyName());
    props.add(constant.getPropertyName());
    props.add(application.getPropertyName());
    props.add(ip.getPropertyName());
    
    //wegen Abwärtskompatibilität alte Property auch beobachten
    if (revision == RevisionManagement.REVISION_DEFAULT_WORKSPACE) {
      props.add("xact.snmp.service.engineid.application.workingset");
    }
    return props;
  }

  public void propertyChanged(){
    try {
      generateEngineId();
    } catch (PersistenceLayerException e) {
      throw new RuntimeException("could not regenerate engine id", e);
    }
  }
}
