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
<xsl:variable name="primaryKey"></xsl:variable>

<xsl:template match="DBObject">

<xsl:text>/*
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
 */
package </xsl:text><xsl:value-of select="@package"/><xsl:text>.gen;

import org.apache.log4j.Logger;

import gip.base.common.*;
import gip.base.db.*;
import gip.base.db.drivers.*;

</xsl:text>
<xsl:if test="@type='table'">
<xsl:text>import </xsl:text><xsl:value-of select="@package"/><xsl:text>.*;
</xsl:text></xsl:if>

<xsl:if test="@type='table'">
<xsl:text>/** </xsl:text><xsl:value-of select="@descr"/> <xsl:text> */ </xsl:text>
</xsl:if>
<xsl:if test="@type='view' or @type='dto'">
<xsl:for-each select="Statement">
  <xsl:if test="not(@javadoc='no')">
  <xsl:text>/** </xsl:text><xsl:value-of select="@descr"/>
 *
 * Zugrundeliegende SQL-Definition des Views: <script> &lt;pre&gt; </script><xsl:text>
</xsl:text>
    <xsl:value-of select="../Statement"/>
    <script> &lt;/pre&gt; </script>
    <xsl:text> */ </xsl:text>
  </xsl:if>
</xsl:for-each>
</xsl:if>
<xsl:text>
@SuppressWarnings("serial")
public class </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
<xsl:if test="@type='table'"><xsl:text>Gen</xsl:text></xsl:if><xsl:text> extends </xsl:text>
<xsl:if test="@type='table'"><xsl:text>OBTableObject</xsl:text> </xsl:if>
<xsl:if test="@type='view' or @type='dto'"><xsl:text>OBDBObject</xsl:text> </xsl:if>
<xsl:text> {

  public static Logger logger = Logger.getLogger(</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
<xsl:if test="@type='table'"><xsl:text>Gen</xsl:text></xsl:if><xsl:text>.class);
</xsl:text> 


<!-- ************* Variablen Definitionen fuer Tk-Klasse und Gen-Klasse ************** -->
<xsl:variable name="tkClass">
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
</xsl:variable>
<xsl:variable name="tkObject">
<xsl:text>new </xsl:text><xsl:copy-of select="$tkClass"></xsl:copy-of><xsl:text>()</xsl:text>
</xsl:variable>
<xsl:variable name="genClass">
<xsl:if test="@type='table'"><xsl:copy-of select="$tkClass"></xsl:copy-of><xsl:text>Gen</xsl:text></xsl:if>
<xsl:if test="@type='view' or @type='dto'"><xsl:copy-of select="$tkClass"></xsl:copy-of></xsl:if>
</xsl:variable>
<xsl:variable name="genObject">
<xsl:text>new </xsl:text><xsl:copy-of select="$genClass"></xsl:copy-of><xsl:text>()</xsl:text>
</xsl:variable>


<xsl:text>  // tkClass = </xsl:text><xsl:copy-of select="$tkClass"></xsl:copy-of><xsl:text>
</xsl:text>
<xsl:text>  // tkObject = </xsl:text><xsl:copy-of select="$tkObject"></xsl:copy-of><xsl:text>
</xsl:text>
<xsl:text>  // genClass = </xsl:text><xsl:copy-of select="$genClass"></xsl:copy-of><xsl:text>
</xsl:text>
<xsl:text>  // genObject = </xsl:text><xsl:copy-of select="$genObject"></xsl:copy-of><xsl:text>

</xsl:text>

<!-- ********************************** Attribute definieren ************************** -->

<xsl:for-each select="Column"><xsl:text>  /** </xsl:text> <xsl:value-of select="@descr"/>
<xsl:text> (</xsl:text> <xsl:value-of select="@javaType"/> <xsl:text>) */
  public OBAttribute </xsl:text> <xsl:value-of select="@name"/><xsl:text>; 
</xsl:text>
</xsl:for-each>

<!-- ******************** Statisches Objekt der BL-Schicht **************************** 
NOTE: Wegen zyklischer Abhaengigkeit der TK-Klasse und der Gen-Klasse kann es beim
Laden der Klassen zum Deadlock kommen, wenn statische Initialisierung benutzt wird.
Das Erzeugen eines statischen TK-Objekts ist daher hier entfernt und durch die 
lokale Erzeugung des jeweils benoetigten (TK-/Gen-) Objekts in den Methoden ersetzt worden.   
<xsl:text>

  /** zugehoerige BL-Klasse */
  private static final </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
<xsl:text> blClass = new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
<xsl:text>();
</xsl:text>
-->

<xsl:text>
  /** taskIds fuer select und delete */
  private static final long selectTaskId = </xsl:text><xsl:choose><xsl:when test="count(Task[@type='select']) &gt; 0"><xsl:value-of select="Task[@type='select']/@id"/><xsl:text>;</xsl:text></xsl:when><xsl:otherwise><xsl:text>-1;</xsl:text></xsl:otherwise></xsl:choose><xsl:text>
  private static final long deleteTaskId = </xsl:text><xsl:choose><xsl:when test="count(Task[@type='delete']) &gt; 0"><xsl:value-of select="Task[@type='delete']/@id"/><xsl:text>;</xsl:text></xsl:when><xsl:otherwise><xsl:text>-1;</xsl:text></xsl:otherwise></xsl:choose><xsl:text>

</xsl:text>  
<xsl:for-each select="Constraint">

  <!-- PK, CHECK, UNIQUE-Constraints -->
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:text>  public static final String </xsl:text><xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
    <xsl:text> = "</xsl:text><xsl:value-of select="translate(@name,$lcletters,$ucletters)"/><xsl:text>"; //$NON-NLS-1$
</xsl:text>
  </xsl:if>

  <!-- FK-Constraints -->
  <xsl:for-each select="ErrorMessage">
    <xsl:text>  public static final String </xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="@suffix"/>
    <xsl:text> = "</xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="@suffix"/><xsl:text>"; //$NON-NLS-1$
</xsl:text>
  </xsl:for-each>

</xsl:for-each>
<!-- NN-Constraints -->
<xsl:for-each select="Column">
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:text>  public static final String </xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
      <xsl:text> = "</xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="translate(@name,$lcletters,$ucletters)"/><xsl:text>"; //$NON-NLS-1$
</xsl:text>
  </xsl:if>
  <xsl:if test="not(string-length(@checkErrorMessage)=0)">
    <xsl:text>  public static final String </xsl:text><xsl:value-of select="translate(@checkConstraintName,$lcletters,$ucletters)"/>
      <xsl:text> = "</xsl:text><xsl:value-of select="translate(@checkConstraintName,$lcletters,$ucletters)"/><xsl:text>"; //$NON-NLS-1$
</xsl:text>
  </xsl:if>
</xsl:for-each>

<!-- Index-Fehler -->
<xsl:for-each select="Index">
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:text>  public static final String </xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
      <xsl:text> = "</xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="translate(@name,$lcletters,$ucletters)"/><xsl:text>"; //$NON-NLS-1$
</xsl:text>
  </xsl:if>
</xsl:for-each>

<!-- Sonstige Fehler -->
<xsl:for-each select="ErrorMessage">
  <xsl:if test="not(string-length(@message)=0)">
    <xsl:for-each select="paramdoc">
      <xsl:if test="position()=1"><xsl:text>  /** Dokumentation der Parameter</xsl:text></xsl:if>
      <xsl:text>
   * @param </xsl:text><xsl:value-of select="position()-1"/> <xsl:text> </xsl:text><xsl:value-of select="@description"/>
      <xsl:if test="position()=last()"><xsl:text> 
   * @return ErrorCode*/
  @SuppressWarnings("javadoc")
</xsl:text></xsl:if>
    </xsl:for-each>
    <xsl:text>  public static final String </xsl:text><xsl:value-of select="@errorCode"/>
      <xsl:text>() { return "</xsl:text><xsl:value-of select="translate(@name,$lcletters,$ucletters)"/><xsl:text>"; } //$NON-NLS-1$
</xsl:text>
  </xsl:if>
</xsl:for-each>

<xsl:text>
  static {
    // FehlerCodes der OBException melden
</xsl:text>
<xsl:for-each select="Constraint">

  <!-- PK, CHECK, UNIQUE-Constraints -->
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:text>    OBException.addErrorMessage(</xsl:text><xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
    <xsl:text>,"</xsl:text><xsl:value-of select="@errorMessage"/><xsl:text>"); //$NON-NLS-1$
</xsl:text>
  </xsl:if>

  <!-- FK-Constraints -->
  <xsl:for-each select="ErrorMessage">
    <xsl:text>    OBException.addErrorMessage(</xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="@suffix"/>
    <xsl:text>,"</xsl:text><xsl:value-of select="@message"/><xsl:text>"); //$NON-NLS-1$
</xsl:text>
  </xsl:for-each>
</xsl:for-each>

<!-- NN-Constraints -->
<xsl:for-each select="Column">
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:text>    OBException.addErrorMessage(</xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
      <xsl:text>,"</xsl:text><xsl:value-of select="@errorMessage"/><xsl:text>"); //$NON-NLS-1$
</xsl:text>
  </xsl:if>
  <xsl:if test="not(string-length(@checkErrorMessage)=0)">
    <xsl:text>    OBException.addErrorMessage(</xsl:text><xsl:value-of select="translate(@checkConstraintName,$lcletters,$ucletters)"/>
      <xsl:text>,"</xsl:text><xsl:value-of select="@checkErrorMessage"/><xsl:text>"); //$NON-NLS-1$
</xsl:text>
  </xsl:if>
</xsl:for-each>

<!-- NN-Constraints -->
<xsl:for-each select="Index">
  <xsl:if test="not(string-length(@errorMessage)=0)">
    <xsl:text>    OBException.addErrorMessage(</xsl:text><xsl:value-of select="translate(../@name,$lcletters,$ucletters)"/>_<xsl:value-of select="translate(@name,$lcletters,$ucletters)"/>
      <xsl:text>,"</xsl:text><xsl:value-of select="@errorMessage"/><xsl:text>"); //$NON-NLS-1$
</xsl:text>
  </xsl:if>
</xsl:for-each>

<!-- Sonstige Fehler -->
<xsl:for-each select="ErrorMessage">
  <xsl:if test="not(string-length(@message)=0)">
    <xsl:text>    OBException.addErrorMessage(</xsl:text><xsl:value-of select="@errorCode"/>
      <xsl:text>(),"</xsl:text><xsl:value-of select="@message"/><xsl:text>"); //$NON-NLS-1$
</xsl:text>
  </xsl:if>
</xsl:for-each>
<xsl:text>  }

  public long getSelectTaskId() { return selectTaskId; }
  public long getDeleteTaskId() { return deleteTaskId; }

</xsl:text>

<xsl:text>

<!-- ********************************** Spaltenlisten fuer Insert und Select  ******** -->

  /** Spaltenliste fuer Insert */
  private final static String tableDetails = "</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@name"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@sqlName"/></xsl:if>
<xsl:text>(</xsl:text>
<xsl:for-each select="Column"><xsl:value-of select="@name"/>
<xsl:if test="not(position()=last())">, </xsl:if> <xsl:if test="position()=last()">) "; //$NON-NLS-1$</xsl:if>
</xsl:for-each>
<xsl:text>
  /** Spaltenliste fuer Select */
  private final static String tableSelect = "</xsl:text>
<xsl:for-each select="Column">
<xsl:if test="not(@type='DATE') and not(@type='TIMESTAMP WITH LOCAL TIME ZONE')"><xsl:value-of select="@name"/> </xsl:if>
<xsl:if test="@type='DATE' or @type='TIMESTAMP WITH LOCAL TIME ZONE'">
  <xsl:text>TO_CHAR(</xsl:text>
  <xsl:value-of select="@name"/>
  <xsl:text>,'</xsl:text>
  <xsl:if test="string-length(@format)=0">
    <xsl:text>DD.MM.YYYY</xsl:text>
  </xsl:if>
  <xsl:if test="string-length(@format)>0">
    <xsl:value-of select="@format"/>
  </xsl:if>
  <xsl:text>') </xsl:text> 
</xsl:if>
<xsl:if test="not(position()=last())">, </xsl:if> <xsl:if test="position()=last()"> "; //$NON-NLS-1$ </xsl:if>
</xsl:for-each>

<!-- ********************************** Konstruktor ohne Attributwerte *************** -->

<xsl:text>
  /** Konstruktor ohne Parameter. Es wird kein Objekt in der DB erzeugt 
      oder aus der DB selectiert. */
  public </xsl:text> 
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:if test="@type='table'"><xsl:text>Gen</xsl:text></xsl:if><xsl:text> () {
    super();
    comment    = "</xsl:text><xsl:value-of select="@descr"/><xsl:text>"; //$NON-NLS-1$ 
    tableName  = "</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
    <xsl:text>"; //$NON-NLS-1$ 
</xsl:text>
<xsl:if test="@type='view' or @type='dto'">
<xsl:for-each select="Column">
<xsl:if test="substring(@descr,1,11)='PRIMARY KEY'"><xsl:text>    primaryKey = "</xsl:text><xsl:value-of select="@name"/><xsl:text>"; //$NON-NLS-1$ </xsl:text></xsl:if>
</xsl:for-each>
</xsl:if>
<xsl:if test="@type='table'">
<xsl:for-each select="Constraint">
<xsl:if test="@type='PK'"><xsl:text>    primaryKey = "</xsl:text><xsl:value-of select="@columnList"/><xsl:text>"; //$NON-NLS-1$ </xsl:text></xsl:if>
</xsl:for-each>
</xsl:if>

<xsl:text>
    attArr = new OBAttribute[</xsl:text>
      <xsl:for-each select="Column">
      <xsl:if test="position()=last()"><xsl:value-of select="last()"/></xsl:if>
      </xsl:for-each>
<xsl:text>];

</xsl:text>

  <!-- ********************************** Einzelne Attribute belegen ***************** -->

<xsl:for-each select="Column"><xsl:text>    </xsl:text> 
<xsl:value-of select="@name"/><xsl:text> = new OBAttribute("</xsl:text>
<xsl:value-of select="@name"/><xsl:text>", OBConstants.</xsl:text>
<xsl:if test="@type='VARCHAR2'">STRING,</xsl:if>
<xsl:if test="@type='NVARCHAR2'">STRING,</xsl:if>
<xsl:if test="@type='CHAR'">STRING,</xsl:if>
<xsl:if test="@type='LONG'">LONGVARCHAR,</xsl:if>
<xsl:if test="@type='CLOB'">CLOB,</xsl:if>
<xsl:if test="@type='BLOB'">BLOB,</xsl:if>
<xsl:if test="@javaType='int'">INTEGER,</xsl:if>
<xsl:if test="@javaType='double'">DOUBLE,</xsl:if>
<xsl:if test="@javaType='boolean'">BOOLEAN,</xsl:if>
<xsl:if test="@javaType='long'">LONG,</xsl:if>
<xsl:if test="@javaType='intEnum'">INTEGER,</xsl:if>
<xsl:if test="@javaType='booleanEnum'">BOOLEAN,</xsl:if>
<xsl:if test="@javaType='longEnum'">LONG,</xsl:if>
<xsl:if test="@type='DATE'">DATE,</xsl:if>
<xsl:if test="@type='TIMESTAMP WITH LOCAL TIME ZONE'">TIMESTAMP_WITH_LOCAL_TIME_ZONE,</xsl:if>
<xsl:if test="@type='DATE'">30,</xsl:if>
<xsl:if test="@type='TIMESTAMP WITH LOCAL TIME ZONE'">30,</xsl:if>
<xsl:if test="@type='CLOB'">OBConstants.MAX_CLOB_LENGTH,</xsl:if>
<xsl:if test="@type='BLOB'">OBConstants.MAX_CLOB_LENGTH,</xsl:if>
<xsl:if test="@javaType='boolean'">1,</xsl:if>
<xsl:if test="@javaType='booleanEnum'">1,</xsl:if>
<xsl:if test="@type='INTEGER' and not(@javaType='boolean') and not(@javaType='booleanEnum')">38,</xsl:if>
<xsl:if test="@type='FLOAT'">38,</xsl:if>
<xsl:if test="not(@type='DATE') and not(@type='TIMESTAMP WITH LOCAL TIME ZONE') and not(@type='FLOAT') and not(@type='INTEGER') and not(@type='CLOB') and not(@type='BLOB')"><xsl:value-of select="@length"/><xsl:text>,</xsl:text></xsl:if>
 
<xsl:if test="@nullable='Y'">true,"</xsl:if>
<xsl:if test="not(@nullable='Y')">false,"</xsl:if>
<xsl:value-of select="@descr"/><xsl:text>");  //$NON-NLS-1$  //$NON-NLS-2$ 
</xsl:text>
<xsl:if test="string-length(@format)>0">
<xsl:text>    </xsl:text><xsl:value-of select="@name"/><xsl:text>.setFormatMask("</xsl:text><xsl:value-of select="@format"/><xsl:text>"); //$NON-NLS-1$
</xsl:text>
</xsl:if>
</xsl:for-each>
<xsl:text>
</xsl:text>
<xsl:for-each select="Column">
<xsl:text>    attArr[</xsl:text><xsl:value-of select="position()-1"/><xsl:text>] = </xsl:text>
<xsl:value-of select="@name"/><xsl:text>;
</xsl:text>
</xsl:for-each>

  <!-- ********************************** Primary Key belegen ********************** -->

<xsl:if test="@type='view' or @type='dto'">
<xsl:for-each select="Column">
<xsl:if test="substring(@descr,1,11)='PRIMARY KEY'">
<xsl:text>
    primaryKeyAtt = </xsl:text><xsl:value-of select="@name"/><xsl:text>;
</xsl:text>
</xsl:if>
</xsl:for-each>
</xsl:if>
<xsl:if test="@type='table'">
<xsl:for-each select="Constraint">
<xsl:if test="@type='PK'">
<xsl:text>
    primaryKeyAtt = </xsl:text><xsl:value-of select="@columnList"/><xsl:text>;
</xsl:text>
</xsl:if>
</xsl:for-each>
</xsl:if>

<xsl:text>
<!--
    
    try {
      setSelectColsDefault();
    }
    catch (Exception e) {
      e.printStackTrace();
    }
-->
  }
</xsl:text>

<!-- ********************************** Objekt ueber UNIQUE-Constraint *************** -->

<xsl:if test="@package='gip.ipnet.db' or string-length(@package)=0">
<xsl:for-each select="Constraint">
<xsl:if test="@type='U'">
<xsl:if test="string-length(@columnList)=0">
<xsl:text>  /** </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text> ueber UNIQUE-Constraint von </xsl:text>
<xsl:for-each select="Column">
<xsl:value-of select="@name"/> <xsl:if test="not(position()=last())">, </xsl:if>
</xsl:for-each>
<xsl:text> */
  public static </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text> getUnique</xsl:text><xsl:value-of select="../@aliasName"/><xsl:if test="not(string-length(@methodSuffix)=0)"><xsl:value-of select="@methodSuffix"/></xsl:if>
<xsl:text>(OBContext context, </xsl:text>
<xsl:for-each select="Column">
<xsl:value-of select="@javaType"/><xsl:text> </xsl:text><xsl:value-of select="@name"/> <xsl:if test="not(position()=last())">, </xsl:if></xsl:for-each>
<xsl:text> ) throws Exception {
    </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text> searchFilter = new </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text>();
</xsl:text>
<xsl:for-each select="Column">
<xsl:text>    searchFilter.set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text><xsl:value-of select="@name"/><xsl:text>);
</xsl:text>
</xsl:for-each>
<xsl:text>
    return </xsl:text><xsl:value-of select="../@aliasName"/><xsl:text>.find(context, searchFilter);    
  }

</xsl:text>
</xsl:if>
</xsl:if>
</xsl:for-each>
</xsl:if>

<!-- ********************************** Zugriffsfunktionen fuer Attribute *************** -->

<xsl:text>  /* Zugriffsfunktionen fuer Attribute */
</xsl:text>

<xsl:for-each select="Column">
<xsl:text>  public </xsl:text>
<xsl:if test="@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum'"><xsl:value-of select="@enumType"/></xsl:if>
<xsl:if test="not(@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum')"><xsl:value-of select="@javaType"/></xsl:if>
<xsl:text> get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>() </xsl:text><xsl:if test="not (@javaType='String')"><xsl:text>throws OBException </xsl:text></xsl:if><xsl:text>{ return </xsl:text>
<xsl:if test="not(string-length(@enumType)=0)"><xsl:value-of select="@enumType"/><xsl:if test="@javaType='enum'">.valueOf(</xsl:if><xsl:if test="not(@javaType='enum')">.staticFromDbVal(</xsl:if></xsl:if>
<xsl:value-of select="@name"/>
<xsl:text>.get</xsl:text>
<xsl:if test="@javaType='int' or @javaType='intEnum'">Int</xsl:if>
<xsl:if test="@javaType='long' or @javaType='longEnum'">Long</xsl:if>
<xsl:if test="@javaType='boolean' or @javaType='booleanEnum'">Boolean</xsl:if>
<xsl:if test="@javaType='double'">Double</xsl:if>
<xsl:if test="@javaType='byte[]'">ByteArray</xsl:if>
<xsl:text>Value()</xsl:text>
<xsl:if test="not(string-length(@enumType)=0)">)</xsl:if>
<xsl:text>; }
</xsl:text>
<xsl:text>  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text>
<xsl:if test="@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum'"><xsl:value-of select="@enumType"/></xsl:if>
<xsl:if test="not(@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum')"><xsl:value-of select="@javaType"/></xsl:if>
<xsl:text> val) </xsl:text><xsl:if test="not (@javaType='String' or @javaType='byte[]')"><xsl:text>throws OBException </xsl:text></xsl:if><xsl:text>{ </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(</xsl:text>
<xsl:if test="not(string-length(@enumType)=0)"><xsl:value-of select="@enumType"/><xsl:if test="@javaType='enum'">.staticName(val)</xsl:if><xsl:if test="not(@javaType='enum')">.staticGetDbVal(val)</xsl:if></xsl:if>
<xsl:if test="string-length(@enumType)=0">val</xsl:if>
<xsl:text>);}
</xsl:text>
<xsl:text>  public </xsl:text>
<xsl:if test="../@type='table' or ../@type='view'"><xsl:value-of select="../@aliasName"/></xsl:if>
<xsl:if test="../@type='dto'"><xsl:value-of select="../@viewName"/></xsl:if>
<xsl:if test="../@type='table'"><xsl:text>Gen</xsl:text></xsl:if>
<xsl:text> fill</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>(</xsl:text>
<xsl:if test="@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum'"><xsl:value-of select="@enumType"/></xsl:if>
<xsl:if test="not(@javaType='longEnum' or @javaType='intEnum' or @javaType='booleanEnum' or @javaType='stringEnum' or @javaType='enum')"><xsl:value-of select="@javaType"/></xsl:if>
<xsl:text> val) </xsl:text><xsl:if test="not (@javaType='String' or @javaType='byte[]')"><xsl:text>throws OBException </xsl:text></xsl:if><xsl:text>{ </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(</xsl:text>
<xsl:if test="not(string-length(@enumType)=0)"><xsl:value-of select="@enumType"/><xsl:if test="@javaType='enum'">.staticName(val)</xsl:if><xsl:if test="not(@javaType='enum')">.staticGetDbVal(val)</xsl:if></xsl:if>
<xsl:if test="string-length(@enumType)=0">val</xsl:if>
<xsl:text>); return this; }
</xsl:text>
<xsl:if test="@javaType='intEnum'">
<xsl:text>  public int get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue() throws OBException { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getIntValue(); }
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue(int value) throws OBException { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(value); }
</xsl:text>
</xsl:if>
<xsl:if test="@javaType='longEnum'">
<xsl:text>  public long get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue() throws OBException { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getLongValue(); }
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue(long value) throws OBException { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(value); }
</xsl:text>
</xsl:if>
<xsl:if test="@javaType='booleanEnum'">
<xsl:text>  public boolean get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue() throws OBException { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getBooleanValue(); }
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue(boolean value) throws OBException { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(value); }
</xsl:text>
</xsl:if>
<xsl:if test="@javaType='stringEnum' or @javaType='enum'">
<xsl:text>  public String get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue() { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getValue(); }
  public void set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>DbValue(String value) { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setValue(value); }
</xsl:text>
</xsl:if>
</xsl:for-each>

<!-- ********************************** Nullfunktionen fuer Attribute *************** -->
 
<xsl:text> 
  /* Null-Behandlungs-Funktionen fuer Attribute */
</xsl:text>

<xsl:for-each select="Column">
<xsl:text>  public boolean is</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>Null() { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.isNull(); }
</xsl:text>
<xsl:text>  public boolean is</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>NotNull() { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.isNotNull(); }
</xsl:text>
<xsl:text>  public </xsl:text>
<xsl:if test="../@type='table' or ../@type='view'"><xsl:value-of select="../@aliasName"/></xsl:if>
<xsl:if test="../@type='dto'"><xsl:value-of select="../@viewName"/></xsl:if>
<xsl:if test="../@type='table'"><xsl:text>Gen</xsl:text></xsl:if>
<xsl:text> set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>Null() { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setNull(); return this; }
</xsl:text>
</xsl:for-each>

<!-- *********************** CompOperator-Funktionen fuer Attribute *************** -->
 
<xsl:text> 
  /* Vergleichs-Operator-Funktionen fuer Attribute */
</xsl:text>

<xsl:for-each select="Column">
<xsl:text>  public </xsl:text>
<xsl:if test="../@type='table' or ../@type='view'"><xsl:value-of select="../@aliasName"/></xsl:if>
<xsl:if test="../@type='dto'"><xsl:value-of select="../@viewName"/></xsl:if>
<xsl:if test="../@type='table'"><xsl:text>Gen</xsl:text></xsl:if>
<xsl:text> set</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>CompOp(String compOp) throws OBException { </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.setCompOp(compOp); return this; }
</xsl:text>
<xsl:text>  public String get</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text>CompOp() { return </xsl:text><xsl:value-of select="@name"/>
<xsl:text>.getCompOp(); }
</xsl:text>
</xsl:for-each>

<!-- ***************** statische name-Konstanten fuer Attribute *************** -->
 
<xsl:text> 
  /* name-Konstanten fuer Attribute */
</xsl:text>
<xsl:for-each select="Column">
<xsl:text>  public static final String name</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text> = "</xsl:text><xsl:value-of select="@name"/><xsl:text>"; //$NON-NLS-1$ 
</xsl:text>
</xsl:for-each>
<xsl:for-each select="Column">
<xsl:text>  public static final String nameComplete</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text> = "</xsl:text><xsl:value-of select="../@aliasName"/>.<xsl:value-of select="@name"/><xsl:text>"; //$NON-NLS-1$ 
</xsl:text>
</xsl:for-each>

<!-- ***************** statische label-Konstanten fuer Attribute *************** -->
 
<xsl:text> 
  /* label-Konstanten fuer Attribute */
</xsl:text>
<xsl:for-each select="Column">
<xsl:if test="string-length(@label)>0">
<xsl:text>  public static final String label</xsl:text>
<xsl:value-of select="translate(substring(@name,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@name,2)"/>
<xsl:text> = "</xsl:text><xsl:value-of select="@label"/>
<xsl:text>";
</xsl:text>
</xsl:if>
</xsl:for-each>

<!-- ********************************** Diverse HilfsFunktionen *************** -->

<xsl:text>
  /** Liefert die AttributListe fuers Insert 
      @return AttributListe fuers Insert
  */
  protected String tableDetails() { 
    return tableDetails; 
  }

  /** Liefert die AttributListe fuers Select 
      @return AttributListe fuers Select
  */
  public String tableSelect() { 
    return tableSelect; 
  }

  /** Liefert die Anzahl der Attribute 
      @return Anzahl der Attribute
  */
  public int numAttr() { 
    return </xsl:text>
<xsl:for-each select="Column">
<xsl:if test="position()=last()"><xsl:value-of select="last()"/></xsl:if>
</xsl:for-each><xsl:text>;
  }
</xsl:text>

<!-- ************************** Funktionen fuer Klassenname und Objekt  *************** -->

<xsl:text>
  /** Liefert den Klassennamen als String zurueck 
      @return Klassenname
  */
  public String getClassName() {
    return "</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
    <xsl:if test="@type='table'"><xsl:text>Gen</xsl:text></xsl:if><xsl:text>"; //$NON-NLS-1$ 
  }
</xsl:text>

<xsl:text>
  /** Liefert ein Objekt des Typs </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>Gen zurueck 
      @return Objekt des Typs </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>Gen
  */
  public Object getObject() {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
    <xsl:text>();
  }
  
  /** Methode, die das abstrakte Schema zurueckliefert (z.B. ipnet)
   * @return das abstrakte Schema
   */
  public String getProjectSchema() {
    return "</xsl:text>
<xsl:if test="string-length(@projectSchema)=0"> <xsl:text>ipnet</xsl:text></xsl:if>
<xsl:if test="string-length(@projectSchema)>0"> <xsl:value-of select="@projectSchema"/></xsl:if>
<xsl:text>";  //$NON-NLS-1$ 
  }
  
  /** Methode, die den SQL-Namen liefert
   * @return SQL-Name
   */
  public String getSQLName() {
    return "</xsl:text> 
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@name"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@sqlName"/></xsl:if>
    <xsl:text>";  //$NON-NLS-1$ 
  }

  /** Methode, die den SQL-Namen mitsamt Schema in der je nach Treiber korrekt liefert
   * @param context Durchgereichtes Context-Objekt
   * @return SQL-Name Treiber- und Schema-spezifisch
   * @throws OBException wird durchgereicht
   */
  public static String getSQLRepresentation(OBContext context) throws OBException {
    </xsl:text><xsl:copy-of select="$genClass"></xsl:copy-of><xsl:text> genObject = </xsl:text><xsl:copy-of select="$genObject"></xsl:copy-of><xsl:text>;
    return OBDriver.getTableName(context,context.getSchema(genObject.getProjectSchema()),genObject.getSQLName());
  }
  
</xsl:text>

<!-- ************************** Kapselung der Methoden fuer Sequences  **************** -->

<xsl:for-each select="Sequence">

<xsl:text>
  /** Liefert den nextVal zur Sequence </xsl:text><xsl:value-of select="@name"/><xsl:text>. 
   * @param context Durchzureichendes Context-Objekt
   * @return nextVal der Sequence
   * @throws OBException Wenn etwas dabei schiefgeht
   */
  public static long nextVal</xsl:text>
<xsl:value-of select="translate(substring(@aliasName,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@aliasName,2)"/>
<xsl:text>(OBContext context) throws OBException {
    return OBDBObject.getNextKeyVal(context,</xsl:text><xsl:copy-of select="$genObject"/><xsl:text>.getProjectSchema(),"</xsl:text>
<xsl:value-of select="@name"/>
<xsl:text>"); //$NON-NLS-1$
  }

  /** Liefert den currentVal zur Sequence </xsl:text><xsl:value-of select="@name"/><xsl:text>. 
   * @param context Durchzureichendes Context-Objekt
   * @return currentVal der Sequence
   * @throws OBException Wenn etwas dabei schiefgeht
   */
  public static long currentVal</xsl:text>
<xsl:value-of select="translate(substring(@aliasName,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@aliasName,2)"/>
<xsl:text>(OBContext context) throws OBException {
    return OBDBObject.getCurrentKeyVal(context,</xsl:text><xsl:copy-of select="$genObject"/><xsl:text>.getProjectSchema(),"</xsl:text>
<xsl:value-of select="@name"/>
<xsl:text>"); //$NON-NLS-1$
  }
</xsl:text>
<xsl:if test="@isPK='Y'">
<xsl:text>
  /** Liefert den nextVal zur Primary Key Sequence. 
   * @param context Durchzureichendes Context-Objekt
   * @return nextVal der Sequence
   * @throws OBException Wenn etwas dabei schiefgeht
   */
  public long nextValPK(OBContext context) throws OBException {
    return nextVal</xsl:text>
<xsl:value-of select="translate(substring(@aliasName,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@aliasName,2)"/>
<xsl:text>(context);
  } 

  /** Liefert den currentVal zur Primary Key Sequence. 
   * @param context Durchzureichendes Context-Objekt
   * @return currentVal der Sequence
   * @throws OBException Wenn etwas dabei schiefgeht
   */
  public long currentValPK(OBContext context) throws OBException {
    return currentVal</xsl:text>
<xsl:value-of select="translate(substring(@aliasName,1,1),$lcletters,$ucletters)"/>
<xsl:value-of select="substring(@aliasName,2)"/>
<xsl:text>(context);
  } 
</xsl:text>
</xsl:if>
</xsl:for-each>

<!-- ************************** Kapselung der Methoden aus OBDBObject  *************** -->

<xsl:text>

  // ******* Methoden, die eine Connection mitbekommen *****************

  // ******* Selectionsmethoden, die ein Objekt erwarten ***************

  /** Suche nach einem Objekt mit Hilfe des Primaerschluessels
      @param context OBContext, immer dabei
      @param pk Primaerschluesselwert
      @return Das gefundene Objekt
      @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war (bei Views)
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text> find(OBContext context, long pk) throws OBException {
    return (</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>) OBDBObject.find(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,pk,"");  //$NON-NLS-1$
  }

  /** Suche nach einem Objekt mit Hilfe des Primaerschluessels
      @param context OBContext, immer dabei
      @param pk Primaerschluesselwert
      @param hint SQL-Hint zu Optimierungszwecken
      @return Das gefundene Objekt
      @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war (bei Views)
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text> find(OBContext context, long pk, String hint) throws OBException {
    return (</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>) OBDBObject.find(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,pk,hint); 
  }

  /** Suche nach einem Objekt mit Hilfe eines Beispiel-Objektes
      @param context OBContext, immer dabei
      @param filter Ein Beispiel-Objekt, wie das zu suchende aussehen soll
      @return Das gefundene Objekt
      @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text> find(OBContext context, </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
<xsl:if test="@type='table'"><xsl:text>Gen</xsl:text></xsl:if>
  <xsl:text> filter) throws OBException {
    return (</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>) OBDBObject.find(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,filter); 
  }

  /** Suche nach einem Objekt mit Hilfe einer SQL-Bedingung
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @return Das gefundene Objekt
      @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text> find(OBContext context, String whereClause) throws OBException {
    return (</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>) OBDBObject.find(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,whereClause,"");  //$NON-NLS-1$
  }

  /** Suche nach einem Objekt mit Hilfe einer SQL-Bedingung
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param hint SQL-Hint zu Optimierungszwecken
      @return Das gefundene Objekt
      @throws OBException wenn der Datensatz nicht gefunden wurde oder nicht eindeutig war
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text> find(OBContext context, String whereClause, String hint) throws OBException {
    return (</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>) OBDBObject.find(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,whereClause,hint); 
  }

  // ******* Selectionsmethoden, die eine Liste von Objekten erwarten **

  /** Suche nach mehreren Objekten mit Hilfe der PKs
      @param context OBContext, immer dabei
      @param pks ARRAY der Primaerschluessel
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, long[] pks) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,pks,"",""));  //$NON-NLS-1$ //$NON-NLS-2$
  }

  /** Suche nach mehreren Objekten mit Hilfe der PKs und einer Ordnungsbedingung
      @param context OBContext, immer dabei
      @param pks ARRAY der Primaerschluessel
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, long[] pks, String orderBy) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,pks,orderBy,""));  //$NON-NLS-1$
  }

  /** Suche nach mehreren Objekten mit Hilfe der PKs und einer Ordnungsbedingung
      @param context OBContext, immer dabei
      @param pks ARRAY der Primaerschluessel
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @param hint SQL-Hint zu Optimierungszwecken
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, long[] pks, String orderBy, String hint) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,pks,orderBy,hint)); 
  }

  /** Suche nach mehreren Objekten mit Hilfe eines Beispielobjektes
      @param context OBContext, immer dabei
      @param filter Ein Beispiel-Objekt, wie die zu suchenden aussehen sollen
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
<xsl:if test="@type='table'"><xsl:text>Gen</xsl:text></xsl:if>
  <xsl:text> filter) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,filter)); 
  }

  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, String whereClause) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,whereClause,"",""));  //$NON-NLS-1$ //$NON-NLS-2$
  }

  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, String whereClause, String orderBy) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,whereClause,orderBy,""));  //$NON-NLS-1$
  }

  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @param hint SQL-Hint zu Optimierungszwecken
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, String whereClause, String orderBy, String hint) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,whereClause,orderBy,hint)); 
  }

  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @param maxRows Hoechstzahl dr gelieferten Datensaetze
      @param attribs zu selektierende Spalten, null fuer alle
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, String whereClause, String orderBy, 
                                       int maxRows, OBAttribute[] attribs) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,whereClause,orderBy,"", maxRows,attribs));  //$NON-NLS-1$
  }

  /** Suche nach mehreren Objekten mit Hilfe einer SQL-Bedingung
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @param hint SQL-Hint zu Optimierungszwecken
      @param maxRows Hoechstzahl dr gelieferten Datensaetze
      @param attribs zu selektierende Spalten, null fuer alle
      @return List-Objekt, das die gefundenen Objekte enthaelt
      @throws OBException Auftretende SQL-Exceptions
  */
  public static </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List findAll(OBContext context, String whereClause, String orderBy, String hint, 
                                       int maxRows, OBAttribute[] attribs) throws OBException {
    return new </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List(OBDBObject.findAll(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,whereClause,orderBy,hint, maxRows,attribs)); 
  }
  
  // ******* Selectionsmethoden, die die Anzahl der gefundenen Objekte liefern ****

  /** Suche mit Hilfe der PKs
      @param context OBContext, immer dabei
      @param pks ARRAY der Primaerschluessel
      @return Anzahl der gefundenen Objekte
      @throws OBException Auftretende SQL-Exceptions
  */
  public static int count(OBContext context, long[] pks) throws OBException {
    return OBDBObject.count(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,pks); 
  }

  /** Suche mit Hilfe eines Beispielobjektes
      @param context OBContext, immer dabei
      @param filter Ein Beispiel-Objekt, wie die zu suchenden aussehen sollen
      @return Anzahl der gefundenen Objekte
      @throws OBException Auftretende SQL-Exceptions
  */
  public static int count(OBContext context, </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
<xsl:if test="@type='table'"><xsl:text>Gen</xsl:text></xsl:if>
  <xsl:text> filter) throws OBException {
    return OBDBObject.count(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,filter); 
  }

  /** Suche mit Hilfe einer SQL-Bedingung
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @return Anzahl der gefundenen Objekte
      @throws OBException Auftretende SQL-Exceptions
  */
  public static int count(OBContext context, String whereClause) throws OBException {
    return OBDBObject.count(context,</xsl:text><xsl:copy-of select="$tkObject"/><xsl:text>,whereClause); 
  }

</xsl:text>
<xsl:if test="@type='table'">
<xsl:text>

  // **********************************************************************  
  // ******* findAndDelete-Methoden ***************************************
  // **********************************************************************


  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context OBContext, immer dabei
      @param filter Ein Beispiel-Objekt, wie die zu suchenden aussehen sollen
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
<xsl:if test="@type='table'"><xsl:text>Gen</xsl:text></xsl:if>
  <xsl:text> filter) throws OBException {
    </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>List delObjects = findAll(context, filter);
    findAndDelete(context, delObjects);
  }

  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   String whereClause) throws OBException {
    </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List delObjects = findAll(context, whereClause);
    findAndDelete(context, delObjects);
  }

  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context OBContext, immer dabei
      @param whereClause Bedingung, muss mit WHERE beginnen
      @param orderBy Ordnungsbedingung, muss mit ORDER BY beginnen
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   String whereClause, 
                                   String orderBy) throws OBException {
    </xsl:text><xsl:value-of select="@aliasName"/><xsl:text>List delObjects = findAll(context, whereClause, orderBy);
    findAndDelete(context, delObjects);
  }

  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context OBContext, immer dabei
      @param pks ARRAY der Primaerschluessel
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   long[] pks) throws OBException {
    </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List delObjects = findAll(context, pks);
    findAndDelete(context, delObjects);
  }

  /** Loescht Datensaetze in der Datenbank.
      Ein Delete erfolgt nur, falls der Wert von lockRow=-1 ist.
      @param context OBContext, immer dabei
      @param delObjects Liste der zu loeschenden Objekte
      @throws OBException Falls Fehler beim Loeschen auftritt
  */
  public static void findAndDelete(OBContext context,
                                   </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>List delObjects) throws OBException {
    </xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text> obtabobj;
    if (delObjects.size() == 0) {
      logger.debug("</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>.findAndDelete, Kein Datensatz geloescht"); //$NON-NLS-1$
    }
    else {
      for (int i=0; i&lt;delObjects.size(); i++) {
        obtabobj = delObjects.elementAt(i);
        obtabobj.delete(context);
      }
      if (delObjects.size() == 1) {
        logger.debug("</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>.findAndDelete, 1 Datensatz geloescht"); //$NON-NLS-1$
      }
      else {
        logger.debug("</xsl:text>
<xsl:if test="@type='table' or @type='view'"><xsl:value-of select="@aliasName"/></xsl:if>
<xsl:if test="@type='dto'"><xsl:value-of select="@viewName"/></xsl:if>
  <xsl:text>.findAndDelete" + delObjects.size() + " Datensaetze geloescht."); //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
  }
</xsl:text>

</xsl:if>
<xsl:text>}
</xsl:text>

</xsl:template>
</xsl:stylesheet>