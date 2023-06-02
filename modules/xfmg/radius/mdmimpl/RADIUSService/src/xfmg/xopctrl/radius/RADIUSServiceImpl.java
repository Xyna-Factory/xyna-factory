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
package xfmg.xopctrl.radius;



import com.gip.xyna.ObjectStringRepresentation;
import com.gip.xyna.XynaFactory;


import com.gip.xyna.XMOM.base.IP;
import com.gip.xyna.XMOM.base.IPv4;
import com.gip.xyna._3._0.XMDM.xdev.tsim.qsXynaFactory.datatypes.ActualValue;
import com.gip.xyna._3._0.XMDM.xdev.tsim.qsXynaFactory.datatypes.AutomatedXynaFactoryQAException;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xfmg.exceptions.XFMG_UserAuthenticationFailedException;
import com.gip.xyna.xfmg.xopctrl.usermanagement.AuthenticationResult;
import com.gip.xyna.xfmg.xopctrl.usermanagement.DomainName;
import com.gip.xyna.xfmg.xopctrl.usermanagement.Role;
import com.gip.xyna.xfmg.xopctrl.usermanagement.User;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserManagement;
import com.gip.xyna.xfmg.xopctrl.usermanagement.UserName;
import com.gip.xyna.xnwh.persistence.PersistenceLayerException;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;


import java.io.IOException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



import net.jradius.client.RadiusClient;
import net.jradius.client.auth.PAPAuthenticator;
import net.jradius.client.auth.RadiusAuthenticator;
import net.jradius.dictionary.Attr_Password;
import net.jradius.dictionary.Attr_UserName;
import net.jradius.exception.RadiusException;
import net.jradius.exception.UnknownAttributeException;
import net.jradius.packet.RadiusPacket;
import net.jradius.packet.attribute.Attr_UnknownVSAttribute;
import net.jradius.packet.attribute.AttributeFactory;
import net.jradius.packet.attribute.RadiusAttribute;
import net.jradius.packet.attribute.AttributeFactory.VendorValue;

import org.apache.log4j.Logger;

import xfmg.xopctrl.ExternalRole;




public class RADIUSServiceImpl implements DeploymentTask {
  
  private static final Logger logger = Logger.getLogger(RADIUSServiceImpl.class);

  protected RADIUSServiceImpl() {
  }

  public void onDeployment() {
    // load attributes
    AttributeFactory.loadAttributeDictionary("net.jradius.dictionary.AttributeDictionaryImpl");
  }

  public void onUndeployment() {

  }

  public static AuthenticationResult checkRoleMapping(UserName username, ExternalRole externalRole) {

    AuthenticationResult result = new AuthenticationResult();
    User user = null;
    try {
      user = XynaFactory.getInstance().getFactoryManagement().getUser(username.getName());
    } catch (PersistenceLayerException e) {
      result.setSuccess(false);
      return result;
    }
    
    if (user == null) {
      result.setSuccess(false);
      return result;
    }
    
    Role role = null;
    try {
      role = XynaFactory.getInstance().getFactoryManagement().getRole(externalRole.getRolename(), externalRole.getDomainname());
    } catch (PersistenceLayerException e) {
      result.setSuccess(false);
      return result;
    }
    
    if (role == null) {
      result.setSuccess(false);
      return result;
    }
    
    if (user.getRole().equals(role.getAlias())) {
      logger.debug(new StringBuilder().append("External role '").append(role.getName()).append("@").append(role.getDomain()).append("' successfully mapped against '").append(role.getAlias()).append("'").toString());
      result.setSuccess(true);
    } else {
      logger.debug(new StringBuilder().append("External role '").append(role.getName()).append("@").append(role.getDomain()).append("' could not be mapped against expected role '").append(user.getRole()).append("'").toString());
      result.setSuccess(false);
    }
    return result;
  }

