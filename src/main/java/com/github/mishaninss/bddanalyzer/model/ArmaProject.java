package com.github.mishaninss.bddanalyzer.model;

import com.github.mishaninss.bddanalyzer.GherkinScanner;
import com.github.mishaninss.bddanalyzer.StepDefinitionsScanner;
import lombok.Data;
import lombok.NonNull;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by Sergey_Mishanin on 9/29/17.
 */
@Data
public class ArmaProject {
    private final String stepDefsRoot;
    private final String featuresRoot;
    private List<ArmaFeature> features;
    private List<ArmaStepDef> stepDefinitions;

    public ArmaProject(String stepDefsRoot, String featuresRoot) {
        this.stepDefsRoot = stepDefsRoot;
        this.featuresRoot = featuresRoot;
        features = new LinkedList<>();
        stepDefinitions = new LinkedList<>();
    }

    public void scan(){
        features.addAll(new GherkinScanner(featuresRoot).collectFeatures());
        stepDefinitions.addAll(new StepDefinitionsScanner(stepDefsRoot).collectStepDefinitions());
        mergeStepsAndStepDefs();
    }

    public List<ArmaScenario> getScenarios(){
        return getFeatures().stream()
                .flatMap(feature -> feature.getScenarios().stream())
                .collect(Collectors.toList());
    }

    public List<ArmaScenarioOutline> getScenarioOutlines(){
        return getScenarios().stream()
                .filter(scenario -> scenario instanceof ArmaScenarioOutline)
                .map(scenario -> (ArmaScenarioOutline) scenario)
                .collect(Collectors.toList());
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
        return getFeatures().stream()
                .filter(ArmaFeature::hasBackground)
                .map(ArmaFeature::getBackground)
                .collect(Collectors.toList());
    }

    public List<ArmaStep> getSteps(){
        return Stream.concat(
            getBackgrounds().stream().flatMap(background -> background.getSteps().stream()),
            getScenarios().stream().flatMap(scenario -> scenario.getSteps().stream())
        ).collect(Collectors.toList());
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
                .distinct()
                .collect(Collectors.toList());
    }

    public List<ArmaStepDef> getNotUsedStepDefinitions(){
        return ListUtils.subtract(stepDefinitions, getUsedStepDefinitions());
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
        table.addRow("Step Definitions", String.valueOf(stepDefinitions.size()));
        table.addRow("Not used Step Definitions", String.valueOf(getNotUsedStepDefinitions().size()));
        table.addRow("Tags", String.valueOf(getTags().size()));
        table.addRow("Tests", String.valueOf(countTests()));
        System.out.println(table);
        return table.toString();
    }

    private void mergeStepsAndStepDefs(){
        if (CollectionUtils.isEmpty(features) || CollectionUtils.isEmpty(stepDefinitions)){
            return;
        }

        features.forEach(feature ->
        {
            if (feature.hasBackground()){
                feature.getBackground().getSteps().forEach(step -> applyStepDef(stepDefinitions, step, step.getText()));
            }
            feature.getScenarios().forEach(scenario ->
            {
                if (scenario instanceof ArmaScenarioOutline){
                    ArmaScenarioOutline aso = (ArmaScenarioOutline) scenario;
                    if (CollectionUtils.isNotEmpty(aso.getExamples())) {
                        scenario.getSteps().forEach(step ->
                            applyStepDef(stepDefinitions, step, ArmaStep.applyExample(step, aso.getExamples().get(0), 0).getText()));
                    } else {
                        scenario.getSteps().forEach(step -> applyStepDef(stepDefinitions, step, step.getText()));
                    }
                } else {
                    scenario.getSteps().forEach(step -> applyStepDef(stepDefinitions, step, step.getText()));
                }
            });
        });
    }

    public Map<ArmaTag, int[]> getTagsUsage(){
        Map<ArmaTag, int[]> usage = new LinkedHashMap<>();
        features.forEach(feature -> {
            if (feature.hasTags()){
                feature.getTags().forEach(tag -> {
                    int[] count = usage.getOrDefault(tag, new int[]{0,0,0,0});
                    count[0] = count[0] + 1;
                    usage.put(tag, count);
                });
            }
        });

        getBackgrounds().forEach(background -> {
            if (background.hasTags()){
                background.getTags().forEach(tag -> {
                    int[] count = usage.getOrDefault(tag, new int[]{0,0,0,0});
                    count[1] = count[1] + 1;
                    usage.put(tag, count);
                });
            }
        });

        getScenarios().forEach(scenario -> {
            if (scenario.hasTags()) {
                scenario.getTags().forEach(tag -> {
                    int[] count = usage.getOrDefault(tag, new int[]{0,0,0,0});
                    count[2] = count[2] + 1;
                    usage.put(tag, count);
                });
            }

            if (scenario instanceof ArmaScenarioOutline) {
                List<ArmaExamples> examples = ((ArmaScenarioOutline) scenario).getExamples();
                if (CollectionUtils.isNotEmpty(examples)) {
                    examples.forEach(ex -> {
                        if (ex.hasTags()) {
                            ex.getTags().forEach(tag -> {
                                int[] count = usage.getOrDefault(tag, new int[]{0,0,0,0});
                                count[3] = count[3] + 1;
                                usage.put(tag, count);
                            });
                        }
                    });
                }
            }
        });
        return usage;
    }

