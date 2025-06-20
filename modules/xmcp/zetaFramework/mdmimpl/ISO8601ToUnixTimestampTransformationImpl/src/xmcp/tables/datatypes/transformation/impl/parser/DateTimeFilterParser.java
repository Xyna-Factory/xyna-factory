/*
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

// Generated from grammar/DateTimeFilter.g4 by ANTLR 4.7.2
package xmcp.tables.datatypes.transformation.impl.parser;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class DateTimeFilterParser extends Parser {
    static { RuntimeMetaData.checkVersion("4.7.2", RuntimeMetaData.VERSION); }

    protected static final DFA[] _decisionToDFA;
    protected static final PredictionContextCache _sharedContextCache =
        new PredictionContextCache();
    public static final int
        T__0=1, T__1=2, T__2=3, ZERO=4, ONE=5, TWO=6, THREE=7, FOUR=8, FIVE=9, 
        SIX=10, SEVEN=11, EIGHT=12, NINE=13, LESS_E=14, LESS_T=15, MORE_E=16, 
        MORE_T=17, AND=18, OR=19, DATE_SEP=20, TIME_SEP=21, TZ_UTC=22, TZ_D_WINTER=23, 
        TZ_D_SUMMER=24, SPACE=25, WS=26;
    public static final int
        RULE_start = 0, RULE_pos_digit = 1, RULE_digit = 2, RULE_days_febuary = 3, 
        RULE_days_short = 4, RULE_days_long = 5, RULE_month_febuary = 6, RULE_month_short = 7, 
        RULE_month_long = 8, RULE_years = 9, RULE_date_year = 10, RULE_date_year_month = 11, 
        RULE_date_full = 12, RULE_date = 13, RULE_hour = 14, RULE_min_or_sec = 15, 
        RULE_time_hour = 16, RULE_time_hour_min = 17, RULE_time_full = 18, RULE_time = 19, 
        RULE_date_time = 20, RULE_tz = 21, RULE_simple_condition = 22, RULE_op_condition = 23, 
        RULE_filter_condition = 24;
    private static String[] makeRuleNames() {
        return new String[] {
            "start", "pos_digit", "digit", "days_febuary", "days_short", "days_long", 
            "month_febuary", "month_short", "month_long", "years", "date_year", "date_year_month", 
            "date_full", "date", "hour", "min_or_sec", "time_hour", "time_hour_min", 
            "time_full", "time", "date_time", "tz", "simple_condition", "op_condition", 
            "filter_condition"
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

    @Override
    public String getGrammarFileName() { return "DateTimeFilter.g4"; }

    @Override
    public String[] getRuleNames() { return ruleNames; }

    @Override
    public String getSerializedATN() { return _serializedATN; }

    @Override
    public ATN getATN() { return _ATN; }

    public DateTimeFilterParser(TokenStream input) {
        super(input);
        _interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
    }

    public static class StartContext extends ParserRuleContext {
        public Filter_conditionContext filter_condition() {
            return getRuleContext(Filter_conditionContext.class,0);
        }
        public TerminalNode EOF() { return getToken(DateTimeFilterParser.EOF, 0); }
        public StartContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_start; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitStart(this);
            else return visitor.visitChildren(this);
        }
    }

    public final StartContext start() throws RecognitionException {
        StartContext _localctx = new StartContext(_ctx, getState());
        enterRule(_localctx, 0, RULE_start);
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(50);
            filter_condition(0);
            setState(51);
            match(EOF);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Pos_digitContext extends ParserRuleContext {
        public TerminalNode ONE() { return getToken(DateTimeFilterParser.ONE, 0); }
        public TerminalNode TWO() { return getToken(DateTimeFilterParser.TWO, 0); }
        public TerminalNode THREE() { return getToken(DateTimeFilterParser.THREE, 0); }
        public TerminalNode FOUR() { return getToken(DateTimeFilterParser.FOUR, 0); }
        public TerminalNode FIVE() { return getToken(DateTimeFilterParser.FIVE, 0); }
        public TerminalNode SIX() { return getToken(DateTimeFilterParser.SIX, 0); }
        public TerminalNode SEVEN() { return getToken(DateTimeFilterParser.SEVEN, 0); }
        public TerminalNode EIGHT() { return getToken(DateTimeFilterParser.EIGHT, 0); }
        public TerminalNode NINE() { return getToken(DateTimeFilterParser.NINE, 0); }
        public Pos_digitContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_pos_digit; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitPos_digit(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Pos_digitContext pos_digit() throws RecognitionException {
        Pos_digitContext _localctx = new Pos_digitContext(_ctx, getState());
        enterRule(_localctx, 2, RULE_pos_digit);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(53);
            _la = _input.LA(1);
            if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ONE) | (1L << TWO) | (1L << THREE) | (1L << FOUR) | (1L << FIVE) | (1L << SIX) | (1L << SEVEN) | (1L << EIGHT) | (1L << NINE))) != 0)) ) {
            _errHandler.recoverInline(this);
            }
            else {
                if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                _errHandler.reportMatch(this);
                consume();
            }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class DigitContext extends ParserRuleContext {
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public Pos_digitContext pos_digit() {
            return getRuleContext(Pos_digitContext.class,0);
        }
        public DigitContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_digit; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDigit(this);
            else return visitor.visitChildren(this);
        }
    }

    public final DigitContext digit() throws RecognitionException {
        DigitContext _localctx = new DigitContext(_ctx, getState());
        enterRule(_localctx, 4, RULE_digit);
        try {
            setState(57);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
            case ZERO:
                enterOuterAlt(_localctx, 1);
                {
                setState(55);
                match(ZERO);
                }
                break;
            case ONE:
            case TWO:
            case THREE:
            case FOUR:
            case FIVE:
            case SIX:
            case SEVEN:
            case EIGHT:
            case NINE:
                enterOuterAlt(_localctx, 2);
                {
                setState(56);
                pos_digit();
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Days_febuaryContext extends ParserRuleContext {
        public Pos_digitContext pos_digit() {
            return getRuleContext(Pos_digitContext.class,0);
        }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public DigitContext digit() {
            return getRuleContext(DigitContext.class,0);
        }
        public TerminalNode ONE() { return getToken(DateTimeFilterParser.ONE, 0); }
        public TerminalNode TWO() { return getToken(DateTimeFilterParser.TWO, 0); }
        public Days_febuaryContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_days_febuary; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDays_febuary(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Days_febuaryContext days_febuary() throws RecognitionException {
        Days_febuaryContext _localctx = new Days_febuaryContext(_ctx, getState());
        enterRule(_localctx, 6, RULE_days_febuary);
        int _la;
        try {
            setState(65);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,2,_ctx) ) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                setState(60);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la==ZERO) {
                    {
                    setState(59);
                    match(ZERO);
                    }
                }

                setState(62);
                pos_digit();
                }
                break;
            case 2:
                enterOuterAlt(_localctx, 2);
                {
                setState(63);
                _la = _input.LA(1);
                if ( !(_la==ONE || _la==TWO) ) {
                _errHandler.recoverInline(this);
                }
                else {
                    if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(64);
                digit();
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Days_shortContext extends ParserRuleContext {
        public Days_febuaryContext days_febuary() {
            return getRuleContext(Days_febuaryContext.class,0);
        }
        public TerminalNode THREE() { return getToken(DateTimeFilterParser.THREE, 0); }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public Days_shortContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_days_short; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDays_short(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Days_shortContext days_short() throws RecognitionException {
        Days_shortContext _localctx = new Days_shortContext(_ctx, getState());
        enterRule(_localctx, 8, RULE_days_short);
        try {
            setState(70);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                setState(67);
                days_febuary();
                }
                break;
            case 2:
                enterOuterAlt(_localctx, 2);
                {
                setState(68);
                match(THREE);
                setState(69);
                match(ZERO);
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Days_longContext extends ParserRuleContext {
        public Days_shortContext days_short() {
            return getRuleContext(Days_shortContext.class,0);
        }
        public TerminalNode THREE() { return getToken(DateTimeFilterParser.THREE, 0); }
        public TerminalNode ONE() { return getToken(DateTimeFilterParser.ONE, 0); }
        public Days_longContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_days_long; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDays_long(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Days_longContext days_long() throws RecognitionException {
        Days_longContext _localctx = new Days_longContext(_ctx, getState());
        enterRule(_localctx, 10, RULE_days_long);
        try {
            setState(75);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,4,_ctx) ) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                setState(72);
                days_short();
                }
                break;
            case 2:
                enterOuterAlt(_localctx, 2);
                {
                setState(73);
                match(THREE);
                setState(74);
                match(ONE);
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Month_febuaryContext extends ParserRuleContext {
        public TerminalNode TWO() { return getToken(DateTimeFilterParser.TWO, 0); }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public Month_febuaryContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_month_febuary; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitMonth_febuary(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Month_febuaryContext month_febuary() throws RecognitionException {
        Month_febuaryContext _localctx = new Month_febuaryContext(_ctx, getState());
        enterRule(_localctx, 12, RULE_month_febuary);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(78);
            _errHandler.sync(this);
            _la = _input.LA(1);
            if (_la==ZERO) {
                {
                setState(77);
                match(ZERO);
                }
            }

            setState(80);
            match(TWO);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Month_shortContext extends ParserRuleContext {
        public TerminalNode FOUR() { return getToken(DateTimeFilterParser.FOUR, 0); }
        public TerminalNode SIX() { return getToken(DateTimeFilterParser.SIX, 0); }
        public TerminalNode NINE() { return getToken(DateTimeFilterParser.NINE, 0); }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public List<TerminalNode> ONE() { return getTokens(DateTimeFilterParser.ONE); }
        public TerminalNode ONE(int i) {
            return getToken(DateTimeFilterParser.ONE, i);
        }
        public Month_shortContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_month_short; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitMonth_short(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Month_shortContext month_short() throws RecognitionException {
        Month_shortContext _localctx = new Month_shortContext(_ctx, getState());
        enterRule(_localctx, 14, RULE_month_short);
        int _la;
        try {
            setState(88);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
            case ZERO:
            case FOUR:
            case SIX:
            case NINE:
                enterOuterAlt(_localctx, 1);
                {
                setState(83);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la==ZERO) {
                    {
                    setState(82);
                    match(ZERO);
                    }
                }

                setState(85);
                _la = _input.LA(1);
                if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << FOUR) | (1L << SIX) | (1L << NINE))) != 0)) ) {
                _errHandler.recoverInline(this);
                }
                else {
                    if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                }
                break;
            case ONE:
                enterOuterAlt(_localctx, 2);
                {
                setState(86);
                match(ONE);
                setState(87);
                match(ONE);
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Month_longContext extends ParserRuleContext {
        public TerminalNode ONE() { return getToken(DateTimeFilterParser.ONE, 0); }
        public TerminalNode THREE() { return getToken(DateTimeFilterParser.THREE, 0); }
        public TerminalNode FIVE() { return getToken(DateTimeFilterParser.FIVE, 0); }
        public TerminalNode SEVEN() { return getToken(DateTimeFilterParser.SEVEN, 0); }
        public TerminalNode EIGHT() { return getToken(DateTimeFilterParser.EIGHT, 0); }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public TerminalNode TWO() { return getToken(DateTimeFilterParser.TWO, 0); }
        public Month_longContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_month_long; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitMonth_long(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Month_longContext month_long() throws RecognitionException {
        Month_longContext _localctx = new Month_longContext(_ctx, getState());
        enterRule(_localctx, 16, RULE_month_long);
        int _la;
        try {
            setState(96);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,9,_ctx) ) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                setState(91);
                _errHandler.sync(this);
                _la = _input.LA(1);
                if (_la==ZERO) {
                    {
                    setState(90);
                    match(ZERO);
                    }
                }

                setState(93);
                _la = _input.LA(1);
                if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ONE) | (1L << THREE) | (1L << FIVE) | (1L << SEVEN) | (1L << EIGHT))) != 0)) ) {
                _errHandler.recoverInline(this);
                }
                else {
                    if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                }
                break;
            case 2:
                enterOuterAlt(_localctx, 2);
                {
                setState(94);
                match(ONE);
                setState(95);
                _la = _input.LA(1);
                if ( !(_la==ZERO || _la==TWO) ) {
                _errHandler.recoverInline(this);
                }
                else {
                    if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class YearsContext extends ParserRuleContext {
        public TerminalNode ONE() { return getToken(DateTimeFilterParser.ONE, 0); }
        public List<TerminalNode> NINE() { return getTokens(DateTimeFilterParser.NINE); }
        public TerminalNode NINE(int i) {
            return getToken(DateTimeFilterParser.NINE, i);
        }
        public List<DigitContext> digit() {
            return getRuleContexts(DigitContext.class);
        }
        public DigitContext digit(int i) {
            return getRuleContext(DigitContext.class,i);
        }
        public TerminalNode SEVEN() { return getToken(DateTimeFilterParser.SEVEN, 0); }
        public TerminalNode EIGHT() { return getToken(DateTimeFilterParser.EIGHT, 0); }
        public TerminalNode TWO() { return getToken(DateTimeFilterParser.TWO, 0); }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public YearsContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_years; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitYears(this);
            else return visitor.visitChildren(this);
        }
    }

    public final YearsContext years() throws RecognitionException {
        YearsContext _localctx = new YearsContext(_ctx, getState());
        enterRule(_localctx, 18, RULE_years);
        int _la;
        try {
            setState(107);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
            case ONE:
                enterOuterAlt(_localctx, 1);
                {
                setState(98);
                match(ONE);
                setState(99);
                match(NINE);
                setState(100);
                _la = _input.LA(1);
                if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << SEVEN) | (1L << EIGHT) | (1L << NINE))) != 0)) ) {
                _errHandler.recoverInline(this);
                }
                else {
                    if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(101);
                digit();
                }
                break;
            case TWO:
                enterOuterAlt(_localctx, 2);
                {
                setState(102);
                match(TWO);
                setState(103);
                match(ZERO);
                setState(104);
                digit();
                setState(105);
                digit();
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Date_yearContext extends ParserRuleContext {
        public YearsContext years() {
            return getRuleContext(YearsContext.class,0);
        }
        public Date_yearContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_date_year; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDate_year(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Date_yearContext date_year() throws RecognitionException {
        Date_yearContext _localctx = new Date_yearContext(_ctx, getState());
        enterRule(_localctx, 20, RULE_date_year);
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(109);
            years();
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Date_year_monthContext extends ParserRuleContext {
        public YearsContext years() {
            return getRuleContext(YearsContext.class,0);
        }
        public TerminalNode DATE_SEP() { return getToken(DateTimeFilterParser.DATE_SEP, 0); }
        public Month_febuaryContext month_febuary() {
            return getRuleContext(Month_febuaryContext.class,0);
        }
        public Month_shortContext month_short() {
            return getRuleContext(Month_shortContext.class,0);
        }
        public Month_longContext month_long() {
            return getRuleContext(Month_longContext.class,0);
        }
        public Date_year_monthContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_date_year_month; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDate_year_month(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Date_year_monthContext date_year_month() throws RecognitionException {
        Date_year_monthContext _localctx = new Date_year_monthContext(_ctx, getState());
        enterRule(_localctx, 22, RULE_date_year_month);
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(111);
            years();
            setState(112);
            match(DATE_SEP);
            setState(116);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,11,_ctx) ) {
            case 1:
                {
                setState(113);
                month_febuary();
                }
                break;
            case 2:
                {
                setState(114);
                month_short();
                }
                break;
            case 3:
                {
                setState(115);
                month_long();
                }
                break;
            }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Date_fullContext extends ParserRuleContext {
        public YearsContext years() {
            return getRuleContext(YearsContext.class,0);
        }
        public List<TerminalNode> DATE_SEP() { return getTokens(DateTimeFilterParser.DATE_SEP); }
        public TerminalNode DATE_SEP(int i) {
            return getToken(DateTimeFilterParser.DATE_SEP, i);
        }
        public Month_febuaryContext month_febuary() {
            return getRuleContext(Month_febuaryContext.class,0);
        }
        public Days_febuaryContext days_febuary() {
            return getRuleContext(Days_febuaryContext.class,0);
        }
        public Month_shortContext month_short() {
            return getRuleContext(Month_shortContext.class,0);
        }
        public Days_shortContext days_short() {
            return getRuleContext(Days_shortContext.class,0);
        }
        public Month_longContext month_long() {
            return getRuleContext(Month_longContext.class,0);
        }
        public Days_longContext days_long() {
            return getRuleContext(Days_longContext.class,0);
        }
        public Date_fullContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_date_full; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDate_full(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Date_fullContext date_full() throws RecognitionException {
        Date_fullContext _localctx = new Date_fullContext(_ctx, getState());
        enterRule(_localctx, 24, RULE_date_full);
        try {
            setState(136);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,12,_ctx) ) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                setState(118);
                years();
                setState(119);
                match(DATE_SEP);
                setState(120);
                month_febuary();
                setState(121);
                match(DATE_SEP);
                setState(122);
                days_febuary();
                }
                break;
            case 2:
                enterOuterAlt(_localctx, 2);
                {
                setState(124);
                years();
                setState(125);
                match(DATE_SEP);
                setState(126);
                month_short();
                setState(127);
                match(DATE_SEP);
                setState(128);
                days_short();
                }
                break;
            case 3:
                enterOuterAlt(_localctx, 3);
                {
                setState(130);
                years();
                setState(131);
                match(DATE_SEP);
                setState(132);
                month_long();
                setState(133);
                match(DATE_SEP);
                setState(134);
                days_long();
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class DateContext extends ParserRuleContext {
        public Date_yearContext date_year() {
            return getRuleContext(Date_yearContext.class,0);
        }
        public Date_year_monthContext date_year_month() {
            return getRuleContext(Date_year_monthContext.class,0);
        }
        public Date_fullContext date_full() {
            return getRuleContext(Date_fullContext.class,0);
        }
        public DateContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_date; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDate(this);
            else return visitor.visitChildren(this);
        }
    }

    public final DateContext date() throws RecognitionException {
        DateContext _localctx = new DateContext(_ctx, getState());
        enterRule(_localctx, 26, RULE_date);
        try {
            setState(141);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,13,_ctx) ) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                setState(138);
                date_year();
                }
                break;
            case 2:
                enterOuterAlt(_localctx, 2);
                {
                setState(139);
                date_year_month();
                }
                break;
            case 3:
                enterOuterAlt(_localctx, 3);
                {
                setState(140);
                date_full();
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class HourContext extends ParserRuleContext {
        public HourContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_hour; }
     
        public HourContext() { }
        public void copyFrom(HourContext ctx) {
            super.copyFrom(ctx);
        }
    }
    public static class TwoHoursContext extends HourContext {
        public List<TerminalNode> TWO() { return getTokens(DateTimeFilterParser.TWO); }
        public TerminalNode TWO(int i) {
            return getToken(DateTimeFilterParser.TWO, i);
        }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public TerminalNode ONE() { return getToken(DateTimeFilterParser.ONE, 0); }
        public TerminalNode THREE() { return getToken(DateTimeFilterParser.THREE, 0); }
        public TwoHoursContext(HourContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitTwoHours(this);
            else return visitor.visitChildren(this);
        }
    }
    public static class SingleDigitHoursContext extends HourContext {
        public DigitContext digit() {
            return getRuleContext(DigitContext.class,0);
        }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public SingleDigitHoursContext(HourContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitSingleDigitHours(this);
            else return visitor.visitChildren(this);
        }
    }
    public static class OneHoursContext extends HourContext {
        public TerminalNode ONE() { return getToken(DateTimeFilterParser.ONE, 0); }
        public DigitContext digit() {
            return getRuleContext(DigitContext.class,0);
        }
        public OneHoursContext(HourContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitOneHours(this);
            else return visitor.visitChildren(this);
        }
    }

    public final HourContext hour() throws RecognitionException {
        HourContext _localctx = new HourContext(_ctx, getState());
        enterRule(_localctx, 28, RULE_hour);
        int _la;
        try {
            setState(151);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,15,_ctx) ) {
            case 1:
                _localctx = new SingleDigitHoursContext(_localctx);
                enterOuterAlt(_localctx, 1);
                {
                setState(144);
                _errHandler.sync(this);
                switch ( getInterpreter().adaptivePredict(_input,14,_ctx) ) {
                case 1:
                    {
                    setState(143);
                    match(ZERO);
                    }
                    break;
                }
                setState(146);
                digit();
                }
                break;
            case 2:
                _localctx = new OneHoursContext(_localctx);
                enterOuterAlt(_localctx, 2);
                {
                setState(147);
                match(ONE);
                setState(148);
                digit();
                }
                break;
            case 3:
                _localctx = new TwoHoursContext(_localctx);
                enterOuterAlt(_localctx, 3);
                {
                setState(149);
                match(TWO);
                setState(150);
                _la = _input.LA(1);
                if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ZERO) | (1L << ONE) | (1L << TWO) | (1L << THREE))) != 0)) ) {
                _errHandler.recoverInline(this);
                }
                else {
                    if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Min_or_secContext extends ParserRuleContext {
        public Min_or_secContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_min_or_sec; }
     
        public Min_or_secContext() { }
        public void copyFrom(Min_or_secContext ctx) {
            super.copyFrom(ctx);
        }
    }
    public static class SingleDigitMinOrSecContext extends Min_or_secContext {
        public DigitContext digit() {
            return getRuleContext(DigitContext.class,0);
        }
        public TerminalNode ZERO() { return getToken(DateTimeFilterParser.ZERO, 0); }
        public SingleDigitMinOrSecContext(Min_or_secContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitSingleDigitMinOrSec(this);
            else return visitor.visitChildren(this);
        }
    }
    public static class DoubleDigitMinOrSecContext extends Min_or_secContext {
        public DigitContext digit() {
            return getRuleContext(DigitContext.class,0);
        }
        public TerminalNode ONE() { return getToken(DateTimeFilterParser.ONE, 0); }
        public TerminalNode TWO() { return getToken(DateTimeFilterParser.TWO, 0); }
        public TerminalNode THREE() { return getToken(DateTimeFilterParser.THREE, 0); }
        public TerminalNode FOUR() { return getToken(DateTimeFilterParser.FOUR, 0); }
        public TerminalNode FIVE() { return getToken(DateTimeFilterParser.FIVE, 0); }
        public DoubleDigitMinOrSecContext(Min_or_secContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDoubleDigitMinOrSec(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Min_or_secContext min_or_sec() throws RecognitionException {
        Min_or_secContext _localctx = new Min_or_secContext(_ctx, getState());
        enterRule(_localctx, 30, RULE_min_or_sec);
        int _la;
        try {
            setState(159);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,17,_ctx) ) {
            case 1:
                _localctx = new SingleDigitMinOrSecContext(_localctx);
                enterOuterAlt(_localctx, 1);
                {
                setState(154);
                _errHandler.sync(this);
                switch ( getInterpreter().adaptivePredict(_input,16,_ctx) ) {
                case 1:
                    {
                    setState(153);
                    match(ZERO);
                    }
                    break;
                }
                setState(156);
                digit();
                }
                break;
            case 2:
                _localctx = new DoubleDigitMinOrSecContext(_localctx);
                enterOuterAlt(_localctx, 2);
                {
                setState(157);
                _la = _input.LA(1);
                if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << ONE) | (1L << TWO) | (1L << THREE) | (1L << FOUR) | (1L << FIVE))) != 0)) ) {
                _errHandler.recoverInline(this);
                }
                else {
                    if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                    _errHandler.reportMatch(this);
                    consume();
                }
                setState(158);
                digit();
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Time_hourContext extends ParserRuleContext {
        public HourContext hour() {
            return getRuleContext(HourContext.class,0);
        }
        public Time_hourContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_time_hour; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitTime_hour(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Time_hourContext time_hour() throws RecognitionException {
        Time_hourContext _localctx = new Time_hourContext(_ctx, getState());
        enterRule(_localctx, 32, RULE_time_hour);
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(161);
            hour();
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Time_hour_minContext extends ParserRuleContext {
        public HourContext hour() {
            return getRuleContext(HourContext.class,0);
        }
        public TerminalNode TIME_SEP() { return getToken(DateTimeFilterParser.TIME_SEP, 0); }
        public Min_or_secContext min_or_sec() {
            return getRuleContext(Min_or_secContext.class,0);
        }
        public Time_hour_minContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_time_hour_min; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitTime_hour_min(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Time_hour_minContext time_hour_min() throws RecognitionException {
        Time_hour_minContext _localctx = new Time_hour_minContext(_ctx, getState());
        enterRule(_localctx, 34, RULE_time_hour_min);
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(163);
            hour();
            setState(164);
            match(TIME_SEP);
            setState(165);
            min_or_sec();
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Time_fullContext extends ParserRuleContext {
        public HourContext hour() {
            return getRuleContext(HourContext.class,0);
        }
        public List<TerminalNode> TIME_SEP() { return getTokens(DateTimeFilterParser.TIME_SEP); }
        public TerminalNode TIME_SEP(int i) {
            return getToken(DateTimeFilterParser.TIME_SEP, i);
        }
        public List<Min_or_secContext> min_or_sec() {
            return getRuleContexts(Min_or_secContext.class);
        }
        public Min_or_secContext min_or_sec(int i) {
            return getRuleContext(Min_or_secContext.class,i);
        }
        public Time_fullContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_time_full; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitTime_full(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Time_fullContext time_full() throws RecognitionException {
        Time_fullContext _localctx = new Time_fullContext(_ctx, getState());
        enterRule(_localctx, 36, RULE_time_full);
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(167);
            hour();
            setState(168);
            match(TIME_SEP);
            setState(169);
            min_or_sec();
            setState(170);
            match(TIME_SEP);
            setState(171);
            min_or_sec();
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class TimeContext extends ParserRuleContext {
        public Time_hourContext time_hour() {
            return getRuleContext(Time_hourContext.class,0);
        }
        public Time_hour_minContext time_hour_min() {
            return getRuleContext(Time_hour_minContext.class,0);
        }
        public Time_fullContext time_full() {
            return getRuleContext(Time_fullContext.class,0);
        }
        public TimeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_time; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitTime(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TimeContext time() throws RecognitionException {
        TimeContext _localctx = new TimeContext(_ctx, getState());
        enterRule(_localctx, 38, RULE_time);
        try {
            setState(176);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,18,_ctx) ) {
            case 1:
                enterOuterAlt(_localctx, 1);
                {
                setState(173);
                time_hour();
                }
                break;
            case 2:
                enterOuterAlt(_localctx, 2);
                {
                setState(174);
                time_hour_min();
                }
                break;
            case 3:
                enterOuterAlt(_localctx, 3);
                {
                setState(175);
                time_full();
                }
                break;
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Date_timeContext extends ParserRuleContext {
        public Date_fullContext date_full() {
            return getRuleContext(Date_fullContext.class,0);
        }
        public TimeContext time() {
            return getRuleContext(TimeContext.class,0);
        }
        public List<TerminalNode> SPACE() { return getTokens(DateTimeFilterParser.SPACE); }
        public TerminalNode SPACE(int i) {
            return getToken(DateTimeFilterParser.SPACE, i);
        }
        public Date_timeContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_date_time; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitDate_time(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Date_timeContext date_time() throws RecognitionException {
        Date_timeContext _localctx = new Date_timeContext(_ctx, getState());
        enterRule(_localctx, 40, RULE_date_time);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(178);
            date_full();
            setState(185);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
            case T__0:
                {
                setState(179);
                match(T__0);
                }
                break;
            case SPACE:
                {
                setState(181); 
                _errHandler.sync(this);
                _la = _input.LA(1);
                do {
                    {
                    {
                    setState(180);
                    match(SPACE);
                    }
                    }
                    setState(183); 
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                } while ( _la==SPACE );
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
            setState(187);
            time();
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class TzContext extends ParserRuleContext {
        public TerminalNode TZ_UTC() { return getToken(DateTimeFilterParser.TZ_UTC, 0); }
        public TerminalNode TZ_D_WINTER() { return getToken(DateTimeFilterParser.TZ_D_WINTER, 0); }
        public TerminalNode TZ_D_SUMMER() { return getToken(DateTimeFilterParser.TZ_D_SUMMER, 0); }
        public TzContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_tz; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitTz(this);
            else return visitor.visitChildren(this);
        }
    }

    public final TzContext tz() throws RecognitionException {
        TzContext _localctx = new TzContext(_ctx, getState());
        enterRule(_localctx, 42, RULE_tz);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(189);
            _la = _input.LA(1);
            if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << TZ_UTC) | (1L << TZ_D_WINTER) | (1L << TZ_D_SUMMER))) != 0)) ) {
            _errHandler.recoverInline(this);
            }
            else {
                if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                _errHandler.reportMatch(this);
                consume();
            }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Simple_conditionContext extends ParserRuleContext {
        public Date_timeContext date_time() {
            return getRuleContext(Date_timeContext.class,0);
        }
        public DateContext date() {
            return getRuleContext(DateContext.class,0);
        }
        public TimeContext time() {
            return getRuleContext(TimeContext.class,0);
        }
        public TzContext tz() {
            return getRuleContext(TzContext.class,0);
        }
        public List<TerminalNode> SPACE() { return getTokens(DateTimeFilterParser.SPACE); }
        public TerminalNode SPACE(int i) {
            return getToken(DateTimeFilterParser.SPACE, i);
        }
        public Simple_conditionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_simple_condition; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitSimple_condition(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Simple_conditionContext simple_condition() throws RecognitionException {
        Simple_conditionContext _localctx = new Simple_conditionContext(_ctx, getState());
        enterRule(_localctx, 44, RULE_simple_condition);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(194);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,21,_ctx) ) {
            case 1:
                {
                setState(191);
                date_time();
                }
                break;
            case 2:
                {
                setState(192);
                date();
                }
                break;
            case 3:
                {
                setState(193);
                time();
                }
                break;
            }
            setState(203);
            _errHandler.sync(this);
            switch ( getInterpreter().adaptivePredict(_input,23,_ctx) ) {
            case 1:
                {
                setState(199);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la==SPACE) {
                    {
                    {
                    setState(196);
                    match(SPACE);
                    }
                    }
                    setState(201);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(202);
                tz();
                }
                break;
            }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Op_conditionContext extends ParserRuleContext {
        public Token operator;
        public Simple_conditionContext simple_condition() {
            return getRuleContext(Simple_conditionContext.class,0);
        }
        public TerminalNode LESS_E() { return getToken(DateTimeFilterParser.LESS_E, 0); }
        public TerminalNode LESS_T() { return getToken(DateTimeFilterParser.LESS_T, 0); }
        public TerminalNode MORE_E() { return getToken(DateTimeFilterParser.MORE_E, 0); }
        public TerminalNode MORE_T() { return getToken(DateTimeFilterParser.MORE_T, 0); }
        public List<TerminalNode> SPACE() { return getTokens(DateTimeFilterParser.SPACE); }
        public TerminalNode SPACE(int i) {
            return getToken(DateTimeFilterParser.SPACE, i);
        }
        public Op_conditionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_op_condition; }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitOp_condition(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Op_conditionContext op_condition() throws RecognitionException {
        Op_conditionContext _localctx = new Op_conditionContext(_ctx, getState());
        enterRule(_localctx, 46, RULE_op_condition);
        int _la;
        try {
            enterOuterAlt(_localctx, 1);
            {
            setState(205);
            ((Op_conditionContext)_localctx).operator = _input.LT(1);
            _la = _input.LA(1);
            if ( !((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << LESS_E) | (1L << LESS_T) | (1L << MORE_E) | (1L << MORE_T))) != 0)) ) {
                ((Op_conditionContext)_localctx).operator = (Token)_errHandler.recoverInline(this);
            }
            else {
                if ( _input.LA(1)==Token.EOF ) matchedEOF = true;
                _errHandler.reportMatch(this);
                consume();
            }
            setState(209);
            _errHandler.sync(this);
            _la = _input.LA(1);
            while (_la==SPACE) {
                {
                {
                setState(206);
                match(SPACE);
                }
                }
                setState(211);
                _errHandler.sync(this);
                _la = _input.LA(1);
            }
            setState(212);
            simple_condition();
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            exitRule();
        }
        return _localctx;
    }

    public static class Filter_conditionContext extends ParserRuleContext {
        public Filter_conditionContext(ParserRuleContext parent, int invokingState) {
            super(parent, invokingState);
        }
        @Override public int getRuleIndex() { return RULE_filter_condition; }
     
        public Filter_conditionContext() { }
        public void copyFrom(Filter_conditionContext ctx) {
            super.copyFrom(ctx);
        }
    }
    public static class OrFilterConditionContext extends Filter_conditionContext {
        public List<Filter_conditionContext> filter_condition() {
            return getRuleContexts(Filter_conditionContext.class);
        }
        public Filter_conditionContext filter_condition(int i) {
            return getRuleContext(Filter_conditionContext.class,i);
        }
        public TerminalNode OR() { return getToken(DateTimeFilterParser.OR, 0); }
        public List<TerminalNode> SPACE() { return getTokens(DateTimeFilterParser.SPACE); }
        public TerminalNode SPACE(int i) {
            return getToken(DateTimeFilterParser.SPACE, i);
        }
        public OrFilterConditionContext(Filter_conditionContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitOrFilterCondition(this);
            else return visitor.visitChildren(this);
        }
    }
    public static class SimpleFilterConditionContext extends Filter_conditionContext {
        public Simple_conditionContext simple_condition() {
            return getRuleContext(Simple_conditionContext.class,0);
        }
        public SimpleFilterConditionContext(Filter_conditionContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitSimpleFilterCondition(this);
            else return visitor.visitChildren(this);
        }
    }
    public static class AndFilterConditionContext extends Filter_conditionContext {
        public List<Filter_conditionContext> filter_condition() {
            return getRuleContexts(Filter_conditionContext.class);
        }
        public Filter_conditionContext filter_condition(int i) {
            return getRuleContext(Filter_conditionContext.class,i);
        }
        public TerminalNode AND() { return getToken(DateTimeFilterParser.AND, 0); }
        public List<TerminalNode> SPACE() { return getTokens(DateTimeFilterParser.SPACE); }
        public TerminalNode SPACE(int i) {
            return getToken(DateTimeFilterParser.SPACE, i);
        }
        public AndFilterConditionContext(Filter_conditionContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitAndFilterCondition(this);
            else return visitor.visitChildren(this);
        }
    }
    public static class OpFilterConditionContext extends Filter_conditionContext {
        public Op_conditionContext op_condition() {
            return getRuleContext(Op_conditionContext.class,0);
        }
        public OpFilterConditionContext(Filter_conditionContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitOpFilterCondition(this);
            else return visitor.visitChildren(this);
        }
    }
    public static class ParenthesisFilterConditionContext extends Filter_conditionContext {
        public Filter_conditionContext filter_condition() {
            return getRuleContext(Filter_conditionContext.class,0);
        }
        public List<TerminalNode> SPACE() { return getTokens(DateTimeFilterParser.SPACE); }
        public TerminalNode SPACE(int i) {
            return getToken(DateTimeFilterParser.SPACE, i);
        }
        public ParenthesisFilterConditionContext(Filter_conditionContext ctx) { copyFrom(ctx); }
        @Override
        public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
            if ( visitor instanceof DateTimeFilterVisitor ) return ((DateTimeFilterVisitor<? extends T>)visitor).visitParenthesisFilterCondition(this);
            else return visitor.visitChildren(this);
        }
    }

    public final Filter_conditionContext filter_condition() throws RecognitionException {
        return filter_condition(0);
    }

    private Filter_conditionContext filter_condition(int _p) throws RecognitionException {
        ParserRuleContext _parentctx = _ctx;
        int _parentState = getState();
        Filter_conditionContext _localctx = new Filter_conditionContext(_ctx, _parentState);
        Filter_conditionContext _prevctx = _localctx;
        int _startState = 48;
        enterRecursionRule(_localctx, 48, RULE_filter_condition, _p);
        int _la;
        try {
            int _alt;
            enterOuterAlt(_localctx, 1);
            {
            setState(233);
            _errHandler.sync(this);
            switch (_input.LA(1)) {
            case T__1:
                {
                _localctx = new ParenthesisFilterConditionContext(_localctx);
                _ctx = _localctx;
                _prevctx = _localctx;

                setState(215);
                match(T__1);
                setState(219);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la==SPACE) {
                    {
                    {
                    setState(216);
                    match(SPACE);
                    }
                    }
                    setState(221);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(222);
                filter_condition(0);
                setState(226);
                _errHandler.sync(this);
                _la = _input.LA(1);
                while (_la==SPACE) {
                    {
                    {
                    setState(223);
                    match(SPACE);
                    }
                    }
                    setState(228);
                    _errHandler.sync(this);
                    _la = _input.LA(1);
                }
                setState(229);
                match(T__2);
                }
                break;
            case LESS_E:
            case LESS_T:
            case MORE_E:
            case MORE_T:
                {
                _localctx = new OpFilterConditionContext(_localctx);
                _ctx = _localctx;
                _prevctx = _localctx;
                setState(231);
                op_condition();
                }
                break;
            case ZERO:
            case ONE:
            case TWO:
            case THREE:
            case FOUR:
            case FIVE:
            case SIX:
            case SEVEN:
            case EIGHT:
            case NINE:
                {
                _localctx = new SimpleFilterConditionContext(_localctx);
                _ctx = _localctx;
                _prevctx = _localctx;
                setState(232);
                simple_condition();
                }
                break;
            default:
                throw new NoViableAltException(this);
            }
            _ctx.stop = _input.LT(-1);
            setState(267);
            _errHandler.sync(this);
            _alt = getInterpreter().adaptivePredict(_input,33,_ctx);
            while ( _alt!=2 && _alt!=org.antlr.v4.runtime.atn.ATN.INVALID_ALT_NUMBER ) {
                if ( _alt==1 ) {
                    if ( _parseListeners!=null ) triggerExitRuleEvent();
                    _prevctx = _localctx;
                    {
                    setState(265);
                    _errHandler.sync(this);
                    switch ( getInterpreter().adaptivePredict(_input,32,_ctx) ) {
                    case 1:
                        {
                        _localctx = new AndFilterConditionContext(new Filter_conditionContext(_parentctx, _parentState));
                        pushNewRecursionContext(_localctx, _startState, RULE_filter_condition);
                        setState(235);
                        if (!(precpred(_ctx, 4))) throw new FailedPredicateException(this, "precpred(_ctx, 4)");
                        setState(239);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la==SPACE) {
                            {
                            {
                            setState(236);
                            match(SPACE);
                            }
                            }
                            setState(241);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(242);
                        match(AND);
                        setState(246);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la==SPACE) {
                            {
                            {
                            setState(243);
                            match(SPACE);
                            }
                            }
                            setState(248);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(249);
                        filter_condition(5);
                        }
                        break;
                    case 2:
                        {
                        _localctx = new OrFilterConditionContext(new Filter_conditionContext(_parentctx, _parentState));
                        pushNewRecursionContext(_localctx, _startState, RULE_filter_condition);
                        setState(250);
                        if (!(precpred(_ctx, 3))) throw new FailedPredicateException(this, "precpred(_ctx, 3)");
                        setState(254);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la==SPACE) {
                            {
                            {
                            setState(251);
                            match(SPACE);
                            }
                            }
                            setState(256);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(257);
                        match(OR);
                        setState(261);
                        _errHandler.sync(this);
                        _la = _input.LA(1);
                        while (_la==SPACE) {
                            {
                            {
                            setState(258);
                            match(SPACE);
                            }
                            }
                            setState(263);
                            _errHandler.sync(this);
                            _la = _input.LA(1);
                        }
                        setState(264);
                        filter_condition(4);
                        }
                        break;
                    }
                    } 
                }
                setState(269);
                _errHandler.sync(this);
                _alt = getInterpreter().adaptivePredict(_input,33,_ctx);
            }
            }
        }
        catch (RecognitionException re) {
            _localctx.exception = re;
            _errHandler.reportError(this, re);
            _errHandler.recover(this, re);
        }
        finally {
            unrollRecursionContexts(_parentctx);
        }
        return _localctx;
    }

    public boolean sempred(RuleContext _localctx, int ruleIndex, int predIndex) {
        switch (ruleIndex) {
        case 24:
            return filter_condition_sempred((Filter_conditionContext)_localctx, predIndex);
        }
        return true;
    }
    private boolean filter_condition_sempred(Filter_conditionContext _localctx, int predIndex) {
        switch (predIndex) {
        case 0:
            return precpred(_ctx, 4);
        case 1:
            return precpred(_ctx, 3);
        }
        return true;
    }

    public static final String _serializedATN =
        "\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\34\u0111\4\2\t\2"+
        "\4\3\t\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\4\t\t\t\4\n\t\n\4\13"+
        "\t\13\4\f\t\f\4\r\t\r\4\16\t\16\4\17\t\17\4\20\t\20\4\21\t\21\4\22\t\22"+
        "\4\23\t\23\4\24\t\24\4\25\t\25\4\26\t\26\4\27\t\27\4\30\t\30\4\31\t\31"+
        "\4\32\t\32\3\2\3\2\3\2\3\3\3\3\3\4\3\4\5\4<\n\4\3\5\5\5?\n\5\3\5\3\5\3"+
        "\5\5\5D\n\5\3\6\3\6\3\6\5\6I\n\6\3\7\3\7\3\7\5\7N\n\7\3\b\5\bQ\n\b\3\b"+
        "\3\b\3\t\5\tV\n\t\3\t\3\t\3\t\5\t[\n\t\3\n\5\n^\n\n\3\n\3\n\3\n\5\nc\n"+
        "\n\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\3\13\5\13n\n\13\3\f\3\f\3\r"+
        "\3\r\3\r\3\r\3\r\5\rw\n\r\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16"+
        "\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\3\16\5\16\u008b\n\16\3\17\3\17"+
        "\3\17\5\17\u0090\n\17\3\20\5\20\u0093\n\20\3\20\3\20\3\20\3\20\3\20\5"+
        "\20\u009a\n\20\3\21\5\21\u009d\n\21\3\21\3\21\3\21\5\21\u00a2\n\21\3\22"+
        "\3\22\3\23\3\23\3\23\3\23\3\24\3\24\3\24\3\24\3\24\3\24\3\25\3\25\3\25"+
        "\5\25\u00b3\n\25\3\26\3\26\3\26\6\26\u00b8\n\26\r\26\16\26\u00b9\5\26"+
        "\u00bc\n\26\3\26\3\26\3\27\3\27\3\30\3\30\3\30\5\30\u00c5\n\30\3\30\7"+
        "\30\u00c8\n\30\f\30\16\30\u00cb\13\30\3\30\5\30\u00ce\n\30\3\31\3\31\7"+
        "\31\u00d2\n\31\f\31\16\31\u00d5\13\31\3\31\3\31\3\32\3\32\3\32\7\32\u00dc"+
        "\n\32\f\32\16\32\u00df\13\32\3\32\3\32\7\32\u00e3\n\32\f\32\16\32\u00e6"+
        "\13\32\3\32\3\32\3\32\3\32\5\32\u00ec\n\32\3\32\3\32\7\32\u00f0\n\32\f"+
        "\32\16\32\u00f3\13\32\3\32\3\32\7\32\u00f7\n\32\f\32\16\32\u00fa\13\32"+
        "\3\32\3\32\3\32\7\32\u00ff\n\32\f\32\16\32\u0102\13\32\3\32\3\32\7\32"+
        "\u0106\n\32\f\32\16\32\u0109\13\32\3\32\7\32\u010c\n\32\f\32\16\32\u010f"+
        "\13\32\3\32\2\3\62\33\2\4\6\b\n\f\16\20\22\24\26\30\32\34\36 \"$&(*,."+
        "\60\62\2\f\3\2\7\17\3\2\7\b\5\2\n\n\f\f\17\17\6\2\7\7\t\t\13\13\r\16\4"+
        "\2\6\6\b\b\3\2\r\17\3\2\6\t\3\2\7\13\3\2\30\32\3\2\20\23\2\u0120\2\64"+
        "\3\2\2\2\4\67\3\2\2\2\6;\3\2\2\2\bC\3\2\2\2\nH\3\2\2\2\fM\3\2\2\2\16P"+
        "\3\2\2\2\20Z\3\2\2\2\22b\3\2\2\2\24m\3\2\2\2\26o\3\2\2\2\30q\3\2\2\2\32"+
        "\u008a\3\2\2\2\34\u008f\3\2\2\2\36\u0099\3\2\2\2 \u00a1\3\2\2\2\"\u00a3"+
        "\3\2\2\2$\u00a5\3\2\2\2&\u00a9\3\2\2\2(\u00b2\3\2\2\2*\u00b4\3\2\2\2,"+
        "\u00bf\3\2\2\2.\u00c4\3\2\2\2\60\u00cf\3\2\2\2\62\u00eb\3\2\2\2\64\65"+
        "\5\62\32\2\65\66\7\2\2\3\66\3\3\2\2\2\678\t\2\2\28\5\3\2\2\29<\7\6\2\2"+
        ":<\5\4\3\2;9\3\2\2\2;:\3\2\2\2<\7\3\2\2\2=?\7\6\2\2>=\3\2\2\2>?\3\2\2"+
        "\2?@\3\2\2\2@D\5\4\3\2AB\t\3\2\2BD\5\6\4\2C>\3\2\2\2CA\3\2\2\2D\t\3\2"+
        "\2\2EI\5\b\5\2FG\7\t\2\2GI\7\6\2\2HE\3\2\2\2HF\3\2\2\2I\13\3\2\2\2JN\5"+
        "\n\6\2KL\7\t\2\2LN\7\7\2\2MJ\3\2\2\2MK\3\2\2\2N\r\3\2\2\2OQ\7\6\2\2PO"+
        "\3\2\2\2PQ\3\2\2\2QR\3\2\2\2RS\7\b\2\2S\17\3\2\2\2TV\7\6\2\2UT\3\2\2\2"+
        "UV\3\2\2\2VW\3\2\2\2W[\t\4\2\2XY\7\7\2\2Y[\7\7\2\2ZU\3\2\2\2ZX\3\2\2\2"+
        "[\21\3\2\2\2\\^\7\6\2\2]\\\3\2\2\2]^\3\2\2\2^_\3\2\2\2_c\t\5\2\2`a\7\7"+
        "\2\2ac\t\6\2\2b]\3\2\2\2b`\3\2\2\2c\23\3\2\2\2de\7\7\2\2ef\7\17\2\2fg"+
        "\t\7\2\2gn\5\6\4\2hi\7\b\2\2ij\7\6\2\2jk\5\6\4\2kl\5\6\4\2ln\3\2\2\2m"+
        "d\3\2\2\2mh\3\2\2\2n\25\3\2\2\2op\5\24\13\2p\27\3\2\2\2qr\5\24\13\2rv"+
        "\7\26\2\2sw\5\16\b\2tw\5\20\t\2uw\5\22\n\2vs\3\2\2\2vt\3\2\2\2vu\3\2\2"+
        "\2w\31\3\2\2\2xy\5\24\13\2yz\7\26\2\2z{\5\16\b\2{|\7\26\2\2|}\5\b\5\2"+
        "}\u008b\3\2\2\2~\177\5\24\13\2\177\u0080\7\26\2\2\u0080\u0081\5\20\t\2"+
        "\u0081\u0082\7\26\2\2\u0082\u0083\5\n\6\2\u0083\u008b\3\2\2\2\u0084\u0085"+
        "\5\24\13\2\u0085\u0086\7\26\2\2\u0086\u0087\5\22\n\2\u0087\u0088\7\26"+
        "\2\2\u0088\u0089\5\f\7\2\u0089\u008b\3\2\2\2\u008ax\3\2\2\2\u008a~\3\2"+
        "\2\2\u008a\u0084\3\2\2\2\u008b\33\3\2\2\2\u008c\u0090\5\26\f\2\u008d\u0090"+
        "\5\30\r\2\u008e\u0090\5\32\16\2\u008f\u008c\3\2\2\2\u008f\u008d\3\2\2"+
        "\2\u008f\u008e\3\2\2\2\u0090\35\3\2\2\2\u0091\u0093\7\6\2\2\u0092\u0091"+
        "\3\2\2\2\u0092\u0093\3\2\2\2\u0093\u0094\3\2\2\2\u0094\u009a\5\6\4\2\u0095"+
        "\u0096\7\7\2\2\u0096\u009a\5\6\4\2\u0097\u0098\7\b\2\2\u0098\u009a\t\b"+
        "\2\2\u0099\u0092\3\2\2\2\u0099\u0095\3\2\2\2\u0099\u0097\3\2\2\2\u009a"+
        "\37\3\2\2\2\u009b\u009d\7\6\2\2\u009c\u009b\3\2\2\2\u009c\u009d\3\2\2"+
        "\2\u009d\u009e\3\2\2\2\u009e\u00a2\5\6\4\2\u009f\u00a0\t\t\2\2\u00a0\u00a2"+
        "\5\6\4\2\u00a1\u009c\3\2\2\2\u00a1\u009f\3\2\2\2\u00a2!\3\2\2\2\u00a3"+
        "\u00a4\5\36\20\2\u00a4#\3\2\2\2\u00a5\u00a6\5\36\20\2\u00a6\u00a7\7\27"+
        "\2\2\u00a7\u00a8\5 \21\2\u00a8%\3\2\2\2\u00a9\u00aa\5\36\20\2\u00aa\u00ab"+
        "\7\27\2\2\u00ab\u00ac\5 \21\2\u00ac\u00ad\7\27\2\2\u00ad\u00ae\5 \21\2"+
        "\u00ae\'\3\2\2\2\u00af\u00b3\5\"\22\2\u00b0\u00b3\5$\23\2\u00b1\u00b3"+
        "\5&\24\2\u00b2\u00af\3\2\2\2\u00b2\u00b0\3\2\2\2\u00b2\u00b1\3\2\2\2\u00b3"+
        ")\3\2\2\2\u00b4\u00bb\5\32\16\2\u00b5\u00bc\7\3\2\2\u00b6\u00b8\7\33\2"+
        "\2\u00b7\u00b6\3\2\2\2\u00b8\u00b9\3\2\2\2\u00b9\u00b7\3\2\2\2\u00b9\u00ba"+
        "\3\2\2\2\u00ba\u00bc\3\2\2\2\u00bb\u00b5\3\2\2\2\u00bb\u00b7\3\2\2\2\u00bc"+
        "\u00bd\3\2\2\2\u00bd\u00be\5(\25\2\u00be+\3\2\2\2\u00bf\u00c0\t\n\2\2"+
        "\u00c0-\3\2\2\2\u00c1\u00c5\5*\26\2\u00c2\u00c5\5\34\17\2\u00c3\u00c5"+
        "\5(\25\2\u00c4\u00c1\3\2\2\2\u00c4\u00c2\3\2\2\2\u00c4\u00c3\3\2\2\2\u00c5"+
        "\u00cd\3\2\2\2\u00c6\u00c8\7\33\2\2\u00c7\u00c6\3\2\2\2\u00c8\u00cb\3"+
        "\2\2\2\u00c9\u00c7\3\2\2\2\u00c9\u00ca\3\2\2\2\u00ca\u00cc\3\2\2\2\u00cb"+
        "\u00c9\3\2\2\2\u00cc\u00ce\5,\27\2\u00cd\u00c9\3\2\2\2\u00cd\u00ce\3\2"+
        "\2\2\u00ce/\3\2\2\2\u00cf\u00d3\t\13\2\2\u00d0\u00d2\7\33\2\2\u00d1\u00d0"+
        "\3\2\2\2\u00d2\u00d5\3\2\2\2\u00d3\u00d1\3\2\2\2\u00d3\u00d4\3\2\2\2\u00d4"+
        "\u00d6\3\2\2\2\u00d5\u00d3\3\2\2\2\u00d6\u00d7\5.\30\2\u00d7\61\3\2\2"+
        "\2\u00d8\u00d9\b\32\1\2\u00d9\u00dd\7\4\2\2\u00da\u00dc\7\33\2\2\u00db"+
        "\u00da\3\2\2\2\u00dc\u00df\3\2\2\2\u00dd\u00db\3\2\2\2\u00dd\u00de\3\2"+
        "\2\2\u00de\u00e0\3\2\2\2\u00df\u00dd\3\2\2\2\u00e0\u00e4\5\62\32\2\u00e1"+
        "\u00e3\7\33\2\2\u00e2\u00e1\3\2\2\2\u00e3\u00e6\3\2\2\2\u00e4\u00e2\3"+
        "\2\2\2\u00e4\u00e5\3\2\2\2\u00e5\u00e7\3\2\2\2\u00e6\u00e4\3\2\2\2\u00e7"+
        "\u00e8\7\5\2\2\u00e8\u00ec\3\2\2\2\u00e9\u00ec\5\60\31\2\u00ea\u00ec\5"+
        ".\30\2\u00eb\u00d8\3\2\2\2\u00eb\u00e9\3\2\2\2\u00eb\u00ea\3\2\2\2\u00ec"+
        "\u010d\3\2\2\2\u00ed\u00f1\f\6\2\2\u00ee\u00f0\7\33\2\2\u00ef\u00ee\3"+
        "\2\2\2\u00f0\u00f3\3\2\2\2\u00f1\u00ef\3\2\2\2\u00f1\u00f2\3\2\2\2\u00f2"+
        "\u00f4\3\2\2\2\u00f3\u00f1\3\2\2\2\u00f4\u00f8\7\24\2\2\u00f5\u00f7\7"+
        "\33\2\2\u00f6\u00f5\3\2\2\2\u00f7\u00fa\3\2\2\2\u00f8\u00f6\3\2\2\2\u00f8"+
        "\u00f9\3\2\2\2\u00f9\u00fb\3\2\2\2\u00fa\u00f8\3\2\2\2\u00fb\u010c\5\62"+
        "\32\7\u00fc\u0100\f\5\2\2\u00fd\u00ff\7\33\2\2\u00fe\u00fd\3\2\2\2\u00ff"+
        "\u0102\3\2\2\2\u0100\u00fe\3\2\2\2\u0100\u0101\3\2\2\2\u0101\u0103\3\2"+
        "\2\2\u0102\u0100\3\2\2\2\u0103\u0107\7\25\2\2\u0104\u0106\7\33\2\2\u0105"+
        "\u0104\3\2\2\2\u0106\u0109\3\2\2\2\u0107\u0105\3\2\2\2\u0107\u0108\3\2"+
        "\2\2\u0108\u010a\3\2\2\2\u0109\u0107\3\2\2\2\u010a\u010c\5\62\32\6\u010b"+
        "\u00ed\3\2\2\2\u010b\u00fc\3\2\2\2\u010c\u010f\3\2\2\2\u010d\u010b\3\2"+
        "\2\2\u010d\u010e\3\2\2\2\u010e\63\3\2\2\2\u010f\u010d\3\2\2\2$;>CHMPU"+
        "Z]bmv\u008a\u008f\u0092\u0099\u009c\u00a1\u00b2\u00b9\u00bb\u00c4\u00c9"+
        "\u00cd\u00d3\u00dd\u00e4\u00eb\u00f1\u00f8\u0100\u0107\u010b\u010d";
    public static final ATN _ATN =
        new ATNDeserializer().deserialize(_serializedATN.toCharArray());
    static {
        _decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
        for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
            _decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
        }
    }
}