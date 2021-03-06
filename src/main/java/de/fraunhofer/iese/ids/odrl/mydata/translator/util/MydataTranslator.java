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
	  MydataPolicy mydataPolicy = BuildMydataPolicyUtils.buildPolicy(odrlPolicy, this.solution);
	  ArrayList<MydataMechanism> mydataMechanisms = new ArrayList<>();
	  if(null != odrlPolicy.getRules() && !odrlPolicy.getRules().isEmpty())
	  {
	  	for(Rule rule: odrlPolicy.getRules())
		{
			Action action = rule.getAction();
			RuleType ruleType = rule.getType();
			if(null != action)
			{
				MydataMechanism mydataMechanism = BuildMydataPolicyUtils.buildMechanism(odrlPolicy, action.getType(), ruleType, this.solution);

				// set target condition to mechanism
				if(null != rule.getTarget())
				{
					this.targetConstraint(mydataMechanism, rule.getTarget().toString());
				}

				if(rule.getType().equals(RuleType.OBLIGATION)){
					ArrayList<ExecuteAction> pxps = new ArrayList<>();
					if(null != action.getRefinements())
					{
						for(Condition odrlRefinement: action.getRefinements()) {
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
								ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
								pips.add(delayPeriodPipBoolean);
								mydataMechanism.setPipBooleans(pips);

								ExecuteAction pxp = new ExecuteAction(solution, action.getType(), null);
								pxps.add(pxp);
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

					ExecuteAction pxp = new ExecuteAction(solution, action.getType(), null);
					pxps.add(pxp);
					mydataMechanism.setPxps(pxps);
				}

				// set the conditions
				if(null != rule.getConstraints()) {
					for (Condition odrlConstraint : rule.getConstraints()) {
						switch (odrlConstraint.getLeftOperand()) {
							case PURPOSE:
								mydataMechanism = this.purposeConstraint(mydataMechanism, odrlConstraint);
								break;
							case EVENT:
								mydataMechanism = this.eventConstraint(mydataMechanism, odrlConstraint);
								break;
							case ABSOLUTE_SPATIAL_POSITION:
								mydataMechanism = this.absoluteSpatialPositionConstraint(mydataMechanism, odrlConstraint);
								break;
							case PAY_AMOUNT:
								mydataMechanism = this.paymentConstraint(mydataMechanism, odrlConstraint);
								break;
							case SYSTEM:
								mydataMechanism = this.systemConstraint(mydataMechanism, odrlConstraint);
								break;
							case APPLICATION:
								mydataMechanism = this.applicationConstraint(mydataMechanism, odrlConstraint);
								break;
							case CONNECTOR:
								mydataMechanism = this.connectorConstraint(mydataMechanism, odrlConstraint);
								break;
							case SECURITY_LEVEL:
								mydataMechanism = this.securityLevelConstraint(mydataMechanism, odrlConstraint);
								break;
							case STATE:
								mydataMechanism = this.stateConstraint(mydataMechanism, odrlConstraint);
								break;
							case ROLE:
								mydataMechanism = this.roleConstraint(mydataMechanism, odrlConstraint);
								break;
							case POLICY_EVALUATION_TIME:
								mydataMechanism = this.timeIntervalConstraint(mydataMechanism, odrlConstraint);
								break;
							case DATE_TIME:
								mydataMechanism = this.dateTimeConstraint(mydataMechanism, odrlConstraint);
								break;
							case COUNT:
								mydataMechanism = countConstraint(mydataMechanism, odrlConstraint);
								break;
							case ENCODING:
								mydataMechanism = encodingConstraint(mydataMechanism, odrlConstraint);
								break;
							case ARTIFACT_STATE:
								mydataMechanism = artifactStateConstraint(mydataMechanism, odrlConstraint);
								break;
							case ELAPSED_TIME:
								mydataMechanism = elapsedTimeConstraint(mydataMechanism, odrlConstraint);
								break;
						}
					}
				}

				ArrayList<ExecuteAction> pxps = new ArrayList<>();
				ArrayList<Modify> modifiers = new ArrayList<>();
				if(null != rule.getPreduties() && !rule.getPreduties().isEmpty()) {
					for (Rule preobligation : rule.getPreduties()) {
						ActionType actionType = preobligation.getAction().getType();
						if (actionType.equals(ActionType.ANONYMIZE) || actionType.equals(ActionType.REPLACE) || actionType.equals(ActionType.DELETE)) {
							ArrayList<Condition> odrlRefinements = preobligation.getAction().getRefinements();
							Modify anonymizeModifier = anonymizePreobligation(actionType, odrlRefinements);
							modifiers.add(anonymizeModifier);
						} else if (actionType.equals(ActionType.NEXT_POLICY)) {
							if (null != preobligation.getAction().getRefinements()) {
								for (Condition odrlRefinement : preobligation.getAction().getRefinements()) {
									ExecuteAction nextPolicyPXP = nextPolicyPreobligation(actionType, odrlRefinement);
									pxps.add(nextPolicyPXP);
								}
							}
						}
					}
				}

				if(null != rule.getPostduties() && !rule.getPostduties().isEmpty()) {
					for (Rule postobligation : rule.getPostduties()) {
						ActionType actionType = postobligation.getAction().getType();
						ArrayList<Condition> odrlRefinements = postobligation.getAction().getRefinements();
						if (actionType.equals(ActionType.DELETE)) {
							ExecuteAction deletePXP = deletePostobligation(actionType, odrlRefinements);
							pxps.add(deletePXP);
						} else if (actionType.equals(ActionType.INFORM) || actionType.equals(ActionType.NOTIFY)) {
							ExecuteAction informPXP = informPostobligation(actionType, odrlRefinements);
							pxps.add(informPXP);
						} else if (actionType.equals(ActionType.LOG)) {
							ExecuteAction logPXP = logPostobligation(actionType, odrlRefinements);
							pxps.add(logPXP);
						} else if (actionType.equals(ActionType.INCREMENT_COUNTER)) {
							ExecuteAction incrementCounterPXP = countPostobligation(actionType);
							pxps.add(incrementCounterPXP);
						}
					}
				}
				if(!pxps.isEmpty())
				{
					mydataMechanism.setHasDuty(true);
					mydataMechanism.setPxps(pxps);
				}
				if(!modifiers.isEmpty())
				{
					mydataMechanism.setModifiers(modifiers);
				}

				mydataMechanisms.add(mydataMechanism);
			}
		}
	  }

	  mydataPolicy.setMechanisms(mydataMechanisms);
	  return mydataPolicy;
 }

	private MydataMechanism elapsedTimeConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint) {
			List<Parameter> timePIPParams = new ArrayList<>();
			//semantically, the elapsed time conditions cannot get more than one right operand; time conflicts occurs.
			for (RightOperandEntity entity : odrlConstraint.getRightOperands().get(0).getEntities()) {
				switch (entity.getEntityType()) {
					case BEGIN:
						RightOperandEntity beginInnerEntity = entity.getInnerEntity();
						Parameter beginParam = new Parameter(ParameterType.STRING, "begin", beginInnerEntity.getValue());
						timePIPParams.add(beginParam);
						break;
					case HASDURATION:
						//Duration d = BuildMydataPolicyUtils.getDurationFromPeriodValue(entity.getValue());
						Parameter durationParam = new Parameter(ParameterType.STRING, "duration", entity.getValue());
						timePIPParams.add(durationParam);
						break;
					case DATETIME:
						Parameter dateTimeParam = new Parameter(ParameterType.STRING, "beginDateTime", entity.getValue());
						timePIPParams.add(dateTimeParam);
						break;
				}
			}

			PIPBoolean elapsedTimePipBoolean = new PIPBoolean(solution, LeftOperand.ELAPSED_TIME, timePIPParams);
			ArrayList<PIPBoolean> timePIPs = mydataMechanism.getPipBooleans();
			timePIPs.add(elapsedTimePipBoolean);
			mydataMechanism.setPipBooleans(timePIPs);
		}
		return mydataMechanism;
	}

	private MydataMechanism artifactStateConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint) {
			List<Parameter> artifactPIPParams = new ArrayList<>();
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter artifactStateParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getMydataLeftOperand(), rightOperand.getValue());
				artifactPIPParams.add(artifactStateParam);
			}
			Operator artifactOp = odrlConstraint.getOperator();
			if (artifactOp.equals(Operator.IN) || artifactOp.equals(Operator.IS_ANY_OF) ||
					artifactOp.equals(Operator.IS_NONE_OF) ||
					artifactOp.equals(Operator.IS_ALL_OF)) {
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", artifactOp.getMydataOp());
				artifactPIPParams.add(operatorParam);
			}
			PIPBoolean artifactStatePipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), artifactPIPParams);
			ArrayList<PIPBoolean> artifactPIPs = mydataMechanism.getPipBooleans();
			artifactPIPs.add(artifactStatePipBoolean);
			mydataMechanism.setPipBooleans(artifactPIPs);
		}
		return mydataMechanism;
	}

	private MydataMechanism encodingConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint) {
			List<Parameter> pipParams = new ArrayList<>();
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter encodingParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getMydataLeftOperand() + "-uri", rightOperand.getValue());
				pipParams.add(encodingParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF)) {
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean encodingPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);
			ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
			pips.add(encodingPipBoolean);
			mydataMechanism.setPipBooleans(pips);
		}
		return mydataMechanism;
	}

	private MydataMechanism countConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint)
		{
			List<Parameter> countParams = Collections.emptyList();
			Count countFirstOperand = new Count(this.solution, null, ActionType.USE, countParams, FixedTime.ALWAYS);
			//semantically, the count conditions cannot get more than one right operand; counter conflict occurs.
			Constant countSecondOperand = new Constant(ParameterType.NUMBER, odrlConstraint.getRightOperands().get(0).getValue());
			MydataCondition countCondition = new MydataCondition(countFirstOperand, Operator.LT, countSecondOperand);
			ArrayList<MydataCondition> cons = mydataMechanism.getConditions();
			cons.add(countCondition);
			mydataMechanism.setConditions(cons);
		}
		return mydataMechanism;
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

	private ExecuteAction nextPolicyPreobligation(ActionType nextpolicy, Condition odrlRefinement) {
		ArrayList<Parameter> params = new ArrayList<>();
		// list of target policies (offer contracts)
		for(RightOperand rightOperand: odrlRefinement.getRightOperands())
		{
			Parameter nextPolicyTargetParam = new Parameter(ParameterType.STRING, LeftOperand.TARGET_POLICY.getMydataLeftOperand() + "-uri", rightOperand.getValue());
			params.add(nextPolicyTargetParam);
		}
		Operator op = odrlRefinement.getOperator();
		if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
				op.equals(Operator.IS_NONE_OF) ||
				op.equals(Operator.IS_ALL_OF))
		{
			//pass the list operator as a parameter to the PIP, too!
			Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
			params.add(operatorParam);
		}
		return new ExecuteAction(solution, nextpolicy, params);
	 }

 private ExecuteAction informPostobligation(ActionType inform, ArrayList<Condition> odrlRefinements) {
	 ArrayList<Parameter> params = new ArrayList<>();
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
			 if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					 op.equals(Operator.IS_NONE_OF) ||
					 op.equals(Operator.IS_ALL_OF))
			 {
				 //pass the list operator as a parameter to the PIP, too!
				 Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				 params.add(operatorParam);
			 }
		 }
	 }
  	return new ExecuteAction(solution, inform, params);
 }

 private ExecuteAction logPostobligation(ActionType log, ArrayList<Condition> odrlRefinements) {
	 ArrayList<Parameter> params = new ArrayList<>();
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
			 if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					 op.equals(Operator.IS_NONE_OF) ||
					 op.equals(Operator.IS_ALL_OF))
			 {
				 //pass the list operator as a parameter to the PIP, too!
				 Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				 params.add(operatorParam);
			 }
		 }
	 }
  return new ExecuteAction(solution, log, params);

 }

 private Modify anonymizePreobligation(ActionType actionType, ArrayList<Condition> odrlRefinements) {

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
	 return new Modify(eventParameterToModify, actionType, jsonPathQuery, params);
 }

 private ExecuteAction deletePostobligation(ActionType delete, ArrayList<Condition> odrlRefinements) {
	 ArrayList<Parameter> params = new ArrayList<>();
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
		 return new ExecuteAction(solution, delete, null);
	 }
	 return new ExecuteAction(solution, delete, params);

 }

	private ExecuteAction countPostobligation(ActionType count) {
 		return new ExecuteAction(solution, count, null);

	}

 private MydataMechanism absoluteSpatialPositionConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
  if(null != odrlConstraint)
  {
	  ArrayList<Parameter> pipParams = new ArrayList<>();
	  // list of locations
  	for (RightOperand rightOperand: odrlConstraint.getRightOperands())
	{
		Parameter locationParam = new Parameter(ParameterType.STRING,odrlConstraint.getLeftOperand().getMydataLeftOperand()+"-uri", rightOperand.getValue());
		pipParams.add(locationParam);
	}
	  Operator op = odrlConstraint.getOperator();
	  if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
			  op.equals(Operator.IS_NONE_OF) ||
			  op.equals(Operator.IS_ALL_OF))
	  {
		  //pass the list operator as a parameter to the PIP, too!
		  Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
		  pipParams.add(operatorParam);
	  }
   PIPBoolean locationPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

   ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
   pips.add(locationPipBoolean);
	  mydataMechanism.setPipBooleans(pips);
  }
  return mydataMechanism;
 }

 private MydataMechanism timeIntervalConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
  if(null != odrlConstraint)
  {
   ArrayList<DateTime> dateTimes = new ArrayList<>();
	  //semantically, the time conditions cannot get more than one right operand; time conflicts occurs.
   for(RightOperandEntity entity: odrlConstraint.getRightOperands().get(0).getEntities())
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

	  mydataMechanism.setDateTimes(dateTimes);
  }
  return mydataMechanism;
 }

