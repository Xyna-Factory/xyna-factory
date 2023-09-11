/*----------------------------------------------------
* Xyna 5.1 (Black Edition)
* Development
*----------------------------------------------------
* Copyright GIP AG 2013
* (http://www.gip.com)
* Hechtsheimer Str. 35-37
* 55131 Mainz
*----------------------------------------------------
* $Revision: 330010 $
* $Date: 2023-09-11 21:19:58 +0200 (Mon, 11 Sep 2023) $
*----------------------------------------------------
*/
package xmcp.yggdrasil.impl;


import xmcp.yggdrasil.MessageBusUtils;
import xmcp.yggdrasil.MessageBusUtilsSuperProxy;


public class MessageBusUtilsInstanceOperationImpl extends MessageBusUtilsSuperProxy {

  private static final long serialVersionUID = 1L;

  public MessageBusUtilsInstanceOperationImpl(MessageBusUtils instanceVar) {
    super(instanceVar);
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
