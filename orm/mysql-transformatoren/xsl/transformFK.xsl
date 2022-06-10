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
<xsl:output method="text" encoding="iso-8859-1" />
<xsl:template match="DBObject"><xsl:text>
-- #############################################################################
-- Tabellen-Foreign-Keys </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
-- #############################################################################</xsl:text>
<xsl:for-each select="Constraint">
<xsl:if test="@type='FK'">
ALTER TABLE <xsl:value-of select="../@name"/> <xsl:text> ADD CONSTRAINT </xsl:text>
  <xsl:value-of select="@name"/> FOREIGN KEY (<xsl:value-of select="@columnList"/>) 
  REFERENCES <xsl:value-of select="@refTable"/>(<xsl:value-of select="@refcolumnList"/><xsl:text>)</xsl:text>
  <xsl:if test="@cascade='Y'">
  <xsl:text> ON DELETE CASCADE</xsl:text></xsl:if>
  <xsl:if test="@deferrable='Y'"> DEFERRABLE</xsl:if>
  <xsl:if test="string-length(@initially)>0"> INITIALLY <xsl:value-of select="@initially"/></xsl:if>
  <xsl:text>;</xsl:text>
</xsl:if>
</xsl:for-each>
</xsl:template>
</xsl:stylesheet>

