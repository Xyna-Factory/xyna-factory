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
 * MetaInfoRow_ctype.java
 *
 * This file was auto-generated from WSDL
 * by the Apache Axis 1.4 Apr 22, 2006 (06:55:48 PDT) WSDL2Java emitter.
 */

package com.gip.www.juno.DHCP.WS.Optionsv4.Messages;

public class MetaInfoRow_ctype  implements java.io.Serializable {
    private boolean visible;  // attribute

    private boolean updates;  // attribute

    private java.lang.String guiname;  // attribute

    private java.lang.String colname;  // attribute

    private java.math.BigInteger colnum;  // attribute

    private java.lang.String childtable;  // attribute

    private java.lang.String parenttable;  // attribute

    private java.lang.String parentcol;  // attribute

    private java.lang.String inputType;  // attribute

    private java.lang.String inputFormat;  // attribute

    private java.lang.String optional;  // attribute

    public MetaInfoRow_ctype() {
    }

    public MetaInfoRow_ctype(
           boolean visible,
           boolean updates,
           java.lang.String guiname,
           java.lang.String colname,
           java.math.BigInteger colnum,
           java.lang.String childtable,
           java.lang.String parenttable,
           java.lang.String parentcol,
           java.lang.String inputType,
           java.lang.String inputFormat,
           java.lang.String optional) {
           this.visible = visible;
           this.updates = updates;
           this.guiname = guiname;
           this.colname = colname;
           this.colnum = colnum;
           this.childtable = childtable;
           this.parenttable = parenttable;
           this.parentcol = parentcol;
           this.inputType = inputType;
           this.inputFormat = inputFormat;
           this.optional = optional;
    }


    /**
     * Gets the visible value for this MetaInfoRow_ctype.
     * 
     * @return visible
     */
    public boolean isVisible() {
        return visible;
    }


    /**
     * Sets the visible value for this MetaInfoRow_ctype.
     * 
     * @param visible
     */
    public void setVisible(boolean visible) {
        this.visible = visible;
    }


    /**
     * Gets the updates value for this MetaInfoRow_ctype.
     * 
     * @return updates
     */
    public boolean isUpdates() {
        return updates;
    }


    /**
     * Sets the updates value for this MetaInfoRow_ctype.
     * 
     * @param updates
     */
    public void setUpdates(boolean updates) {
        this.updates = updates;
    }


    /**
     * Gets the guiname value for this MetaInfoRow_ctype.
     * 
     * @return guiname
     */
    public java.lang.String getGuiname() {
        return guiname;
    }


    /**
     * Sets the guiname value for this MetaInfoRow_ctype.
     * 
     * @param guiname
     */
    public void setGuiname(java.lang.String guiname) {
        this.guiname = guiname;
    }


    /**
     * Gets the colname value for this MetaInfoRow_ctype.
     * 
     * @return colname
     */
    public java.lang.String getColname() {
        return colname;
    }


    /**
     * Sets the colname value for this MetaInfoRow_ctype.
     * 
     * @param colname
     */
    public void setColname(java.lang.String colname) {
        this.colname = colname;
    }


    /**
     * Gets the colnum value for this MetaInfoRow_ctype.
     * 
     * @return colnum
     */
    public java.math.BigInteger getColnum() {
        return colnum;
    }


    /**
     * Sets the colnum value for this MetaInfoRow_ctype.
     * 
     * @param colnum
     */
    public void setColnum(java.math.BigInteger colnum) {
        this.colnum = colnum;
    }


    /**
     * Gets the childtable value for this MetaInfoRow_ctype.
     * 
     * @return childtable
     */
    public java.lang.String getChildtable() {
        return childtable;
    }


    /**
     * Sets the childtable value for this MetaInfoRow_ctype.
     * 
     * @param childtable
     */
    public void setChildtable(java.lang.String childtable) {
        this.childtable = childtable;
    }


    /**
     * Gets the parenttable value for this MetaInfoRow_ctype.
     * 
     * @return parenttable
     */
    public java.lang.String getParenttable() {
        return parenttable;
    }


    /**
     * Sets the parenttable value for this MetaInfoRow_ctype.
     * 
     * @param parenttable
     */
    public void setParenttable(java.lang.String parenttable) {
        this.parenttable = parenttable;
    }


    /**
     * Gets the parentcol value for this MetaInfoRow_ctype.
     * 
     * @return parentcol
     */
    public java.lang.String getParentcol() {
        return parentcol;
    }


    /**
     * Sets the parentcol value for this MetaInfoRow_ctype.
     * 
     * @param parentcol
     */
    public void setParentcol(java.lang.String parentcol) {
        this.parentcol = parentcol;
    }


    /**
     * Gets the inputType value for this MetaInfoRow_ctype.
     * 
     * @return inputType
     */
    public java.lang.String getInputType() {
        return inputType;
    }


    /**
     * Sets the inputType value for this MetaInfoRow_ctype.
     * 
     * @param inputType
     */
    public void setInputType(java.lang.String inputType) {
        this.inputType = inputType;
    }


