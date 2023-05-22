<!--
 * - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
 * Copyright 2023 Xyna GmbH, Germany
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

<xsl:template match="DBObject"><xsl:text>
#############################################################################
## Fehler zu Objekt </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
#############################################################################
</xsl:text>

<xsl:for-each select="Constraint">
  <xsl:if test="string-length(@name)>30">
    <xsl:message>Constraint-Name to long (>30): @name</xsl:message>
  <xsl:message terminate="yes"></xsl:message>
  </xsl:if>

  <!-- PK, CHECK, UNIQUE-Constraints -->
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
    <xsl:text>=</xsl:text><xsl:value-of select="@errorMessage"/><xsl:text>
</xsl:text>
  </xsl:if>

  <!-- FK-Constraints -->
  <xsl:for-each select="ErrorMessage">
    <xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="@suffix"/>
    <xsl:text>=</xsl:text><xsl:value-of select="@message"/><xsl:text>
</xsl:text>
  </xsl:for-each>
</xsl:for-each>

<!-- NN-Constraints -->
<xsl:for-each select="Column">
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
      <xsl:text>=</xsl:text><xsl:value-of select="@errorMessage"/><xsl:text>
</xsl:text>
  </xsl:if>
</xsl:for-each>

<!-- Index -->
<xsl:for-each select="Index">
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
      <xsl:text>=</xsl:text><xsl:value-of select="@errorMessage"/><xsl:text>
</xsl:text>
  </xsl:if>
</xsl:for-each>

<!-- Sonstige Fehler -->
<xsl:for-each select="ErrorMessage">
  <xsl:if test="not(string-length(@message)=0)">
    <xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
      <xsl:text>=</xsl:text><xsl:value-of select="@message"/><xsl:text>
</xsl:text>
  </xsl:if>
</xsl:for-each>

</xsl:template>
</xsl:stylesheet>
