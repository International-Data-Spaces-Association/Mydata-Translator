package de.fraunhofer.iese.ids.odrl.mydata.translator.util;


import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.fraunhofer.iese.ids.odrl.mydata.translator.model.*;
import de.fraunhofer.iese.ids.odrl.policy.library.model.*;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.*;
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
			 //semantically, the time conditions cannot get more than one right operand; time conflicts occurs.
			 for (RightOperandEntity entity : odrlRefinement.getRightOperands().get(0).getEntities()) {
				 switch (entity.getEntityType()) {
					 case BEGIN:
						 RightOperandEntity beginInnerEntity = entity.getInnerEntity();
						 Parameter beginParam = new Parameter(ParameterType.STRING, "begin", beginInnerEntity.getValue());
						 pipParams.add(beginParam);
						 break;
					 case DATETIME:
						 Parameter datetimeParam = new Parameter(ParameterType.STRING, "beginDateTime", entity.getValue());
						 pipParams.add(datetimeParam);
						 break;
					 case HASDURATION:
						 TimeUnit tu = getTimerUnit(entity.getValue());
						 Timer timer = new Timer(tu, "", mydataPolicy.getPid(), solution, ActionType.DELETE, null);
						 mydataPolicy.setTimer(timer);

						 Parameter durationParam = new Parameter(ParameterType.STRING, "delay", String.valueOf(entity.getValue()));
						 pipParams.add(durationParam);
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
			 //semantically, the date time conditions cannot get more than one right operand; time conflicts occurs.
			 for (RightOperandEntity entity : odrlRefinement.getRightOperands().get(0).getEntities()) {
				 switch (entity.getEntityType()) {
					 case END:
						 RightOperandEntity endInnerEntity = entity.getInnerEntity();
						 DateTime endDateTime = new DateTime(IntervalCondition.EQ, endInnerEntity.getValue());
						 String endCron = createCron(endDateTime.getYear(), endDateTime.getMonth(), endDateTime.getDay(), endDateTime.getHour(), endDateTime.getMinute(), endDateTime.getSecond());
						 Timer endTimer = new Timer(null, endCron, mydataPolicy.getPid(), solution, ActionType.DELETE, null);
						 mydataPolicy.setTimer(endTimer);
						 break;
					 case DATETIME:
						 DateTime dateTime = new DateTime(IntervalCondition.EQ, entity.getValue());
						 String cron = createCron(dateTime.getYear(), dateTime.getMonth(), dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(), dateTime.getSecond());
						 Timer timer = new Timer(null, cron, mydataPolicy.getPid(), solution, ActionType.DELETE, null);
						 mydataPolicy.setTimer(timer);
						 break;
				 }
			 }
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
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ABSOLUTE_SPATIAL_POSITION)) {
	     mydataPolicy = this.absoluteSpatialPositionConstraint(mydataPolicy, odrlConstraint);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.PAY_AMOUNT)) {
	     mydataPolicy = this.paymentConstraint(mydataPolicy, odrlConstraint);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.SYSTEM)) {
	     mydataPolicy = this.systemConstraint(mydataPolicy, odrlConstraint);
	    }else if (odrlConstraint.getLeftOperand().equals(LeftOperand.APPLICATION)) {
			mydataPolicy = this.applicationConstraint(mydataPolicy, odrlConstraint);
		}else if (odrlConstraint.getLeftOperand().equals(LeftOperand.CONNECTOR)) {
			mydataPolicy = this.connectorConstraint(mydataPolicy, odrlConstraint);
		} else if (odrlConstraint.getLeftOperand().equals(LeftOperand.SECURITY_LEVEL)) {
			mydataPolicy = this.securityLevelConstraint(mydataPolicy, odrlConstraint);
		} else if (odrlConstraint.getLeftOperand().equals(LeftOperand.STATE)) {
			mydataPolicy = this.stateConstraint(mydataPolicy, odrlConstraint);
		} else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ROLE)) {
			mydataPolicy = this.roleConstraint(mydataPolicy, odrlConstraint);
		}else if (odrlConstraint.getLeftOperand().equals(LeftOperand.POLICY_EVALUATION_TIME)) {
	     mydataPolicy = this.timeIntervalConstraint(mydataPolicy, odrlConstraint);
	    }else if (odrlConstraint.getLeftOperand().equals(LeftOperand.DATE_TIME)) {
			mydataPolicy = this.dateTimeConstraint(mydataPolicy, odrlConstraint);
		} else if (odrlConstraint.getLeftOperand().equals(LeftOperand.COUNT)) {
			 List<Parameter> countParams = Collections.emptyList();
			 Count countFirstOperand = new Count(this.solution, null, ActionType.USE, countParams, FixedTime.ALWAYS);
			//semantically, the count conditions cannot get more than one right operand; counter conflict occurs.
			 Constant countSecondOperand = new Constant(ParameterType.NUMBER, odrlConstraint.getRightOperands().get(0).getValue());
			 MydataCondition countCondition = new MydataCondition(countFirstOperand, Operator.LT, countSecondOperand);
			 List<MydataCondition> cons = mydataPolicy.getConditions();
			 cons.add(countCondition);
			 mydataPolicy.setConditions(cons);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ENCODING)) {
			List<Parameter> pipParams = new ArrayList<>();
	    	for(RightOperand rightOperand: odrlConstraint.getRightOperands())
			{
				Parameter encodingParam = new Parameter(ParameterType.STRING, LeftOperand.ENCODING.getMydataLeftOperand() + "-uri", rightOperand.getValue());
				pipParams.add(encodingParam);
			}
			Operator op = odrlConstraint.getOperator();
			if(op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean encodingPipBoolean = new PIPBoolean(this.solution, LeftOperand.ENCODING, pipParams);
			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(encodingPipBoolean);
			mydataPolicy.setPipBooleans(pips);
	    } else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ARTIFACT_STATE)) {
			List<Parameter> pipParams = new ArrayList<>();
			for(RightOperand rightOperand: odrlConstraint.getRightOperands())
			{
				Parameter artifactStateParam = new Parameter(ParameterType.STRING, LeftOperand.ARTIFACT_STATE.getMydataLeftOperand(), rightOperand.getValue());
				pipParams.add(artifactStateParam);
			}
			Operator op = odrlConstraint.getOperator();
			if(op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean artifactStatePipBoolean = new PIPBoolean(this.solution, LeftOperand.ENCODING, pipParams);
			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(artifactStatePipBoolean);
			mydataPolicy.setPipBooleans(pips);
		} else if (odrlConstraint.getLeftOperand().equals(LeftOperand.ELAPSED_TIME)) {
			List<Parameter> pipParams = new ArrayList<>();
			//semantically, the elapsed time conditions cannot get more than one right operand; time conflicts occurs.
	    	for(RightOperandEntity entity: odrlConstraint.getRightOperands().get(0).getEntities())
			{
				switch (entity.getEntityType()) {
					case BEGIN:
						RightOperandEntity beginInnerEntity = entity.getInnerEntity();
						Parameter beginParam = new Parameter(ParameterType.STRING, "begin", beginInnerEntity.getValue());
						pipParams.add(beginParam);
						break;
					case HASDURATION:
						//Duration d = BuildMydataPolicyUtils.getDurationFromPeriodValue(entity.getValue());
						Parameter durationParam = new Parameter(ParameterType.STRING, "duration", entity.getValue());
						pipParams.add(durationParam);
						break;
					case DATETIME:
						Parameter dateTimeParam = new Parameter(ParameterType.STRING, "beginDateTime", entity.getValue());
						pipParams.add(dateTimeParam);
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
	   	ActionType actionType = preobligation.getAction().getType();
	    if (actionType.equals(ActionType.ANONYMIZE) || actionType.equals(ActionType.REPLACE) || actionType.equals(ActionType.DELETE)) {
			ArrayList<Condition> odrlRefinements = preobligation.getAction().getRefinements();
			anonymizePreobligation(mydataPolicy, actionType, odrlRefinements);
	    } else if (actionType.equals(ActionType.NEXT_POLICY)) {
	     if (null != preobligation.getAction().getRefinements()) {
	      for (Condition odrlRefinement : preobligation.getAction().getRefinements()) {
	       nextPolicyPreobligation(mydataPolicy, actionType, odrlRefinement);
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
	    } else if (actionType.equals(ActionType.INCREMENT_COUNTER)) {
			countPostobligation(mydataPolicy, actionType);
		}
	   }
	  }
	  return mydataPolicy;
 }

	private TimeUnit getTimerUnit(String value) {
 	//start from the smallest time unit
 	if(value.contains("H")){
 		return TimeUnit.HOURS;
	}else if(value.contains("D")){
			return TimeUnit.DAYS;
 	}else if(value.contains("M")){
		return TimeUnit.MONTHS;
	}else {
		return TimeUnit.YEARS;
	}
	}

	private void nextPolicyPreobligation(MydataPolicy mydataPolicy, ActionType nextpolicy, Condition odrlRefinement) {
		List<Parameter> params = new ArrayList<>();
		// list of target policies (offer contracts)
		for(RightOperand rightOperand: odrlRefinement.getRightOperands())
		{
			Parameter nextPolicyTargetParam = new Parameter(ParameterType.STRING, LeftOperand.TARGET_POLICY.getMydataLeftOperand() + "-uri", rightOperand.getValue());
			params.add(nextPolicyTargetParam);
		}
		Operator op = odrlRefinement.getOperator();
		if(op.equals(Operator.IS_ANY_OF) ||
				op.equals(Operator.IS_NONE_OF) ||
				op.equals(Operator.IS_ALL_OF))
		{
			//pass the list operator as a parameter to the PIP, too!
			Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
			params.add(operatorParam);
		}
		ExecuteAction pxp = new ExecuteAction(solution, nextpolicy, params);
		mydataPolicy.setPxp(pxp);
		mydataPolicy.setHasDuty(true);
	 }

 private void informPostobligation(MydataPolicy mydataPolicy, ActionType inform, ArrayList<Condition> odrlRefinements) {
	 List<Parameter> params = new ArrayList<>();
	 if(null != odrlRefinements) {
		 for (Condition odrlRefinement : odrlRefinements) {
		 	// list of recipients or informed parties
			if (odrlRefinement.getLeftOperand().equals(LeftOperand.INFORMEDPARTY)) {
				for(RightOperand rightOperand: odrlRefinement.getRightOperands()) {
					Parameter informedPartyParam = new Parameter(ParameterType.STRING, LeftOperand.INFORMEDPARTY.getMydataLeftOperand() + "-uri", rightOperand.getValue());
					params.add(informedPartyParam);
				}
			} else if (odrlRefinement.getLeftOperand().equals(LeftOperand.RECIPIENT)) {
				for(RightOperand rightOperand: odrlRefinement.getRightOperands()) {
					Parameter recipientParam = new Parameter(ParameterType.STRING, LeftOperand.RECIPIENT.getMydataLeftOperand() + "-uri", rightOperand.getValue());
					params.add(recipientParam);
				}
			} else if (odrlRefinement.getLeftOperand().equals(LeftOperand.NOTIFICATION_LEVEL)) {
				for(RightOperand rightOperand: odrlRefinement.getRightOperands()) {
					Parameter notifLevelParam = new Parameter(ParameterType.STRING, LeftOperand.NOTIFICATION_LEVEL.getMydataLeftOperand(), rightOperand.getValue());
					params.add(notifLevelParam);
				}
			}
			 Operator op = odrlRefinement.getOperator();
			 if(op.equals(Operator.IS_ANY_OF) ||
					 op.equals(Operator.IS_NONE_OF) ||
					 op.equals(Operator.IS_ALL_OF))
			 {
				 //pass the list operator as a parameter to the PIP, too!
				 Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				 params.add(operatorParam);
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
		 	//list of system devices

			if (odrlRefinement.getLeftOperand().equals(LeftOperand.SYSTEM_DEVICE)) {
				for (RightOperand rightOperand: odrlRefinement.getRightOperands()) {
					Parameter systemDeviceParam = new Parameter(ParameterType.STRING, LeftOperand.SYSTEM_DEVICE.getMydataLeftOperand() + "-uri", rightOperand.getValue());
					params.add(systemDeviceParam);
				}
			} else if (odrlRefinement.getLeftOperand().equals(LeftOperand.LOG_LEVEL)) {
				for (RightOperand rightOperand: odrlRefinement.getRightOperands()) {
					Parameter logLevelParam = new Parameter(ParameterType.STRING, LeftOperand.LOG_LEVEL.getMydataLeftOperand(), rightOperand.getValue());
					params.add(logLevelParam);
				}
			}
			 Operator op = odrlRefinement.getOperator();
			 if(op.equals(Operator.IS_ANY_OF) ||
					 op.equals(Operator.IS_NONE_OF) ||
					 op.equals(Operator.IS_ALL_OF))
			 {
				 //pass the list operator as a parameter to the PIP, too!
				 Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				 params.add(operatorParam);
			 }
		 }
	 }
  ExecuteAction pxp = new ExecuteAction(solution, log, params);
  mydataPolicy.setPxp(pxp);
  mydataPolicy.setHasDuty(true);
 }

 private void anonymizePreobligation(MydataPolicy mydataPolicy, ActionType actionType, ArrayList<Condition> odrlRefinements) {
	 Modify modify = null;
	 String eventParameterToModify = "DataObject";
	 List<Parameter> params = null;
	 String jsonPathQuery = "";
	 if(null != odrlRefinements) {
		 for (Condition odrlRefinement : odrlRefinements) {
		 	if(odrlRefinement.getLeftOperand().equals(LeftOperand.REPLACE_WITH)){
				ParameterType paramType = ParameterType.STRING;
				if(odrlRefinement.getType().equals(RightOperandType.INTEGER) ||
						odrlRefinement.getType().equals(RightOperandType.DECIMAL)) {
					paramType = ParameterType.NUMBER;
				}
				// semantically, you can replace a field with only one value.
				Parameter replaceWithParam = new Parameter(paramType, "replaceWith", odrlRefinement.getRightOperands().get(0).getValue());
				params = new ArrayList<>();
				params.add(replaceWithParam);
			}
			 if (odrlRefinement.getLeftOperand().equals(LeftOperand.JSON_PATH)) {
			 	// Currently, the MYDATA modifier accepts only one JSON Path for a modification.
			 	jsonPathQuery = odrlRefinement.getRightOperands().get(0).getValue();
			 }
		 }
	 }
	 modify = new Modify(eventParameterToModify, actionType, jsonPathQuery, params);
	 mydataPolicy.setModify(modify);
 }

 private void deletePostobligation(MydataPolicy mydataPolicy, ActionType delete, ArrayList<Condition> odrlRefinements) {
 	List<Parameter> params = new ArrayList<>();
	 if(null != odrlRefinements) {
		 for (Condition odrlRefinement : odrlRefinements) {
			 if (null != odrlRefinement) {
				 if (odrlRefinement.getLeftOperand().equals(LeftOperand.DELAY)) {
					 //semantically, the time conditions cannot get more than one right operand; time conflicts occurs.
					 for (RightOperandEntity entity : odrlRefinement.getRightOperands().get(0).getEntities()) {
						 switch (entity.getEntityType()) {
							 case BEGIN:
								 RightOperandEntity beginInnerEntity = entity.getInnerEntity();
								 Parameter beginParam = new Parameter(ParameterType.STRING, "begin", beginInnerEntity.getValue());
								 params.add(beginParam);
								 break;
							 case HASDURATION:
								 Parameter durationParam = new Parameter(ParameterType.STRING, "delay", String.valueOf(entity.getValue()));
								 params.add(durationParam);
								 break;
						 }
					 }
				 } else if (odrlRefinement.getLeftOperand().equals(LeftOperand.DATE_TIME)) {

					 //semantically, the time conditions cannot get more than one right operand; time conflicts occurs.
					 for(RightOperandEntity entity: odrlRefinement.getRightOperands().get(0).getEntities())
					 {
						 switch (entity.getEntityType()) {
							 case BEGIN:
								 RightOperandEntity beginInnerEntity = entity.getInnerEntity();
								 Parameter beginParam = new Parameter(ParameterType.STRING, "beginTime", beginInnerEntity.getValue());
								 params.add(beginParam);
								 break;
							 case END:
								 RightOperandEntity endInnerEntity = entity.getInnerEntity();
								 Parameter endParam = new Parameter(ParameterType.STRING, "deadline", endInnerEntity.getValue());
								 params.add(endParam);
								 break;
							 case DATETIME:
								 Parameter dateTimeParam = new Parameter(ParameterType.STRING, "dateTime", entity.getValue());
								 params.add(dateTimeParam);
								 break;
						 }
					 }
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
	  List<Parameter> pipParams = new ArrayList<>();
	  // list of locations
  	for (RightOperand rightOperand: absoluteSpatialPositionConstraint.getRightOperands())
	{
		Parameter locationParam = new Parameter(ParameterType.STRING,LeftOperand.ABSOLUTE_SPATIAL_POSITION.getMydataLeftOperand()+"-uri", rightOperand.getValue());
		pipParams.add(locationParam);
	}
	  Operator op = absoluteSpatialPositionConstraint.getOperator();
	  if(op.equals(Operator.IS_ANY_OF) ||
			  op.equals(Operator.IS_NONE_OF) ||
			  op.equals(Operator.IS_ALL_OF))
	  {
		  //pass the list operator as a parameter to the PIP, too!
		  Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
		  pipParams.add(operatorParam);
	  }
   PIPBoolean locationPipBoolean = new PIPBoolean(this.solution, LeftOperand.ABSOLUTE_SPATIAL_POSITION, pipParams);

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
	  //semantically, the time conditions cannot get more than one right operand; time conflicts occurs.
   for(RightOperandEntity entity: timeIntervalConstraint.getRightOperands().get(0).getEntities())
   {
	   switch (entity.getEntityType()) {
		   case BEGIN:
			   RightOperandEntity beginInnerEntity = entity.getInnerEntity();
			   String start = beginInnerEntity.getValue();
			   DateTime startTime = new DateTime(IntervalCondition.GT, start);
			   dateTimes.add(startTime);
			   break;
		   case END:
			   RightOperandEntity endInnerEntity = entity.getInnerEntity();
			   String end = endInnerEntity.getValue();
			   DateTime endTime = new DateTime(IntervalCondition.LT, end);
			   dateTimes.add(endTime);
			   break;
	   }
   }

   mydataPolicy.setDateTimes(dateTimes);
  }
  return mydataPolicy;
 }

private MydataPolicy dateTimeConstraint(MydataPolicy mydataPolicy, Condition dateTimeConstraint) {
	if(null != dateTimeConstraint)
	{
		List<DateTime> dateTimes = new ArrayList<>();
		//semantically, the time conditions cannot get more than one right operand; time conflicts occurs.
		for(RightOperandEntity entity: dateTimeConstraint.getRightOperands().get(0).getEntities())
		{
			switch (entity.getEntityType()) {
				case BEGIN:
					RightOperandEntity beginInnerEntity = entity.getInnerEntity();
					String startDateTime = beginInnerEntity.getValue();
					DateTime startTime = new DateTime(IntervalCondition.GT, startDateTime);
					dateTimes.add(startTime);
					break;
				case END:
					RightOperandEntity endInnerEntity = entity.getInnerEntity();
					String endDatetime = endInnerEntity.getValue();
					DateTime endTime = new DateTime(IntervalCondition.LT, endDatetime);
					dateTimes.add(endTime);
					break;
				case DATETIME:
					String datetime = entity.getValue();
					DateTime dTime = new DateTime(IntervalCondition.LT, datetime);
					dateTimes.add(dTime);
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
	  List<Parameter> pipParams = new ArrayList<>();
	  // list of purposes
  	for(RightOperand rightOperand: purposeConstraint.getRightOperands())
	{
		Parameter purposeParam = new Parameter(ParameterType.STRING, LeftOperand.PURPOSE.getMydataLeftOperand()+"-uri", rightOperand.getValue());
		pipParams.add(purposeParam);
	}
	  Operator op = purposeConstraint.getOperator();
	  if(op.equals(Operator.IS_ANY_OF) ||
			  op.equals(Operator.IS_NONE_OF) ||
			  op.equals(Operator.IS_ALL_OF))
	  {
		  //pass the list operator as a parameter to the PIP, too!
		  Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
		  pipParams.add(operatorParam);
	  }
   Event event = new Event(ParameterType.STRING, "MsgTarget", "appUri");
   Parameter msgTargetParam = new Parameter(ParameterType.STRING, "MsgTargetAppUri", event);

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
			List<Parameter> pipParams = new ArrayList<>();
			// list of systems
			for(RightOperand rightOperand: systemConstraint.getRightOperands())
			{
				Parameter systemParam = new Parameter(ParameterType.STRING,LeftOperand.SYSTEM.getMydataLeftOperand()+"-uri", rightOperand.getValue());
				pipParams.add(systemParam);
			}
			Operator op = systemConstraint.getOperator();
			if(op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
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
			List<Parameter> pipParams = new ArrayList<>();
			//list of applications
			for (RightOperand rightOperand: applicationConstraint.getRightOperands())
			{
				Parameter applicationParam = new Parameter(ParameterType.STRING,LeftOperand.APPLICATION.getMydataLeftOperand()+"-uri", rightOperand.getValue());
				pipParams.add(applicationParam);
			}
			Operator op = applicationConstraint.getOperator();
			if(op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
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
			List<Parameter> pipParams = new ArrayList<>();
			//List of connectors
			for(RightOperand rightOperand: connectorConstraint.getRightOperands())
			{
				Parameter connectorParam = new Parameter(ParameterType.STRING,LeftOperand.CONNECTOR.getMydataLeftOperand()+"-uri", rightOperand.getValue());
				pipParams.add(connectorParam);
			}
			Operator op = connectorConstraint.getOperator();
			if(op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}

			PIPBoolean connectorPipBoolean = new PIPBoolean(this.solution, LeftOperand.CONNECTOR, pipParams);

			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(connectorPipBoolean);
			mydataPolicy.setPipBooleans(pips);
		}
		return mydataPolicy;
	}

	private MydataPolicy securityLevelConstraint(MydataPolicy mydataPolicy, Condition securityLevelConstraint) {
		if(null != securityLevelConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			//list of security levels
			for(RightOperand rightOperand: securityLevelConstraint.getRightOperands())
			{
				Parameter securityLevelParam = new Parameter(ParameterType.STRING,LeftOperand.SECURITY_LEVEL.getMydataLeftOperand(), rightOperand.getValue());
				pipParams.add(securityLevelParam);
			}
			Operator op = securityLevelConstraint.getOperator();
			if(op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}

			PIPBoolean securityLevelPipBoolean = new PIPBoolean(this.solution, LeftOperand.SECURITY_LEVEL, pipParams);

			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(securityLevelPipBoolean);
			mydataPolicy.setPipBooleans(pips);
		}
		return mydataPolicy;
	}

	private MydataPolicy stateConstraint(MydataPolicy mydataPolicy, Condition stateConstraint) {
		if(null != stateConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			//list of states
			for(RightOperand rightOperand: stateConstraint.getRightOperands())
			{
				Parameter stateParam = new Parameter(ParameterType.STRING,LeftOperand.STATE.getMydataLeftOperand(), rightOperand.getValue());
				pipParams.add(stateParam);
			}
			Operator op = stateConstraint.getOperator();
			if(op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean statePipBoolean = new PIPBoolean(this.solution, LeftOperand.STATE, pipParams);

			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(statePipBoolean);
			mydataPolicy.setPipBooleans(pips);
		}
		return mydataPolicy;
	}

	private MydataPolicy roleConstraint(MydataPolicy mydataPolicy, Condition roleConstraint) {
		if(null != roleConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			//list of roles
			for(RightOperand rightOperand: roleConstraint.getRightOperands())
			{
				Parameter roleParam = new Parameter(ParameterType.STRING,LeftOperand.ROLE.getMydataLeftOperand(), rightOperand.getValue());
				pipParams.add(roleParam);
			}
			Operator op = roleConstraint.getOperator();
			if(op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean rolePipBoolean = new PIPBoolean(this.solution, LeftOperand.ROLE, pipParams);

			List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
			pips.add(rolePipBoolean);
			mydataPolicy.setPipBooleans(pips);
		}
		return mydataPolicy;
	}

 private MydataPolicy paymentConstraint(MydataPolicy mydataPolicy, Condition paymentConstraint) {
  if(null != paymentConstraint)
  {
  	// semantically, only one value must be set for the payment; conflict occurs!
   Parameter valueParam = new Parameter(ParameterType.NUMBER,"value",String.valueOf(paymentConstraint.getRightOperands().get(0).getValue()));
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
	  List<Parameter> pipParams = new ArrayList<>();
	  // list of events occurring right now!
	  for(RightOperand rightOperand: eventConstraint.getRightOperands())
	  {
		  Parameter eventParam = new Parameter(ParameterType.STRING, LeftOperand.EVENT.getMydataLeftOperand()+"-uri", rightOperand.getValue());
		  pipParams.add(eventParam);
	  }
	  Operator op = eventConstraint.getOperator();
	  if(op.equals(Operator.IS_ANY_OF) ||
			  op.equals(Operator.IS_NONE_OF) ||
			  op.equals(Operator.IS_ALL_OF))
	  {
		  //pass the list operator as a parameter to the PIP, too!
		  Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
		  pipParams.add(operatorParam);
	  }
	  Event event = new Event(ParameterType.STRING, "MsgTarget", "appUri");
	  Parameter msgTargetParam = new Parameter(ParameterType.STRING, "MsgTargetAppUri", event);

	  pipParams.add(msgTargetParam);
	  PIPBoolean eventPipBoolean = new PIPBoolean(this.solution, LeftOperand.EVENT, pipParams);

	  List<PIPBoolean> pips = mydataPolicy.getPipBooleans();
	  pips.add(eventPipBoolean);
	  mydataPolicy.setPipBooleans(pips);
  }
  return mydataPolicy;
 }

 private String createCron(String y, String m, String d, String th, String tm, String ts)
 {
  String cron = ts + " " + tm + " " + th + " " + d + " " + m + " ? " + y ;
  return cron;
 }
}