private MydataMechanism dateTimeConstraint( MydataMechanism mydataMechanism, Condition odrlConstraint) {
	if(null != odrlConstraint)
	{
		ArrayList<DateTime> dateTimes = new ArrayList<>();
		//semantically, the time conditions cannot get more than one right operand; time conflicts occurs.
		for(RightOperandEntity entity: odrlConstraint.getRightOperands().get(0).getEntities())
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

		mydataMechanism.setDateTimes(dateTimes);
	}
	return mydataMechanism;
}

private MydataMechanism targetConstraint(MydataMechanism mydataMechanism, String target) {
	if(null != target)
	{
		// get conditions
		Event targetFirstOperand = new Event(ParameterType.STRING, EventParameter.TARGET.getEventParameter(), null);
		Constant targetSecondOperand = new Constant(ParameterType.STRING, target);
		MydataCondition targetCondition = new MydataCondition(targetFirstOperand, Operator.EQ, targetSecondOperand);

		//set conditions
		mydataMechanism.setTarget(target);

		ArrayList<MydataCondition> cons = mydataMechanism.getConditions();
		cons.add(targetCondition);
		mydataMechanism.setConditions(cons);
	}
	return mydataMechanism;
}

 private MydataMechanism purposeConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
  if(null != odrlConstraint)
  {
	  List<Parameter> pipParams = new ArrayList<>();
	  // list of purposes
  	for(RightOperand rightOperand: odrlConstraint.getRightOperands())
	{
		Parameter purposeParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getMydataLeftOperand()+"-uri", rightOperand.getValue());
		pipParams.add(purposeParam);
	}
	  Operator op = odrlConstraint.getOperator();
	  if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
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
   PIPBoolean purposePipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

   ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
   pips.add(purposePipBoolean);
	  mydataMechanism.setPipBooleans(pips);
  }
  return mydataMechanism;
 }

 private MydataMechanism systemConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			// list of systems
			for(RightOperand rightOperand: odrlConstraint.getRightOperands())
			{
				Parameter systemParam = new Parameter(ParameterType.STRING,odrlConstraint.getLeftOperand().getMydataLeftOperand()+"-uri", rightOperand.getValue());
				pipParams.add(systemParam);
			}
			Operator op = odrlConstraint.getOperator();
			if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean systemPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
			pips.add(systemPipBoolean);
			mydataMechanism.setPipBooleans(pips);
		}
		return mydataMechanism;
	}

	private MydataMechanism applicationConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			//list of applications
			for (RightOperand rightOperand: odrlConstraint.getRightOperands())
			{
				Parameter applicationParam = new Parameter(ParameterType.STRING,odrlConstraint.getLeftOperand().getMydataLeftOperand()+"-uri", rightOperand.getValue());
				pipParams.add(applicationParam);
			}
			Operator op = odrlConstraint.getOperator();
			if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean applicationPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
			pips.add(applicationPipBoolean);
			mydataMechanism.setPipBooleans(pips);
		}
		return mydataMechanism;
	}

	private MydataMechanism connectorConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			//List of connectors
			for(RightOperand rightOperand: odrlConstraint.getRightOperands())
			{
				Parameter connectorParam = new Parameter(ParameterType.STRING,odrlConstraint.getLeftOperand().getMydataLeftOperand()+"-uri", rightOperand.getValue());
				pipParams.add(connectorParam);
			}
			Operator op = odrlConstraint.getOperator();
			if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}

			PIPBoolean connectorPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
			pips.add(connectorPipBoolean);
			mydataMechanism.setPipBooleans(pips);
		}
		return mydataMechanism;
	}

	private MydataMechanism securityLevelConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			//list of security levels
			for(RightOperand rightOperand: odrlConstraint.getRightOperands())
			{
				Parameter securityLevelParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getMydataLeftOperand(), rightOperand.getValue());
				pipParams.add(securityLevelParam);
			}
			Operator op = odrlConstraint.getOperator();
			if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}

			PIPBoolean securityLevelPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
			pips.add(securityLevelPipBoolean);
			mydataMechanism.setPipBooleans(pips);
		}
		return mydataMechanism;
	}

	private MydataMechanism stateConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			//list of states
			for(RightOperand rightOperand: odrlConstraint.getRightOperands())
			{
				Parameter stateParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getMydataLeftOperand(), rightOperand.getValue());
				pipParams.add(stateParam);
			}
			Operator op = odrlConstraint.getOperator();
			if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean statePipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
			pips.add(statePipBoolean);
			mydataMechanism.setPipBooleans(pips);
		}
		return mydataMechanism;
	}

	private MydataMechanism roleConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
		if(null != odrlConstraint)
		{
			List<Parameter> pipParams = new ArrayList<>();
			//list of roles
			for(RightOperand rightOperand: odrlConstraint.getRightOperands())
			{
				Parameter roleParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getMydataLeftOperand(), rightOperand.getValue());
				pipParams.add(roleParam);
			}
			Operator op = odrlConstraint.getOperator();
			if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
					op.equals(Operator.IS_NONE_OF) ||
					op.equals(Operator.IS_ALL_OF))
			{
				//pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean rolePipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
			pips.add(rolePipBoolean);
			mydataMechanism.setPipBooleans(pips);
		}
		return mydataMechanism;
	}

 private MydataMechanism paymentConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
  if(null != odrlConstraint)
  {
  	// semantically, only one value must be set for the payment; conflict occurs!
   Parameter valueParam = new Parameter(ParameterType.NUMBER,"value",String.valueOf(odrlConstraint.getRightOperands().get(0).getValue()));
   Parameter contractParam = new Parameter(ParameterType.STRING,"value", odrlConstraint.getContract());
   List<Parameter> pipParams = new ArrayList<>();
   pipParams.add(valueParam);
   pipParams.add(contractParam);
   PIPBoolean paymentPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

	  ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
   pips.add(paymentPipBoolean);
	  mydataMechanism.setPipBooleans(pips);
  }
  return mydataMechanism;
 }

 private MydataMechanism eventConstraint(MydataMechanism mydataMechanism, Condition odrlConstraint) {
  if(null != odrlConstraint)
  {
	  List<Parameter> pipParams = new ArrayList<>();
	  // list of events occurring right now!
	  for(RightOperand rightOperand: odrlConstraint.getRightOperands())
	  {
		  Parameter eventParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getMydataLeftOperand()+"-uri", rightOperand.getValue());
		  pipParams.add(eventParam);
	  }
	  Operator op = odrlConstraint.getOperator();
	  if(op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) ||
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
	  PIPBoolean eventPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

	  ArrayList<PIPBoolean> pips = mydataMechanism.getPipBooleans();
	  pips.add(eventPipBoolean);
	  mydataMechanism.setPipBooleans(pips);
  }
  return mydataMechanism;
 }

 private String createCron(String y, String m, String d, String th, String tm, String ts)
 {
  String cron = ts + " " + tm + " " + th + " " + d + " " + m + " ? " + y ;
  return cron;
 }
}