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

package com.gip.www.juno.DHCP.WS.Pool.Messages;

public class Row_ctype  implements java.io.Serializable {
    private java.lang.String poolID;

    private java.lang.String subnetID;

    private java.lang.String subnet;

    private java.lang.String poolTypeID;

    private java.lang.String poolType;

    private java.lang.String rangeStart;

    private java.lang.String rangeStop;

    private java.lang.String targetState;

    private java.lang.String isDeployed;

    private java.lang.String useForStatistics;

    private java.lang.String exclusions;

    private java.lang.String migrationState;

    public Row_ctype() {
    }

    public Row_ctype(
           java.lang.String poolID,
           java.lang.String subnetID,
           java.lang.String subnet,
           java.lang.String poolTypeID,
           java.lang.String poolType,
           java.lang.String rangeStart,
           java.lang.String rangeStop,
           java.lang.String targetState,
           java.lang.String isDeployed,
           java.lang.String useForStatistics,
           java.lang.String exclusions,
           java.lang.String migrationState) {
           this.poolID = poolID;
           this.subnetID = subnetID;
           this.subnet = subnet;
           this.poolTypeID = poolTypeID;
           this.poolType = poolType;
           this.rangeStart = rangeStart;
           this.rangeStop = rangeStop;
           this.targetState = targetState;
           this.isDeployed = isDeployed;
           this.useForStatistics = useForStatistics;
           this.exclusions = exclusions;
           this.migrationState = migrationState;
    }


    /**
     * Gets the poolID value for this Row_ctype.
     * 
     * @return poolID
     */
    public java.lang.String getPoolID() {
        return poolID;
    }


    /**
     * Sets the poolID value for this Row_ctype.
     * 
     * @param poolID
     */
    public void setPoolID(java.lang.String poolID) {
        this.poolID = poolID;
    }


    /**
     * Gets the subnetID value for this Row_ctype.
     * 
     * @return subnetID
     */
    public java.lang.String getSubnetID() {
        return subnetID;
    }


    /**
     * Sets the subnetID value for this Row_ctype.
     * 
     * @param subnetID
     */
    public void setSubnetID(java.lang.String subnetID) {
        this.subnetID = subnetID;
    }


    /**
     * Gets the subnet value for this Row_ctype.
     * 
     * @return subnet
     */
    public java.lang.String getSubnet() {
        return subnet;
    }


