package de.fraunhofer.iese.ids.odrl.mydata.translator.model;


import de.fraunhofer.iese.ids.odrl.mydata.translator.interfaces.ICondition;
import de.fraunhofer.iese.ids.odrl.mydata.translator.interfaces.IOperand;
import de.fraunhofer.iese.ids.odrl.policy.library.model.enums.Operator;
import lombok.Data;

@Data
public class MydataCondition implements ICondition {

 IOperand firstOperand;
 Operator operator;
 IOperand secondOperand;

 public MydataCondition(IOperand firstOperand, Operator operator, IOperand secondOperand)
 {
  this.firstOperand = firstOperand;
  this.operator = operator;
  this.secondOperand = secondOperand;
 }

 public MydataCondition() {
 }

 public void setOperator(Operator op){
  this.operator = op;
 }

 public Operator getOperator() {
  return operator;
 }

 @Override
 public String toString() {
  return  "      <" + operator.getMydataOp() + "> " + System.lineSeparator() +
          firstOperand +
          secondOperand +
          "      </" + operator.getMydataOp() + "> " + System.lineSeparator();
 }
}
