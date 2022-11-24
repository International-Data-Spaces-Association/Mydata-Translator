package de.fraunhofer.iese.ids.odrl.mydata.translator.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.fraunhofer.iese.ids.odrl.mydata.translator.interfaces.ICondition;
import de.fraunhofer.iese.ids.odrl.mydata.translator.interfaces.PIP;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.Constant;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.Count;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.DateTime;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.Event;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.EventParameter;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.ExecuteAction;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.FixedTime;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.Modify;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.MydataCondition;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.MydataMechanism;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.MydataPolicy;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.PIPBoolean;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.Parameter;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.ParameterType;
import de.fraunhofer.iese.ids.odrl.mydata.translator.model.Timer;
import de.fraunhofer.iese.ids.odrl.mydata.translator.util.BuildMydataPolicyUtils;
import de.fraunhofer.iese.ids.odrl.policy.library.model.Action;
import de.fraunhofer.iese.ids.odrl.policy.library.model.Condition;
import de.fraunhofer.iese.ids.odrl.policy.library.model.OdrlPolicy;
import de.fraunhofer.iese.ids.odrl.policy.library.model.RightOperand;
import de.fraunhofer.iese.ids.odrl.policy.library.model.RightOperandEntity;
import de.fraunhofer.iese.ids.odrl.policy.library.model.Rule;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.ActionType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.LeftOperand;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.Operator;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RightOperandType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.RuleType;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.TimeUnit;
import de.fraunhofer.iese.ids.odrl.policy.library.model.interfaces.ITranslateCondition;
import de.fraunhofer.iese.ids.odrl.policy.library.model.interfaces.ITranslateDuty;
import de.fraunhofer.iese.ids.odrl.policy.library.model.interfaces.ITranslator;
import de.fraunhofer.iese.ids.odrl.policy.library.model.tooling.PatternUtil;
import lombok.Data;

@Data
public class MydataTranslator implements ITranslator {

	private String solution = "";

	@Override
	public String translateComplexPolicy(OdrlPolicy odrlPolicy) {
		MydataPolicy mydataPolicy = createMydataPolicy(odrlPolicy);
		return new StringBuilder(mydataPolicy.getTimerForPolicy()).append(System.getProperty("line.separator"))
				.append(mydataPolicy.toString()).toString();
	}

	@Override
	public ITranslateCondition translateElapsedTimeConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> timePIPParams = new ArrayList<>();
			List<RightOperand> rightOperands = odrlConstraint.getRightOperands();
			if(BuildMydataPolicyUtils.isNotNull(rightOperands.get(0)) && !rightOperands.isEmpty())
			{
				List<RightOperandEntity> rightOperandEntities = rightOperands.get(0).getRightOperandEntities();
				if(BuildMydataPolicyUtils.isNotNull(rightOperandEntities) && !rightOperandEntities.isEmpty())
				{
					for (RightOperandEntity rightOperandEntity : rightOperandEntities) {
						switch (rightOperandEntity.getEntityType()) {
							case BEGIN:
								RightOperandEntity beginInnerEntity = rightOperandEntity.getInnerEntity();
								Parameter beginParam = new Parameter(ParameterType.STRING, "begin", beginInnerEntity.getValue());
								timePIPParams.add(beginParam);
								break;
							case HASDURATION:
								// Duration d =
								// BuildMydataPolicyUtils.getDurationFromPeriodValue(entity.getValue());
								Parameter durationParam = new Parameter(ParameterType.STRING, "duration", rightOperandEntity.getValue());
								timePIPParams.add(durationParam);
								break;
							case DATETIME:
								Parameter dateTimeParam = new Parameter(ParameterType.STRING, "beginDateTime", rightOperandEntity.getValue());
								timePIPParams.add(dateTimeParam);
								break;
						}
					}
				}else {
					Parameter durationParam = new Parameter(ParameterType.STRING, "duration", rightOperands.get(0).getValue());
					timePIPParams.add(durationParam);
				}
			}

			PIPBoolean elapsedTimePipBoolean = new PIPBoolean(solution, LeftOperand.ELAPSED_TIME, timePIPParams);

