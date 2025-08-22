

package xfmg.oas.generation.storage.filter.parser;


public class Enums {

  // lexed operator-type
  // and or not lesser greater equal open close single-quote double-quote
  public static enum LexedOperatorCategory {
    AND, OR, NOT, LESS_THAN, GREATER_THAN, EQUALS, OPEN, CLOSE, SINGLE_QUOTE, DOUBLE_QUOTE
  }
  
  // unary op type
  // not lesser greater equal
  
  // binary op type
  // and or
  
  // parsed elem type
  // container, relational-op, logical-op
}
