

package xfmg.oas.generation.storage.filter.parser;

import java.util.Optional;

// impl lexed-token
public class Whitespace extends LexedToken {

  private Whitespace(String originalInput) {
    super(originalInput);
  }

  
  public static Optional<LexedToken> buildIfMatches(String input) {
    if (input.matches("\\s+")) {
      return Optional.of(new Whitespace(input));
    }
    return Optional.empty();
  }

}
