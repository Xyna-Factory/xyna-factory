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

package xmcp.tables.datatypes.transformation.impl;

import xmcp.tables.datatypes.transformation.impl.parser.DateTimeFilterBaseVisitor;
import xmcp.tables.datatypes.transformation.impl.parser.DateTimeFilterParser;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.time.*;

class DateTimeFilterVisitorImpl extends DateTimeFilterBaseVisitor<String> {

    private final String path;

    public DateTimeFilterVisitorImpl(String path) {
        super();
        this.path = path != null
            ? (((path.startsWith("%0%.")) ? "" : "%0%.") + path)
            : "";
    }

    private String appendChildren(final ParserRuleContext ctx) {
        String result = "";
        for (int i = 0; i < ctx.getChildCount(); ++i) {
            if (ctx.getChild(i) instanceof TerminalNode)
            result += ctx.getChild(i).getText();
            else
            result += visit(ctx.getChild(i));
        }
        return result;
    }

    @Override
    public String visitPos_digit(DateTimeFilterParser.Pos_digitContext ctx) {
        return ctx.getText();
    }

    @Override
    public String visitDigit(DateTimeFilterParser.DigitContext ctx) {
        if (ctx.pos_digit() != null)
            return visitPos_digit(ctx.pos_digit());
        else
            return ctx.getText();
    }

    @Override
    public String visitDays_febuary(DateTimeFilterParser.Days_febuaryContext ctx) {
         if (ctx.pos_digit() != null && ctx.ZERO() == null)
            return "0" + visitPos_digit(ctx.pos_digit());

        return appendChildren(ctx);
    }

    @Override
    public String visitDays_short(DateTimeFilterParser.Days_shortContext ctx) {
        if (ctx.days_febuary() != null)
            return visitDays_febuary(ctx.days_febuary());

        return "30";
    }

    @Override
    public String visitDays_long(DateTimeFilterParser.Days_longContext ctx) {
        if (ctx.days_short() != null)
            return visitDays_short(ctx.days_short());

        return "31";
    }

    @Override
    public String visitMonth_febuary(DateTimeFilterParser.Month_febuaryContext ctx) {
        return "02";
    }

    @Override
    public String visitMonth_short(DateTimeFilterParser.Month_shortContext ctx) {
        if (ctx.FOUR() != null)
          return "04";
        if (ctx.SIX() != null)
          return "06";
        if (ctx.NINE() != null)
          return "09";

        return "11";
    }

    @Override
    public String visitMonth_long(DateTimeFilterParser.Month_longContext ctx) {
        if (ctx.THREE() != null)
          return "03";
        if (ctx.FIVE() != null)
          return "05";
        if (ctx.SEVEN() != null)
          return "07";
        if (ctx.EIGHT() != null)
          return "08";

        if (ctx.ONE() != null && ctx.TWO() != null)
          return "12";

        if (ctx.ZERO() == null && ctx.ONE() != null)
          return "01";

        return appendChildren(ctx);
    }

    @Override
    public String visitYears(DateTimeFilterParser.YearsContext ctx) {
        return appendChildren(ctx);
    }

    @Override
    public String visitDate_year(DateTimeFilterParser.Date_yearContext ctx) {
        return appendChildren(ctx) + ((useLowerLimit) ? "-01-01" : "-12-31");
    }

    @Override
    public String visitDate_year_month(DateTimeFilterParser.Date_year_monthContext ctx) {
        String years_str = visitYears(ctx.years());
        String result = years_str + "-";

        if (ctx.month_febuary() != null) {
            if (!useLowerLimit) {
                String day_str = "-28";
                int years = Integer.parseInt(years_str);

                if (leapYear(years)) day_str = "-29";

                return result + visitMonth_febuary(ctx.month_febuary()) + day_str;
            }

            return result + visitMonth_febuary(ctx.month_febuary()) + "-01";
        }

        if (ctx.month_short() != null)
           return result += visitMonth_short(ctx.month_short()) + ((useLowerLimit) ? "-01" : "-30");

        return result + visitMonth_long(ctx.month_long()) + ((useLowerLimit) ? "-01" : "-31");
    }

    private boolean leapYear(int year) {
       return year % 4 == 0 && (year % 100 != 0 || year % 400 == 0);
    }

    @Override
    public String visitDate_full(DateTimeFilterParser.Date_fullContext ctx) {
        String years_str = visitYears(ctx.years());
        String result = years_str + "-";

        if (ctx.month_febuary() != null)
           result += visitMonth_febuary(ctx.month_febuary());

        if (ctx.month_short() != null)
           result += visitMonth_short(ctx.month_short());

        if (ctx.month_long() != null)
           result += visitMonth_long(ctx.month_long());

        result += "-";

        if (ctx.days_febuary() != null) {
           String day_str = visitDays_febuary(ctx.days_febuary());
           if (ctx.month_febuary() != null && Integer.parseInt(day_str) >= 29) {
              int years = Integer.parseInt(years_str);
              if (!leapYear(years))
                 return years_str + "-03-01";
           }
           return result + day_str;
        }

        if (ctx.days_short() != null)
           return result + visitDays_short(ctx.days_short());

        return result + visitDays_long(ctx.days_long());
    }

