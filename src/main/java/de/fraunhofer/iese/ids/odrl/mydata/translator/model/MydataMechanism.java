package de.fraunhofer.iese.ids.odrl.mydata.translator.model;


import java.util.ArrayList;
import java.util.List;

import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RuleType;
import lombok.Data;

@Data
public class MydataMechanism {
 List<MydataCondition> conditions;
 List<PIPBoolean> pipBooleans;
 List<DateTime> dateTimes;
 String solution;
 ActionType action;
 RuleType decision;
 List<ExecuteAction> pxps;
 boolean hasDuty;
 List<Modify> modifiers;
 String target;

 public MydataMechanism(String solution, ActionType action, RuleType decision, boolean hasDuty, List<Modify> modifiers)
 {
  this.conditions = new ArrayList<>();
  this.pipBooleans = new ArrayList<>();
  this.dateTimes = new ArrayList<>();
  this.solution = solution;
  this.action = action;
  this.decision = decision;
  this.hasDuty = hasDuty;
  this.modifiers = modifiers;
 }

 @Override
 public String toString() {
		String mydataMechanism = "";

		String actionString = action.name().toLowerCase();
		String conditionsBlock = getConditionsBlock();
		String decisionBlock = getDecisionBlock();

		if (null != actionString && null != conditionsBlock && null != decisionBlock && null != this.target) {
         mydataMechanism = String.format(
                 "  <mechanism event='urn:action:ids:%1$s'>\n"
							+ "    <if>\n"
							+ "%2$s"
							+ "%3$s"
							+ "  </mechanism>\n",
					actionString, conditionsBlock, decisionBlock, this.target);
		}
		return mydataMechanism;
 }


 private String getDecisionBlock() {

  String thenBlock = "";
  String elseBlock = "";
  if(null != modifiers)
  {
   for(Modify modifier: this.modifiers)
   {
    thenBlock = thenBlock.concat("\r\n" + modifier.toString());
   }
  }
  //if(decision.equals(RuleType.OBLIGATION) || (decision.equals(RuleType.PERMISSION) && this.hasDuty))
  //{
   if(null != pxps)
   {
    for (ExecuteAction pxp: this.pxps)
    {
     if(pxp.parameters != null){
      for(Parameter p:pxp.parameters)
      {
       if((p.name.equals("logLevel") && p.value.equals("idsc:ON_DENY")) ||
               (p.name.equals("notificationLevel") && p.value.equals("idsc:ON_DENY"))){
        elseBlock = elseBlock.concat("\r\n" + pxp.toString());
       }else{
        thenBlock = thenBlock.concat("\r\n" + pxp.toString());
        break;
       }
      }
     }else{
      thenBlock = thenBlock.concat("\r\n" + pxp.toString());
     }
    }
   }

  //}
  return getDecision(thenBlock, elseBlock);

 }

 private String getDecision(String thenBlock, String elseBlock) {
  RuleType elseDecision = getElseDecision();
  return  "        <then> \n"
          + "        <" + decision.getMydataDecision() + "/> \n"
          + thenBlock
          + "        </then>  \r\n"
          + "      </if>   \r\n"
          + "    <elseif>\n"
          + "      <equals>\n" + "        <constant:string value='" + target + "'/>\n"
          + "        <event:string eventParameter='TargetDataUri' default=''/>\n"
          + "      </equals>\n"
          + "      <then>\n"
          + "          <" + elseDecision.getMydataDecision() + "/>  \r\n"
          + elseBlock
          + "      </then>\n"
          + "    </elseif>\n" ;
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