  public static RADIUSResponse remoteAuthentication(XynaOrder correlatedXynaOrder, UserName username, com.gip.xyna.xfmg.xopctrl.radius.RADIUSConnectionConfig config) throws XynaException {
    
    logger.debug("Authenticate user " + username.getName() + " against radius server.");
    
    final String STORAGE_DESTINATION = "xfmg.xopctrl.usermanagement.radius";
    Object storedObj = XynaFactory.getInstance().getXynaNetworkWarehouse().retrieve(STORAGE_DESTINATION, Long.toString(correlatedXynaOrder.getId()));
    if (storedObj == null || !(storedObj instanceof String)) {
      throw new XFMG_UserAuthenticationFailedException(username.getName());
    }
    String password = (String)storedObj;
    RadiusClient radiusClient;
    try {
      radiusClient = buildRadiusClientConnectionFromConfig(config);
    } catch (Throwable e) {
      throw new XFMG_UserAuthenticationFailedException(username.getName());
    }
           
    net.jradius.packet.AccessRequest request = new net.jradius.packet.AccessRequest(radiusClient);
    request.addAttribute(new Attr_UserName(username.getName()));
    request.addAttribute(new Attr_Password(password));
    
    
    RadiusAuthenticator authenticator = new PAPAuthenticator();
    
    RadiusPacket reply = null;
    try {
      reply = radiusClient.authenticate(request, authenticator, config.getMaxRetries());
      logger.debug("got reply without error: " + reply);

      if(reply instanceof net.jradius.packet.AccessReject) {
        logger.debug("access rejected");
        throw new XFMG_UserAuthenticationFailedException(username.getName());
      }

    } catch (UnknownAttributeException e) {
      throw new XFMG_UserAuthenticationFailedException(username.getName(),e);
    } catch (RadiusException e) {
      throw new XFMG_UserAuthenticationFailedException(username.getName(),e);
    }
    
    //Such a search would only work if the Attrib was known to JRadius
    /*logger.debug("reply before searchiong attrib: " + reply);
    RadiusAttribute attrib = new Attr_VendorSpecific("Baum");
    try {
      logger.debug("starting to search");
      attrib = reply.findAttribute("External-Rolename");
      logger.debug("attrib after find: " + attrib);
    } catch (UnknownAttributeException e) {
      e.printStackTrace();
    }*/
    
    /*String value = null;
    List<RadiusAttribute> list = reply.getAttributes().getAttributeList();
    for (RadiusAttribute ra : list) {
      if  (ra instanceof Attr_UnknownVSAttribute) {
        Attr_UnknownVSAttribute vsa = (Attr_UnknownVSAttribute) ra;
        final long XYNA_VENDOR_ID = 15555l;
        final long XYNA_ATTRIB_ID = 1l;
        //logger.debug("VendorID: " + vsa.getVendorId());
        //logger.debug("VendorID: " + vsa.getVsaAttributeType());
        if (vsa.getVendorId() == XYNA_VENDOR_ID && vsa.getVsaAttributeType() == XYNA_ATTRIB_ID) {
          value = new String(vsa.getValue().getBytes());
          
        }
      }
    }*/
    
    List<RadiusAttribute> list = reply.getAttributes().getAttributeList();
    List<RADIUSAttribute> attributes = new ArrayList<RADIUSAttribute>();
    for (RadiusAttribute ra : list) {
      StringBuilder sb = new StringBuilder();
      try {
        ObjectStringRepresentation.createStringRepOfObject(sb, ra);
      } catch (IllegalArgumentException e) {
        sb.append(e);
      } catch (IllegalAccessException e) {
        sb.append(e);
      }
      attributes.add(convertRadiusAttributeToXynaRepresentation(ra));
    }

    return new RADIUSResponse(attributes);
  }
  
  
  private static RADIUSAttribute convertRadiusAttributeToXynaRepresentation(RadiusAttribute attribute) {
    RADIUSAttribute conversion = new RADIUSAttribute();
    conversion.setAttributeName(attribute.getAttributeName());
    byte[] bytes = attribute.getValue().getBytes();
    List<Integer> ints = new ArrayList<Integer>();
    for (int i = 0; i < bytes.length; i+=4) {
      int bytesToCopy = Math.min(bytes.length-i, 4);
      byte[] localBuffer = new byte[4];
      System.arraycopy(bytes, i, localBuffer, 0, bytesToCopy);
      ints.add(convertByteArrayToInt(localBuffer));
    }
    conversion.setAttributeByteValue(ints);
    System.out.println("ByteConversion done");
    for (Integer integer : ints) {
      System.out.print(integer.intValue() + " ");
    }
    StringBuilder sb = new StringBuilder();
    try {
      ObjectStringRepresentation.createStringRepOfObject(sb, attribute);
    } catch (IllegalAccessException e) {
      sb.append(e);
    }
    conversion.setCompleteStringRepresentation(sb.toString());
    return conversion;
  }
  

  
  static byte[] convertIntToByteArray(int val) {
    byte[] bytes= new byte[4];
  
    bytes[0] = (byte)(val >>> 24);
    bytes[1] = (byte)(val >>> 16);
    bytes[2] = (byte)(val >>> 8);
    bytes[3] = (byte)(val);
  
    return bytes;
  }

  static int convertByteArrayToInt(byte[] bytes) {
    int val = (0xFF & bytes[0]) << 24;
    val += (0xFF & bytes[1]) << 16;
    val += (0xFF & bytes[2]) << 8;
    val += 0xFF & bytes[3];
  
    return val;
  }
  

  private static RadiusClient buildRadiusClientConnectionFromConfig(com.gip.xyna.xfmg.xopctrl.radius.RADIUSConnectionConfig config) throws Exception {

    IP ip = config.getIp();
    com.gip.xyna.xfmg.xopctrl.radius.RADIUSServerPort port = config.getPort();

    com.gip.xyna.xfmg.xopctrl.radius.PresharedKey key = config.getPresharedKey();
    
    InetAddress address = InetAddress.getByName(ip.getValue());
    if (address == null) {
      logger.error("Radius server ip not known or not configured");
      throw new Exception();
    }
    
    RadiusClient radiusClient;
    try {
      radiusClient = new RadiusClient(address, key.getKey());
      radiusClient.setAuthPort(port.getValue());
      radiusClient.setSocketTimeout(config.getConnectionTimeout());
    }
    catch (IOException e) {
      logger.error("Error initiating connection to radius server." + e);
      throw new Exception("Error initiating connection to radius server.",e);
    }    
    
    return radiusClient;
  }
  
}
