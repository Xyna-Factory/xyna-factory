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
-- Tabellen-ErrorMessages </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>
-- #############################################################################
</xsl:text>
<!-- PK, CHECK, UNIQUE-Constraints -->
<xsl:for-each select="Constraint">
<xsl:if test="string-length(@name)>30">
<xsl:message>Constraint-Name to long (>30): @name</xsl:message>
<xsl:message terminate="yes"></xsl:message>
</xsl:if>
<xsl:if test="not(string-length(@errorMessage)=0)">
<xsl:text>INSERT INTO ErrorMessage (errorMessageId,messageName,messageText,inDate,changeDate,remark, lockrow)
VALUES (keyErrorMessage.nextVal,UPPER('</xsl:text><xsl:value-of select="@name"/> 
<xsl:text>'),'</xsl:text><xsl:value-of select="@errorMessage"/> 
<xsl:text>',sysdate,sysdate,'</xsl:text><xsl:value-of select="../@name"/> 
<xsl:text>',-1); 
</xsl:text> 
</xsl:if>
</xsl:for-each>
<xsl:for-each select="Index">
<xsl:if test="not(string-length(@errorMessage)=0)">
<xsl:text>INSERT INTO ErrorMessage (errorMessageId,messageName,messageText,inDate,changeDate,remark, lockrow)
VALUES (keyErrorMessage.nextVal,UPPER('</xsl:text><xsl:value-of select="@name"/> 
<xsl:text>'),'</xsl:text><xsl:value-of select="@errorMessage"/> 
<xsl:text>',sysdate,sysdate,'</xsl:text><xsl:value-of select="../@name"/> 
<xsl:text>',-1); 
</xsl:text> 
</xsl:if>
</xsl:for-each>

<!-- NOT NULL - Constraints -->
</xsl:template>
</xsl:stylesheet>
