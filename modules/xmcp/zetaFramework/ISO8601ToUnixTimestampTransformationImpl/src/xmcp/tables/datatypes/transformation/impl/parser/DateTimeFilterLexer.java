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

// Generated from grammar/DateTimeFilter.g4 by ANTLR 4.7.2
package xmcp.tables.datatypes.transformation.impl.parser;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DateTimeFilterLexer extends Lexer {
    static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
        new PredictionContextCache();
    public static final int
        T__0=1, T__1=2, T__2=3, ZERO=4, ONE=5, TWO=6, THREE=7, FOUR=8, FIVE=9, 
        SIX=10, SEVEN=11, EIGHT=12, NINE=13, LESS_E=14, LESS_T=15, MORE_E=16, 
        MORE_T=17, AND=18, OR=19, DATE_SEP=20, TIME_SEP=21, TZ_UTC=22, TZ_D_WINTER=23, 
        TZ_D_SUMMER=24, SPACE=25, WS=26;
    public static String[] channelNames = {
        "DEFAULT_TOKEN_CHANNEL", "HIDDEN"
    };

    public static String[] modeNames = {
        "DEFAULT_MODE"
    };

    private static String[] makeRuleNames() {
        return new String[] {
            "T__0", "T__1", "T__2", "ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", 
            "SIX", "SEVEN", "EIGHT", "NINE", "LESS_E", "LESS_T", "MORE_E", "MORE_T", 
            "AND", "OR", "DATE_SEP", "TIME_SEP", "TZ_UTC", "TZ_D_WINTER", "TZ_D_SUMMER", 
            "SPACE", "WS"
        };
    }
    public static final String[] ruleNames = makeRuleNames();

    private static String[] makeLiteralNames() {
        return new String[] {
            null, "'T'", "'('", "')'", "'0'", "'1'", "'2'", "'3'", "'4'", "'5'", 
            "'6'", "'7'", "'8'", "'9'", "'<='", "'<'", "'>='", "'>'", null, null, 
            null, "':'", null, null, null, "' '"
        };
    }
    private static final String[] _LITERAL_NAMES = makeLiteralNames();
    private static String[] makeSymbolicNames() {
        return new String[] {
            null, null, null, null, "ZERO", "ONE", "TWO", "THREE", "FOUR", "FIVE", 
            "SIX", "SEVEN", "EIGHT", "NINE", "LESS_E", "LESS_T", "MORE_E", "MORE_T", 
            "AND", "OR", "DATE_SEP", "TIME_SEP", "TZ_UTC", "TZ_D_WINTER", "TZ_D_SUMMER", 
            "SPACE", "WS"
        };
    }
    private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
    public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

    /**
     * @deprecated Use {@link #VOCABULARY} instead.
     */
    @Deprecated
    public static final String[] tokenNames;
    static {
        tokenNames = new String[_SYMBOLIC_NAMES.length];
        for (int i = 0; i < tokenNames.length; i++) {
            tokenNames[i] = VOCABULARY.getLiteralName(i);
            if (tokenNames[i] == null) {
                tokenNames[i] = VOCABULARY.getSymbolicName(i);
            }

            if (tokenNames[i] == null) {
                tokenNames[i] = "<INVALID>";
            }
        }
    }

    @Override
    @Deprecated
    public String[] getTokenNames() {
        return tokenNames;
    }

    @Override

    public Vocabulary getVocabulary() {
        return VOCABULARY;
    }


    public DateTimeFilterLexer(CharStream input) {
        super(input);
        _interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
    }

    @Override
    public String getGrammarFileName() { return "DateTimeFilter.g4"; }

    @Override
    public String[] getRuleNames() { return ruleNames; }

    @Override
    public String getSerializedATN() { return _serializedATN; }

    @Override
    public String[] getChannelNames() { return channelNames; }

    @Override
    public String[] getModeNames() { return modeNames; }

    @Override
    public ATN getATN() { return _ATN; }

    public static final String _serializedATN =
        "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\2\34\u00ac\b\1\4\2"+
        "\t\2\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4"+
        "\13\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22"+
        "\t\22\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31"+
        "\t\31\4\32\t\32\4\33\t\33\3\2\3\2\3\3\3\3\3\4\3\4\3\5\3\5\3\6\3\6\3\7"+
        "\3\7\3\b\3\b\3\t\3\t\3\n\3\n\3\13\3\13\3\f\3\f\3\r\3\r\3\16\3\16\3\17"+
        "\3\17\3\17\3\20\3\20\3\21\3\21\3\21\3\22\3\22\3\23\3\23\3\23\3\23\3\23"+
        "\5\23a\n\23\3\24\3\24\3\24\3\24\5\24g\n\24\3\25\3\25\3\26\3\26\3\27\3"+
        "\27\3\27\3\27\3\27\3\27\3\27\5\27t\n\27\3\30\3\30\3\30\3\30\3\30\3\30"+
        "\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30\3\30"+
        "\3\30\3\30\5\30\u008c\n\30\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
        "\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31\3\31"+
        "\5\31\u00a5\n\31\3\32\3\32\3\33\3\33\3\33\3\33\2\2\34\3\3\5\4\7\5\t\6"+
        "\13\7\r\b\17\t\21\n\23\13\25\f\27\r\31\16\33\17\35\20\37\21!\22#\23%\24"+
        "\'\25)\26+\27-\30/\31\61\32\63\33\65\34\3\2\3\4\2\13\f\17\17\2\u00b5\2"+
        "\3\3\2\2\2\2\5\3\2\2\2\2\7\3\2\2\2\2\t\3\2\2\2\2\13\3\2\2\2\2\r\3\2\2"+
        "\2\2\17\3\2\2\2\2\21\3\2\2\2\2\23\3\2\2\2\2\25\3\2\2\2\2\27\3\2\2\2\2"+
        "\31\3\2\2\2\2\33\3\2\2\2\2\35\3\2\2\2\2\37\3\2\2\2\2!\3\2\2\2\2#\3\2\2"+
        "\2\2%\3\2\2\2\2\'\3\2\2\2\2)\3\2\2\2\2+\3\2\2\2\2-\3\2\2\2\2/\3\2\2\2"+
        "\2\61\3\2\2\2\2\63\3\2\2\2\2\65\3\2\2\2\3\67\3\2\2\2\59\3\2\2\2\7;\3\2"+
        "\2\2\t=\3\2\2\2\13?\3\2\2\2\rA\3\2\2\2\17C\3\2\2\2\21E\3\2\2\2\23G\3\2"+
        "\2\2\25I\3\2\2\2\27K\3\2\2\2\31M\3\2\2\2\33O\3\2\2\2\35Q\3\2\2\2\37T\3"+
        "\2\2\2!V\3\2\2\2#Y\3\2\2\2%`\3\2\2\2\'f\3\2\2\2)h\3\2\2\2+j\3\2\2\2-s"+
        "\3\2\2\2/\u008b\3\2\2\2\61\u00a4\3\2\2\2\63\u00a6\3\2\2\2\65\u00a8\3\2"+
        "\2\2\678\7V\2\28\4\3\2\2\29:\7*\2\2:\6\3\2\2\2;<\7+\2\2<\b\3\2\2\2=>\7"+
        "\62\2\2>\n\3\2\2\2?@\7\63\2\2@\f\3\2\2\2AB\7\64\2\2B\16\3\2\2\2CD\7\65"+
        "\2\2D\20\3\2\2\2EF\7\66\2\2F\22\3\2\2\2GH\7\67\2\2H\24\3\2\2\2IJ\78\2"+
        "\2J\26\3\2\2\2KL\79\2\2L\30\3\2\2\2MN\7:\2\2N\32\3\2\2\2OP\7;\2\2P\34"+
        "\3\2\2\2QR\7>\2\2RS\7?\2\2S\36\3\2\2\2TU\7>\2\2U \3\2\2\2VW\7@\2\2WX\7"+
        "?\2\2X\"\3\2\2\2YZ\7@\2\2Z$\3\2\2\2[\\\7(\2\2\\a\7(\2\2]^\7C\2\2^_\7P"+
        "\2\2_a\7F\2\2`[\3\2\2\2`]\3\2\2\2a&\3\2\2\2bc\7~\2\2cg\7~\2\2de\7Q\2\2"+
        "eg\7T\2\2fb\3\2\2\2fd\3\2\2\2g(\3\2\2\2hi\4/\60\2i*\3\2\2\2jk\7<\2\2k"+
        ",\3\2\2\2lt\7\\\2\2mn\7I\2\2no\7O\2\2ot\7V\2\2pq\7W\2\2qr\7V\2\2rt\7E"+
        "\2\2sl\3\2\2\2sm\3\2\2\2sp\3\2\2\2t.\3\2\2\2uv\7E\2\2vw\7G\2\2w\u008c"+
        "\7V\2\2xy\7I\2\2yz\7O\2\2z{\7V\2\2{|\7-\2\2|\u008c\7\63\2\2}~\7I\2\2~"+
        "\177\7O\2\2\177\u0080\7V\2\2\u0080\u0081\7-\2\2\u0081\u0082\7\62\2\2\u0082"+
        "\u008c\7\63\2\2\u0083\u0084\7I\2\2\u0084\u0085\7O\2\2\u0085\u0086\7V\2"+
        "\2\u0086\u0087\7-\2\2\u0087\u0088\7\62\2\2\u0088\u0089\7\63\2\2\u0089"+
        "\u008a\7\62\2\2\u008a\u008c\7\62\2\2\u008bu\3\2\2\2\u008bx\3\2\2\2\u008b"+
        "}\3\2\2\2\u008b\u0083\3\2\2\2\u008c\60\3\2\2\2\u008d\u008e\7E\2\2\u008e"+
        "\u008f\7G\2\2\u008f\u0090\7U\2\2\u0090\u00a5\7V\2\2\u0091\u0092\7I\2\2"+
        "\u0092\u0093\7O\2\2\u0093\u0094\7V\2\2\u0094\u0095\7-\2\2\u0095\u00a5"+
        "\7\64\2\2\u0096\u0097\7I\2\2\u0097\u0098\7O\2\2\u0098\u0099\7V\2\2\u0099"+
        "\u009a\7-\2\2\u009a\u009b\7\62\2\2\u009b\u00a5\7\64\2\2\u009c\u009d\7"+
        "I\2\2\u009d\u009e\7O\2\2\u009e\u009f\7V\2\2\u009f\u00a0\7-\2\2\u00a0\u00a1"+
        "\7\62\2\2\u00a1\u00a2\7\64\2\2\u00a2\u00a3\7\62\2\2\u00a3\u00a5\7\62\2"+
        "\2\u00a4\u008d\3\2\2\2\u00a4\u0091\3\2\2\2\u00a4\u0096\3\2\2\2\u00a4\u009c"+
        "\3\2\2\2\u00a5\62\3\2\2\2\u00a6\u00a7\7\"\2\2\u00a7\64\3\2\2\2\u00a8\u00a9"+
        "\t\2\2\2\u00a9\u00aa\3\2\2\2\u00aa\u00ab\b\33\2\2\u00ab\66\3\2\2\2\b\2"+
        "`fs\u008b\u00a4\3\b\2\2";
    public static final ATN _ATN =
        new ATNDeserializer().deserialize(_serializedATN.toCharArray());
    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}