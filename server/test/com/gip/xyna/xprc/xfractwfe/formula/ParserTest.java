/*
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
package com.gip.xyna.xprc.xfractwfe.formula;



import junit.framework.TestCase;

import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableIdException;
import com.gip.xyna.xprc.exceptions.XPRC_InvalidVariableMemberNameException;
import com.gip.xyna.xprc.exceptions.XPRC_ParsingModelledExpressionException;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression;
import com.gip.xyna.xprc.xfractwfe.generation.ModelledExpression.IdentityCreationVisitor;
import com.gip.xyna.xprc.xfractwfe.generation.VariableContextIdentification;



public class ParserTest extends TestCase {
  
  private VariableContextIdentification helperContext() {
    return new VariableContextIdentification() {
      public VariableInfo createVariableInfo(Variable v, boolean followAccessParts) throws XPRC_InvalidVariableIdException, XPRC_InvalidVariableMemberNameException {
        throw new RuntimeException();
      }
      public TypeInfo getTypeInfo(String originalXmlName) {
        throw new RuntimeException();
      }
      public Long getRevision() {
        return null;
      }
      public VariableInfo createVariableInfo(TypeInfo resultType) {
        throw new RuntimeException();
      }
    };
  }
  
  public void testBug17155() throws XPRC_ParsingModelledExpressionException {
   
    String valid = "((%0%.deviceTypeId.deviceTypeId==\"3\")&&(%0%.currentFirmwareVersion.firmwareVersion==\"03.08.00.S\"))";
    String invalid = ";truncate devicetype1; select * from devicetype1 where id = ((\"\"))";
    try {
      ModelledExpression.parse(helperContext(), valid + invalid);
      fail("expected exception");
    } catch (XPRC_ParsingModelledExpressionException e) {
      //kein semikolon an der stelle erwartet
      assertEquals(valid.length() + 1, e.getPosition());
    }
  }


  public void testMultiply() throws XPRC_ParsingModelledExpressionException {
    String valid = "%3%.x=(%0%.abc*%1%.cde)*%2%.yxy.ds";
    ModelledExpression me = ModelledExpression.parse(helperContext(), valid);
    System.out.println(me);
  }

  public void testNew() throws XPRC_ParsingModelledExpressionException {
    String valid = "%1%.base~=new(\"bg.test.xfl.newAndCast.Sub2\")";
    ModelledExpression.parse(helperContext(), valid);
  }
  
  public void testCast2ArgsExpression() throws XPRC_ParsingModelledExpressionException {
    String valid = "%2%.text=concat(\"Network Access Control O\",%0%#cast(\"cl.dhcpbeispiel.SomeData\").deviceData#cast(\"cl.dhcpbeispiel.DeviceData\").mode+\"2\"+%1%#cast(\"cl.dhcpbeispiel.SomeData\").data,\"bject:1\")";
    ModelledExpression me = ModelledExpression.parse(helperContext(), valid);
    IdentityCreationVisitor icv = new IdentityCreationVisitor();
    me.visitSourceExpression(icv);
    System.out.println(icv.getXFLExpression());
  }
  
  
  public void testEmptyExpression() throws XPRC_ParsingModelledExpressionException {
    String valid = "(%4%,)";
    try {
      ModelledExpression.parse(helperContext(), valid);
      fail("expected exception");
    } catch (XPRC_ParsingModelledExpressionException e) {
      
    }
  }
  
}