    public void printTagUsage(){
        Map<ArmaTag, int[]> usage = getTagsUsage();
        ArmaDataTable table = new ArmaDataTable();
        table.addRow("Tag", "Features", "Backgrounds", "Scenarios", "Examples");
        usage.entrySet().stream()
                .sorted((o1, o2) -> {
                    Integer total1 = Arrays.stream(o1.getValue()).sum();
                    Integer total2 = Arrays.stream(o2.getValue()).sum();
                    return total2.compareTo(total1);
                })
                .forEach(entry ->
                    table.addRow(entry.getKey().getName(),
                            String.valueOf(entry.getValue()[0]),
                            String.valueOf(entry.getValue()[1]),
                            String.valueOf(entry.getValue()[2]),
                            String.valueOf(entry.getValue()[3]))
                );
        System.out.println(table);
    }

    public List<List<ArmaStepDef>> getStepDefScenarios(){
        List<List<ArmaStepDef>> scenarios = new LinkedList<>();
        getScenarios().forEach(scenario -> {

        });
        return scenarios;
    }

    public Map<ArmaStepDef, Integer> getStepDefsUsage(){
        Map<ArmaStepDef, Integer> usage = new LinkedHashMap<>();
        getSteps().forEach(step -> {
            if (step.isImplemented()){
                usage.put(step.getStepDef(), usage.getOrDefault(step.getStepDef(), 0) + 1);
            }
        });
        return usage;
    }

    public void printStepDefsUsage(){
        Map<ArmaStepDef, Integer> usage = getStepDefsUsage();
        ArmaDataTable table = new ArmaDataTable();
        usage.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .forEach(entry -> {
                    table.addRow(new ArmaTableRow(entry.getKey().getText(), entry.getValue().toString()));
                    table.addRow(new ArmaTableRow(entry.getKey().getLocation().toShortString(), ""));
                    table.addRow(new ArmaTableRow("", ""));
                });
        System.out.println(table);
    }

    public List<Map<ArmaScenario, String>> findDuplicatedScenarios(){
        List<Map<ArmaScenario, String>> allDuplicates = new LinkedList<>();
        List<ArmaScenario> scenarios = getScenarios();
        while (!scenarios.isEmpty()){
            Iterator<ArmaScenario> iterator = scenarios.iterator();
            ArmaScenario scenario = iterator.next();
            iterator.remove();

            Map<ArmaScenario, String> duplicates = new LinkedHashMap<>();
            duplicates.put(scenario, "ORIGIN");

            while (iterator.hasNext()){
                ArmaScenario anotherScenario = iterator.next();
                if (scenario.isFullyEqualTo(anotherScenario)){
                    duplicates.put(anotherScenario, "FULL");
                    iterator.remove();
                } else if (scenario.isEqualToIgnoreParameters(anotherScenario)){
                    duplicates.put(anotherScenario, "IGNORE PARAMETERS");
                    iterator.remove();
                }
            }
            if (duplicates.size()>1) {
                allDuplicates.add(duplicates);
            }
        }
        return allDuplicates;
    }

    public void printDuplicatedScenarios(){
        List<Map<ArmaScenario, String>> allDuplicates = findDuplicatedScenarios();
        ArmaDataTable table = new ArmaDataTable();
        table.addRow("SCENARIO", "LOCATION", "EQUALITY");
        table.addRow("", "", "");
        allDuplicates.forEach(map -> {
            map.forEach((key, value) -> {
                table.addRow(key.getName(), key.getLocation().toShortString(), value);
            });
            table.addRow("", "", "");
        });
        System.out.println(table);
    }

    public List<ArmaFeature> applyTagFilters(String... tagFilters){
        return getFeatures().stream()
            .map(feature -> feature.applyTagFilters(tagFilters))
            .filter(ArmaFeature::hasScenarios)
            .collect(Collectors.toList());
    }

    private static void applyStepDef(List<ArmaStepDef> stepDefs, ArmaStep step, String text){
        for (ArmaStepDef stepDef: stepDefs){
            Pattern pattern = Pattern.compile(stepDef.getText());
            Matcher matcher = pattern.matcher(text);
            if (matcher.matches()){
                step.setStepDef(stepDef);
                return;
            }
        }
    }

