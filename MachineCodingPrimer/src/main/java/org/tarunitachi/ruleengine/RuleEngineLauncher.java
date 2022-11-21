package org.tarunitachi.ruleengine;

//implement some kind of rule engine.
// so rules can be either to increment/decrement a value from some third party apis,
// user should be able to schedule the rule at any time of the day,
// example a increment x to 1.5x at 10Am - 3PM now x can be from third party apis
// and this rule should increment the value at 10 Am and reverse its effect at 3PM.
// similary when there are million rules how we can efficiently schedule all these rules automatically,
// how to handle rule collision and what will happen if the rules fails to execute at the start time.
public class RuleEngineLauncher {
}