			MydataCondition elapsedTimeCondition = new MydataCondition(elapsedTimePipBoolean, null, null);
			return elapsedTimeCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateArtifactStateConstraint(Condition odrlConstraint) {
		if (null != odrlConstraint) {
			List<Parameter> artifactPIPParams = new ArrayList<>();
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter artifactStateParam = new Parameter(ParameterType.STRING,
						odrlConstraint.getLeftOperand().getLabel(), rightOperand.getValue());
				artifactPIPParams.add(artifactStateParam);
			}
			Operator artifactOp = odrlConstraint.getOperator();
			if (artifactOp.equals(Operator.IN) || artifactOp.equals(Operator.IS_ANY_OF)
					|| artifactOp.equals(Operator.IS_NONE_OF) || artifactOp.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", artifactOp.getMydataOp());
				artifactPIPParams.add(operatorParam);
			}
			PIPBoolean artifactStatePipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(),
					artifactPIPParams);
			MydataCondition artifactStateCondition = new MydataCondition(artifactStatePipBoolean, null, null);
			return artifactStateCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateEncodingConstraint(Condition odrlConstraint) {
		return null;
	}

	@Override
	public ITranslateCondition translateCountConstraint(Condition odrlConstraint) {
		if (null != odrlConstraint) {
			List<Parameter> countParams = Collections.emptyList();
			Count countFirstOperand = new Count(this.solution, null, ActionType.USE, countParams, FixedTime.ALWAYS);
			// semantically, the count conditions cannot get more than one right operand;
			// counter conflict occurs.
			Constant countSecondOperand = new Constant(ParameterType.NUMBER,
					odrlConstraint.getRightOperands().get(0).getValue());
			MydataCondition countCondition = new MydataCondition(countFirstOperand, Operator.LT, countSecondOperand);
			return countCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateAbsoluteSpatialPositionConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// list of locations
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter locationParam = new Parameter(ParameterType.STRING,
						odrlConstraint.getLeftOperand().getLabel() + "-uri", rightOperand.getValue());
				pipParams.add(locationParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean locationPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			MydataCondition locationCondition = new MydataCondition(locationPipBoolean, null, null);
			return locationCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateDateTimeConstraint(Condition odrlConstraint) {

		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {

			List<RightOperand> rightOperands = odrlConstraint.getRightOperands();
			if(BuildMydataPolicyUtils.isNotNull(rightOperands.get(0)))
			{
				List<RightOperandEntity> rightOperandEntities = rightOperands.get(0).getRightOperandEntities();
				if(BuildMydataPolicyUtils.isNotNull(rightOperandEntities) && !rightOperandEntities.isEmpty())
				{
					for (RightOperandEntity rightOperandEntity : rightOperandEntities) {
						switch (rightOperandEntity.getEntityType()) {
							case BEGIN:
								RightOperandEntity beginInnerEntity = rightOperandEntity.getInnerEntity();
								String startDateTime = beginInnerEntity.getValue();
								DateTime startTime = new DateTime(Operator.AFTER, startDateTime);
								return startTime;
							case END:
								RightOperandEntity endInnerEntity = rightOperandEntity.getInnerEntity();
								String endDatetime = endInnerEntity.getValue();
								DateTime endTime = new DateTime(Operator.BEFORE, endDatetime);
								return endTime;
							case DATETIME:
								String datetime = rightOperandEntity.getValue();
								DateTime dTime = new DateTime(odrlConstraint.getOperator(), datetime);
								return dTime;
						}
					}
				}else{
					String datetime = rightOperands.get(0).getValue();
					DateTime dTime = new DateTime(odrlConstraint.getOperator(), datetime);
					return dTime;
				}
			}
		}

		return null;
	}

	@Override
	public ITranslateCondition translatePurposeConstraint(Condition odrlConstraint) {

		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// list of purposes
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter purposeParam = new Parameter(ParameterType.STRING,
						odrlConstraint.getLeftOperand().getLabel() + "-uri", rightOperand.getValue());
				pipParams.add(purposeParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			Event event = new Event(ParameterType.STRING, "MsgTarget", "appUri");
			Parameter msgTargetParam = new Parameter(ParameterType.STRING, "MsgTargetAppUri", event);

			pipParams.add(msgTargetParam);
			PIPBoolean purposePipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			MydataCondition purposeCondition = new MydataCondition(purposePipBoolean, null, null);
			return purposeCondition;
		}

		return null;
	}

	@Override
	public ITranslateCondition translateSystemConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// list of systems
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter systemParam = new Parameter(ParameterType.STRING,
						odrlConstraint.getLeftOperand().getLabel() + "-uri", rightOperand.getValue());
				pipParams.add(systemParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}

			PIPBoolean systemPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			MydataCondition systemCondition = new MydataCondition(systemPipBoolean, null, null);
			return systemCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateApplicationConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// list of applications
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter applicationParam = new Parameter(ParameterType.STRING,
						odrlConstraint.getLeftOperand().getLabel() + "-uri", rightOperand.getValue());
				pipParams.add(applicationParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean applicationPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(),
					pipParams);

			MydataCondition applicationCondition = new MydataCondition(applicationPipBoolean, null, null);
			return applicationCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateConnectorConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// List of connectors
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter connectorParam = new Parameter(ParameterType.STRING,
						odrlConstraint.getLeftOperand().getLabel() + "-uri", rightOperand.getValue());
				pipParams.add(connectorParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}

			PIPBoolean connectorPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			MydataCondition connectorCondition = new MydataCondition(connectorPipBoolean, null, null);
			return connectorCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateSecurityLevelConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// list of security levels
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter securityLevelParam = new Parameter(ParameterType.STRING,
						odrlConstraint.getLeftOperand().getLabel(), rightOperand.getValue());
				pipParams.add(securityLevelParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}

			PIPBoolean securityLevelPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(),
					pipParams);

			MydataCondition securityLevelCondition = new MydataCondition(securityLevelPipBoolean, null, null);
			return securityLevelCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateStateConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// list of states
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter stateParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getLabel(),
						rightOperand.getValue());
				pipParams.add(stateParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean statePipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			MydataCondition stateCondition = new MydataCondition(statePipBoolean, null, null);
			return stateCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateRoleConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// list of roles
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter roleParam = new Parameter(ParameterType.STRING, odrlConstraint.getLeftOperand().getLabel(),
						rightOperand.getValue());
				pipParams.add(roleParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			PIPBoolean rolePipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			MydataCondition roleCondition = new MydataCondition(rolePipBoolean, null, null);
			return roleCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translatePaymentConstraint(Condition odrlConstraint) {
		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			// semantically, only one value must be set for the payment; conflict occurs!
			List<Parameter> pipParams = new ArrayList<>();
			Parameter valueParam = new Parameter(ParameterType.NUMBER, "value",
					String.valueOf(odrlConstraint.getRightOperands().get(0).getValue()));
			pipParams.add(valueParam);
			if(!BuildMydataPolicyUtils.isNullOrEmpty(odrlConstraint.getContract()))
			{
				Parameter contractParam = new Parameter(ParameterType.STRING, "contract", odrlConstraint.getContract());
				pipParams.add(contractParam);
			}
			if(!BuildMydataPolicyUtils.isNullOrEmpty(odrlConstraint.getUnit()))
			{
				Parameter unitParam = new Parameter(ParameterType.STRING, "unit", odrlConstraint.getUnit());
				pipParams.add(unitParam);
			}

			PIPBoolean paymentPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			MydataCondition paymentCondition = new MydataCondition(paymentPipBoolean, null, null);
			return paymentCondition;
		}
		return null;
	}

	@Override
	public ITranslateCondition translateEventConstraint(Condition odrlConstraint) {

		if (BuildMydataPolicyUtils.isNotNull(odrlConstraint)) {
			List<Parameter> pipParams = new ArrayList<>();
			// list of events occurring right now!
			for (RightOperand rightOperand : odrlConstraint.getRightOperands()) {
				Parameter eventParam = new Parameter(ParameterType.STRING,
						odrlConstraint.getLeftOperand().getLabel() + "-uri", rightOperand.getValue());
				pipParams.add(eventParam);
			}
			Operator op = odrlConstraint.getOperator();
			if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
					|| op.equals(Operator.IS_ALL_OF)) {
				// pass the list operator as a parameter to the PIP, too!
				Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
				pipParams.add(operatorParam);
			}
			Event event = new Event(ParameterType.STRING, "MsgTarget", "appUri");
			Parameter msgTargetParam = new Parameter(ParameterType.STRING, "MsgTargetAppUri", event);

			pipParams.add(msgTargetParam);
			PIPBoolean eventPipBoolean = new PIPBoolean(this.solution, odrlConstraint.getLeftOperand(), pipParams);

			MydataCondition eventCondition = new MydataCondition(eventPipBoolean, null, null);
			return eventCondition;
		}
		return null;
	}

	@Override
	public ITranslateDuty translateNextPolicyDuty(Rule odrlDuty) {
		return null;
	}

	@Override
	public ITranslateDuty translateInformDuty(Rule odrlDuty) {
		ActionType actionType = odrlDuty.getAction().getType();
		List<Condition> odrlRefinements = odrlDuty.getAction().getRefinements();
		List<Parameter> params = new ArrayList<>();
		if (BuildMydataPolicyUtils.isNotNull(odrlRefinements) && !odrlRefinements.isEmpty()) {
			for (Condition odrlRefinement : odrlRefinements) {
				// list of recipients or informed parties
//			if (odrlRefinement.getLeftOperand().equals(LeftOperand.INFORMEDPARTY)) {
//				for(RightOperand rightOperand: odrlRefinement.getRightOperands()) {
//					Parameter informedPartyParam = new Parameter(ParameterType.STRING, LeftOperand.INFORMEDPARTY.getLabel() + "-uri", rightOperand.getValue());
//					params.add(informedPartyParam);
//				}
//			} else
				if (odrlRefinement.getLeftOperand().equals(LeftOperand.RECIPIENT)) {
					for (RightOperand rightOperand : odrlRefinement.getRightOperands()) {
						Parameter recipientParam = new Parameter(ParameterType.STRING,
								LeftOperand.RECIPIENT.getLabel() + "-uri", rightOperand.getValue());
						params.add(recipientParam);
					}
				} else if (odrlRefinement.getLeftOperand().equals(LeftOperand.NOTIFICATION_LEVEL)) {
					for (RightOperand rightOperand : odrlRefinement.getRightOperands()) {
						Parameter notifLevelParam = new Parameter(ParameterType.STRING,
								LeftOperand.NOTIFICATION_LEVEL.getLabel(), rightOperand.getValue());
						params.add(notifLevelParam);
					}
				}
				Operator op = odrlRefinement.getOperator();
				if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
						|| op.equals(Operator.IS_ALL_OF)) {
					// pass the list operator as a parameter to the PIP, too!
					Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
					params.add(operatorParam);
				}
			}
		}
		return new ExecuteAction(solution, actionType, params);
	}

