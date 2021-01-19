package de.fraunhofer.iese.ids.odrl.mydata.translator.model;

import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.TimeUnit;
import lombok.Data;

@Data
public class Timer {
    TimeUnit timeUnit;
    String cron;
    String tid;
    String solution;
    ActionType action;
    Parameter parameter;

    public Timer(TimeUnit timeUnit, String exactCron, String tid, String solution, ActionType action, Parameter parameter) {
        this.timeUnit = timeUnit;
        this.cron = exactCron;
        this.tid = tid;
        this.solution = solution;
        this.action = action;
        this.parameter = parameter;
    }

    public Timer(String tid) {
        this.tid = tid;
    }


    @Override
    public String toString() {
        return  "  <timer cron='"+ getCron() +"' id='urn:timer:"+ solution +":"+ tid +"'> " + System.lineSeparator() +
                "    <event action='urn:action:"+ solution +":"+ action.name().toLowerCase() +"'> " + System.lineSeparator() +
                getParameter() +
                "    </event> " + System.lineSeparator() +
                "  </timer>" + System.lineSeparator() ;
    }

    private String getParameter() {
        if (null != parameter)
        {
            return "      "+ parameter.toString() + System.lineSeparator();
        }
        return "";
    }

    private String getCron(){
        if(null != timeUnit)
        {
            return timeUnit.getMydataCron();
        }else
        {
            return cron;
        }
    }
}
