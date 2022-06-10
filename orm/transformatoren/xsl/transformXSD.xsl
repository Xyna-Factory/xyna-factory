<!--
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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
    <xsl:output method="text" encoding="iso-8859-1"/>
    
    <xsl:template match="DBObject">
    <!-- write header -->
    <xsl:text disable-output-escaping="yes">&lt;?xml version="1.0" encoding="windows-1252" ?></xsl:text>
    <xsl:text disable-output-escaping="yes">&lt;xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema"</xsl:text>
    <xsl:text disable-output-escaping="yes"> xmlns="http://www.MY-NAMESPACE.ORG"</xsl:text>
    <xsl:text disable-output-escaping="yes"> targetNamespace="http://www.MY-NAMESPACE.ORG"</xsl:text>
    <xsl:text disable-output-escaping="yes"> elementFormDefault="qualified"> </xsl:text>
    <xsl:text disable-output-escaping="yes">&lt;xsd:complexType name="NAME_ctype"&gt;</xsl:text>
    <xsl:text disable-output-escaping="yes">&lt;xsd:sequence&gt;</xsl:text>
    
    <!-- write first element AutomaticFormSave - required to press save and close forms afterwards. -->
    <xsl:text disable-output-escaping="yes">&lt;xsd:element name="AutomaticFormSave" type="xsd:int" minOccurs="0"&gt;</xsl:text>
    <!-- write entry for each element -->
        <xsl:for-each select="Column">
            <xsl:text disable-output-escaping="yes">&lt;xsd:element name="</xsl:text>
            <xsl:value-of select="@name"/>
            <xsl:text>" </xsl:text>
            <xsl:for-each select="@type"/>
            <xsl:if test="@type='INTEGER'">
                <xsl:text> type="xsd:int" minOccurs="0"/&gt; </xsl:text>
            </xsl:if>
            <xsl:if test="@type='VARCHAR2'">
                <xsl:text> type="xsd:string" minOccurs="0"/&gt; </xsl:text>
            </xsl:if>
            <xsl:if test="@type='DATE'">
                <xsl:text> type="xsd:string" minOccurs="0"/&gt; </xsl:text>
            </xsl:if>
            <xsl:if test="@type='TIMESTAMP WITH LOCAL TIME ZONE'">
                <xsl:text> type="xsd:string" minOccurs="0"/&gt; </xsl:text>
            </xsl:if>
        </xsl:for-each>
        
    <!-- write final lines -->
        <xsl:text disable-output-escaping="yes">&lt;/xsd:sequence&gt;</xsl:text>
        <xsl:text disable-output-escaping="yes">&lt;/xsd:complexType name="NAME_ctype"&gt;</xsl:text>
        <xsl:text disable-output-escaping="yes">&lt;xsd:element name="NAME" type="NAME_ctype"/&gt;</xsl:text>
        <xsl:text disable-output-escaping="yes">&lt;/xsd:schema></xsl:text>
    </xsl:template>
</xsl:stylesheet>



















