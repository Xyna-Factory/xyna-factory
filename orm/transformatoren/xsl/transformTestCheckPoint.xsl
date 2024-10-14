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
-- Capabilities </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO
-- #############################################################################
</xsl:text>
<xsl:for-each select="Capability">
<xsl:choose>
  <xsl:when test="@type='DELETE'"><xsl:text>
INSERT INTO TestCheckPoint(testCheckPointId, checkPoint, checkPointType, checkPointInfo, checkPointReached)
  VALUES (keyTestCheckPoint.nextVal,-1 * (</xsl:text><xsl:value-of select="../@id"/><xsl:text>00+30), 'DTO','</xsl:text><xsl:value-of select="../@tkName"/><xsl:text>DMGen.delete30', 0);</xsl:text>
  </xsl:when>
  <xsl:when test="@type='NOTIFY'"><xsl:text></xsl:text>
  </xsl:when>
  <xsl:otherwise><xsl:text>
INSERT INTO TestCheckPoint(testCheckPointId, checkPoint, checkPointType, checkPointInfo, checkPointReached)
  VALUES (keyTestCheckPoint.nextVal,-1 * (</xsl:text><xsl:value-of select="../@id"/>00+<xsl:value-of select="@id"/>)<xsl:text>, 'DTO','</xsl:text><xsl:value-of select="../@tkName"/>DMGen.<xsl:value-of select="@name"/><xsl:value-of select="@id"/><xsl:text>', 0);</xsl:text></xsl:otherwise>
</xsl:choose>
</xsl:for-each>
<xsl:text>

</xsl:text></xsl:template>
</xsl:stylesheet>
