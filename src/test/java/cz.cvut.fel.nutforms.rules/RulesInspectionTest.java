package cz.cvut.fel.nutforms.rules;

import cz.cvut.fel.nutforms.rules.inspection.Inspector;
import cz.cvut.fel.nutforms.rules.metamodel.Declaration;
import org.drools.core.base.mvel.MVELCompilationUnit;
import org.drools.core.definitions.rule.impl.RuleImpl;
import org.drools.core.rule.Pattern;
import org.drools.core.rule.RuleConditionElement;
import org.drools.core.rule.constraint.MvelConstraint;
import org.drools.core.spi.Constraint;
import org.junit.Before;
import org.junit.Test;
import org.kie.api.KieBase;
import org.kie.api.KieServices;
import org.kie.api.definition.KiePackage;
import org.kie.api.definition.rule.Rule;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.StatelessKieSession;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * Rules model inspection test.
 */
public class RulesInspectionTest {

    private KieContainer kieContainer;
    private StatelessKieSession accountSession;

    private Inspector inspector;

    @Before
    public void setUp() throws Exception {
        // KieSession creation
        KieServices kieServices = KieServices.Factory.get();
        kieContainer = kieServices.getKieClasspathContainer();
        // Specific session will be created here
        accountSession = kieContainer.newStatelessKieSession("accountsession");
        accountSession.setGlobal("status", "verified");

        inspector = new Inspector();
    }

    @Test
    public void inspectUserEntityRules() {
        StatelessKieSession userSession = kieContainer.newStatelessKieSession("usersession");
        Collection<KiePackage> kiePackages = userSession.getKieBase().getKiePackages();
        assertEquals(2, kiePackages.size());    // 2 pkgs, the other one is imported from entity
        for (KiePackage kiePackage : kiePackages) {
            if (kiePackage.getName().equals("cz.cvut.fel.nutforms.rules.entity")) {
                continue;
            }
            assertEquals(3, kiePackage.getRules().size());
            assertEquals("userentity", kiePackage.getName());
            for (Rule rule : kiePackage.getRules()) {
                assertUserEntityRules(inspector.inspectRule((RuleImpl) rule));
            }
        }
    }

    /**
     * Tests functionality of {@link Inspector#inspectBase(KieBase)}
     */
    @Test
    public void testInspectBase() {
        StatelessKieSession userSession = kieContainer.newStatelessKieSession("usersession");
        Map<String, Set<cz.cvut.fel.nutforms.rules.metamodel.Rule>> packages = inspector.inspectBase(userSession.getKieBase());
        assertEquals(1, packages.size());
        for (Map.Entry<String, Set<cz.cvut.fel.nutforms.rules.metamodel.Rule>> packageRules : packages.entrySet()) {
            assertEquals(3, packageRules.getValue().size());
            assertEquals("userentity", packageRules.getKey());
            for (cz.cvut.fel.nutforms.rules.metamodel.Rule rule : packageRules.getValue()) {
                assertUserEntityRules(rule);
            }
        }
    }

    /**
     * Prints rule content specified in Drools
     *
     * @param rule rule to be inspected
     * @deprecated Not type safe, works only for {@link Pattern} type rules. Use {@link Inspector}
     * instead
     */
    private void inspectRule(RuleImpl rule) {
        System.out.println("\tName: ");
        System.out.println("\t\t" + rule.getName());
        System.out.println("\tDeclarations: ");
        for (String s : rule.getDeclarations().keySet()) {
            System.out.println("\t\t" + s + " [" + rule.getDeclaration(s).getBoxedTypeName() + "]");
        }
        System.out.println("\tConstraints: ");
        for (RuleConditionElement ruleConditionElement : rule.getLhs().getChildren()) {
            for (Constraint constraint : ((Pattern) ruleConditionElement).getConstraints()) {
                System.out.println("\t\t" + "Object type: " + ((Pattern) ruleConditionElement).getObjectType());
                System.out.println("\t\t" + ((MvelConstraint) constraint).getExpression());
            }
        }
        if (rule.getConsequence() != null) {
            System.out.println("\tConsequence: ");
            try {
                Field unit = rule.getConsequence().getClass().getDeclaredField("unit");
                unit.setAccessible(true);
                System.out.println("\t\t" + ((MVELCompilationUnit) unit.get(rule.getConsequence())).getExpression());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private void assertRule(cz.cvut.fel.nutforms.rules.metamodel.Rule rule, String name, String pckg, String condition) {
        assertEquals(name, rule.getName());
        assertEquals(pckg, rule.getPckg());
        assertEquals(condition, rule.getCondition());
    }

    private void assertDeclaration(Declaration declaration, String name, String type, String entity, String field) {
        assertEquals(name, declaration.getName());
        assertEquals(type, declaration.getType());
        assertEquals(entity, declaration.getEntity());
        assertEquals(field, declaration.getField());
    }

    private void assertUserEntityRules(cz.cvut.fel.nutforms.rules.metamodel.Rule inspectedRule) {
        Declaration status = inspectedRule.getGlobals().get("$status");
        assertDeclaration(status, "$status", "java.lang.String", null, null);
        switch (inspectedRule.getName()) {
            case "Is adult":
                assertRule(inspectedRule, "Is adult", "userentity", "age >= 18");
                assertEquals(1, inspectedRule.getDeclarations().size());
                assertDeclaration(inspectedRule.getDeclarations().get("$user"), "$user",
                        "cz.cvut.fel.nutforms.rules.entity.UserEntity", null, null);
                break;
            case "Password is long enough":
                assertRule(inspectedRule, "Password is long enough", "userentity",
                        "$password.length() > 5 && $password.length() < 20");
                assertDeclaration(inspectedRule.getDeclarations().get("$user"), "$user",
                        "cz.cvut.fel.nutforms.rules.entity.UserEntity", null, null);
                assertDeclaration(inspectedRule.getDeclarations().get("$password"), "$password",
                        "java.lang.String", "cz.cvut.fel.nutforms.rules.entity.UserEntity", "password");
                break;
            case "Is verified":
                assertRule(inspectedRule, "Is verified", "userentity", "$status == \"verified\"");
                break;
            default:
                throw new IllegalArgumentException("Unexpected rule in usersession: " + inspectedRule.getName());
        }
    }
}
