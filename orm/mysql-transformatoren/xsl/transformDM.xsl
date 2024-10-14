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

<xsl:text>/*
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
 */
package gip.core.demultiplexing;

import gip.base.common.OBAttribute;
import gip.base.common.OBDTO;
import gip.base.common.OBException;
import gip.base.db.OBContext;
import gip.common.dto.</xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO;
import gip.core.demultiplexing.demultiplexinggen.</xsl:text><xsl:value-of select="@tkName"/><xsl:text>DMGen;

</xsl:text>

public class <xsl:value-of select="@tkName"/><xsl:text>DM extends </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DMGen {

</xsl:text> 

<!-- ************************** Allgemeine Capabilities  ******************************* -->

<xsl:text>
  /* -------------------------- find-Methoden ------------------------------------------ */

  /** Ergaenzt den Datensatz je nach Verwendungszweck
   * @param context Durchzureichendes Context-Object
   * @param dto Das Objekt, das noch weiter gefuellt werden soll
   * @return Das gefuellte Objekt
   * @throws OBException Wird durchgereicht
   */
  public static </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO findAdditional(OBContext context, </xsl:text>
                <xsl:value-of select="@tkName"/><xsl:text>DTO dto) throws OBException {
    // Zusaetzliche Fuellanweisungen je nach Zweck
    return dto;
  }

  /** Liefert den NetTypeContext zum Datensatz
   * @param context Durchzureichendes Context-Object
   * @param baseDto Das Objekt, das zugeordnet werden soll
   * @return ID des NetTypeContextes
   * @throws OBException Wird durchgereicht
   */
  public long getNetTypeContextId(OBContext context, OBDTO baseDto) throws OBException {
    // </xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO dto = (</xsl:text><xsl:value-of select="@tkName"/><xsl:text>DTO) baseDto;
    // Hier muss der Netztyp anhand der Daten aus dem DTO festgestellt werden.
    return OBAttribute.NULL;
  }
}
</xsl:text>
</xsl:template>
</xsl:stylesheet>
