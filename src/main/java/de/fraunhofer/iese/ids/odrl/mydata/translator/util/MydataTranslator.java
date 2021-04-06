package de.fraunhofer.iese.ids.odrl.mydata.translator.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.fraunhofer.iese.ids.odrl.mydata.translator.model.*;
import de.fraunhofer.iese.ids.odrl.policy.library.model.*;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.IntervalCondition;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.LeftOperand;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ModificationMethod;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.Operator;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RightOperandType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RuleType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.interfaces.ITranslator;
import lombok.Data;


@Data
public class MydataTranslator implements ITranslator {

 private String solution = "";


 @Override
 public String translateComplexPolicy(OdrlPolicy odrlPolicy) {
	MydataPolicy mydataPolicy = createMydataPolicy(odrlPolicy);
	return new StringBuilder(mydataPolicy.getTimerForPolicy()).append(System.getProperty("line.separator")).append(mydataPolicy.toString()).toString();
 }
 
 public MydataPolicy createMydataPolicy(OdrlPolicy odrlPolicy) {
	  this.solution = BuildMydataPolicyUtils.getSolution(odrlPolicy);
	  MydataPolicy mydataPolicy = BuildMydataPolicyUtils.buildPolicy(odrlPolicy,
	          odrlPolicy.getRules().get(0).getAction().getType(), odrlPolicy.getRules().get(0).getType(), this.solution);
	  if(!odrlPolicy.getRules().isEmpty())
	  {
		  //set mydataPolicy target
		  String target = "";
		  if (null != odrlPolicy.getRules().get(0).getTarget()) {
			  target = odrlPolicy.getRules().get(0).getTarget().toString();
		  }
		  this.targetConstraint(mydataPolicy, target);
	  }

	  if(odrlPolicy.getRules().get(0).getType().equals(RuleType.OBLIGATION)){

	   if(null != odrlPolicy.getRules().get(0).getAction().getRefinements())
	   {
	    for(Condition odrlRefinement: odrlPolicy.getRules().get(0).getAction().getRefinements()) {
	     if (odrlRefinement.getLeftOperand().equals(LeftOperand.DELAY)) {
			 List<Parameter> pipParams = new ArrayList<>();
			 for(RightOperandEntity entity: odrlRefinement.getRightOperand().getEntities())
			 {
				 switch (entity.getEntityType()) {
					 case BEGIN:
						 Parameter beginParam = new Parameter(ParameterType.STRING, "begin", entity.getValue());
						 pipParams.add(beginParam);
						 break;
					 case HASDURATION:
						 Duration d = BuildMydataPolicyUtils.getDurationFromPeriodValue(entity.getValue());

						 Timer timer = new Timer(entity.getTimeUnit(), "",mydataPolicy.getPid(), solution, ActionType.DELETE, null);
						 mydataPolicy.setTimer(timer);

						 Parameter valueParam = new Parameter(ParameterType.NUMBER, "value", String.valueOf(d.getValue()));
						 Parameter unitParam = new Parameter(ParameterType.STRING, "value", entity.getTimeUnit().toString());
						 pipParams.add(valueParam);
						 pipParams.add(unitParam);
						 break;
				 }
			 }

	      PIPBoolean delayPeriodPipBoolean = new PIPBoolean(solution, LeftOperand.DELAY, pipParams);
	      List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
	      pips.add(delayPeriodPipBoolean);
	      mydataPolicy.setPipBooleans(pips);

	      ExecuteAction pxp = new ExecuteAction(solution, ActionType.DELETE, null);
	      mydataPolicy.setPxp(pxp);
	     }else if (odrlRefinement.getLeftOperand().equals(LeftOperand.DATE_TIME))
	     {
	      DateTime dateTime = new DateTime(IntervalCondition.EQ, odrlRefinement.getRightOperand().getValue());
	      String cron = createCron(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
	      Timer timer = new Timer(null,cron,mydataPolicy.getPid(), solution, ActionType.DELETE,null);
	      mydataPolicy.setTimer(timer);
	     }
	    }
	   }

	   ExecuteAction pxp = new ExecuteAction(solution, odrlPolicy.getRules().get(0).getAction().getType(), null);
	   mydataPolicy.setPxp(pxp);
	  }

	  //TODO: for all rules!
	  if(null != odrlPolicy.getRules().get(0).getConstraints()) {
	   for (Condition odrlConstraint : odrlPolicy.getRules().get(0).getConstraints()) {
	    if (odrlConstraint.getLeftOperand().equals(LeftOperand.PURPOSE)) {
	     mydataPolicy = this.purposeConstraint(mydataPolicy, odrlConstraint);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.EVENT)) {
	     mydataPolicy = this.eventConstraint(mydataPolicy, odrlConstraint);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ABSOLUTESPATIALPOSITION)) {
	     mydataPolicy = this.absoluteSpatialPositionConstraint(mydataPolicy, odrlConstraint);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.PAY_AMOUNT)) {
	     mydataPolicy = this.paymentConstraint(mydataPolicy, odrlConstraint);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.SYSTEM)) {
	     mydataPolicy = this.systemConstraint(mydataPolicy, odrlConstraint);
	    }else if (odrlConstraint.getLeftOperand().equals(LeftOperand.APPLICATION)) {
			mydataPolicy = this.applicationConstraint(mydataPolicy, odrlConstraint);
		}else if (odrlConstraint.getLeftOperand().equals(LeftOperand.CONNECTOR)) {
			mydataPolicy = this.connectorConstraint(mydataPolicy, odrlConstraint);
		} else if (odrlConstraint.getLeftOperand().equals(LeftOperand.POLICY_EVALUATION_TIME)) {
	     mydataPolicy = this.timeIntervalConstraint(mydataPolicy, odrlConstraint);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.COUNT)) {
	     List<Parameter> countParams = Collections.emptyList();
	     Count countFirstOperand = new Count(this.solution, null, ActionType.USE, countParams, FixedTime.ALWAYS);
	     Constant countSecondOperand = new Constant(ParameterType.NUMBER, odrlConstraint.getRightOperand().getValue());
	     MydataCondition countCondition = new MydataCondition(countFirstOperand, Operator.LT, countSecondOperand);
	     List<MydataCondition> cons = mydataPolicy.getConditions();
	     cons.add(countCondition);
	     mydataPolicy.setConditions(cons);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ENCODING)) {
	     Parameter encodingParam = new Parameter(ParameterType.STRING, LeftOperand.ENCODING.getMydataLeftOperand() + "-uri", odrlConstraint.getRightOperand().getValue());
	     List<Parameter> pipParams = new ArrayList<>();
	     pipParams.add(encodingParam);
	     PIPBoolean encodingPipBoolean = new PIPBoolean(this.solution, LeftOperand.ENCODING, pipParams);
	     List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
	     pips.add(encodingPipBoolean);
	     mydataPolicy.setPipBooleans(pips);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ARTIFACT_STATE)) {
			Parameter artifactStateParam = new Parameter(ParameterType.STRING, LeftOperand.ARTIFACT_STATE.getMydataLeftOperand(), odrlConstraint.getRightOperand().getValue());
			List<Parameter> pipParams = new ArrayList<>();
			pipParams.add(artifactStateParam);
			PIPBoolean artifactStatePipBoolean = new PIPBoolean(this.solution, LeftOperand.ENCODING, pipParams);
			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(artifactStatePipBoolean);
			mydataPolicy.setPipBooleans(pips);
		} else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ELAPSED_TIME)) {
			List<Parameter> pipParams = new ArrayList<>();
	    	for(RightOperandEntity entity: odrlConstraint.getRightOperand().getEntities())
			{
				switch (entity.getEntityType()) {
					case BEGIN:
						Parameter beginParam = new Parameter(ParameterType.STRING, "begin", entity.getValue());
						pipParams.add(beginParam);
						break;
					case HASDURATION:
						Duration d = BuildMydataPolicyUtils.getDurationFromPeriodValue(entity.getValue());
						Parameter valueParam = new Parameter(ParameterType.NUMBER, "value", String.valueOf(d.getValue()));
						Parameter unitParam = new Parameter(ParameterType.STRING, "value", entity.getTimeUnit().toString());
						pipParams.add(valueParam);
						pipParams.add(unitParam);
						break;
				}
			}

	     PIPBoolean elapsedTimePipBoolean = new PIPBoolean(solution, LeftOperand.ELAPSED_TIME, pipParams);
	     List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
	     pips.add(elapsedTimePipBoolean);
	     mydataPolicy.setPipBooleans(pips);
	    }
	   }
	  }

	  if(null != odrlPolicy.getRules().get(0).getPreduties()) {
	   for (Rule preobligation : odrlPolicy.getRules().get(0).getPreduties()) {
	    if (preobligation.getAction().getType().equals(ActionType.ANONYMIZE)) {
	     if(null != preobligation.getAction().getRefinements()) {
	      for (Condition odrlRefinement : preobligation.getAction().getRefinements()) {
	       if (odrlRefinement.getLeftOperand().equals(LeftOperand.MODIFICATIONMETHOD)) {
	        anonymizePreobligation(mydataPolicy, odrlRefinement);
	       }
	      }
	     }
	    } else if (preobligation.getAction().getType().equals(ActionType.NEXT_POLICY)) {
	     if (null != preobligation.getAction().getRefinements()) {
	      for (Condition odrlRefinement : preobligation.getAction().getRefinements()) {
	       nextPolicyPreobligation(mydataPolicy, ActionType.NEXT_POLICY, odrlRefinement);
	      }
	     }
	    }
	   }
	  }

	  if(null != odrlPolicy.getRules().get(0).getPostduties()) {
	   for (Rule postobligation : odrlPolicy.getRules().get(0).getPostduties()) {
	   	ActionType actionType = postobligation.getAction().getType();
		   ArrayList<Condition> odrlRefinements = postobligation.getAction().getRefinements();
	    if (actionType.equals(ActionType.DELETE)) {
	    	deletePostobligation(mydataPolicy, actionType, odrlRefinements);
	    } else if (actionType.equals(ActionType.INFORM) || actionType.equals(ActionType.NOTIFY)) {
	        informPostobligation(mydataPolicy, actionType, odrlRefinements);
	    } else if (actionType.equals(ActionType.LOG)) {
	    	logPostobligation(mydataPolicy, actionType, odrlRefinements);
	    } else if (actionType.equals(ActionType.COUNT)) {
			countPostobligation(mydataPolicy, actionType);
		}
	   }
	  }
	  return mydataPolicy;
 }

 private void nextPolicyPreobligation(MydataPolicy mydataPolicy, ActionType nextpolicy, Condition odrlRefinement) {
  Parameter nextPolicyTargetParam = new Parameter(ParameterType.STRING, LeftOperand.TARGET_POLICY.getMydataLeftOperand() + "-uri", odrlRefinement.getRightOperand().getValue());
  List<Parameter> params = new ArrayList<>();
  params.add(nextPolicyTargetParam);
  ExecuteAction pxp = new ExecuteAction(solution, nextpolicy, params);
  mydataPolicy.setPxp(pxp);
  mydataPolicy.setHasDuty(true);
 }

 private void informPostobligation(MydataPolicy mydataPolicy, ActionType inform, ArrayList<Condition> odrlRefinements) {
	 List<Parameter> params = new ArrayList<>();
	 if(null != odrlRefinements) {
		 for (Condition odrlRefinement : odrlRefinements) {
			 if (odrlRefinement.getLeftOperand().equals(LeftOperand.INFORMEDPARTY)) {
				 Parameter informedPartyParam = new Parameter(ParameterType.STRING, LeftOperand.INFORMEDPARTY.getMydataLeftOperand() + "-uri", odrlRefinement.getRightOperand().getValue());
				 params.add(informedPartyParam);
			 } else if (odrlRefinement.getLeftOperand().equals(LeftOperand.RECIPIENT)) {
				 Parameter recipientParam = new Parameter(ParameterType.STRING, LeftOperand.RECIPIENT.getMydataLeftOperand() + "-uri", odrlRefinement.getRightOperand().getValue());
				 params.add(recipientParam);
			 }else if (odrlRefinement.getLeftOperand().equals(LeftOperand.NOTIFICATION_LEVEL)) {
				 Parameter notifLevelParam = new Parameter(ParameterType.STRING, LeftOperand.NOTIFICATION_LEVEL.getMydataLeftOperand(), odrlRefinement.getRightOperand().getValue());
				 params.add(notifLevelParam);
			 }
		 }
	 }

  ExecuteAction pxp = new ExecuteAction(solution, inform, params);
  mydataPolicy.setPxp(pxp);
  mydataPolicy.setHasDuty(true);
 }

 private void logPostobligation(MydataPolicy mydataPolicy, ActionType log, ArrayList<Condition> odrlRefinements) {
	 List<Parameter> params = new ArrayList<>();
	 if(null != odrlRefinements) {
		 for (Condition odrlRefinement : odrlRefinements) {
			 if (odrlRefinement.getLeftOperand().equals(LeftOperand.SYSTEM_DEVICE)) {
				 Parameter systemDeviceParam = new Parameter(ParameterType.STRING, LeftOperand.SYSTEM_DEVICE.getMydataLeftOperand() + "-uri", odrlRefinement.getRightOperand().getValue());
				 params.add(systemDeviceParam);
			 } else if (odrlRefinement.getLeftOperand().equals(LeftOperand.LOG_LEVEL)) {
				 Parameter logLevelParam = new Parameter(ParameterType.STRING, LeftOperand.LOG_LEVEL.getMydataLeftOperand(), odrlRefinement.getRightOperand().getValue());
				 params.add(logLevelParam);
			 }
		 }
	 }
  ExecuteAction pxp = new ExecuteAction(solution, log, params);
  mydataPolicy.setPxp(pxp);
  mydataPolicy.setHasDuty(true);
 }

 private void anonymizePreobligation(MydataPolicy mydataPolicy, Condition odrlRefinement) {
	 Modify modify = null;
	 String eventParameterToModify = "DataObject";
 	if(odrlRefinement.getRightOperand().getValue().equals(ModificationMethod.DELETE.getIdsMethod()))
	{
		modify = new Modify(eventParameterToModify, ModificationMethod.DELETE, odrlRefinement.getJsonPath(), null);

	}else{
		ParameterType paramType = ParameterType.STRING;
		if(odrlRefinement.getReplaceWith().getType().equals(RightOperandType.INTEGER) ||
				odrlRefinement.getReplaceWith().getType().equals(RightOperandType.DECIMAL))
		{
			paramType = ParameterType.NUMBER;
		}
		Parameter replaceWithParam = new Parameter(paramType, "replaceWith", odrlRefinement.getReplaceWith().getValue());
		  List<Parameter> params = new ArrayList<>();
		  params.add(replaceWithParam);
		modify = new Modify(eventParameterToModify, ModificationMethod.REPLACE, odrlRefinement.getJsonPath(), params);
	}

	 mydataPolicy.setModify(modify);
 }

 private void deletePostobligation(MydataPolicy mydataPolicy, ActionType delete, ArrayList<Condition> odrlRefinements) {
 	List<Parameter> params = new ArrayList<>();
	 if(null != odrlRefinements) {
		 for (Condition odrlRefinement : odrlRefinements) {
			 if (null != odrlRefinement) {
				 if (odrlRefinement.getLeftOperand().equals(LeftOperand.DELAY)) {
					 for (RightOperandEntity entity : odrlRefinement.getRightOperand().getEntities()) {
						 switch (entity.getEntityType()) {
							 case BEGIN:
								 Parameter beginParam = new Parameter(ParameterType.STRING, "begin", entity.getValue());
								 params.add(beginParam);
								 break;
							 case HASDURATION:
								 Duration d = BuildMydataPolicyUtils.getDurationFromPeriodValue(entity.getValue());
								 Parameter valueParam = new Parameter(ParameterType.NUMBER, "value", String.valueOf(d.getValue()));
								 Parameter unitParam = new Parameter(ParameterType.STRING, "value", entity.getTimeUnit().toString());
								 params.add(valueParam);
								 params.add(unitParam);
								 break;
						 }
					 }
				 } else if (odrlRefinement.getLeftOperand().equals(LeftOperand.DATE_TIME)) {
					 Parameter datetimeParam = new Parameter(ParameterType.STRING, LeftOperand.DATE_TIME.getMydataLeftOperand(), odrlRefinement.getRightOperand().getValue());
					 params.add(datetimeParam);
				 }
			 }
		 }
	 }
	 if(params.isEmpty())
	 {
		 ExecuteAction pxp = new ExecuteAction(solution, delete, null);
	 }
	  ExecuteAction pxp = new ExecuteAction(solution, delete, params);
	  mydataPolicy.setPxp(pxp);
	  mydataPolicy.setHasDuty(true);
 }

	private void countPostobligation(MydataPolicy mydataPolicy, ActionType count) {
		ExecuteAction pxp = new ExecuteAction(solution, count, null);
		mydataPolicy.setPxp(pxp);
		mydataPolicy.setHasDuty(true);
	}

 private MydataPolicy absoluteSpatialPositionConstraint(MydataPolicy mydataPolicy, Condition absoluteSpatialPositionConstraint) {
  if(null != absoluteSpatialPositionConstraint)
  {
   Parameter locationParam = new Parameter(ParameterType.STRING,LeftOperand.ABSOLUTESPATIALPOSITION.getMydataLeftOperand()+"-uri", absoluteSpatialPositionConstraint.getRightOperand().getValue());
   List<Parameter> pipParams = new ArrayList<>();
   pipParams.add(locationParam);
   
   PIPBoolean locationPipBoolean = new PIPBoolean(this.solution, LeftOperand.ABSOLUTESPATIALPOSITION, pipParams);

   List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
   pips.add(locationPipBoolean);
   mydataPolicy.setPipBooleans(pips);
  }
  return mydataPolicy;
 }

 private MydataPolicy timeIntervalConstraint(MydataPolicy mydataPolicy, Condition timeIntervalConstraint) {
  if(null != timeIntervalConstraint)
  {
   List<DateTime> dateTimes = new ArrayList<>();
   for(RightOperandEntity entity: timeIntervalConstraint.getRightOperand().getEntities())
   {
	   switch (entity.getEntityType()) {
		   case BEGIN:
			   String start = entity.getValue();
			   DateTime startTime = new DateTime(IntervalCondition.GT, start);
			   dateTimes.add(startTime);
			   break;
		   case END:
			   String end = entity.getValue();
			   DateTime endTime = new DateTime(IntervalCondition.LT, end);
			   dateTimes.add(endTime);
			   break;
	   }
   }

   mydataPolicy.setDateTimes(dateTimes);
  }
  return mydataPolicy;
 }

