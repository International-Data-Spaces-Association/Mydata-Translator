package de.fraunhofer.iese.ids.odrl.mydata.translator.model;


import java.util.ArrayList;
import java.util.List;

import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RuleType;
import lombok.Data;

@Data
public class MydataPolicy {
 Timer timer;
 String pid;
 String solution;
ArrayList<MydataMechanism> mechanisms;

 public MydataPolicy(String pid, String solution)
 {
  this.pid = pid;
  this.solution = solution;
 }

 public MydataPolicy(String pid, String solution, ArrayList<MydataMechanism> mechanisms)
 {
  this.pid = pid;
  this.solution = solution;
  this.mechanisms = mechanisms;
 }

 @Override
 public String toString() {
		String mydataPolicy = "";

		String policyId = new StringBuilder("urn:policy:").append(solution).append(":").append(pid).toString();
		String mechanismsBlock = getMechanismsBlock();

		if (null != policyId && null != mechanisms) {
			mydataPolicy = String.format(
					"<policy id='%1$s' description='This is the generated usage policy for %1$s. ' xmlns='http://www.mydata-control.de/4.0/mydataLanguage' xmlns:tns='http://www.mydata-control.de/4.0/mydataLanguage' xmlns:parameter='http://www.mydata-control.de/4.0/parameter' xmlns:pip='http://www.mydata-control.de/4.0/pip' xmlns:function='http://www.mydata-control.de/4.0/function' xmlns:event='http://www.mydata-control.de/4.0/event' xmlns:constant='http://www.mydata-control.de/4.0/constant' xmlns:variable='http://www.mydata-control.de/4.0/variable' xmlns:variableDeclaration='http://www.mydata-control.de/4.0/variableDeclaration' xmlns:valueChanged='http://www.mydata-control.de/4.0/valueChanged' xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:date='http://www.mydata-control.de/4.0/date' xmlns:time='http://www.mydata-control.de/4.0/time' xmlns:day='http://www.mydata-control.de/4.0/day'>\n"
                            + "%2$s"
                            + "</policy>",
					policyId, mechanismsBlock);
		}
		return mydataPolicy;
 }

 private String getMechanismsBlock() {
  String mechanismsBlock = "";
  if(null != this.mechanisms && !this.mechanisms.isEmpty())
  {
   for(MydataMechanism mech:this.mechanisms)
   {
    mechanismsBlock = mechanismsBlock.concat(mech.toString());
   }
  }
  return mechanismsBlock;
 }

    public String getTimerForPolicy() {
        if (null != timer)
        {
            return timer.toString();
        }
        return "";
    }
}