    /**
     * Sets the subnet value for this Row_ctype.
     * 
     * @param subnet
     */
    public void setSubnet(java.lang.String subnet) {
        this.subnet = subnet;
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
     * Gets the poolType value for this Row_ctype.
     * 
     * @return poolType
     */
    public java.lang.String getPoolType() {
        return poolType;
    }


    /**
     * Sets the poolType value for this Row_ctype.
     * 
     * @param poolType
     */
    public void setPoolType(java.lang.String poolType) {
        this.poolType = poolType;
    }


    /**
     * Gets the rangeStart value for this Row_ctype.
     * 
     * @return rangeStart
     */
    public java.lang.String getRangeStart() {
        return rangeStart;
    }


    /**
     * Sets the rangeStart value for this Row_ctype.
     * 
     * @param rangeStart
     */
    public void setRangeStart(java.lang.String rangeStart) {
        this.rangeStart = rangeStart;
    }


    /**
     * Gets the rangeStop value for this Row_ctype.
     * 
     * @return rangeStop
     */
    public java.lang.String getRangeStop() {
        return rangeStop;
    }


    /**
     * Sets the rangeStop value for this Row_ctype.
     * 
     * @param rangeStop
     */
    public void setRangeStop(java.lang.String rangeStop) {
        this.rangeStop = rangeStop;
    }


    /**
     * Gets the targetState value for this Row_ctype.
     * 
     * @return targetState
     */
    public java.lang.String getTargetState() {
        return targetState;
    }


    /**
     * Sets the targetState value for this Row_ctype.
     * 
     * @param targetState
     */
    public void setTargetState(java.lang.String targetState) {
        this.targetState = targetState;
    }


    /**
     * Gets the isDeployed value for this Row_ctype.
     * 
     * @return isDeployed
     */
    public java.lang.String getIsDeployed() {
        return isDeployed;
    }


    /**
     * Sets the isDeployed value for this Row_ctype.
     * 
     * @param isDeployed
     */
    public void setIsDeployed(java.lang.String isDeployed) {
        this.isDeployed = isDeployed;
    }


    /**
     * Gets the useForStatistics value for this Row_ctype.
     * 
     * @return useForStatistics
     */
    public java.lang.String getUseForStatistics() {
        return useForStatistics;
    }


    /**
     * Sets the useForStatistics value for this Row_ctype.
     * 
     * @param useForStatistics
     */
    public void setUseForStatistics(java.lang.String useForStatistics) {
        this.useForStatistics = useForStatistics;
    }


    /**
     * Gets the exclusions value for this Row_ctype.
     * 
     * @return exclusions
     */
    public java.lang.String getExclusions() {
        return exclusions;
    }


    /**
     * Sets the exclusions value for this Row_ctype.
     * 
     * @param exclusions
     */
    public void setExclusions(java.lang.String exclusions) {
        this.exclusions = exclusions;
    }


    /**
     * Gets the migrationState value for this Row_ctype.
     * 
     * @return migrationState
     */
    public java.lang.String getMigrationState() {
        return migrationState;
    }


    /**
     * Sets the migrationState value for this Row_ctype.
     * 
     * @param migrationState
     */
    public void setMigrationState(java.lang.String migrationState) {
        this.migrationState = migrationState;
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
            ((this.poolID==null && other.getPoolID()==null) || 
             (this.poolID!=null &&
              this.poolID.equals(other.getPoolID()))) &&
            ((this.subnetID==null && other.getSubnetID()==null) || 
             (this.subnetID!=null &&
              this.subnetID.equals(other.getSubnetID()))) &&
            ((this.subnet==null && other.getSubnet()==null) || 
             (this.subnet!=null &&
              this.subnet.equals(other.getSubnet()))) &&
            ((this.poolTypeID==null && other.getPoolTypeID()==null) || 
             (this.poolTypeID!=null &&
              this.poolTypeID.equals(other.getPoolTypeID()))) &&
            ((this.poolType==null && other.getPoolType()==null) || 
             (this.poolType!=null &&
              this.poolType.equals(other.getPoolType()))) &&
            ((this.rangeStart==null && other.getRangeStart()==null) || 
             (this.rangeStart!=null &&
              this.rangeStart.equals(other.getRangeStart()))) &&
            ((this.rangeStop==null && other.getRangeStop()==null) || 
             (this.rangeStop!=null &&
              this.rangeStop.equals(other.getRangeStop()))) &&
            ((this.targetState==null && other.getTargetState()==null) || 
             (this.targetState!=null &&
              this.targetState.equals(other.getTargetState()))) &&
            ((this.isDeployed==null && other.getIsDeployed()==null) || 
             (this.isDeployed!=null &&
              this.isDeployed.equals(other.getIsDeployed()))) &&
            ((this.useForStatistics==null && other.getUseForStatistics()==null) || 
             (this.useForStatistics!=null &&
              this.useForStatistics.equals(other.getUseForStatistics()))) &&
            ((this.exclusions==null && other.getExclusions()==null) || 
             (this.exclusions!=null &&
              this.exclusions.equals(other.getExclusions()))) &&
            ((this.migrationState==null && other.getMigrationState()==null) || 
             (this.migrationState!=null &&
              this.migrationState.equals(other.getMigrationState())));
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
        if (getPoolID() != null) {
            _hashCode += getPoolID().hashCode();
        }
        if (getSubnetID() != null) {
            _hashCode += getSubnetID().hashCode();
        }
        if (getSubnet() != null) {
            _hashCode += getSubnet().hashCode();
        }
        if (getPoolTypeID() != null) {
            _hashCode += getPoolTypeID().hashCode();
        }
        if (getPoolType() != null) {
            _hashCode += getPoolType().hashCode();
        }
        if (getRangeStart() != null) {
            _hashCode += getRangeStart().hashCode();
        }
        if (getRangeStop() != null) {
            _hashCode += getRangeStop().hashCode();
        }
        if (getTargetState() != null) {
            _hashCode += getTargetState().hashCode();
        }
        if (getIsDeployed() != null) {
            _hashCode += getIsDeployed().hashCode();
        }
        if (getUseForStatistics() != null) {
            _hashCode += getUseForStatistics().hashCode();
        }
        if (getExclusions() != null) {
            _hashCode += getExclusions().hashCode();
        }
        if (getMigrationState() != null) {
            _hashCode += getMigrationState().hashCode();
        }
        __hashCodeCalc = false;
        return _hashCode;
    }

    // Type metadata
    private static org.apache.axis.description.TypeDesc typeDesc =
        new org.apache.axis.description.TypeDesc(Row_ctype.class, true);

    static {
        typeDesc.setXmlType(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "Row_ctype"));
        org.apache.axis.description.ElementDesc elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "PoolID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnetID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "SubnetID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("subnet");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "Subnet"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolTypeID");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "PoolTypeID"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("poolType");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "PoolType"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("rangeStart");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "RangeStart"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("rangeStop");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "RangeStop"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("targetState");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "TargetState"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("isDeployed");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "IsDeployed"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("useForStatistics");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "UseForStatistics"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("exclusions");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "Exclusions"));
        elemField.setXmlType(new javax.xml.namespace.QName("http://www.w3.org/2001/XMLSchema", "string"));
        elemField.setMinOccurs(0);
        elemField.setNillable(false);
        typeDesc.addFieldDesc(elemField);
        elemField = new org.apache.axis.description.ElementDesc();
        elemField.setFieldName("migrationState");
        elemField.setXmlName(new javax.xml.namespace.QName("http://www.gip.com/juno/DHCP/WS/Pool/Messages", "MigrationState"));
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
