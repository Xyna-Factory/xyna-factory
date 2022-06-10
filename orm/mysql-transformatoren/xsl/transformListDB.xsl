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
<xsl:variable name="lcletters">abcdefghijklmnopqrstuvwxyz</xsl:variable>
<xsl:variable name="ucletters">ABCDEFGHIJKLMNOPQRSTUVWXYZ</xsl:variable>
<xsl:variable name="primaryKey"></xsl:variable>

<xsl:template match="DBObject">

<xsl:text>/*
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
package </xsl:text><xsl:value-of select="@package"/><xsl:text>.gen;

import java.util.Vector;
import gip.base.common.*;
</xsl:text>
<xsl:if test="@type='table'">
<xsl:text>import </xsl:text>
<xsl:value-of select="@package"/>
<xsl:text>.</xsl:text> <xsl:value-of select="@aliasName"/> <xsl:text>;
</xsl:text>
</xsl:if>
<xsl:text>
/** </xsl:text><xsl:value-of select="@descr"/> <xsl:text> */ 
public class </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List extends OBListObject&lt;</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>&gt; {

</xsl:text> 

<!-- ******************** Kommentar zum Konstruktor *********************************** -->

<xsl:text>
  /** Konstruktor mit einem OBListObject
  */
</xsl:text>

<!-- ******************** Konstruktor mit einem OBListObject ************************** -->

<xsl:text>  public </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List () {
    super();
    this.content = new Vector&lt;</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>&gt;();
  }

</xsl:text>

<!-- ******************** Kommentar zum Konstruktor *********************************** -->

<xsl:text>
  /** Konstruktor mit einem OBListObject
      @param newContent Zu uebertragendes OBListObject
  */
</xsl:text>

<!-- ******************** Konstruktor mit einem OBListObject ************************** -->

<xsl:text>  public </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List (OBListObject&lt;</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>&gt; newContent) {
    super();
    this.content = newContent.getVector();
    super.setTotalLines(newContent.getTotalLines());
    super.setFirstLine(newContent.getFirstLine());
  }

</xsl:text>

<!-- ********************************** Spaltenlisten fuer Insert und Select  ******** -->

<xsl:text>
  // *******************************************************************
  // ******* Kapselung der Methoden aus OBListObject *******************
  // *******************************************************************

  /** Gibt das gewuenschte Element der Liste, wenn es vorhanden ist.  
      @return das gewuenschte Element
      @param index Position in der Liste
      @throws OBException NoSuchElementException wenn das Element nicht existiert
  */
  public </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text> elementAt(int index) throws OBException {
    return super.elementAt(index);
  }

}
</xsl:text>
</xsl:template>
</xsl:stylesheet>
