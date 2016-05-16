package cz.cvut.fel.nutforms.rules;

import cz.cvut.fel.nutforms.rules.inspection.interpreter.ExpressionInterpreter;
import cz.cvut.fel.nutforms.rules.metamodel.condition.Condition;
import cz.cvut.fel.nutforms.rules.metamodel.condition.Eval;
import cz.cvut.fel.nutforms.rules.metamodel.condition.Group;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Tests the functionality of {@link cz.cvut.fel.nutforms.rules.inspection.interpreter.ExpressionInterpreter}.
 */
public class ExpressionInterpreterTest {

    @Test
    public void testInterpret() {
        ExpressionInterpreter interpreter = new ExpressionInterpreter();
        String expression1 = "user.name == null, user.email == null";
        Group interpret = (Group) interpreter.interpret(expression1);
        assertEquals(Group.Operator.COMMA, interpret.getOperator());    // different priority than AND
        assertEquals("user.name == null", ((Eval)interpret.getLeft()).getConstraint());
        assertEquals("user.email == null", ((Eval)interpret.getRight()).getConstraint());
        assertFalse(interpret.isNegated());

        String expression3 = "password.length() > 5 && password.length() < 20 || age >= 18";
        Group condition = (Group)interpreter.interpret(expression3);
        assertEquals(Group.Operator.OR, condition.getOperator());
        assertEquals("age >= 18", ((Eval)condition.getRight()).getConstraint());
        assertFalse(condition.isNegated());
        Group left1 = (Group) condition.getLeft();
        assertFalse(left1.isNegated());
        assertEquals(Group.Operator.AND, left1.getOperator());
        assertEquals("password.length() > 5", ((Eval)left1.getLeft()).getConstraint());
        assertEquals("password.length() < 20", ((Eval)left1.getRight()).getConstraint());
    }
}
