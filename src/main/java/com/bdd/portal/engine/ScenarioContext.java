package com.bdd.portal.engine;

import com.bdd.portal.entity.ScenarioExecution;

public class ScenarioContext {
    private static final ThreadLocal<ScenarioExecution> currentScenario = new ThreadLocal<>();

    public static void setScenarioExecution(ScenarioExecution scenario) {
        currentScenario.set(scenario);
    }

    public static ScenarioExecution getScenarioExecution() {
        return currentScenario.get();
    }

    public static void clear() {
        currentScenario.remove();
    }
}
