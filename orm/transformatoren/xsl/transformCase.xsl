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

<xsl:template match="DBObject"><xsl:text>
-- #############################################################################
-- Capapilities zur DTO </xsl:text><xsl:value-of select="@tkName"/><xsl:text>
-- #############################################################################
</xsl:text>

<xsl:for-each select="Capability">
<xsl:if test="@type='DELETE'">
<xsl:text>
INSERT INTO Case(caseId, capabilityId, netTypeContextId, lockRow, changeDate, inDate)
  SELECT netTypeContextId*100000+</xsl:text><xsl:value-of select="../@id"/><xsl:text>00+30, </xsl:text>
        <xsl:value-of select="../@id"/><xsl:text>00+30, netTypeContextId, -1, sysdate, sysdate 
    FROM NetTypeContext WHERE netTypeContextId IN (-1</xsl:text>
<xsl:if test="string-length(NetTypeContext/@ntId)=0">
<xsl:for-each select="../NetTypeContext">
<xsl:text>, </xsl:text><xsl:value-of select="@ntId"/>
</xsl:for-each>
</xsl:if>
<xsl:if test="string-length(NetTypeContext/@ntId)>0">
<xsl:for-each select="NetTypeContext">
<xsl:text>, </xsl:text><xsl:value-of select="@ntId"/>
</xsl:for-each>
</xsl:if>
<xsl:text>);
</xsl:text>
</xsl:if>
<xsl:if test="@isChecked='yes' or @isChecked='true' or @type='GUI'">
<xsl:text>
INSERT INTO Case(caseId, capabilityId, netTypeContextId, lockRow, changeDate, inDate)
  SELECT netTypeContextId*100000+</xsl:text><xsl:value-of select="../@id"/><xsl:text>00+</xsl:text><xsl:value-of select="@id"/><xsl:text>, </xsl:text>
        <xsl:value-of select="../@id"/><xsl:text>00+</xsl:text><xsl:value-of select="@id"/><xsl:text>, netTypeContextId, -1, sysdate, sysdate 
    FROM NetTypeContext WHERE netTypeContextId IN (-1</xsl:text>
    
<xsl:if test="string-length(NetTypeContext/@ntId)=0">
<xsl:for-each select="../NetTypeContext">
<xsl:text>, </xsl:text><xsl:value-of select="@ntId"/>
</xsl:for-each>
</xsl:if>
<xsl:if test="string-length(NetTypeContext/@ntId)>0">
<xsl:for-each select="NetTypeContext">
<xsl:text>, </xsl:text><xsl:value-of select="@ntId"/>
</xsl:for-each>
</xsl:if>
<xsl:text>);
</xsl:text>
</xsl:if>
</xsl:for-each>

</xsl:template>
</xsl:stylesheet>

