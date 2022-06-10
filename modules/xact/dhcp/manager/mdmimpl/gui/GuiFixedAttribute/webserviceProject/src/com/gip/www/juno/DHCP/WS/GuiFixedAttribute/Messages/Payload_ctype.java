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
 * Payload_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages;

public class Payload_ctype  implements java.io.Serializable {
    private com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] metaInfoOutput;

    private com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype[] getAllRowsOutput;

    private com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype[] searchRowsOutput;

    private com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype updateRowOutput;

    private com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype insertRowOutput;

    private java.lang.String deleteRowsOutput;

    private java.lang.String countAllRowsOutput;

    private java.lang.String countRowsWithConditionOutput;

    public Payload_ctype() {
    }

    public Payload_ctype(
           com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] metaInfoOutput,
           com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype[] getAllRowsOutput,
           com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype[] searchRowsOutput,
           com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype updateRowOutput,
           com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype insertRowOutput,
           java.lang.String deleteRowsOutput,
           java.lang.String countAllRowsOutput,
           java.lang.String countRowsWithConditionOutput) {
           this.metaInfoOutput = metaInfoOutput;
           this.getAllRowsOutput = getAllRowsOutput;
           this.searchRowsOutput = searchRowsOutput;
           this.updateRowOutput = updateRowOutput;
           this.insertRowOutput = insertRowOutput;
           this.deleteRowsOutput = deleteRowsOutput;
           this.countAllRowsOutput = countAllRowsOutput;
           this.countRowsWithConditionOutput = countRowsWithConditionOutput;
    }


    /**
     * Gets the metaInfoOutput value for this Payload_ctype.
     * 
     * @return metaInfoOutput
     */
    public com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] getMetaInfoOutput() {
        return metaInfoOutput;
    }


    /**
     * Sets the metaInfoOutput value for this Payload_ctype.
     * 
     * @param metaInfoOutput
     */
    public void setMetaInfoOutput(com.gip.www.juno.Gui.WS.Messages.MetaInfoRow_ctype[] metaInfoOutput) {
        this.metaInfoOutput = metaInfoOutput;
    }


    /**
     * Gets the getAllRowsOutput value for this Payload_ctype.
     * 
     * @return getAllRowsOutput
     */
    public com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype[] getGetAllRowsOutput() {
        return getAllRowsOutput;
    }


    /**
     * Sets the getAllRowsOutput value for this Payload_ctype.
     * 
     * @param getAllRowsOutput
     */
    public void setGetAllRowsOutput(com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype[] getAllRowsOutput) {
        this.getAllRowsOutput = getAllRowsOutput;
    }


    /**
     * Gets the searchRowsOutput value for this Payload_ctype.
     * 
     * @return searchRowsOutput
     */
    public com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype[] getSearchRowsOutput() {
        return searchRowsOutput;
    }


    /**
     * Sets the searchRowsOutput value for this Payload_ctype.
     * 
     * @param searchRowsOutput
     */
    public void setSearchRowsOutput(com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype[] searchRowsOutput) {
        this.searchRowsOutput = searchRowsOutput;
    }


    /**
     * Gets the updateRowOutput value for this Payload_ctype.
     * 
     * @return updateRowOutput
     */
    public com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype getUpdateRowOutput() {
        return updateRowOutput;
    }


    /**
     * Sets the updateRowOutput value for this Payload_ctype.
     * 
     * @param updateRowOutput
     */
    public void setUpdateRowOutput(com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype updateRowOutput) {
        this.updateRowOutput = updateRowOutput;
    }


    /**
     * Gets the insertRowOutput value for this Payload_ctype.
     * 
     * @return insertRowOutput
     */
    public com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype getInsertRowOutput() {
        return insertRowOutput;
    }


    /**
     * Sets the insertRowOutput value for this Payload_ctype.
     * 
     * @param insertRowOutput
     */
    public void setInsertRowOutput(com.gip.www.juno.DHCP.WS.GuiFixedAttribute.Messages.Row_ctype insertRowOutput) {
        this.insertRowOutput = insertRowOutput;
    }


    /**
     * Gets the deleteRowsOutput value for this Payload_ctype.
     * 
     * @return deleteRowsOutput
     */
    public java.lang.String getDeleteRowsOutput() {
        return deleteRowsOutput;
    }


    /**
     * Sets the deleteRowsOutput value for this Payload_ctype.
     * 
     * @param deleteRowsOutput
     */
    public void setDeleteRowsOutput(java.lang.String deleteRowsOutput) {
        this.deleteRowsOutput = deleteRowsOutput;
    }


    /**
     * Gets the countAllRowsOutput value for this Payload_ctype.
     * 
     * @return countAllRowsOutput
     */
    public java.lang.String getCountAllRowsOutput() {
        return countAllRowsOutput;
    }


    /**
     * Sets the countAllRowsOutput value for this Payload_ctype.
     * 
     * @param countAllRowsOutput
     */
    public void setCountAllRowsOutput(java.lang.String countAllRowsOutput) {
        this.countAllRowsOutput = countAllRowsOutput;
    }


    /**
     * Gets the countRowsWithConditionOutput value for this Payload_ctype.
     * 
     * @return countRowsWithConditionOutput
     */
    public java.lang.String getCountRowsWithConditionOutput() {
        return countRowsWithConditionOutput;
    }


    /**
     * Sets the countRowsWithConditionOutput value for this Payload_ctype.
     * 
     * @param countRowsWithConditionOutput
     */
    public void setCountRowsWithConditionOutput(java.lang.String countRowsWithConditionOutput) {
        this.countRowsWithConditionOutput = countRowsWithConditionOutput;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof Payload_ctype)) return false;
        Payload_ctype other = (Payload_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            ((this.metaInfoOutput==null && other.getMetaInfoOutput()==null) || 
             (this.metaInfoOutput!=null &&
              java.util.Arrays.equals(this.metaInfoOutput, other.getMetaInfoOutput()))) &&
            ((this.getAllRowsOutput==null && other.getGetAllRowsOutput()==null) || 
             (this.getAllRowsOutput!=null &&
              java.util.Arrays.equals(this.getAllRowsOutput, other.getGetAllRowsOutput()))) &&
            ((this.searchRowsOutput==null && other.getSearchRowsOutput()==null) || 
             (this.searchRowsOutput!=null &&
              java.util.Arrays.equals(this.searchRowsOutput, other.getSearchRowsOutput()))) &&
            ((this.updateRowOutput==null && other.getUpdateRowOutput()==null) || 
             (this.updateRowOutput!=null &&
              this.updateRowOutput.equals(other.getUpdateRowOutput()))) &&
            ((this.insertRowOutput==null && other.getInsertRowOutput()==null) || 
             (this.insertRowOutput!=null &&
              this.insertRowOutput.equals(other.getInsertRowOutput()))) &&
            ((this.deleteRowsOutput==null && other.getDeleteRowsOutput()==null) || 
             (this.deleteRowsOutput!=null &&
              this.deleteRowsOutput.equals(other.getDeleteRowsOutput()))) &&
            ((this.countAllRowsOutput==null && other.getCountAllRowsOutput()==null) || 
             (this.countAllRowsOutput!=null &&
              this.countAllRowsOutput.equals(other.getCountAllRowsOutput()))) &&
            ((this.countRowsWithConditionOutput==null && other.getCountRowsWithConditionOutput()==null) || 
             (this.countRowsWithConditionOutput!=null &&
              this.countRowsWithConditionOutput.equals(other.getCountRowsWithConditionOutput())));
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
        if (getMetaInfoOutput() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getMetaInfoOutput());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getMetaInfoOutput(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getGetAllRowsOutput() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getGetAllRowsOutput());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getGetAllRowsOutput(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getSearchRowsOutput() != null) {
            for (int i=0;
                 i<java.lang.reflect.Array.getLength(getSearchRowsOutput());
                 i++) {
                java.lang.Object obj = java.lang.reflect.Array.get(getSearchRowsOutput(), i);
                if (obj != null &&
                    !obj.getClass().isArray()) {
                    _hashCode += obj.hashCode();
                }
            }
        }
        if (getUpdateRowOutput() != null) {
            _hashCode += getUpdateRowOutput().hashCode();
        }
        if (getInsertRowOutput() != null) {
            _hashCode += getInsertRowOutput().hashCode();
        }
        if (getDeleteRowsOutput() != null) {
            _hashCode += getDeleteRowsOutput().hashCode();
        }
        if (getCountAllRowsOutput() != null) {
            _hashCode += getCountAllRowsOutput().hashCode();
        }
        if (getCountRowsWithConditionOutput() != null) {
            _hashCode += getCountRowsWithConditionOutput().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Payload_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "Payload_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("metaInfoOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "MetaInfoOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "MetaInfoRow_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        elemField.setItemQName(new javax.xml.namespace.QName("http://www.gip.com/juno/Gui/WS/Messages", "col"));
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("getAllRowsOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "GetAllRowsOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "Row"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("searchRowsOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "SearchRowsOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "Row"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("updateRowOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "UpdateRowOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "Row_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("insertRowOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "InsertRowOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "Row_ctype"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("deleteRowsOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "DeleteRowsOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("countAllRowsOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "CountAllRowsOutput"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("countRowsWithConditionOutput");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/GuiFixedAttribute/Messages", "CountRowsWithConditionOutput"));
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