private MydataPolicy targetConstraint(MydataPolicy mydataPolicy, String target) {
	if(null != target)
	{
		// get conditions
		Event targetFirstOperand = new Event(ParameterType.STRING, EventParameter.TARGET.getEventParameter(), null);
		Constant targetSecondOperand = new Constant(ParameterType.STRING, target);
		MydataCondition targetCondition = new MydataCondition(targetFirstOperand, Operator.EQ, targetSecondOperand);

		//set conditions
		mydataPolicy.setTarget(target);

		List<MydataCondition> cons = mydataPolicy.getConditions();
		cons.add(targetCondition);
		mydataPolicy.setConditions(cons);
	}
	return mydataPolicy;
}

 private MydataPolicy purposeConstraint(MydataPolicy mydataPolicy, Condition purposeConstraint) {
  if(null != purposeConstraint)
  {
   Parameter purposeParam = new Parameter(ParameterType.STRING, LeftOperand.PURPOSE.getMydataLeftOperand()+"-uri", purposeConstraint.getRightOperand().getValue());
   Event event = new Event(ParameterType.STRING, "MsgTarget", "appUri");
   Parameter msgTargetParam = new Parameter(ParameterType.STRING, "MsgTargetAppUri", event);


   List<Parameter> pipParams = new ArrayList<>();
   pipParams.add(purposeParam);
   pipParams.add(msgTargetParam);
   PIPBoolean purposePipBoolean = new PIPBoolean(this.solution, LeftOperand.PURPOSE, pipParams);

   List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
   pips.add(purposePipBoolean);
   mydataPolicy.setPipBooleans(pips);
  }
  return mydataPolicy;
 }

 private MydataPolicy systemConstraint(MydataPolicy mydataPolicy, Condition systemConstraint) {
		if(null != systemConstraint)
		{
			Parameter systemParam = new Parameter(ParameterType.STRING,LeftOperand.SYSTEM.getMydataLeftOperand()+"-uri", systemConstraint.getRightOperand().getValue());

			List<Parameter> pipParams = new ArrayList<>();
			pipParams.add(systemParam);
			PIPBoolean systemPipBoolean = new PIPBoolean(this.solution, LeftOperand.SYSTEM, pipParams);

			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(systemPipBoolean);
			mydataPolicy.setPipBooleans(pips);
		}
		return mydataPolicy;
	}

	private MydataPolicy applicationConstraint(MydataPolicy mydataPolicy, Condition applicationConstraint) {
		if(null != applicationConstraint)
		{
			Parameter applicationParam = new Parameter(ParameterType.STRING,LeftOperand.APPLICATION.getMydataLeftOperand()+"-uri", applicationConstraint.getRightOperand().getValue());

			List<Parameter> pipParams = new ArrayList<>();
			pipParams.add(applicationParam);
			PIPBoolean applicationPipBoolean = new PIPBoolean(this.solution, LeftOperand.APPLICATION, pipParams);

			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(applicationPipBoolean);
			mydataPolicy.setPipBooleans(pips);
		}
		return mydataPolicy;
	}

	private MydataPolicy connectorConstraint(MydataPolicy mydataPolicy, Condition connectorConstraint) {
		if(null != connectorConstraint)
		{
			Parameter connectorParam = new Parameter(ParameterType.STRING,LeftOperand.CONNECTOR.getMydataLeftOperand()+"-uri", connectorConstraint.getRightOperand().getValue());

			List<Parameter> pipParams = new ArrayList<>();
			pipParams.add(connectorParam);
			PIPBoolean connectorPipBoolean = new PIPBoolean(this.solution, LeftOperand.CONNECTOR, pipParams);

			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(connectorPipBoolean);
			mydataPolicy.setPipBooleans(pips);
		}
		return mydataPolicy;
	}

 private MydataPolicy paymentConstraint(MydataPolicy mydataPolicy, Condition paymentConstraint) {
  if(null != paymentConstraint)
  {
   Parameter valueParam = new Parameter(ParameterType.NUMBER,"value",String.valueOf(paymentConstraint.getRightOperand().getValue()));
   Parameter contractParam = new Parameter(ParameterType.STRING,"value", paymentConstraint.getContract());
   List<Parameter> pipParams = new ArrayList<>();
   pipParams.add(valueParam);
   pipParams.add(contractParam);
   PIPBoolean paymentPipBoolean = new PIPBoolean(this.solution, LeftOperand.PAY_AMOUNT, pipParams);

   List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
   pips.add(paymentPipBoolean);
   mydataPolicy.setPipBooleans(pips);
  }
  return mydataPolicy;
 }

 private MydataPolicy eventConstraint(MydataPolicy mydataPolicy, Condition eventConstraint) {
  if(null != eventConstraint)
  {
   Parameter eventParam = new Parameter(ParameterType.STRING,"event-uri", eventConstraint.getRightOperand().getValue());
   List<Parameter> eventParams = new ArrayList<>();
   eventParams.add(eventParam);
   Count eventFirstOperand = new Count(this.solution, LeftOperand.EVENT, null, eventParams, FixedTime.THIS_HOUR);
   Constant eventSecondOperand = new Constant(ParameterType.NUMBER, "1");
   MydataCondition eventCondition = new MydataCondition(eventFirstOperand, Operator.GTEQ, eventSecondOperand);
   //set conditions
   List<MydataCondition> cons = mydataPolicy.getConditions();
   cons.add(eventCondition);
   mydataPolicy.setConditions(cons);
  }
  return mydataPolicy;
 }

 private String createCron(String y, String m, String d, String th, String tm, String ts)
 {
  String cron = ts + " " + tm + " " + th + " " + d + " " + m + " ? " + y ;
  return cron;
 }
}