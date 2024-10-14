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
package com.gip.xyna.xact.filter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.snmp4j.security.AuthMD5;
import org.snmp4j.security.PrivDES;
import org.snmp4j.security.UsmUser;
import org.snmp4j.smi.OctetString;

import xact.snmp.OID;
import xact.snmp.VarBinding;
import xact.snmp.VariableContent;
import xact.snmp.types.SNMPCounter32;
import xact.snmp.types.SNMPCounter64;
import xact.snmp.types.SNMPGauge32;
import xact.snmp.types.SNMPInteger;
import xact.snmp.types.SNMPNull;
import xact.snmp.types.SNMPOctet;
import xact.snmp.types.SNMPString;
import xact.snmp.types.SNMPTimeTicks;
import xact.snmp.types.SNMPUnsignedInteger;

import com.gip.xyna.CentralFactoryLogging;
import com.gip.xyna.XynaFactory;
import com.gip.xyna.XMOM.base.IPv4;
import com.gip.xyna.utils.exceptions.XynaException;
import com.gip.xyna.utils.snmp.varbind.ByteArrayVarBind;
import com.gip.xyna.utils.snmp.varbind.Counter32VarBind;
import com.gip.xyna.utils.snmp.varbind.Counter64VarBind;
import com.gip.xyna.utils.snmp.varbind.Gauge32VarBind;
import com.gip.xyna.utils.snmp.varbind.IntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.NullVarBind;
import com.gip.xyna.utils.snmp.varbind.OIDVarBind;
import com.gip.xyna.utils.snmp.varbind.StringVarBind;
import com.gip.xyna.utils.snmp.varbind.TimeTicksVarBind;
import com.gip.xyna.utils.snmp.varbind.UnsIntegerVarBind;
import com.gip.xyna.utils.snmp.varbind.VarBind;
import com.gip.xyna.utils.snmp.varbind.VarBindList;
import com.gip.xyna.xact.trigger.SNMPTrigger;
import com.gip.xyna.xact.trigger.SNMPTriggerConnection;
import com.gip.xyna.xact.trigger.XynaOrderSnmpRequestHandler;
import com.gip.xyna.xdev.xfractmod.xmdm.ConnectionFilter;
import com.gip.xyna.xdev.xfractmod.xmdm.Container;
import com.gip.xyna.xdev.xfractmod.xmdm.EventListener;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObject;
import com.gip.xyna.xdev.xfractmod.xmdm.XynaObjectList;
import com.gip.xyna.xfmg.xods.configuration.IPropertyChangeListener;
import com.gip.xyna.xfmg.xods.configuration.XynaPropertyUtils.XynaPropertyString;
import com.gip.xyna.xprc.XynaOrder;
import com.gip.xyna.xprc.xpce.dispatcher.DestinationKey;