    public List<ArmaScenario> getSequenceUsage(@NonNull List<ArmaStepDef> sequence){
        List<ArmaScenario> foundScenarios = new LinkedList<>();
        if (CollectionUtils.isEmpty(sequence)){
            return foundScenarios;
        }

        List<ArmaScenario> scenarios = getScenarios();
        for (ArmaScenario scenario: scenarios){
            List<ArmaStepDef> stepDefs = scenario.mapStepsToStepDefinitions();
            if (sequence.size() >= stepDefs.size()){
                if (sequence.size() == stepDefs.size() && sequence.equals(stepDefs)) {
                    foundScenarios.add(scenario);
                }
                continue;
            }

            for (int i = 0; i < stepDefs.size() - sequence.size() + 1; i++) {
                ArmaStepDef stepDef = stepDefs.get(i);
                if (stepDef.equals(sequence.get(0))) {
                    boolean found = true;
                    for (int j = 1; j < sequence.size(); j++) {
                        stepDef = stepDefs.get(i + j);
                        if (!stepDef.equals(sequence.get(j))) {
                            found = false;
                            break;
                        }
                    }
                    if (found){
                        foundScenarios.add(scenario);
                    }
                }
            }
        }
        return foundScenarios;
    }

    public int countSequenceUsage(@NonNull List<ArmaStepDef> sequence){
        int usage = 0;
        if (CollectionUtils.isEmpty(sequence)){
            return usage;
        }

        List<ArmaScenario> scenarios = getScenarios();
        for (ArmaScenario scenario: scenarios){
            List<ArmaStepDef> stepDefs = scenario.mapStepsToStepDefinitions();
            if (sequence.size() >= stepDefs.size()){
                if (sequence.size() == stepDefs.size() && sequence.equals(stepDefs)) {
                    usage++;
                }
                continue;
            }

            for (int i = 0; i < stepDefs.size() - sequence.size() + 1; i++) {
                ArmaStepDef stepDef = stepDefs.get(i);
                if (stepDef.equals(sequence.get(0))) {
                    usage++;
                    for (int j = 1; j < sequence.size(); j++) {
                        stepDef = stepDefs.get(i + j);
                        if (!stepDef.equals(sequence.get(j))) {
                            usage--;
                            break;
                        }
                    }
                }
            }
        }
        return usage;
    }

    public Map<ArmaScenario, Integer> findStepDefUsage(ArmaStepDef stepDef){
        return getScenarios().stream()
                .collect(Collectors.toMap(scenario -> scenario, scenario -> scenario.getStepDefUsage(stepDef).size()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() > 0)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void findRepetedSequences(List<ArmaFeature> features){
        List<ArmaStepDef> stepDefs = new LinkedList<>();
        features.forEach(feature -> {
            feature.getScenarios().forEach(scenario -> {
                scenario.getSteps().forEach(step -> {
                    stepDefs.add(step.getStepDef());
                });
                stepDefs.add(null);
            });
        });

        Set<List<ArmaStepDef>> sequences = detectSequences(stepDefs);
        Map<List<ArmaStepDef>, Integer> usage = new LinkedHashMap<>();
        sequences.forEach(sequence -> usage.put(sequence, countSequenceUsage(sequence)));

        usage.entrySet().stream()
                .sorted(Comparator.comparing(Map.Entry::getValue))
                .forEach(entry -> {
                    System.out.println();
                    System.out.println("used " + entry.getValue() + " times");
                    entry.getKey().forEach(stepDef-> System.out.println(stepDef.getText()));
                });
    }

    public Set<List<ArmaStepDef>> findRepetedSequences(ArmaFeature feature){
        List<ArmaStepDef> stepDefs = new LinkedList<>();
        feature.getScenarios().forEach(scenario -> {
            scenario.getSteps().forEach(step -> stepDefs.add(step.getStepDef()));
            stepDefs.add(null);
        });

        return detectSequences(stepDefs);
    }

    public Set<List<ArmaStepDef>> detectSequences(List<ArmaStepDef> stepDefs){
        byte[][] matrix = new byte[stepDefs.size()][stepDefs.size()];

        for (int i=0; i< stepDefs.size(); i++){
            for (int j=i; j<stepDefs.size(); j++){
                ArmaStepDef yStepDef = stepDefs.get(i);
                if (yStepDef != null) {
                    ArmaStepDef xStepDef = stepDefs.get(j);
                    if (xStepDef != null && xStepDef.equals(yStepDef)){
                        matrix[i][j] = 1;
                    }
                }
            }
        }

        Set<List<ArmaStepDef>> sequences = new LinkedHashSet<>();
        for (int i=1; i<stepDefs.size(); i++){
            List<ArmaStepDef> sequence = new ArrayList<>();
            for (int j=0; j<stepDefs.size()-i; j++){
                if (matrix[j][j+i] == 1){
                    sequence.add(stepDefs.get(j));
                } else {
                    if (sequence.size() > 1) {
                        sequences.add(sequence);
                    }
                    sequence = new ArrayList<>();
                }
            }
            if (sequence.size() > 1){
                sequences.add(sequence);
            }
        }

        return sequences;
    }
}
