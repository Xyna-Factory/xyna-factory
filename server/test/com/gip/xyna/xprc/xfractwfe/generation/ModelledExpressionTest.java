/*
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
package com.gip.xyna.xprc.xfractwfe.generation;

import junit.framework.TestCase;

import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.formula.TypeInfo;
import com.gip.xyna.xprc.xfractwfe.formula.Variable;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.IdentityCreationVisitor;


public class ModelledExpressionTest extends TestCase{

  public void testIdentityCreationVisitor() throws XPRC_ParsingModelledExpressionException {
    VariableContextIdentification vci = new VariableContextIdentification() {

      public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
        throw new RuntimeException();
      }

      public TypeInfo getTypeInfo(String originalXmlName) {
        throw new RuntimeException();
      }

      public Long getRevision() {
        throw new RuntimeException();
      }

      public VariableInfo createVariableInfo(TypeInfo resultType) {
        throw new RuntimeException();
      }
    };
    
    String expression //= "%1%[\"31\"].x.y~=%0%.service((%2%.x+%3%.service2()), \"base.File\")[\"0\"]";
   // = "%1%.crm_auftragsnr~=substring(%0%.cRMAuftragsPositionsNummer,\"0\",(length(%0%.cRMAuftragsPositionsNummer)-\"4\"))";
    //= "%1%.crm_auftragsnr~=substring(%0%.cRMAuftragsPositionsNummer, \"0\", length((%0%.cRMAuftragsPositionsNummer-\"4\"))";
    = "%4%.doBootloaderUpdate=length(%1%.firmwareversion)>\"0\"&&%1%.firmwareversion!=%3%.bootloaderVersion";
    //%4%.doBootloaderUpdate=((length(%1%.firmwareversion)>"0")&&(%1%.firmwareversion!=%3%.bootloaderVersion))

    ModelledExpression me = ModelledExpression.parse(vci, expression);
    IdentityCreationVisitor icv = new IdentityCreationVisitor();
    me.visitTargetExpression(icv);
    String target = icv.getXFLExpression();
    
    String assign = me.getFoundAssign().toXFL();
    
    icv = new IdentityCreationVisitor();
    me.visitSourceExpression(icv);
    String source = icv.getXFLExpression();

    String newExpr = target+assign+source;
    System.out.println(newExpr);
    assertEquals(expression, newExpr);
  }
  
  public void testIsNumber() {
    assertTrue(ModelledExpression.isNumber("7124"));
    assertTrue(ModelledExpression.isNumber("7124.1e-4"));
    assertTrue(ModelledExpression.isNumber("+0.1e2"));
    assertTrue(ModelledExpression.isNumber("-2.1"));
    assertTrue(ModelledExpression.isNumber("-2.1E5"));
    assertFalse(ModelledExpression.isNumber("4d"));
    assertFalse(ModelledExpression.isNumber("0x4"));
    assertFalse(ModelledExpression.isNumber("2e4e"));
    assertFalse(ModelledExpression.isNumber("1+4"));
    assertFalse(ModelledExpression.isNumber("x24"));
    assertFalse(ModelledExpression.isNumber("7p3"));
  }
  
  public void testIsLong() {
    assertTrue(ModelledExpression.isLong("124123"));
    assertTrue(ModelledExpression.isLong("0124123"));
    assertTrue(ModelledExpression.isLong("-124123"));
    assertTrue(ModelledExpression.isLong("+3"));
    assertFalse(ModelledExpression.isLong("+3L"));
    assertFalse(ModelledExpression.isLong("3e1"));
    assertFalse(ModelledExpression.isLong("3.1"));
  }
  
}
