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

package com.gip.www.juno.DHCP.WS.GuiAttribute.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String guiAttributeID;

    private java.lang.String name;

    private java.lang.String dhcpConf;

    private java.lang.String wertebereich;

    private java.lang.String optionEncoding;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String guiAttributeID,
           java.lang.String name,
           java.lang.String dhcpConf,
           java.lang.String wertebereich,
           java.lang.String optionEncoding) {
           this.guiAttributeID = guiAttributeID;
           this.name = name;
           this.dhcpConf = dhcpConf;
           this.wertebereich = wertebereich;
           this.optionEncoding = optionEncoding;
    }


    /**
     * Gets the guiAttributeID value for this Row_ctype.
     * 
     * @return guiAttributeID
     */
    public java.lang.String getGuiAttributeID() {
        return guiAttributeID;
    }


    /**
     * Sets the guiAttributeID value for this Row_ctype.
     * 
     * @param guiAttributeID
     */
    public void setGuiAttributeID(java.lang.String guiAttributeID) {
        this.guiAttributeID = guiAttributeID;
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
     * Gets the dhcpConf value for this Row_ctype.
     * 
     * @return dhcpConf
     */
    public java.lang.String getDhcpConf() {
        return dhcpConf;
    }


    /**
     * Sets the dhcpConf value for this Row_ctype.
     * 
     * @param dhcpConf
     */
    public void setDhcpConf(java.lang.String dhcpConf) {
        this.dhcpConf = dhcpConf;
    }


    /**
     * Gets the wertebereich value for this Row_ctype.
     * 
     * @return wertebereich
     */
    public java.lang.String getWertebereich() {
        return wertebereich;
    }


    /**
     * Sets the wertebereich value for this Row_ctype.
     * 
     * @param wertebereich
     */
    public void setWertebereich(java.lang.String wertebereich) {
        this.wertebereich = wertebereich;
    }


    /**
     * Gets the optionEncoding value for this Row_ctype.
     * 
     * @return optionEncoding
     */
    public java.lang.String getOptionEncoding() {
        return optionEncoding;
    }


    /**
     * Sets the optionEncoding value for this Row_ctype.
     * 
     * @param optionEncoding
     */
    public void setOptionEncoding(java.lang.String optionEncoding) {
        this.optionEncoding = optionEncoding;
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
            ((this.guiAttributeID==null && other.getGuiAttributeID()==null) || 
             (this.guiAttributeID!=null &&
              this.guiAttributeID.equals(other.getGuiAttributeID()))) &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName()))) &&
            ((this.dhcpConf==null && other.getDhcpConf()==null) || 
             (this.dhcpConf!=null &&
              this.dhcpConf.equals(other.getDhcpConf()))) &&
            ((this.wertebereich==null && other.getWertebereich()==null) || 
             (this.wertebereich!=null &&
              this.wertebereich.equals(other.getWertebereich()))) &&
            ((this.optionEncoding==null && other.getOptionEncoding()==null) || 
             (this.optionEncoding!=null &&
              this.optionEncoding.equals(other.getOptionEncoding())));
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
        if (getGuiAttributeID() != null) {
            _hashCode += getGuiAttributeID().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        if (getDhcpConf() != null) {
            _hashCode += getDhcpConf().hashCode();
        }
        if (getWertebereich() != null) {
            _hashCode += getWertebereich().hashCode();
        }
        if (getOptionEncoding() != null) {
            _hashCode += getOptionEncoding().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiAttribute/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("guiAttributeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiAttribute/Messages", "GuiAttributeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiAttribute/Messages", "Name"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("dhcpConf");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiAttribute/Messages", "DhcpConf"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("wertebereich");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiAttribute/Messages", "Wertebereich"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("optionEncoding");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiAttribute/Messages", "OptionEncoding"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
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