	@Override
	public ITranslateDuty translateLogDuty(Rule odrlDuty) {
		ActionType actionType = odrlDuty.getAction().getType();
		List<Condition> odrlRefinements = odrlDuty.getAction().getRefinements();
		List<Parameter> params = new ArrayList<>();
		if (BuildMydataPolicyUtils.isNotNull(odrlRefinements) && !odrlRefinements.isEmpty()) {
			for (Condition odrlRefinement : odrlRefinements) {
				// list of system devices

				if (odrlRefinement.getLeftOperand().equals(LeftOperand.SYSTEM_DEVICE)) {
					for (RightOperand rightOperand : odrlRefinement.getRightOperands()) {
						Parameter systemDeviceParam = new Parameter(ParameterType.STRING,
								LeftOperand.SYSTEM_DEVICE.getLabel() + "-uri", rightOperand.getValue());
						params.add(systemDeviceParam);
					}
				} else if (odrlRefinement.getLeftOperand().equals(LeftOperand.LOG_LEVEL)) {
					for (RightOperand rightOperand : odrlRefinement.getRightOperands()) {
						Parameter logLevelParam = new Parameter(ParameterType.STRING, LeftOperand.LOG_LEVEL.getLabel(),
								rightOperand.getValue());
						params.add(logLevelParam);
					}
				}
				Operator op = odrlRefinement.getOperator();
				if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
						|| op.equals(Operator.IS_ALL_OF)) {
					// pass the list operator as a parameter to the PIP, too!
					Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
					params.add(operatorParam);
				}
			}
		}
		return new ExecuteAction(solution, actionType, params);
	}

	@Override
	public ITranslateDuty translateAnonymizeDuty(Rule odrlDuty) {
		ActionType actionType = odrlDuty.getAction().getType();
		List<Condition> odrlRefinements = odrlDuty.getAction().getRefinements();
		String eventParameterToModify = "DataObject";
		List<Parameter> params = null;
		String jsonPathQuery = "";
		if (BuildMydataPolicyUtils.isNotNull(odrlRefinements) && !odrlRefinements.isEmpty()) {
			for (Condition odrlRefinement : odrlRefinements) {
				if (odrlRefinement.getLeftOperand().equals(LeftOperand.REPLACE_WITH)) {
					ParameterType paramType = ParameterType.STRING;
					if (odrlRefinement.getType().equals(RightOperandType.INTEGER)
							|| odrlRefinement.getType().equals(RightOperandType.DECIMAL)) {
						paramType = ParameterType.NUMBER;
					}
					// semantically, you can replace a field with only one value.
					Parameter replaceWithParam = new Parameter(paramType, "replaceWith",
							odrlRefinement.getRightOperands().get(0).getValue());
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

	@Override
	public ITranslateDuty translateDeleteDuty(Rule odrlDuty) {
		ActionType actionType = odrlDuty.getAction().getType();
		List<Condition> odrlRefinements = odrlDuty.getAction().getRefinements();
		List<Parameter> params = new ArrayList<>();
		if (BuildMydataPolicyUtils.isNotNull(odrlRefinements) && !odrlRefinements.isEmpty()) {
			for (Condition odrlRefinement : odrlRefinements) {
				if (null != odrlRefinement) {
					List<RightOperand> rightOperands = odrlRefinement.getRightOperands();
					if(BuildMydataPolicyUtils.isNotNull(rightOperands.get(0)))
					{
						List<RightOperandEntity> rightOperandEntities = rightOperands.get(0).getRightOperandEntities();
						if(BuildMydataPolicyUtils.isNotNull(rightOperandEntities) && !rightOperandEntities.isEmpty())
						{
							for (RightOperandEntity rightOperandEntity : rightOperandEntities) {
								switch (rightOperandEntity.getEntityType()) {
									case BEGIN:
										RightOperandEntity beginInnerEntity = rightOperandEntity.getInnerEntity();
										Parameter beginParam = new Parameter(ParameterType.STRING, "begin",
												beginInnerEntity.getValue());
										params.add(beginParam);
										break;
									case HASDURATION:
										Parameter durationParam = new Parameter(ParameterType.STRING, "delay",
												String.valueOf(rightOperandEntity.getValue()));
										params.add(durationParam);
										break;
									case END:
										RightOperandEntity endInnerEntity = rightOperandEntity.getInnerEntity();
										Parameter endParam = new Parameter(ParameterType.STRING, "deadline",
												endInnerEntity.getValue());
										params.add(endParam);
										break;
									case DATETIME:
										Parameter dateTimeParam = new Parameter(ParameterType.STRING, "dateTime",
												rightOperandEntity.getValue());
										params.add(dateTimeParam);
										break;
								}
							}
						}else{
							String datetime = rightOperands.get(0).getValue();
							Parameter durationParam = new Parameter(ParameterType.STRING, odrlRefinement.getLeftOperand().getOdrlLeftOperand(),
									String.valueOf(datetime));
							params.add(durationParam);
						}
					}
				}
			}
		}

		if (params.isEmpty()) {
			return new ExecuteAction(solution, actionType, null);
		}
		return new ExecuteAction(solution, actionType, params);
	}

	@Override
	public ITranslateDuty translateCountDuty(Rule odrlDuty) {
		return null;
	}

	@Override
	public ITranslateCondition translateConstraint(Condition odrlConstraint) {
		return null;
	}

	@Override
	public ITranslateDuty translateDuty(Rule odrlDuty) {
		return null;
	}

	public MydataPolicy createMydataPolicy(OdrlPolicy odrlPolicy) {

		this.solution = BuildMydataPolicyUtils.getSolution(odrlPolicy);
		MydataPolicy mydataPolicy = BuildMydataPolicyUtils.buildPolicy(odrlPolicy, this.solution);
		List<MydataMechanism> mydataMechanisms = new ArrayList<>();

		if (PatternUtil.isNotEmpty(odrlPolicy.getRules())) {
			for (Rule rule : odrlPolicy.getRules()) {
				Action action = rule.getAction();
				RuleType ruleType = rule.getRuleType();
				if (null != action) {
					MydataMechanism mydataMechanism = new MydataMechanism(this.solution, action.getType(), ruleType,
							false, null);

					// set target condition to mechanism
					if (null != rule.getTarget()) {
						this.addTargetConstraintToMechansim(mydataMechanism, rule.getTarget().getUri().toString());
					}

					if (rule.getRuleType().equals(RuleType.OBLIGATION)) {
						List<ExecuteAction> pxps = new ArrayList<>();
						if (null != action.getRefinements()) {
							for (Condition odrlRefinement : action.getRefinements()) {
								if (odrlRefinement.getLeftOperand().equals(LeftOperand.DELAY_PERIOD)) {
									List<Parameter> pipParams = new ArrayList<>();
									// semantically, the time conditions cannot get more than one right operand;
									// time conflicts occurs.
									for (RightOperandEntity entity : odrlRefinement.getRightOperands().get(0)
											.getRightOperandEntities()) {
										switch (entity.getEntityType()) {
										case BEGIN:
											RightOperandEntity beginInnerEntity = entity.getInnerEntity();
											Parameter beginParam = new Parameter(ParameterType.STRING, "begin",
													beginInnerEntity.getValue());
											pipParams.add(beginParam);
											break;
										case DATETIME:
											Parameter datetimeParam = new Parameter(ParameterType.STRING,
													"beginDateTime", entity.getValue());
											pipParams.add(datetimeParam);
											break;
										case HASDURATION:
											TimeUnit tu = getTimerUnit(entity.getValue());
											Timer timer = new Timer(tu, "", mydataPolicy.getPid(), solution,
													ActionType.DELETE, null);
											mydataPolicy.setTimer(timer);

											Parameter durationParam = new Parameter(ParameterType.STRING, "delay",
													String.valueOf(entity.getValue()));
											pipParams.add(durationParam);
											break;
										}
									}

									PIPBoolean delayPeriodPipBoolean = new PIPBoolean(solution, LeftOperand.DELAY_PERIOD,
											pipParams);
									List<PIPBoolean> pips = mydataMechanism.getPipBooleans();
									pips.add(delayPeriodPipBoolean);
									mydataMechanism.setPipBooleans(pips);

									ExecuteAction pxp = new ExecuteAction(solution, action.getType(), null);
									pxps.add(pxp);
								} else if (odrlRefinement.getLeftOperand().equals(LeftOperand.DATE_TIME)) {
									// semantically, the date time conditions cannot get more than one right
									// operand; time conflicts occurs.
									for (RightOperandEntity entity : odrlRefinement.getRightOperands().get(0)
											.getRightOperandEntities()) {
										switch (entity.getEntityType()) {
										case END:
											RightOperandEntity endInnerEntity = entity.getInnerEntity();
											DateTime endDateTime = new DateTime(odrlRefinement.getOperator(), //TODO: fix DATETIME class to handle it
													endInnerEntity.getValue());
											String endCron = createCron(endDateTime.getYear(), endDateTime.getMonth(),
													endDateTime.getDay(), endDateTime.getHour(),
													endDateTime.getMinute(), endDateTime.getSecond());
											Timer endTimer = new Timer(null, endCron, mydataPolicy.getPid(), solution,
													ActionType.DELETE, null);
											mydataPolicy.setTimer(endTimer);
											break;
										case DATETIME:
											DateTime dateTime = new DateTime(odrlRefinement.getOperator(), entity.getValue()); //TODO: fix DATETIME class to handle it
											String cron = createCron(dateTime.getYear(), dateTime.getMonth(),
													dateTime.getDay(), dateTime.getHour(), dateTime.getMinute(),
													dateTime.getSecond());
											Timer timer = new Timer(null, cron, mydataPolicy.getPid(), solution,
													ActionType.DELETE, null);
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
					if (null != rule.getConstraints()) {
						for (Condition odrlConstraint : rule.getConstraints()) {
							switch (odrlConstraint.getLeftOperand()) {
							case PURPOSE:
								MydataCondition purposeCondition = (MydataCondition) this.translatePurposeConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, purposeCondition);
								break;
							case EVENT:
								MydataCondition eventCondition = (MydataCondition) this.translateEventConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, eventCondition);
								break;
							case ABSOLUTE_SPATIAL_POSITION:
								MydataCondition locationCondition = (MydataCondition) this.translateAbsoluteSpatialPositionConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, locationCondition);
								break;
							case PAY_AMOUNT:
								MydataCondition paymentCondition = (MydataCondition) this.translatePaymentConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, paymentCondition);
								break;
							case SYSTEM:
								MydataCondition systemCondition = (MydataCondition) this.translateSystemConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, systemCondition);
								break;
							case APPLICATION:
								MydataCondition applicationCondition = (MydataCondition) this.translateApplicationConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, applicationCondition);
								break;
							case CONNECTOR:
								MydataCondition connectorCondition = (MydataCondition) this.translateConnectorConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, connectorCondition);
								break;
							case SECURITY_LEVEL:
								MydataCondition securityLevelCondition = (MydataCondition) this.translateSecurityLevelConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, securityLevelCondition);
								break;
							case STATE:
								MydataCondition stateCondition = (MydataCondition) this.translateStateConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, stateCondition);
								break;
							case ROLE:
								MydataCondition roleCondition = (MydataCondition) this.translateRoleConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, roleCondition);
								break;
								//same for both cases
							case POLICY_EVALUATION_TIME:
							case DATE_TIME:
								DateTime dateTimeCondition = (DateTime) this.translateDateTimeConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, dateTimeCondition);
								break;
							case COUNT:
								MydataCondition countCondition = (MydataCondition) this.translateCountConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, countCondition);
								break;
							case ARTIFACT_STATE:
								MydataCondition ArtifactStateCondition = (MydataCondition) this.translateArtifactStateConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, ArtifactStateCondition);
								break;
							case ELAPSED_TIME:
								MydataCondition elapsedTimeCondition = (MydataCondition) this.translateElapsedTimeConstraint(odrlConstraint);
								addConditionToMechanism(mydataMechanism, elapsedTimeCondition);
								break;
							}
						}
					}

					List<ExecuteAction> pxps = new ArrayList<>();
					List<Modify> modifiers = new ArrayList<>();
					if (null != rule.getDuties() && !rule.getDuties().isEmpty()) {
						for (Rule duty : rule.getDuties()) {
							ActionType actionType = duty.getAction().getType();
							List<Condition> odrlRefinements = duty.getAction().getRefinements();
							switch (actionType) {
								case ANONYMIZE:
								case REPLACE:
								case DROP:
									Modify anonymizeModifier = (Modify) translateAnonymizeDuty(duty);
									modifiers.add(anonymizeModifier);
									break;
								case NEXT_POLICY:
									if (null != odrlRefinements) {
										for (Condition odrlRefinement : odrlRefinements) {
											ExecuteAction nextPolicyPXP = nextPolicyPreobligation(actionType,
													odrlRefinement);
											pxps.add(nextPolicyPXP);
										}
									}
									break;
								case DELETE:
									ExecuteAction deletePXP = (ExecuteAction) translateDeleteDuty(duty);
									pxps.add(deletePXP);
									break;
								case INFORM:
								case NOTIFY:
									ExecuteAction informPXP = (ExecuteAction) translateInformDuty(duty);
									pxps.add(informPXP);
									break;
								case LOG:
									ExecuteAction logPXP = (ExecuteAction) translateLogDuty(duty);
									pxps.add(logPXP);
									break;
								case INCREMENT_COUNTER:
									ExecuteAction incrementCounterPXP = countPostobligation(actionType);
									pxps.add(incrementCounterPXP);
									break;

							}
						}
					}

					if (!pxps.isEmpty()) {
						mydataMechanism.setHasDuty(true);
						mydataMechanism.setPxps(pxps);
					}
					if (!modifiers.isEmpty()) {
						mydataMechanism.setModifiers(modifiers);
					}

					mydataMechanisms.add(mydataMechanism);
				}
			}
		}

		mydataPolicy.setMechanisms(mydataMechanisms);
		return mydataPolicy;
	}

	private void addConditionToMechanism(MydataMechanism mydataMechanism, ICondition condition) {
		if(null != condition)
		{
			if(condition instanceof MydataCondition)
			{
				if(null != ((MydataCondition) condition).getSecondOperand() && null != ((MydataCondition) condition).getOperator())
				{
					mydataMechanism.getConditions().add((MydataCondition) condition);
				}else if (null != ((MydataCondition) condition).getFirstOperand())
				{
					if(((MydataCondition) condition).getFirstOperand() instanceof PIP)
					{
						mydataMechanism.getPipBooleans().add((PIPBoolean) ((MydataCondition) condition).getFirstOperand());
					}
				}
			}else if (condition instanceof DateTime)
			{
				mydataMechanism.getDateTimes().add((DateTime) condition);
			}

		}
	}

	private TimeUnit getTimerUnit(String value) {
		// start from the smallest time unit
		if (value.contains("H")) {
			return TimeUnit.HOURS;
		} else if (value.contains("D")) {
			return TimeUnit.DAYS;
		} else if (value.contains("M")) {
			return TimeUnit.MONTHS;
		} else {
			return TimeUnit.YEARS;
		}
	}

	private ExecuteAction nextPolicyPreobligation(ActionType nextpolicy, Condition odrlRefinement) {
		List<Parameter> params = new ArrayList<>();
		// list of target policies (offer contracts)
		for (RightOperand rightOperand : odrlRefinement.getRightOperands()) {
			Parameter nextPolicyTargetParam = new Parameter(ParameterType.STRING,
					LeftOperand.TARGET_POLICY.getLabel() + "-uri", rightOperand.getValue());
			params.add(nextPolicyTargetParam);
		}
		Operator op = odrlRefinement.getOperator();
		if (op.equals(Operator.IN) || op.equals(Operator.IS_ANY_OF) || op.equals(Operator.IS_NONE_OF)
				|| op.equals(Operator.IS_ALL_OF)) {
			// pass the list operator as a parameter to the PIP, too!
			Parameter operatorParam = new Parameter(ParameterType.STRING, "operator", op.getMydataOp());
			params.add(operatorParam);
		}
		return new ExecuteAction(solution, nextpolicy, params);
	}

	private ExecuteAction countPostobligation(ActionType count) {
		return new ExecuteAction(solution, count, null);

	}

	private void addTargetConstraintToMechansim(MydataMechanism mydataMechanism, String target) {
		if (null != target) {
			// get conditions
			Event targetFirstOperand = new Event(ParameterType.STRING, EventParameter.TARGET.getEventParameter(), null);
			Constant targetSecondOperand = new Constant(ParameterType.STRING, target);
			MydataCondition targetCondition = new MydataCondition(targetFirstOperand, Operator.EQ, targetSecondOperand);

			// set conditions
			mydataMechanism.setTarget(target);
			mydataMechanism.getConditions().add(targetCondition);
		}
	}

	private String createCron(String y, String m, String d, String th, String tm, String ts) {
		String cron = ts + " " + tm + " " + th + " " + d + " " + m + " ? " + y;
		return cron;
	}
}