public class SNMPTrapFilter extends ConnectionFilter<SNMPTriggerConnection>
        implements IPropertyChangeListener {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public static final XynaPropertyString XYNA_PROPERTY_TRAP_HANDLING_ORDER_TYPE = new XynaPropertyString("xact.snmp.trap_filter.ordertype", "xact.snmp.HandleTrap");
    
    private static final String XYNA_PROPERTY_USERNAME      = "xact.snmp.trap_target.username";
    private static final String XYNA_PROPERTY_AUTH_PASSWORD = "xact.snmp.trap_target.auth_password_md5";
    private static final String XYNA_PROPERTY_PRIV_PASSWORD = "xact.snmp.trap_target.priv_password_des";
    
    private String trapUser = null;
    private String trapAuthPassword = null;
    private String trapPrivPassword = null;
    private transient SNMPTrigger snmpTrigger = null;

    private static SNMPTrapFilter filterInstance;
    
    private static Logger logger = CentralFactoryLogging
            .getLogger(SNMPTrapFilter.class);

    @Override
    public void onDeployment(EventListener trigger) {
        logger.debug("onDeployment SNMPTrapFilter filter");
        super.onDeployment(trigger);
        filterInstance = this;
        this.snmpTrigger = (SNMPTrigger) trigger;
        XynaFactory.getInstance().getFactoryManagement()
                .getXynaFactoryManagementODS().getConfiguration()
                .addPropertyChangeListener(this);    
        propertyChanged();
    }

    @Override
    public void onUndeployment(EventListener trigger) {
        XynaFactory.getInstance().getFactoryManagement()
        .getXynaFactoryManagementODS().getConfiguration().removePropertyChangeListener(filterInstance);
        removeSnmpUser();
    }
    
    public void propertyChanged() {
        logger.debug("Changing SNMP username and/or password due to new property values");

        if (snmpTrigger == null) {
            logger.warn("Could not adapt to username/password changes: Could not obtain relevant SNMP trigger");
            return;
        }

        if (trapUser != null) {
            logger.debug("Removing old SNMP user");
            removeSnmpUser();
        }

        trapUser = XynaFactory.getPortalInstance().getFactoryManagementPortal()
                .getProperty(XYNA_PROPERTY_USERNAME);
        trapAuthPassword = XynaFactory.getPortalInstance()
                .getFactoryManagementPortal()
                .getProperty(XYNA_PROPERTY_AUTH_PASSWORD);
        trapPrivPassword = XynaFactory.getPortalInstance()
                .getFactoryManagementPortal()
                .getProperty(XYNA_PROPERTY_PRIV_PASSWORD);

        if (trapUser == null || trapUser.equals("")) {
            logger.warn("SNMP user is no longer configured, no SNMP user will be created");
            return;
        }
        if (trapAuthPassword == null || trapAuthPassword.equals("")) {
            logger.warn("SNMP trap auth password is no longer configured, no SNMP user will be created");
            return;
        }
        if (trapPrivPassword == null || trapPrivPassword.equals("")) {
            logger.warn("SNMP trap priv password is no longer configured, no SNMP user will be created");
            return;
        }

        logger.debug("### Creating new SNMP user");
        logger.debug("### Trigger instance : " + snmpTrigger);
        
        snmpTrigger.executeWhenTriggerIsInitialized(new Runnable() {

            public void run() {
              snmpTrigger
                .getSnmp()
                .getUSM()
                .addUser(
                         new OctetString(trapUser),
                         new UsmUser(new OctetString(trapUser), AuthMD5.ID,
                                     new OctetString(trapAuthPassword), PrivDES.ID,
                                     new OctetString(trapPrivPassword)));
            }
          });
             
        logger.debug("Setting SNMP Credentials: "+trapUser+" auth:  "+trapAuthPassword +" priv: "+ trapPrivPassword);
    }

    private static XynaOrderSnmpRequestHandler trapHandler = new XynaOrderSnmpRequestHandler() {
       

        @Override
        public XynaOrder snmpGet(VarBindList arg0) throws XynaException,
                InterruptedException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public XynaOrder snmpGetNext(VarBindList arg0) throws XynaException,
                InterruptedException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public XynaOrder snmpInform(com.gip.xyna.utils.snmp.OID arg0,
                VarBindList arg1) throws XynaException, InterruptedException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public XynaOrder snmpSet(VarBindList arg0) throws XynaException,
                InterruptedException {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public XynaOrder snmpTrap(com.gip.xyna.utils.snmp.OID trapOid,
                VarBindList vbl) throws XynaException, InterruptedException {
            DestinationKey dk = new DestinationKey(XYNA_PROPERTY_TRAP_HANDLING_ORDER_TYPE.get());

            if(logger.isDebugEnabled()) {
                logger.debug("DestinationKey for Xyna Order: "+dk.getOrderType());
            }
            
            XynaObjectList<VarBinding> xol = new XynaObjectList<VarBinding>(VarBinding.class);
            
            for(VarBinding vb : getSNMPUtilsVarBindingToMDMVarBinding(vbl) ) {
                xol.add(vb);
            }
            

            Container c = new Container();
            c.add(new IPv4(vbl.getReceivedFromHost()));
            c.add(new xact.snmp.OID(trapOid.getOid()));
            c.add(xol);
           
            
            
            return new XynaOrder(dk, c);
        }



    };

    /**
     * Analyzes TriggerConnection and creates XynaOrder if it accepts the
     * connection. The method return a FilterResponse object, which can include
     * the XynaOrder if the filter is responsibleb for the request. # If this
     * filter is not responsible the returned object must be:
     * FilterResponse.notResponsible() # If this filter is responsible the
     * returned object must be: FilterResponse.responsible(XynaOrder order) # If
     * this filter is responsible but it handle the request without creating a
     * XynaOrder the returned object must be:
     * FilterResponse.responsibleWithoutXynaorder() # If this filter is
     * responsible but the version of this filter is too new the returned object
     * must be: FilterResponse.responsibleButTooNew(). The trigger will try an
     * older version of the filter.
     * 
     * @param tc
     * @return FilterResponse object
     * @throws XynaException
     *             caused by errors reading data from triggerconnection or
     *             having an internal error. results in onError() being called
     *             by Xyna Processing.
     */
    public FilterResponse createXynaOrder(SNMPTriggerConnection tc)
            throws XynaException {

        XynaOrder xo;
        try {
            xo = tc.handleEvent(trapHandler);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            
            logger.error("Error creating xyna order for trap handling", e);
            
            e.printStackTrace();
            return FilterResponse.notResponsible();
        } 
        
        return FilterResponse.responsible(xo);
    }

    /**
     * called when above XynaOrder returns successfully.
     * 
     * @param response
     *            by XynaOrder returned XynaObject
     * @param tc
     *            corresponding triggerconnection
     */
    public void onResponse(XynaObject response, SNMPTriggerConnection tc) {
        // TODO implementation
        // TODO update dependency xml file
    }


    /**
     * @return description of this filter
     */
    public String getClassDescription() {
        // TODO implementation
        // TODO update dependency xml file
        return null;
    }

    @Override
    public ArrayList<String> getWatchedProperties() {
        ArrayList<String> propertyList = new ArrayList<String>();
        propertyList.add("xact.snmp.trap_target.username");
        propertyList.add("xact.snmp.trap_target.auth_password_md5");
        propertyList.add("xact.snmp.trap_target.priv_password_des");
        
        return propertyList;
        
    }

    private void removeSnmpUser() {
        if (snmpTrigger == null) {
          logger.warn("Could not remove old SNMP user because the relevant trigger is not known");
          return;
        }
        if (trapUser == null) {
          logger.warn("Could not remove old SNMP user because user was not set");
          return;
        }
        snmpTrigger.getSnmp().getUSM().removeUser(new OctetString(snmpTrigger.getLocalEngineId()),
                                                  new OctetString(trapUser));
      }
    
    
    private static VarBindList getMDMVarBindingToSNMPUtilsVarBinding(List<? extends VarBinding> varBindings, boolean typeMustBeKnownBeforehand) {
        VarBindList vbl = new VarBindList();
        for (VarBinding vb : varBindings) {
          if (vb.getType() instanceof SNMPString) {
            vbl.add(new StringVarBind(vb.getOID().getOID(), vb.getValue().getContent()));
          } else if (vb.getType() instanceof SNMPInteger) {
            try {
              int v = Integer.valueOf(vb.getValue().getContent());
              vbl.add(new IntegerVarBind(vb.getOID().getOID(), v));
            } catch (NumberFormatException e) {
              throw new RuntimeException("invalid variable content. expected integer, got \"" + vb.getValue().getContent()
                              + "\"", e);
            }
          } else if (vb.getType() instanceof SNMPOctet) {
            byte[] b = new byte[vb.getValue().getContent().length() / 2];
            for (int i = 0; i < b.length; i++) {
              b[i] = (byte) Integer.parseInt(vb.getValue().getContent().substring(2 * i, 2 * i + 2), 16);
            }
            vbl.add(new ByteArrayVarBind(vb.getOID().getOID(), b));
          } else if (vb.getType() instanceof SNMPUnsignedInteger) {
            try {
              long l = Long.valueOf(vb.getValue().getContent());
              vbl.add(new UnsIntegerVarBind(vb.getOID().getOID(), l));
            } catch (NumberFormatException e) {
              throw new RuntimeException("invalid variable content. expected unsigned integer, got \""
                              + vb.getValue().getContent() + "\"", e);
            }
          } else if (vb.getType() instanceof SNMPTimeTicks) {
            try {
              long v = Long.valueOf(vb.getValue().getContent());
              vbl.add(new TimeTicksVarBind(vb.getOID().getOID(), v));
            } catch (NumberFormatException e) {
              throw new RuntimeException("invalid variable content. expected time tick, got \"" + vb.getValue().getContent() + "\"", e);
            }
          } else if (vb.getType() instanceof SNMPCounter32) {
            try {
              long v = Long.valueOf(vb.getValue().getContent());
              vbl.add(new Counter32VarBind(vb.getOID().getOID(), v));
            } catch (NumberFormatException e) {
              throw new RuntimeException("invalid variable content. expected counter32, got \"" + vb.getValue().getContent() + "\"", e);
            }
          } else if (vb.getType() instanceof SNMPGauge32) {
            try {
              long v = Long.valueOf(vb.getValue().getContent());
              vbl.add(new Gauge32VarBind(vb.getOID().getOID(), v));
            } catch (NumberFormatException e) {
              throw new RuntimeException("invalid variable content. expected gauge32, got \"" + vb.getValue().getContent() + "\"", e);
            }
            
//          } else if (vb.getType() instanceof SNMPIpAddress) {
//            try {
//              vbl.add(new IpAddressVarBind(vb.getOID().getOID(), vb.getValue().getContent()));
//            } catch (NumberFormatException e) {
//              throw new RuntimeException("invalid variable content. expected ipAddress, got \""
//                              + vb.getValue().getContent() + "\"", e);
//            }
          } else {
            if (typeMustBeKnownBeforehand) {
              throw new RuntimeException("unsupported snmp variable type: " + vb.getType());
            } else {
              vbl.add(new NullVarBind(vb.getOID().getOID()));
            }
          }
        }
        return vbl;
      }


      private static List<VarBinding> getSNMPUtilsVarBindingToMDMVarBinding(VarBindList vbl)  {
        List<VarBinding> list = new ArrayList<VarBinding>();
        for (int i = 0; i < vbl.size(); i++) {
          VarBind v = vbl.get(i);
          VarBinding vb = new VarBinding();
          vb.setOID(new xact.snmp.OID(v.getObjectIdentifier()));
          //type
          if (v instanceof StringVarBind) {
            vb.setType(new SNMPString());
          } else if (v instanceof IntegerVarBind) {
            vb.setType(new SNMPInteger());
          } else if (v instanceof UnsIntegerVarBind) {
            vb.setType(new SNMPUnsignedInteger());
          } else if (v instanceof OIDVarBind) {
            vb.setType(new SNMPString()); // TODO: here should be an OID type
          } else if (v instanceof NullVarBind) {
            vb.setType(new SNMPNull()); 
          } else if (v instanceof Counter64VarBind) {
            vb.setType(new SNMPCounter64());
          } else if (v instanceof TimeTicksVarBind) {
            vb.setType(new SNMPTimeTicks());
          } else if (v instanceof Counter32VarBind) {
            vb.setType(new SNMPCounter32());
          } else if (v instanceof Gauge32VarBind) {
            vb.setType(new SNMPGauge32());
          } else {
            throw new RuntimeException("got unsupported varBindType: " + v.getClass().getName() + " = " + v);
          }
          //value (evtl anders fuer andere typen??)
          vb.setValue(new VariableContent(String.valueOf(v.getValue())));
          list.add(vb);
        }
        return list;
      }

    @Override
    public void onError(XynaException[] arg0, SNMPTriggerConnection arg1) {
        // TODO Auto-generated method stub
        
    }

    
}
