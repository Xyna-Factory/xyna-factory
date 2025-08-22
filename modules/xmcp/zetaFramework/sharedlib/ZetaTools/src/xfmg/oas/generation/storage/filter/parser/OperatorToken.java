

package xfmg.oas.generation.storage.filter.parser;

import java.util.Optional;
import java.util.regex.Pattern;

// impl lexed-token
public class OperatorToken extends LexedToken {

  private static Pattern OPERATOR_PATTERN = Pattern.compile("[&|!()'\"]{1}");
  
  private final Enums.LexedOperatorCategory category;
  
  
  private OperatorToken(String input) {
    super(input);
    this.category = determineCategory(input);
  }

  private Enums.LexedOperatorCategory determineCategory(String input) {
    if ("&".equals(input) ) { return Enums.LexedOperatorCategory.AND; }
    if ("|".equals(input) ) { return Enums.LexedOperatorCategory.OR; }
    if ("!".equals(input) ) { return Enums.LexedOperatorCategory.NOT; }
    if ("(".equals(input) ) { return Enums.LexedOperatorCategory.OPEN; }
    if (")".equals(input) ) { return Enums.LexedOperatorCategory.CLOSE; }
    if ("'".equals(input) ) { return Enums.LexedOperatorCategory.SINGLE_QUOTE; }
    if ("\"".equals(input) ) { return Enums.LexedOperatorCategory.DOUBLE_QUOTE; }
    throw new IllegalArgumentException("Error in lexer. Unexpected character for operator: " + input);
  }
  
  
  public static Optional<LexedToken> buildIfMatches(String input) {
    if (input == null) { return Optional.empty(); }
    if (input.length() != 1) { return Optional.empty(); }
    if (OPERATOR_PATTERN.matcher(input).matches()) {
      return Optional.of(new OperatorToken(input));
    }
    return Optional.empty();
  }

  
  public Enums.LexedOperatorCategory getCategory() {
    return category;
  }

}
