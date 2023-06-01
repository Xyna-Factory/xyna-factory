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

// Generated from grammar/DateTimeFilter.g4 by ANTLR 4.7.2
package xmcp.tables.datatypes.transformation.impl.parser;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link DateTimeFilterParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface DateTimeFilterVisitor<T> extends ParseTreeVisitor<T> {
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#start}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitStart(DateTimeFilterParser.StartContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#pos_digit}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitPos_digit(DateTimeFilterParser.Pos_digitContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#digit}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDigit(DateTimeFilterParser.DigitContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#days_febuary}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDays_febuary(DateTimeFilterParser.Days_febuaryContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#days_short}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDays_short(DateTimeFilterParser.Days_shortContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#days_long}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDays_long(DateTimeFilterParser.Days_longContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#month_febuary}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitMonth_febuary(DateTimeFilterParser.Month_febuaryContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#month_short}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitMonth_short(DateTimeFilterParser.Month_shortContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#month_long}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitMonth_long(DateTimeFilterParser.Month_longContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#years}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitYears(DateTimeFilterParser.YearsContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#date_year}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDate_year(DateTimeFilterParser.Date_yearContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#date_year_month}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDate_year_month(DateTimeFilterParser.Date_year_monthContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#date_full}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDate_full(DateTimeFilterParser.Date_fullContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#date}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDate(DateTimeFilterParser.DateContext ctx);
    /**
     * Visit a parse tree produced by the {@code singleDigitHours}
     * labeled alternative in {@link DateTimeFilterParser#hour}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitSingleDigitHours(DateTimeFilterParser.SingleDigitHoursContext ctx);
    /**
     * Visit a parse tree produced by the {@code oneHours}
     * labeled alternative in {@link DateTimeFilterParser#hour}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitOneHours(DateTimeFilterParser.OneHoursContext ctx);
    /**
     * Visit a parse tree produced by the {@code twoHours}
     * labeled alternative in {@link DateTimeFilterParser#hour}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitTwoHours(DateTimeFilterParser.TwoHoursContext ctx);
    /**
     * Visit a parse tree produced by the {@code singleDigitMinOrSec}
     * labeled alternative in {@link DateTimeFilterParser#min_or_sec}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitSingleDigitMinOrSec(DateTimeFilterParser.SingleDigitMinOrSecContext ctx);
    /**
     * Visit a parse tree produced by the {@code doubleDigitMinOrSec}
     * labeled alternative in {@link DateTimeFilterParser#min_or_sec}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDoubleDigitMinOrSec(DateTimeFilterParser.DoubleDigitMinOrSecContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#time_hour}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitTime_hour(DateTimeFilterParser.Time_hourContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#time_hour_min}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitTime_hour_min(DateTimeFilterParser.Time_hour_minContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#time_full}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitTime_full(DateTimeFilterParser.Time_fullContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#time}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitTime(DateTimeFilterParser.TimeContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#date_time}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitDate_time(DateTimeFilterParser.Date_timeContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#tz}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitTz(DateTimeFilterParser.TzContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#simple_condition}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitSimple_condition(DateTimeFilterParser.Simple_conditionContext ctx);
    /**
     * Visit a parse tree produced by {@link DateTimeFilterParser#op_condition}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitOp_condition(DateTimeFilterParser.Op_conditionContext ctx);
    /**
     * Visit a parse tree produced by the {@code orFilterCondition}
     * labeled alternative in {@link DateTimeFilterParser#filter_condition}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitOrFilterCondition(DateTimeFilterParser.OrFilterConditionContext ctx);
    /**
     * Visit a parse tree produced by the {@code simpleFilterCondition}
     * labeled alternative in {@link DateTimeFilterParser#filter_condition}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitSimpleFilterCondition(DateTimeFilterParser.SimpleFilterConditionContext ctx);
    /**
     * Visit a parse tree produced by the {@code andFilterCondition}
     * labeled alternative in {@link DateTimeFilterParser#filter_condition}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitAndFilterCondition(DateTimeFilterParser.AndFilterConditionContext ctx);
    /**
     * Visit a parse tree produced by the {@code opFilterCondition}
     * labeled alternative in {@link DateTimeFilterParser#filter_condition}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitOpFilterCondition(DateTimeFilterParser.OpFilterConditionContext ctx);
    /**
     * Visit a parse tree produced by the {@code parenthesisFilterCondition}
     * labeled alternative in {@link DateTimeFilterParser#filter_condition}.
     * @param ctx the parse tree
     * @return the visitor result
     */
    T visitParenthesisFilterCondition(DateTimeFilterParser.ParenthesisFilterConditionContext ctx);
}