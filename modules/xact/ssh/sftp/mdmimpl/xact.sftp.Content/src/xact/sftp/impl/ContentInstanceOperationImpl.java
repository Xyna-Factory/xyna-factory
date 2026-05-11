/*----------------------------------------------------
* Xyna 5.1 (Black Edition)
* Development
*----------------------------------------------------
* Copyright GIP AG 2013
* (http://www.gip.com)
* Hechtsheimer Str. 35-37
* 55131 Mainz
*----------------------------------------------------
* $Revision: 165268 $
* $Date: 2015-03-16 16:10:26 +0100 (Mo, 16 Mrz 2015) $
*----------------------------------------------------
*/
package xact.sftp.impl;


import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.xdev.exceptions.XDEV_PARAMETER_NAME_NOT_FOUND;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.GeneralXynaObjectList;
import com.gip.xyna.xdev.xfractmod.xmdm.XOUtils;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaExceptionBase;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.BehaviorAfterOnUnDeploymentTimeout;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.DeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.ExtendedDeploymentTask;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject.XMLHelper;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.Constants;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedException;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedObject;
import com.gip.xyna.xfmg.xfctrl.classloading.persistence.SerializableClassloadedXynaObject;
import com.gip.xyna.xprc.XynaOrderServerExtension;
import com.gip.xyna.xprc.exceptions.XPRC_MDMDeploymentException;
import com.gip.xyna.xprc.xfractwfe.InvalidObjectPathException;
import com.gip.xyna.xprc.xfractwfe.generation.XynaObjectAnnotation;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventHandling;
import com.gip.xyna.xprc.xfractwfe.servicestepeventhandling.ServiceStepEventSource;

import java.io.UnsupportedEncodingException;
import java.lang.ClassNotFoundException;
import java.lang.IllegalArgumentException;
import java.util.Arrays;
import java.util.List;


import xact.sftp.ContentSuperProxy;
import xact.sftp.ContentInstanceOperation;
import xact.sftp.Content;


public class ContentInstanceOperationImpl extends ContentSuperProxy implements ContentInstanceOperation {

  private static final long serialVersionUID = 1L;
  
  private static enum ContentType {
    STRING("String"), BYTES("Bytes");
    
    private final String typeIdentifier;
   
    private ContentType(String typeIdentifier) {
      this.typeIdentifier = typeIdentifier;
    }
    
    public static ContentType byIdentifier(String typeIdentifier) {
      for (ContentType value : values()) {
        if (value.typeIdentifier.equalsIgnoreCase(typeIdentifier)) {
          return value;
        }
      }
      return STRING;
    }
  }
  
  private byte[] raw;

  public ContentInstanceOperationImpl(Content instanceVar) {
    super(instanceVar);
  }

  public List<Integer> getRawContent() {
    byte[] rawContent;
    switch (byIdentifier()) {
      case BYTES :
        rawContent = raw;
        break;
      case STRING :
        rawContent = toByte(instanceVar.getContent() == null ? "" : instanceVar.getContent());
        break;
      default :
        throw new IllegalArgumentException("Illegal type identifier: " + String.valueOf(instanceVar.getType()));
    }
    List container = Arrays.asList(rawContent);
    return container;
  }

  
  public void setRawContent(List<Integer> list) {
    Object untyped = ((List)list).get(0);
    if (untyped instanceof byte[]) {
      raw = (byte[]) untyped;
    } else if (untyped != null) {
      raw = toByte(untyped.toString());
    } // else leave it unset
  }
  
  
  public void calculateSize() {
    switch (byIdentifier()) {
      case BYTES :
        instanceVar.setSize(raw.length);
        break;
      case STRING :
        instanceVar.setSize(toByte(instanceVar.getContent() == null ? "" : instanceVar.getContent()).length);
        break;
      default :
        throw new IllegalArgumentException("Illegal type identifier: " + String.valueOf(instanceVar.getType()));
    }
  }
  
  
  private static byte[] toByte(String value) {
    try {
      return value.getBytes(Constants.DEFAULT_ENCODING);
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Unsupported default enconding!", e);
    }
  }
  
  private ContentType byIdentifier() {
    return ContentType.byIdentifier(instanceVar.getType());
  }
  
  

  private void writeObject(java.io.ObjectOutputStream s) throws java.io.IOException {
    //change if needed to store instance context
    s.defaultWriteObject();
  }

  private void readObject(java.io.ObjectInputStream s) throws java.io.IOException, ClassNotFoundException {
    //change if needed to restore instance-context during deserialization of order
    s.defaultReadObject();
  }


}
