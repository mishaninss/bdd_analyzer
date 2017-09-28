package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.Examples;
import gherkin.ast.ScenarioOutline;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
@Data
public class ArmaScenarioOutline extends ArmaScenario {

    private List<ArmaExamples> examples;

    public ArmaScenarioOutline(){
        this.keyword = "Scenario Outline";
    }

    public ArmaScenarioOutline(ScenarioOutline gherkinScenarioOutline){
        setKeyword(gherkinScenarioOutline.getKeyword());
        setName(gherkinScenarioOutline.getName());
        setDescription(gherkinScenarioOutline.getDescription());
        setGherkinSteps(gherkinScenarioOutline.getSteps());
        setGherkinTags(gherkinScenarioOutline.getTags());
        setGherkinExamples(gherkinScenarioOutline.getExamples());
    }

    public ArmaScenarioOutline(ArmaScenarioOutline scenarioOutline){
        setKeyword(scenarioOutline.getKeyword());
        setName(scenarioOutline.getName());
        setDescription(scenarioOutline.getDescription());
        setLocation(new ArmaLocation(scenarioOutline.getLocation()));
        steps = new ArrayList<>();
        List<ArmaStep> originSteps = scenarioOutline.getSteps();
        if (CollectionUtils.isNotEmpty(originSteps)){
            originSteps.forEach(originStep -> steps.add(new ArmaStep(originStep)));
        }
        tags = new ArrayList<>();
        List<ArmaTag> originTags = scenarioOutline.getTags();
        if (CollectionUtils.isNotEmpty(originTags)){
            originTags.forEach(originTag -> tags.add(new ArmaTag(originTag)));
        }
        examples = new ArrayList<>();
        List<ArmaExamples> originExamples = scenarioOutline.getExamples();
        if (CollectionUtils.isNotEmpty(originExamples)){
            originExamples.forEach(originExample -> examples.add(new ArmaExamples(originExample)));
        }
    }

    public void setGherkinExamples(List<Examples> gherkinExamples){
        if (CollectionUtils.isEmpty(gherkinExamples)){
            return;
        }
        examples = gherkinExamples.stream().map(ArmaExamples::new).collect(Collectors.toList());
    }

    public void optimizeExamples(){
        ArmaScenarioOutline newOutline = new ArmaScenarioOutline(this);
        if (CollectionUtils.isNotEmpty(newOutline.examples)){
            newOutline.removeEmptyExamples();

            newOutline.joinExamplesOfTheSameScope();
            System.out.println(newOutline);

            newOutline.removeNotUsedColumns();
            System.out.println(newOutline);

            newOutline.replaceNeedlessParameters();
            newOutline.removeEmptyExamples();
            System.out.println(newOutline);
        }
    }

    public void moveParametersToExamples(){
        ArmaScenarioOutline newOutline = new ArmaScenarioOutline(this);
        if (CollectionUtils.isNotEmpty(newOutline.examples)){
            newOutline.removeEmptyExamples();

            newOutline.joinExamplesOfTheSameScope();
            System.out.println(newOutline);

            newOutline.removeNotUsedColumns();
            System.out.println(newOutline);

            newOutline.replaceNeedlessParameters();
            newOutline.removeEmptyExamples();
            System.out.println(newOutline);
        }
    }

    private void removeNotUsedColumns(){
        if (CollectionUtils.isNotEmpty(examples)) {
            ArmaTableRow mergedParams = new ArmaTableRow(examples.get(0).getTableHeader());
            for (int i = 1; i < examples.size(); i++) {
                mergedParams = mergedParams.mergeValues(examples.get(i).getTableHeader());
            }

            List<String> notUsedParams =
                    ListUtils.removeAll(mergedParams.getValues(), getParametersUsage().keySet());

            examples.forEach(example -> example.removeColumns(notUsedParams));
            examples.forEach(ArmaExamples::removeDuplicatedRows);
        }
    }

    private void removeEmptyExamples(){
        if (CollectionUtils.isNotEmpty(examples)) {
            examples.removeIf(examples -> CollectionUtils.isEmpty(examples.getTableBody()));
        }
    }

    private void joinExamplesOfTheSameScope(){
        if (CollectionUtils.size(examples) > 1){
            List<List<ArmaExamples>> groups = groupExamplesByScope();
            List<ArmaExamples> newExamples = new ArrayList<>();
            groups.forEach(group -> newExamples.add(ArmaExamples.join(group)));
            setExamples(newExamples);
        }
    }

    private void replaceNeedlessParameters(){
        ArmaExamples newExamples = ArmaExamples.join(getExamples());
        List<String> onceUsedParams = getParametersUsage().entrySet().stream()
                .filter(entry -> entry.getValue() == 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        List<String> constantColumnNames = newExamples.getParamNames(newExamples.findConstantColumns());
        List<String> paramsToReplace = ListUtils.intersection(onceUsedParams, constantColumnNames);
        if (CollectionUtils.isNotEmpty(paramsToReplace)) {
            Map<String, String> values = newExamples.toMap(0, paramsToReplace);
            getSteps().forEach(step -> step.applyParameters(values));
        }
        removeNotUsedColumns();
    }

    public List<List<ArmaExamples>> groupExamplesByScope(){
        List<List<ArmaExamples>> groups = new ArrayList<>();
        List<ArmaExamples> examplesCopy = new ArrayList<>(examples);
        Iterator<ArmaExamples> iterator = examplesCopy.iterator();
        while (iterator.hasNext()){
            List<ArmaExamples> group = new ArrayList<>();
            groups.add(group);
            ArmaExamples thisExample = iterator.next();
            group.add(thisExample);
            iterator.remove();
            Iterator<ArmaExamples> innerIterator = examplesCopy.iterator();
            while (innerIterator.hasNext()){
                ArmaExamples thatExample = innerIterator.next();
                if (thisExample.hasTheSameScopeWith(thatExample)){
                    group.add(thatExample);
                    innerIterator.remove();
                }
            }
        }
        return groups;
    }

    public Map<String, Integer> getParametersUsage(){
        Map<String, Integer> paramsUsage = new LinkedHashMap<>();
        if (CollectionUtils.isEmpty(steps)){
            return paramsUsage;
        }
        steps.forEach(step -> {
            Map<String, Integer> stepParamUsage = step.getParametersUsage();
            stepParamUsage.forEach((key, value) -> {
                int paramUsage = paramsUsage.getOrDefault(key, 0) + value;
                paramsUsage.put(key, paramUsage);
            });
        });

        return paramsUsage;
    }

    public boolean isParameterUsed(String paramName){
        if (CollectionUtils.isEmpty(steps)){
            return false;
        }
        for (ArmaStep step: steps){
            if (step.containsParameter(paramName)){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append(super.toString());
        if (examples != null){
            examples.forEach(example ->
                    sb.append("\n\n")
                        .append(example.toString()));
        }

        return sb.toString();
    }
}
