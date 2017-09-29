package com.github.mishaninss.bddanalyzer.model;

import com.github.mishaninss.bddanalyzer.GherkinScanner;
import com.github.mishaninss.bddanalyzer.StepDefinitionsScanner;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by Sergey_Mishanin on 9/29/17.
 */
@Data
public class ArmaProject {
    private final String stepDefsRoot;
    private final String featuresRoot;
    private List<ArmaFeature> features;
    private List<ArmaStepDef> stepDefs;

    public ArmaProject(String stepDefsRoot, String featuresRoot) {
        this.stepDefsRoot = stepDefsRoot;
        this.featuresRoot = featuresRoot;
        features = new LinkedList<>();
        stepDefs = new LinkedList<>();
    }

    public void scan(){
        features.addAll(new GherkinScanner(featuresRoot).collectFeatures());
        stepDefs.addAll(new StepDefinitionsScanner(stepDefsRoot).collectStepDefinitions());
        mergeStepsAndStepDefs();
    }

    public List<ArmaScenario> getScenarios(){
        List<ArmaScenario> scenarios = new LinkedList<>();
        getFeatures().forEach(feature -> scenarios.addAll(feature.getScenarios()));
        return scenarios;
    }

    public List<ArmaScenarioOutline> getScenarioOutlines(){
        List<ArmaScenarioOutline> scenarioOutlines = new LinkedList<>();
        getScenarios().stream()
                .filter(scenario -> scenario instanceof ArmaScenarioOutline)
                .forEach(scenario -> scenarioOutlines.add((ArmaScenarioOutline) scenario));
        return scenarioOutlines;
    }

    public List<ArmaStep> getNotImplementedSteps(){
        return getSteps().stream()
                .filter(step -> !step.isImplemented())
                .collect(Collectors.toList());
    }

    public List<ArmaStep> getImplementedSteps(){
        return getSteps().stream()
                .filter(ArmaStep::isImplemented)
                .collect(Collectors.toList());
    }

    public List<ArmaBackground> getBackgrounds(){
        return features.stream()
                .filter(feature -> feature.getBackground() != null)
                .map(ArmaFeature::getBackground)
                .collect(Collectors.toList());
    }

    public List<ArmaStep> getSteps(){
        List<ArmaStep> steps = new ArrayList<>();
        getBackgrounds().forEach(background -> steps.addAll(background.getSteps()));
        getScenarios().forEach(scenario -> steps.addAll(scenario.getSteps()));
        return steps;
    }

    public Set<ArmaTag> getTags(){
        Set<ArmaTag> allTags = new HashSet<>();
        features.forEach(feature -> allTags.addAll(feature.getTags()));

        getBackgrounds().forEach(background -> allTags.addAll(background.getTags()));

        getScenarios().forEach(scenario -> {
            allTags.addAll(scenario.getTags());

            if (scenario instanceof ArmaScenarioOutline){
                List<ArmaExamples> examples = ((ArmaScenarioOutline) scenario).getExamples();
                if (CollectionUtils.isNotEmpty(examples)){
                    examples.forEach(example -> allTags.addAll(example.getTags()));
                }
            }
        });

        return allTags;
    }

    public List<ArmaStepDef> getUsedStepDefinitions(){
        return getSteps().stream()
                .filter(ArmaStep::isImplemented)
                .map(ArmaStep::getStepDef)
                .collect(Collectors.toList());
    }

    public List<ArmaStepDef> getNotUsedStepDefinitions(){
        return ListUtils.subtract(stepDefs, getUsedStepDefinitions());
    }

    public long countTests(){
        long testsCount = 0;
        for (ArmaScenario scenario: getScenarios()){
            if (scenario instanceof ArmaScenarioOutline){
                for (ArmaExamples examples: ((ArmaScenarioOutline) scenario).getExamples()){
                    if (examples.getTableBody() != null){
                        testsCount += examples.getTableBody().size();
                    } else {
                        testsCount++;
                    }
                }
            } else {
                testsCount++;
            }
        }
        return testsCount;
    }

    public String printStatistics(){
        ArmaDataTable table = new ArmaDataTable();
        table.addRow("Features", String.valueOf(getFeatures().size()));
        table.addRow("Scenarios", String.valueOf(getScenarios().size()));
        table.addRow("Scenario Outlines", String.valueOf(getScenarioOutlines().size()));
        table.addRow("Backgrounds", String.valueOf(getBackgrounds().size()));
        table.addRow("Steps", String.valueOf(getSteps().size()));
        table.addRow("Implemented steps", String.valueOf(getImplementedSteps().size()));
        table.addRow("Not implemented steps", String.valueOf(getNotImplementedSteps().size()));
        table.addRow("Step Definitions", String.valueOf(stepDefs.size()));
        table.addRow("Not used Step Definitions", String.valueOf(getNotUsedStepDefinitions().size()));
        table.addRow("Tags", String.valueOf(getTags().size()));
        table.addRow("Tests", String.valueOf(countTests()));
        System.out.println(table);
        return table.toString();
    }

    private void mergeStepsAndStepDefs(){
        if (CollectionUtils.isEmpty(features) || CollectionUtils.isEmpty(stepDefs)){
            return;
        }

        features.forEach(feature ->
        {
            if (feature.getBackground() != null){
                feature.getBackground().getSteps().forEach(step -> applyStepDef(stepDefs, step, step.getText()));
            }
            feature.getScenarios().forEach(scenario ->
            {
                if (scenario instanceof ArmaScenarioOutline){
                    ArmaScenarioOutline aso = (ArmaScenarioOutline) scenario;
                    if (CollectionUtils.isNotEmpty(aso.getExamples())) {
                        scenario.getSteps().forEach(step ->
                            applyStepDef(stepDefs, step, ArmaStep.applyExample(step, aso.getExamples().get(0), 0).getText()));
                    } else {
                        scenario.getSteps().forEach(step -> applyStepDef(stepDefs, step, step.getText()));
                    }
                } else {
                    scenario.getSteps().forEach(step -> applyStepDef(stepDefs, step, step.getText()));
                }
            });
        });
    }

    private static void applyStepDef(List<ArmaStepDef> stepDefs, ArmaStep step, String text){
        for (ArmaStepDef stepDef: stepDefs){
            Pattern pattern = Pattern.compile(stepDef.getText());
            Matcher matcher = pattern.matcher(text);
            if (matcher.matches()){
                step.setStepDef(stepDef);
/*
                int paramsCount = matcher.groupCount();
                if (paramsCount > 0){
                    //System.out.println(text);
                    //System.out.println(paramsCount);
                    for (int i = 1; i <= paramsCount; i++){
                        //System.out.println(matcher.group(i));
                    }
                }
*/
                return;
            }
        }
    }
}