   @Override
   public String visitSingleDigitHours(DateTimeFilterParser.SingleDigitHoursContext ctx) {
       return "0" + visitDigit(ctx.digit());
   }

   @Override
   public String visitOneHours(DateTimeFilterParser.OneHoursContext ctx) {
       return appendChildren(ctx);
   }

   @Override
   public String visitTwoHours(DateTimeFilterParser.TwoHoursContext ctx) {
      return appendChildren(ctx);
   }

   @Override
   public String visitSingleDigitMinOrSec(DateTimeFilterParser.SingleDigitMinOrSecContext ctx) {
       return "0" + visitDigit(ctx.digit());
   }

   @Override
   public String visitDoubleDigitMinOrSec(DateTimeFilterParser.DoubleDigitMinOrSecContext ctx) {
       return appendChildren(ctx);
   }

   @Override
   public String visitTime_hour(DateTimeFilterParser.Time_hourContext ctx) {
       return visit(ctx.hour()) + ((useLowerLimit) ? ":00:00" : ":59:59");
   }

   @Override
   public String visitTime_hour_min(DateTimeFilterParser.Time_hour_minContext ctx) {
       return visit(ctx.hour()) + ":" + visit(ctx.min_or_sec()) + ((useLowerLimit) ? ":00" : ":59");
   }

   @Override
   public String visitTime_full(DateTimeFilterParser.Time_fullContext ctx) {
    return visit(ctx.hour()) + ":" + visit(ctx.min_or_sec(0)) + ":" + visit(ctx.min_or_sec(1));
   }


   @Override
   public String visitDate_time(DateTimeFilterParser.Date_timeContext ctx) {
       return visit(ctx.date_full()) + "T" + visit(ctx.time());
   }

   @Override
   public String visitTz(DateTimeFilterParser.TzContext ctx) {
       if (ctx.TZ_D_WINTER() != null)
          return "+01:00";

       if (ctx.TZ_D_SUMMER() != null)
          return "+02:00";

       return "Z";
   }

   @Override
   public String visitSimple_condition(DateTimeFilterParser.Simple_conditionContext ctx) {
       String offset = "Z";

       if (ctx.tz() != null)
          offset = visitTz(ctx.tz());

       String date_time = "";

       if (ctx.date_time() != null)
            date_time = visit(ctx.date_time());
       else if (ctx.date() != null)
            date_time = visit(ctx.date()) + ((useLowerLimit) ? "T00:00:00" : "T23:59:59");
       else if (ctx.time() != null) {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of(offset));
            int month = now.getMonthValue();
            int day = now.getDayOfMonth();
            date_time = now.getYear() + "-"
                        +  ((month<10) ? "0" : "") + month + "-"
                        +  ((day < 10) ? "0" : "") + day + "T"
                        + visit(ctx.time());
       }

       ZonedDateTime then = ZonedDateTime.parse(date_time + offset);

       //System.out.println(then.toString());

       return '"' + String.valueOf(then.toEpochSecond()*1000L + ((useLowerLimit) ? 0L : 1000L)) + '"';
   }

   @Override
   public String visitOp_condition(DateTimeFilterParser.Op_conditionContext ctx) {
       String result = path;

       if (ctx.LESS_E() != null)
         result += " <= ";
       else  if (ctx.LESS_T() != null)
         result += " < ";
       else  if (ctx.MORE_E() != null)
         result += " >= ";
       else  if (ctx.MORE_T() != null)
         result += " > ";

       return result + visit(ctx.simple_condition());
   }

   @Override
   public String visitParenthesisFilterCondition(DateTimeFilterParser.ParenthesisFilterConditionContext ctx) {
       return "(" + visit(ctx.filter_condition()) + ")";
   }

   @Override
   public String visitAndFilterCondition(DateTimeFilterParser.AndFilterConditionContext ctx) {
       return visit(ctx.filter_condition(0)) + " && " + visit(ctx.filter_condition(1));
   }

   @Override
   public String visitOrFilterCondition(DateTimeFilterParser.OrFilterConditionContext ctx) {
       return visit(ctx.filter_condition(0)) + " || " + visit(ctx.filter_condition(1));
   }

   private boolean useLowerLimit = true;

   @Override
   public String visitSimpleFilterCondition(DateTimeFilterParser.SimpleFilterConditionContext ctx) {
       useLowerLimit = true;
       String result = "( (" + path + " >= " + visit(ctx.simple_condition()) + ") && ";
       useLowerLimit = false;
       result += "(" + path + " < " + visit(ctx.simple_condition()) + ") )";
       useLowerLimit = true;
       return result;
   }

   @Override
   public String visitStart(DateTimeFilterParser.StartContext ctx) {
       return visit(ctx.filter_condition());
   }

}