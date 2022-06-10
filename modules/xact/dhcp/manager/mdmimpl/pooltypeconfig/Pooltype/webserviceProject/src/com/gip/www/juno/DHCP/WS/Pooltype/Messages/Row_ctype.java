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

package com.gip.www.juno.DHCP.WS.Pooltype.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String poolTypeID;

    private java.lang.String name;

    private java.lang.String classes;

    private java.lang.String classIDs;

    private java.lang.String negation;

    private java.lang.String attributes;

    private java.lang.String fixedAttributes;

    private java.lang.String isDefault;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String poolTypeID,
           java.lang.String name,
           java.lang.String classes,
           java.lang.String classIDs,
           java.lang.String negation,
           java.lang.String attributes,
           java.lang.String fixedAttributes,
           java.lang.String isDefault) {
           this.poolTypeID = poolTypeID;
           this.name = name;
           this.classes = classes;
           this.classIDs = classIDs;
           this.negation = negation;
           this.attributes = attributes;
           this.fixedAttributes = fixedAttributes;
           this.isDefault = isDefault;
    }


    /**
     * Gets the poolTypeID value for this Row_ctype.
     * 
     * @return poolTypeID
     */
    public java.lang.String getPoolTypeID() {
        return poolTypeID;
    }


    /**
     * Sets the poolTypeID value for this Row_ctype.
     * 
     * @param poolTypeID
     */
    public void setPoolTypeID(java.lang.String poolTypeID) {
        this.poolTypeID = poolTypeID;
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
     * Gets the classes value for this Row_ctype.
     * 
     * @return classes
     */
    public java.lang.String getClasses() {
        return classes;
    }


    /**
     * Sets the classes value for this Row_ctype.
     * 
     * @param classes
     */
    public void setClasses(java.lang.String classes) {
        this.classes = classes;
    }


    /**
     * Gets the classIDs value for this Row_ctype.
     * 
     * @return classIDs
     */
    public java.lang.String getClassIDs() {
        return classIDs;
    }


    /**
     * Sets the classIDs value for this Row_ctype.
     * 
     * @param classIDs
     */
    public void setClassIDs(java.lang.String classIDs) {
        this.classIDs = classIDs;
    }


    /**
     * Gets the negation value for this Row_ctype.
     * 
     * @return negation
     */
    public java.lang.String getNegation() {
        return negation;
    }


    /**
     * Sets the negation value for this Row_ctype.
     * 
     * @param negation
     */
    public void setNegation(java.lang.String negation) {
        this.negation = negation;
    }


    /**
     * Gets the attributes value for this Row_ctype.
     * 
     * @return attributes
     */
    public java.lang.String getAttributes() {
        return attributes;
    }


    /**
     * Sets the attributes value for this Row_ctype.
     * 
     * @param attributes
     */
    public void setAttributes(java.lang.String attributes) {
        this.attributes = attributes;
    }


    /**
     * Gets the fixedAttributes value for this Row_ctype.
     * 
     * @return fixedAttributes
     */
    public java.lang.String getFixedAttributes() {
        return fixedAttributes;
    }


    /**
     * Sets the fixedAttributes value for this Row_ctype.
     * 
     * @param fixedAttributes
     */
    public void setFixedAttributes(java.lang.String fixedAttributes) {
        this.fixedAttributes = fixedAttributes;
    }


    /**
     * Gets the isDefault value for this Row_ctype.
     * 
     * @return isDefault
     */
    public java.lang.String getIsDefault() {
        return isDefault;
    }


    /**
     * Sets the isDefault value for this Row_ctype.
     * 
     * @param isDefault
     */
    public void setIsDefault(java.lang.String isDefault) {
        this.isDefault = isDefault;
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
            ((this.poolTypeID==null && other.getPoolTypeID()==null) || 
             (this.poolTypeID!=null &&
              this.poolTypeID.equals(other.getPoolTypeID()))) &&
            ((this.name==null && other.getName()==null) || 
             (this.name!=null &&
              this.name.equals(other.getName()))) &&
            ((this.classes==null && other.getClasses()==null) || 
             (this.classes!=null &&
              this.classes.equals(other.getClasses()))) &&
            ((this.classIDs==null && other.getClassIDs()==null) || 
             (this.classIDs!=null &&
              this.classIDs.equals(other.getClassIDs()))) &&
            ((this.negation==null && other.getNegation()==null) || 
             (this.negation!=null &&
              this.negation.equals(other.getNegation()))) &&
            ((this.attributes==null && other.getAttributes()==null) || 
             (this.attributes!=null &&
              this.attributes.equals(other.getAttributes()))) &&
            ((this.fixedAttributes==null && other.getFixedAttributes()==null) || 
             (this.fixedAttributes!=null &&
              this.fixedAttributes.equals(other.getFixedAttributes()))) &&
            ((this.isDefault==null && other.getIsDefault()==null) || 
             (this.isDefault!=null &&
              this.isDefault.equals(other.getIsDefault())));
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
        if (getPoolTypeID() != null) {
            _hashCode += getPoolTypeID().hashCode();
        }
        if (getName() != null) {
            _hashCode += getName().hashCode();
        }
        if (getClasses() != null) {
            _hashCode += getClasses().hashCode();
        }
        if (getClassIDs() != null) {
            _hashCode += getClassIDs().hashCode();
        }
        if (getNegation() != null) {
            _hashCode += getNegation().hashCode();
        }
        if (getAttributes() != null) {
            _hashCode += getAttributes().hashCode();
        }
        if (getFixedAttributes() != null) {
            _hashCode += getFixedAttributes().hashCode();
        }
        if (getIsDefault() != null) {
            _hashCode += getIsDefault().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolTypeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "PoolTypeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("name");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "Name"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("classes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "Classes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("classIDs");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "ClassIDs"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("negation");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "Negation"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("attributes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "Attributes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("fixedAttributes");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "FixedAttributes"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("isDefault");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pooltype/Messages", "IsDefault"));
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
