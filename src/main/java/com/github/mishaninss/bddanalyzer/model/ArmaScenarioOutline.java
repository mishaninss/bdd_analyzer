package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.Examples;
import gherkin.ast.ScenarioOutline;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Objective representation of a Gherkin Scenario Outline
 * Created by Sergey_Mishanin on 11/16/16.
 * @see ScenarioOutline
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

    public ArmaScenarioOutline(@NonNull ArmaScenarioOutline scenarioOutline){
        super(scenarioOutline);

        if (scenarioOutline.hasExamples()){
            scenarioOutline.getExamples()
                    .forEach(originExample -> examples.add(new ArmaExamples(originExample)));
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
        List<Map<String, Integer>> paramsData = new LinkedList<>();
        paramsData.add(mergeParametersUsage(steps.stream()
            .map(ArmaStep::getParametersUsage)
            .collect(Collectors.toList())));

        paramsData.add(ArmaScenarioOutline.getParametersUsage(name));

        return mergeParametersUsage(paramsData);
    }

    public boolean isParameterUsed(String paramName){
        return getParametersUsage().get(paramName) != null;
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

    public boolean hasExamples(){
        return CollectionUtils.isNotEmpty(examples);
    }

    public static Map<String, Integer> getParametersUsage(String text){
        Map<String, Integer> paramsUsage = new LinkedHashMap<>();
        if (StringUtils.isBlank(text)){
            return paramsUsage;
        }
        String[] paramNames = StringUtils.substringsBetween(text, "<", ">");
        if (paramNames != null) {
            for (String paramName : paramNames) {
                int count = StringUtils.countMatches(text, "<" + paramName + ">");
                paramsUsage.put(paramName, count);
            }
        }
        return paramsUsage;
    }

    public static Map<String, Integer> getParametersUsage(String... values){
        List<Map<String,Integer>> paramsData = new LinkedList<>();
        for(String value: values){
            paramsData.add(getParametersUsage(value));
        }
        return mergeParametersUsage(paramsData);
    }

    public static Map<String, Integer> mergeParametersUsage(Iterable<Map<String, Integer>> paramsData){
        Map<String, Integer> paramsUsage = new LinkedHashMap<>();
        for (Map<String, Integer> data: paramsData){
            data.forEach((key, val) ->
                    paramsUsage.put(key, paramsUsage.getOrDefault(key, 0) + val)
            );
        }
        return paramsUsage;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArmaScenarioOutline)) return false;
        if (!super.equals(o)) return false;
        ArmaScenarioOutline that = (ArmaScenarioOutline) o;
        return Objects.equals(examples, that.examples);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), examples);
    }
}
