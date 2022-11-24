package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import de.fraunhofer.iese.ids.odrl.mydata.translator.interfaces.ICondition;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.Operator;
import lombok.Data;

@Data
public class DateTime implements ICondition {

    Operator operator;
    String dateTime;
    ZonedDateTime zonedDateTime;
    DateTimeFormatter mydataDateFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    DateTimeFormatter mydataTimeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    public DateTime() {
    }

    public DateTime(Operator operator, String dateTime) {
        this.operator = operator;
        this.dateTime = dateTime;
        this.zonedDateTime = ZonedDateTime.parse(dateTime);
    }

    @Override
    public String toString() {
        if(this.operator.equals(Operator.AFTER) || this.operator.equals(Operator.GTEQ) || this.operator.equals(Operator.GT))
        {
            return "          <or> \r\n" +
                    "            <and> \r\n" +
                    "              <date is='exactly' value='"+ getDate() +"'/> \r\n" +
                    "              <time is='"+ operator.getMydataOp() +"' value='"+ getTime() +"'/> \r\n" +
                    "            </and> \r\n" +
                    "            <date is='"+ operator.getMydataOp() +"' value='"+ getDate() +"'/> \r\n" +
                    "          </or> \r\n";
        }else if(this.operator.equals(Operator.BEFORE)|| this.operator.equals(Operator.LTEQ) || this.operator.equals(Operator.LT))
        {
            return "          <or> \r\n" +
                    "            <date is='"+ operator.getMydataOp() +"' value='"+ getDate() +"'/> \r\n" +
                    "            <and> \r\n" +
                    "              <date is='exactly' value='"+ getDate() +"'/> \r\n" +
                    "              <time is='"+ operator.getMydataOp() +"' value='"+ getTime() +"'/> \r\n" +
                    "            </and> \r\n" +
                    "          </or> \r\n";
        }
        return "";
        // We may implement dateTime function for MYDATA in near future
        //return  "          <dateTime is='"+ is.getMydataInterval() +"' value='"+ getMydataDateTime() +"'/> \r\n";
    }

    


    private String getDate()
    {
        return zonedDateTime.format(mydataDateFormatter);
    }

    private String getTime()
    {
        return zonedDateTime.format(mydataTimeFormatter);
    }

    public String getYear()
    {
        return String.valueOf(zonedDateTime.getYear());
    }

    public String getMonth()
    {
        return String.valueOf(zonedDateTime.getMonthValue());
    }

    public String getDay()
    {
        return String.valueOf(zonedDateTime.getDayOfMonth());
    }

    public String getHour()
    {
        return String.valueOf(zonedDateTime.getHour());
    }

    public String getMinute()
    {
        return String.valueOf(zonedDateTime.getMinute());
    }

    public String getSecond()
    {
        return String.valueOf(zonedDateTime.getSecond());
    }
}
