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
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','find', <xsl:value-of select="@id"/><xsl:text>00+01);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','findPK', <xsl:value-of select="@id"/><xsl:text>00+02);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','findFilter', <xsl:value-of select="@id"/><xsl:text>00+03);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','findWC', <xsl:value-of select="@id"/><xsl:text>00+04);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','findAll', <xsl:value-of select="@id"/><xsl:text>00+10);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','findAllPKs', <xsl:value-of select="@id"/><xsl:text>00+11);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','findAllFilter', <xsl:value-of select="@id"/><xsl:text>00+12);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','findAllWC', <xsl:value-of select="@id"/><xsl:text>00+13);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','count', <xsl:value-of select="@id"/><xsl:text>00+20);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','countPKs', <xsl:value-of select="@id"/><xsl:text>00+21);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','countFilter', <xsl:value-of select="@id"/><xsl:text>00+22);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="@tkName"/>DTO','countWC', <xsl:value-of select="@id"/><xsl:text>00+23);
</xsl:text>
<xsl:for-each select="Capability">
<xsl:choose>
  <xsl:when test="@type='DELETE'"><xsl:text>
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="../@tkName"/>DTO','delete', <xsl:value-of select="../@id"/><xsl:text>00+30);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="../@tkName"/>DTO','findAndDeletePKs', <xsl:value-of select="../@id"/><xsl:text>00+31);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="../@tkName"/>DTO','findAndDeleteFilter', <xsl:value-of select="../@id"/><xsl:text>00+32);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="../@tkName"/>DTO','findAndDeleteWC', <xsl:value-of select="../@id"/><xsl:text>00+33);
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="../@tkName"/>DTO','findAndDeleteList', <xsl:value-of select="../@id"/><xsl:text>00+34);</xsl:text>
  </xsl:when>
  <xsl:when test="@type='NOTIFY'"><xsl:text>
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="../@tkName"/>DTO','notify', <xsl:value-of select="../@id"/>00+35);</xsl:when>
  <xsl:otherwise><xsl:text>
INSERT INTO AIDACapabilityCheck(aidaCapabilityCheckId, dtoName, capabilityName,capabilityId)
  VALUES (keyAIDACapabilityCheck.nextVal,'</xsl:text><xsl:value-of select="../@tkName"/>DTO','<xsl:value-of select="@name"/>', <xsl:value-of select="../@id"/>00+<xsl:value-of select="@id"/>);</xsl:otherwise>
</xsl:choose>
</xsl:for-each>
<xsl:text>

</xsl:text></xsl:template>
</xsl:stylesheet>