    /**
     * Gets the inputFormat value for this MetaInfoRow_ctype.
     * 
     * @return inputFormat
     */
    public java.lang.String getInputFormat() {
        return inputFormat;
    }


    /**
     * Sets the inputFormat value for this MetaInfoRow_ctype.
     * 
     * @param inputFormat
     */
    public void setInputFormat(java.lang.String inputFormat) {
        this.inputFormat = inputFormat;
    }


    /**
     * Gets the optional value for this MetaInfoRow_ctype.
     * 
     * @return optional
     */
    public java.lang.String getOptional() {
        return optional;
    }


    /**
     * Sets the optional value for this MetaInfoRow_ctype.
     * 
     * @param optional
     */
    public void setOptional(java.lang.String optional) {
        this.optional = optional;
    }

    private java.lang.Object __equalsCalc = null;
    public synchronized boolean equals(java.lang.Object obj) {
        if (!(obj instanceof MetaInfoRow_ctype)) return false;
        MetaInfoRow_ctype other = (MetaInfoRow_ctype) obj;
        if (obj == null) return false;
        if (this == obj) return true;
        if (__equalsCalc != null) {
            return (__equalsCalc == obj);
        }
        __equalsCalc = obj;
        boolean _equals;
        _equals = true && 
            this.visible == other.isVisible() &&
            this.updates == other.isUpdates() &&
            ((this.guiname==null && other.getGuiname()==null) || 
             (this.guiname!=null &&
              this.guiname.equals(other.getGuiname()))) &&
            ((this.colname==null && other.getColname()==null) || 
             (this.colname!=null &&
              this.colname.equals(other.getColname()))) &&
            ((this.colnum==null && other.getColnum()==null) || 
             (this.colnum!=null &&
              this.colnum.equals(other.getColnum()))) &&
            ((this.childtable==null && other.getChildtable()==null) || 
             (this.childtable!=null &&
              this.childtable.equals(other.getChildtable()))) &&
            ((this.parenttable==null && other.getParenttable()==null) || 
             (this.parenttable!=null &&
              this.parenttable.equals(other.getParenttable()))) &&
            ((this.parentcol==null && other.getParentcol()==null) || 
             (this.parentcol!=null &&
              this.parentcol.equals(other.getParentcol()))) &&
            ((this.inputType==null && other.getInputType()==null) || 
             (this.inputType!=null &&
              this.inputType.equals(other.getInputType()))) &&
            ((this.inputFormat==null && other.getInputFormat()==null) || 
             (this.inputFormat!=null &&
              this.inputFormat.equals(other.getInputFormat()))) &&
            ((this.optional==null && other.getOptional()==null) || 
             (this.optional!=null &&
              this.optional.equals(other.getOptional())));
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
        _hashCode += (isVisible() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        _hashCode += (isUpdates() ? Boolean.TRUE : Boolean.FALSE).hashCode();
        if (getGuiname() != null) {
            _hashCode += getGuiname().hashCode();
        }
        if (getColname() != null) {
            _hashCode += getColname().hashCode();
        }
        if (getColnum() != null) {
            _hashCode += getColnum().hashCode();
        }
        if (getChildtable() != null) {
            _hashCode += getChildtable().hashCode();
        }
        if (getParenttable() != null) {
            _hashCode += getParenttable().hashCode();
        }
        if (getParentcol() != null) {
            _hashCode += getParentcol().hashCode();
        }
        if (getInputType() != null) {
            _hashCode += getInputType().hashCode();
        }
        if (getInputFormat() != null) {
            _hashCode += getInputFormat().hashCode();
        }
        if (getOptional() != null) {
            _hashCode += getOptional().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(MetaInfoRow_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Optionsv4/Messages", "MetaInfoRow_ctype"));
        org.apache.axis.description.AttributeDesc attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("visible");
        attrField.setXmlName(new javax.xml.namespace.QName("", "visible"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("updates");
        attrField.setXmlName(new javax.xml.namespace.QName("", "updates"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "boolean"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("guiname");
        attrField.setXmlName(new javax.xml.namespace.QName("", "guiname"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("colname");
        attrField.setXmlName(new javax.xml.namespace.QName("", "colname"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("colnum");
        attrField.setXmlName(new javax.xml.namespace.QName("", "colnum"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "integer"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("childtable");
        attrField.setXmlName(new javax.xml.namespace.QName("", "childtable"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("parenttable");
        attrField.setXmlName(new javax.xml.namespace.QName("", "parenttable"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("parentcol");
        attrField.setXmlName(new javax.xml.namespace.QName("", "parentcol"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("inputType");
        attrField.setXmlName(new javax.xml.namespace.QName("", "inputType"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("inputFormat");
        attrField.setXmlName(new javax.xml.namespace.QName("", "inputFormat"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
        attrField = new org.apache.axis.description.AttributeDesc();
        attrField.setFieldName("optional");
        attrField.setXmlName(new javax.xml.namespace.QName("", "optional"));
        attrField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        typeDesc.addFieldDesc(attrField);
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
