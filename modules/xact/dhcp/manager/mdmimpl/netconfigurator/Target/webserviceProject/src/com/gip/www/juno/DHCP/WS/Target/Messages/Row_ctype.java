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
/**
 * Row_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.Target.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String targetID;

    private java.lang.String dppFixedAttributeId;

    private java.lang.String dppFixedAttribute;

    private java.lang.String standortGruppeID;

    private java.lang.String standortGruppe;

    private java.lang.String connectDataID;

    private java.lang.String name;

    private java.lang.String clustergroup;

    private java.lang.String nodetype;

    private java.lang.String dienst;

    private java.lang.String payload;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String targetID,
           java.lang.String dppFixedAttributeId,
           java.lang.String dppFixedAttribute,
           java.lang.String standortGruppeID,
           java.lang.String standortGruppe,
           java.lang.String connectDataID,
           java.lang.String name,
           java.lang.String clustergroup,
           java.lang.String nodetype,
           java.lang.String dienst,
           java.lang.String payload) {
           this.targetID = targetID;
           this.dppFixedAttributeId = dppFixedAttributeId;
           this.dppFixedAttribute = dppFixedAttribute;
           this.standortGruppeID = standortGruppeID;
           this.standortGruppe = standortGruppe;
           this.connectDataID = connectDataID;
           this.name = name;
           this.clustergroup = clustergroup;
           this.nodetype = nodetype;
           this.dienst = dienst;
           this.payload = payload;
    }


    /**
     * Gets the targetID value for this Row_ctype.
     * 
     * @return targetID
     */
    public java.lang.String getTargetID() {
        return targetID;
    }


    /**
     * Sets the targetID value for this Row_ctype.
     * 
     * @param targetID
     */
    public void setTargetID(java.lang.String targetID) {
        this.targetID = targetID;
    }


    /**
     * Gets the dppFixedAttributeId value for this Row_ctype.
     * 
     * @return dppFixedAttributeId
     */
    public java.lang.String getDppFixedAttributeId() {
        return dppFixedAttributeId;
    }


    /**
     * Sets the dppFixedAttributeId value for this Row_ctype.
     * 
     * @param dppFixedAttributeId
     */
    public void setDppFixedAttributeId(java.lang.String dppFixedAttributeId) {
        this.dppFixedAttributeId = dppFixedAttributeId;
    }


    /**
     * Gets the dppFixedAttribute value for this Row_ctype.
     * 
     * @return dppFixedAttribute
     */
    public java.lang.String getDppFixedAttribute() {
        return dppFixedAttribute;
    }


    /**
     * Sets the dppFixedAttribute value for this Row_ctype.
     * 
     * @param dppFixedAttribute
     */
    public void setDppFixedAttribute(java.lang.String dppFixedAttribute) {
        this.dppFixedAttribute = dppFixedAttribute;
    }


    /**
     * Gets the standortGruppeID value for this Row_ctype.
     * 
     * @return standortGruppeID
     */
    public java.lang.String getStandortGruppeID() {
        return standortGruppeID;
    }


    /**
     * Sets the standortGruppeID value for this Row_ctype.
     * 
     * @param standortGruppeID
     */
    public void setStandortGruppeID(java.lang.String standortGruppeID) {
        this.standortGruppeID = standortGruppeID;
    }


    /**
     * Gets the standortGruppe value for this Row_ctype.
     * 
     * @return standortGruppe
     */
    public java.lang.String getStandortGruppe() {
        return standortGruppe;
    }


    /**
     * Sets the standortGruppe value for this Row_ctype.
     * 
     * @param standortGruppe
     */
    public void setStandortGruppe(java.lang.String standortGruppe) {
        this.standortGruppe = standortGruppe;
    }


    /**
     * Gets the connectDataID value for this Row_ctype.
     * 
     * @return connectDataID
     */
    public java.lang.String getConnectDataID() {
        return connectDataID;
    }


    /**
     * Sets the connectDataID value for this Row_ctype.
     * 
     * @param connectDataID
     */
    public void setConnectDataID(java.lang.String connectDataID) {
        this.connectDataID = connectDataID;
    }


    /**
     * Gets the name value for this Row_ctype.
     * 
     * @return name
     */
    public java.lang.String getName() {
        return name;
    }


    /**
     * Sets the name value for this Row_ctype.
     * 
     * @param name
     */
    public void setName(java.lang.String name) {
        this.name = name;
    }


    /**
     * Gets the clustergroup value for this Row_ctype.
     * 
     * @return clustergroup
     */
    public java.lang.String getClustergroup() {
        return clustergroup;
    }


    /**
     * Sets the clustergroup value for this Row_ctype.
     * 
     * @param clustergroup
     */
    public void setClustergroup(java.lang.String clustergroup) {
        this.clustergroup = clustergroup;
    }


    /**
     * Gets the nodetype value for this Row_ctype.
     * 
     * @return nodetype
     */
    public java.lang.String getNodetype() {
        return nodetype;
    }


    /**
     * Sets the nodetype value for this Row_ctype.
     * 
     * @param nodetype
     */
    public void setNodetype(java.lang.String nodetype) {
        this.nodetype = nodetype;
    }


    /**
     * Gets the dienst value for this Row_ctype.
     * 
     * @return dienst
     */
    public java.lang.String getDienst() {
        return dienst;
    }


    /**
     * Sets the dienst value for this Row_ctype.
     * 
     * @param dienst
     */
    public void setDienst(java.lang.String dienst) {
        this.dienst = dienst;
    }


    /**
     * Gets the payload value for this Row_ctype.
     * 
     * @return payload
     */
    public java.lang.String getPayload() {
        return payload;
    }


    /**
     * Sets the payload value for this Row_ctype.
     * 
     * @param payload
     */
    public void setPayload(java.lang.String payload) {
        this.payload = payload;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Row_ctype)) return false;
        Row_ctype other = (Row_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.targetID==null && other.getTargetID()==null) || 
             (this.targetID!=null &&
              this.targetID.equals(other.getTargetID()))) &&
            ((this.dppFixedAttributeId==null && other.getDppFixedAttributeId()==null) || 
             (this.dppFixedAttributeId!=null &&
              this.dppFixedAttributeId.equals(other.getDppFixedAttributeId()))) &&
            ((this.dppFixedAttribute==null && other.getDppFixedAttribute()==null) || 
             (this.dppFixedAttribute!=null &&
              this.dppFixedAttribute.equals(other.getDppFixedAttribute()))) &&
            ((this.standortGruppeID==null && other.getStandortGruppeID()==null) || 
             (this.standortGruppeID!=null &&
              this.standortGruppeID.equals(other.getStandortGruppeID()))) &&
            ((this.standortGruppe==null && other.getStandortGruppe()==null) || 
             (this.standortGruppe!=null &&
              this.standortGruppe.equals(other.getStandortGruppe()))) &&
            ((this.connectDataID==null && other.getConnectDataID()==null) || 
             (this.connectDataID!=null &&
              this.connectDataID.equals(other.getConnectDataID()))) &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName()))) &&
            ((this.clustergroup==null && other.getClustergroup()==null) || 
             (this.clustergroup!=null &&
              this.clustergroup.equals(other.getClustergroup()))) &&
            ((this.nodetype==null && other.getNodetype()==null) || 
             (this.nodetype!=null &&
              this.nodetype.equals(other.getNodetype()))) &&
            ((this.dienst==null && other.getDienst()==null) || 
             (this.dienst!=null &&
              this.dienst.equals(other.getDienst()))) &&
            ((this.payload==null && other.getPayload()==null) || 
             (this.payload!=null &&
              this.payload.equals(other.getPayload())));
        __equalsCalc = null;
        return _equals;
    }

    private boolean __hashCodeCalc = false;
    public synchronized int hashCode() {
        if (__hashCodeCalc) {
            return 0;
        }
        __hashCodeCalc = true;
        int _hashCode = 1;
        if (getTargetID() != null) {
            _hashCode += getTargetID().hashCode();
        }
        if (getDppFixedAttributeId() != null) {
            _hashCode += getDppFixedAttributeId().hashCode();
        }
        if (getDppFixedAttribute() != null) {
            _hashCode += getDppFixedAttribute().hashCode();
        }
        if (getStandortGruppeID() != null) {
            _hashCode += getStandortGruppeID().hashCode();
        }
        if (getStandortGruppe() != null) {
            _hashCode += getStandortGruppe().hashCode();
        }
        if (getConnectDataID() != null) {
            _hashCode += getConnectDataID().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        if (getClustergroup() != null) {
            _hashCode += getClustergroup().hashCode();
        }
        if (getNodetype() != null) {
            _hashCode += getNodetype().hashCode();
        }
        if (getDienst() != null) {
            _hashCode += getDienst().hashCode();
        }
        if (getPayload() != null) {
            _hashCode += getPayload().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("targetID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "TargetID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dppFixedAttributeId");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "DppFixedAttributeId"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dppFixedAttribute");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "DppFixedAttribute"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortGruppeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "StandortGruppeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("standortGruppe");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "StandortGruppe"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("connectDataID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "ConnectDataID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "Name"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("clustergroup");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "Clustergroup"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("nodetype");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "Nodetype"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dienst");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "Dienst"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("payload");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Target/Messages", "Payload"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
    }

    /**
     * Return type metadata object
     */
    public static org.apache.axis.description.TypeDesc getTypeDesc() {
        return typeDesc;
    }

    /**
     * Get Custom Serializer
     */
    public static org.apache.axis.encoding.Serializer getSerializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanSerializer(
            _javaType, _xmlType, typeDesc);
    }

    /**
     * Get Custom Deserializer
     */
    public static org.apache.axis.encoding.Deserializer getDeserializer(
           java.lang.String mechType, 
           java.lang.Class _javaType,  
           javax.xml.namespace.QName _xmlType) {
        return 
          new  org.apache.axis.encoding.ser.BeanDeserializer(
            _javaType, _xmlType, typeDesc);
    }

}
