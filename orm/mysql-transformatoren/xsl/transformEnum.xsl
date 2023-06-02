<!--
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
-->
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" version="1.0">
<xsl:output method="text" encoding="iso-8859-1" />
<xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
<xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
<xsl:variable name="primaryKey"></xsl:variable>

<xsl:template match="DBObject">

<xsl:text>
// ************************************************************************
// ** Enums fuer Object </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if> (<xsl:value-of select="@type"/>
<xsl:text>) **
// ************************************************************************
</xsl:text>

<!-- ********************************** Enums fuer Attribute *************** -->

<xsl:for-each select="Column"> 
  <xsl:if test="not(string-length(@enumName)=0)">
<xsl:text>  public enum </xsl:text><xsl:value-of select="@enumName"/>
<xsl:text> implements gip.base.common.OBEnumInterface {
</xsl:text>
<xsl:for-each select="AllowedValue">
<xsl:text>    </xsl:text>
<xsl:if test="../@javaType='enum'"><xsl:value-of select="@value"/></xsl:if>
<xsl:if test="../@javaType='intEnum'"><xsl:value-of select="@name"/>(<xsl:value-of select="@value"/>)</xsl:if>
<xsl:if test="../@javaType='longEnum'"><xsl:value-of select="@name"/>(<xsl:value-of select="@value"/>)</xsl:if>
<xsl:if test="../@javaType='booleanEnum'"><xsl:value-of select="@name"/>(<xsl:value-of select="@value"/>)</xsl:if>
<xsl:if test="../@javaType='stringEnum'"><xsl:value-of select="@name"/>("<xsl:value-of select="@value"/>")</xsl:if>
<xsl:if test="not(position()=last())"><xsl:text>,
</xsl:text></xsl:if>
<xsl:if test="position()=last()"><xsl:text>;
</xsl:text></xsl:if>
</xsl:for-each>
<xsl:if test="@javaType='intEnum'"><xsl:text>    private int _dbVal;
    private </xsl:text><xsl:value-of select="@enumName"/><xsl:text>(int dbVal) {
       this._dbVal=dbVal;
    }
    public int getDbVal() {
      return this._dbVal;
    }
    public static int staticGetDbVal(</xsl:text><xsl:value-of select="@enumName"/><xsl:text> value) {
      if (value==null) {
        return gip.base.common.OBAttribute.NULL;
      }
      return value.getDbVal();
    }
    public static </xsl:text><xsl:value-of select="@enumName"/><xsl:text> staticFromDbVal(int value) throws gip.base.common.OBException {
      if (value==gip.base.common.OBAttribute.NULL) {
        return null;
      }
      for( </xsl:text><xsl:value-of select="@enumName"/><xsl:text> t : values() ) {
        if( t._dbVal == value ) {
          return t;
        }
      }
      throw new gip.base.common.OBException(gip.base.common.OBException.OBErrorNumber.noEnumValue1, 
                                            new String[] {String.valueOf(value)});
    }
    public String getStringValue() {
      return String.valueOf(getDbVal());
    }
    public boolean getUseQuotes() {
      return false;
    }
</xsl:text></xsl:if>
<xsl:if test="@javaType='longEnum'"><xsl:text>    private long _dbVal;
    private </xsl:text><xsl:value-of select="@enumName"/><xsl:text>(long dbVal) {
      this._dbVal=dbVal;
    }
    public long getDbVal() {
      return this._dbVal;
    }
    public static long staticGetDbVal(</xsl:text><xsl:value-of select="@enumName"/><xsl:text> value) {
      if (value==null) {
        return gip.base.common.OBAttribute.NULL;
      }
      return value.getDbVal();
    }
    public static </xsl:text><xsl:value-of select="@enumName"/><xsl:text> staticFromDbVal(long value) throws gip.base.common.OBException {
      if (value==gip.base.common.OBAttribute.NULL) {
        return null;
      }
      for(</xsl:text><xsl:value-of select="@enumName"/><xsl:text> t : values() ) {
        if( t._dbVal == value ) {
          return t;
        }
      }
      throw new gip.base.common.OBException(gip.base.common.OBException.OBErrorNumber.noEnumValue1, 
                                            new String[] {String.valueOf(value)});
    }
    public String getStringValue() {
      return String.valueOf(getDbVal());
    }
    public boolean getUseQuotes() {
      return false;
    }
</xsl:text></xsl:if>
<xsl:if test="@javaType='booleanEnum'"><xsl:text>    private boolean _dbVal;
    private </xsl:text><xsl:value-of select="@enumName"/><xsl:text>boolean dbVal) {
       this._dbVal=dbVal;
    }
    public boolean getDbVal() {
      return this._dbVal;
    }
    public static boolean staticGetDbVal(</xsl:text><xsl:value-of select="@enumName"/><xsl:text> value) {
      if (value==null) {
        return false;
      }
      return value.getDbVal();
    }
    public static </xsl:text><xsl:value-of select="@enumName"/><xsl:text> staticFromDbVal(boolean value) throws gip.base.common.OBException {
      for(</xsl:text><xsl:value-of select="@enumName"/><xsl:text> t : values() ) {
        if( t._dbVal == value ) {
          return t;
        }
      }
      throw new gip.base.common.OBException(gip.base.common.OBException.OBErrorNumber.noEnumValue1, 
                                            new String[] {String.valueOf(value)});
    }
    public String getStringValue() {
      return String.valueOf(getDbVal());
    }
    public boolean getUseQuotes() {
      return false;
    }
</xsl:text></xsl:if>
<xsl:if test="@javaType='stringEnum'"><xsl:text>    private String _dbVal;
    private </xsl:text><xsl:value-of select="@enumName"/><xsl:text>(String dbVal) {
       this._dbVal=dbVal;
    }
    public String getDbVal() {
      return this._dbVal;
    }
    public static String staticGetDbVal(</xsl:text><xsl:value-of select="@enumName"/><xsl:text> value) {
      if (value==null) {
        return "";
      }
      return value.getDbVal();
    }
    public static </xsl:text><xsl:value-of select="@enumName"/><xsl:text> staticFromDbVal(String value) throws gip.base.common.OBException {
      if (value==null || value.length()==0) {
        return null;
      }
      for(</xsl:text><xsl:value-of select="@enumName"/><xsl:text> t : values() ) {
        if( t._dbVal.equals(value)) {
          return t;
        }
      }
      throw new gip.base.common.OBException(gip.base.common.OBException.OBErrorNumber.noEnumValue1, 
                                            new String[] {value});
    }
    public String getStringValue() {
      return getDbVal();
    }
    public boolean getUseQuotes() {
      return true;
    }
</xsl:text></xsl:if>
<xsl:if test="@javaType='enum'"><xsl:text>
    public static String staticName(</xsl:text><xsl:value-of select="@enumName"/><xsl:text> value) {
      if (value==null) {
        return "";
      }
      return value.name();
    }
    public String getStringValue() {
      return name();
    }
    public boolean getUseQuotes() {
      return true;
    }
</xsl:text></xsl:if>
<xsl:text>  }
</xsl:text>
  </xsl:if>
</xsl:for-each>

</xsl:template>
</xsl:stylesheet>
