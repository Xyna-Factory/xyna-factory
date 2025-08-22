

package xfmg.oas.generation.storage.filter.parser;


// impl lexed-token
public class Literal extends LexedToken {

  public Literal(String input) {
    super(input);
    if (input == null) {
      throw new IllegalArgumentException("Literal: Input is null.");
    }
    if (input.isBlank()) {
      throw new IllegalArgumentException("Literal: Input is only whitespace.");
    }
  }

  
  
  // adapt wildcard
  
  // contains wildcard
}
