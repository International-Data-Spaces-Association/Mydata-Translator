package de.fraunhofer.iese.ids.odrl.mydata.translator.model;


import java.util.ArrayList;
import java.util.List;

import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RuleType;
import lombok.Data;

@Data
public class MydataPolicy {
 Timer timer;
 List<MydataCondition> conditions;
 List<PIPBoolean> pipBooleans;
 List<DateTime> dateTimes;
 String solution;
 String pid;
 ActionType action;
 RuleType decision;
 ExecuteAction pxp;
 boolean hasDuty;
 Modify modify;
 String target;

 public MydataPolicy(String solution, String pid, ActionType action, RuleType decision, boolean hasDuty, Modify modify)
 {
  this.conditions = new ArrayList<>();
  this.pipBooleans = new ArrayList<>();
  this.dateTimes = new ArrayList<>();
  this.solution = solution;
  this.pid = pid;
  this.action = action;
  this.decision = decision;
  this.hasDuty = hasDuty;
  this.modify = modify;
 }

 @Override
 public String toString() {
		String mydataPolicy = "";

		String policyId = new StringBuilder("urn:policy:").append(solution).append(":").append(pid).toString();
		String actionString = action.name().toLowerCase();
		String conditionsBlock = getConditionsBlock();
		String decisionBlock = getDecisionBlock();

		if (null != policyId && null != actionString && null != conditionsBlock && null != decisionBlock && null != target) {
			mydataPolicy = String.format(
					"<policy id='%1$s' description='This is the generated usage policy for %1$s. ' xmlns='http://www.mydata-control.de/4.0/mydataLanguage' xmlns:tns='http://www.mydata-control.de/4.0/mydataLanguage' xmlns:parameter='http://www.mydata-control.de/4.0/parameter' xmlns:pip='http://www.mydata-control.de/4.0/pip' xmlns:function='http://www.mydata-control.de/4.0/function' xmlns:event='http://www.mydata-control.de/4.0/event' xmlns:constant='http://www.mydata-control.de/4.0/constant' xmlns:variable='http://www.mydata-control.de/4.0/variable' xmlns:variableDeclaration='http://www.mydata-control.de/4.0/variableDeclaration' xmlns:valueChanged='http://www.mydata-control.de/4.0/valueChanged' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:date='http://www.mydata-control.de/4.0/date' xmlns:time='http://www.mydata-control.de/4.0/time' xmlns:day='http://www.mydata-control.de/4.0/day'>\n"
							+ "  <mechanism event='urn:action:ids:%2$s'>\n"
							+ "    <if>\n"
							+ "%3$s"
							+ "%4$s"
							+ "  </mechanism>\n" + "</policy>",
					policyId, actionString, conditionsBlock, decisionBlock, target);
		}
		return mydataPolicy;
 }


 private String getDecisionBlock() {
  RuleType elseDecision = getElseDecision();
  if(null != modify)
  {
   return  "        <then>  \r\n" +
           modify.toString() +
           "        </then>  \r\n" +
           "      </if>   \r\n"
           + "    <elseif>\n"
           + "      <equals>\n" + "        <constant:string value='" + target + "'/>\n"
           + "        <event:string eventParameter='TargetDataUri' default=''/>\n"
           + "      </equals>\n"
           + "      <then>\n"
           + "          <" + elseDecision.getMydataDecision() + "/>  \r\n"
           + "      </then>\n"
           + "    </elseif>\n" ;
  } else if(decision.equals(RuleType.OBLIGATION) || (decision.equals(RuleType.PERMISSION) && this.hasDuty))
  {
   if(null != pxp)
   {
    for(Parameter p:pxp.parameters)
    {
     if((p.name.equals("logLevel") && p.value.equals("idsc:ON_DENY")) ||
             (p.name.equals("notificationLevel") && p.value.equals("idsc:ON_DENY"))){
      return  "        <then>  \r\n" +
              "        <" + decision.getMydataDecision() + "/>  \r\n" +
              "        </then>  \r\n" +
              "      </if>   \r\n"
              + "    <elseif>\n"
              + "      <equals>\n" + "        <constant:string value='" + target + "'/>\n"
              + "        <event:string eventParameter='TargetDataUri' default=''/>\n"
              + "      </equals>\n"
              + "      <then>\n"
              + "          <" + elseDecision.getMydataDecision() + "/>  \r\n"
              + pxp.toString()
              + "      </then>\n"
              + "    </elseif>\n" ;
     }else{
      return  "        <then>  \r\n" +
              "        <" + decision.getMydataDecision() + "/>  \r\n" +
              pxp.toString() +
              "        </then>  \r\n" +
              "      </if>   \r\n"
              + "    <elseif>\n"
              + "      <equals>\n" + "        <constant:string value='" + target + "'/>\n"
              + "        <event:string eventParameter='TargetDataUri' default=''/>\n"
              + "      </equals>\n"
              + "      <then>\n"
              + "          <" + elseDecision.getMydataDecision() + "/>  \r\n"
              + "      </then>\n"
              + "    </elseif>\n" ;
     }
    }
   }else {
    return "";
   }

  }
   return  "      <then>  \r\n" +
           "        <" + decision.getMydataDecision() + "/>  \r\n" +
           "      </then>  \r\n" +
           "    </if>   \r\n"
           + "    <elseif>\n"
           + "      <equals>\n" + "        <constant:string value='" + target + "'/>\n"
           + "        <event:string eventParameter='TargetDataUri' default=''/>\n"
           + "      </equals>\n"
           + "      <then>\n"
           + "          <" + elseDecision.getMydataDecision() + "/>  \r\n"
           + "      </then>\n"
           + "    </elseif>\n" ;

 }

 public String getTimerForPolicy() {
  if (null != timer)
  {
   return timer.toString();
  }
   return "";
 }

 private RuleType getElseDecision() {
  if(decision.equals(RuleType.PERMISSION) || decision.equals(RuleType.OBLIGATION))
  {
   return RuleType.PROHIBITION;
  }else if (decision.equals(RuleType.PROHIBITION))
  {
   return RuleType.PERMISSION;
  }
  return null;
 }

 private String getConditionsBlock() {
  if(conditions.size() == 0 && pipBooleans.size() == 0 && dateTimes.size() == 0)
  {
   return "";
  }else if (conditions.size() == 1 && pipBooleans.size() == 0 && dateTimes.size() == 0)
  {
   return conditions.get(0).toString();
  }else if (conditions.size() == 0 && pipBooleans.size() == 1 && dateTimes.size() == 0 )
  {
   return pipBooleans.get(0).toString();
  }else if(conditions.size() == 0 && pipBooleans.size() == 0 && dateTimes.size() == 1)
  {
   return dateTimes.get(0).toString();
  }else //if bigger
  {
   String conditions = "";
   String pips= "";
   String dates= "";
   if(conditions != null ){
    //for(int i = 0; i< this.conditions.size(); i++)
    for(MydataCondition c: this.conditions)
    {
     conditions += c.toString();
    }
   }
   if(pipBooleans != null)
   {
    //for(int i=0 ; i<pipBooleans.size(); i++)
    for(PIPBoolean pip: this.pipBooleans)
    {
     pips += pip.toString();
    }
   }
   if(dateTimes != null)
   {
    //for(int i=0 ; i<dateTimes.size(); i++)
    for(DateTime dt: this.dateTimes)
    {
     dates += dt.toString();
    }
   }


   return  "        <and>  \r\n" +
           conditions +
           pips +
           dates +
           "        </and>  \r\n";
  }
 }
}
