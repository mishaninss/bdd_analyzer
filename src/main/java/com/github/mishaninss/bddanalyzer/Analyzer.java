package com.github.mishaninss.bddanalyzer;


import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.BodyDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.comments.JavadocComment;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.SingleMemberAnnotationExpr;
import com.github.mishaninss.bddanalyzer.model.*;
import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.ast.GherkinDocument;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Analyzer {

    private static final String STEP_DEF_ROOT = "";
    private static final String FEATURES_ROOT = "";
    private static final Set<String> STEP_ANNOTATIONS = new HashSet<>();

    private List<ArmaStepDef> stepDefs = new ArrayList<>();
    private List<ArmaFeature> features = new ArrayList<>();

    public void collectStepDefs(String stepDefRoot){
        File stepDefDir = new File(stepDefRoot);
        Collection<File> stepDefFiles = FileUtils.listFiles(stepDefDir, new String[] {"java"}, true);
        stepDefFiles.forEach(file -> stepDefs.addAll(scanStepDefFile(file)));
    }

    public void collectFeatures(String featuresRoot){
        File stepDefDir = new File(featuresRoot);
        Collection<File> featureFiles = FileUtils.listFiles(stepDefDir, new String[] {"feature"}, true);

        Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        featureFiles.forEach(featureFile ->
        {
            GherkinDocument gherkinDocument = null;
            try {
                System.out.println();
                System.out.println(featureFile.getCanonicalPath());
                System.out.println();
                gherkinDocument = parser.parse(FileUtils.readFileToString(featureFile));
                ArmaFeature feature = new ArmaFeature(gherkinDocument.getFeature());
                feature.setLocation(featureFile);
                features.add(feature);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    public void mergeStepsAndStepDefs(){
        features.forEach(feature ->
        {
            if (feature.getBackground() != null){
                feature.getBackground().getSteps().forEach(step -> applyStepDef(getStepDefinitions(), step, step.getText()));
            }
            feature.getScenarios().forEach(scenario ->
            {
                if (scenario instanceof ArmaScenarioOutline){
                    ArmaScenarioOutline aso = (ArmaScenarioOutline) scenario;
                    if (CollectionUtils.isNotEmpty(aso.getExamples())) {
                        scenario.getSteps().forEach(step -> {
                            applyStepDef(getStepDefinitions(), step, ArmaStep.applyExample(step, aso.getExamples().get(0), 0).getText());
                        });
                    } else {
                        scenario.getSteps().forEach(step -> applyStepDef(getStepDefinitions(), step, step.getText()));
                    }
                } else {
                    scenario.getSteps().forEach(step -> applyStepDef(getStepDefinitions(), step, step.getText()));
                }
            });
        });
    }

    public Set<ArmaTag> getTags(){
        Set<ArmaTag> allTags = new HashSet<>();
        List<ArmaFeature> features = getFeatures();
        features.forEach(feature -> {
            List<ArmaTag> tags = feature.getTags();
            if (CollectionUtils.isNotEmpty(tags)){
                allTags.addAll(tags);
            }
        });

        List<ArmaBackground> backgrounds = getBackgrounds();
        backgrounds.forEach(background -> {
            List<ArmaTag> tags = background.getTags();
            if (CollectionUtils.isNotEmpty(tags)){
                allTags.addAll(tags);
            }
        });

        List<ArmaScenario> scenarios = getScenarios();
        scenarios.forEach(scenario -> {
            List<ArmaTag> tags = scenario.getTags();
            if (CollectionUtils.isNotEmpty(tags)){
                allTags.addAll(tags);
            }
            if (scenario instanceof ArmaScenarioOutline){
                List<ArmaExamples> examples = ((ArmaScenarioOutline) scenario).getExamples();
                if (CollectionUtils.isNotEmpty(examples)){
                    examples.forEach(example -> {
                        List<ArmaTag> etags = example.getTags();
                        if (CollectionUtils.isNotEmpty(etags)){
                            allTags.addAll(etags);
                        }
                    });
                }
            }
        });

        System.out.println();
        return allTags;
    }

    public List<ArmaStepDef> getStepDefinitions(){
        return stepDefs;
    }

    public List<ArmaFeature> getFeatures(){
        return features;
    }

    public List<ArmaScenario> getScenarios(){
        List<ArmaScenario> scenarios = new ArrayList<>();
        getFeatures().stream().forEach(feature -> scenarios.addAll(feature.getScenarios()));
        return scenarios;
    }

    public List<ArmaBackground> getBackgrounds(){
        List<ArmaBackground> backgrouds = new ArrayList<>();
        getFeatures().stream().forEach(feature ->
        {
            if (feature.getBackground() != null){
                backgrouds.add(feature.getBackground());
            }
        });
        return backgrouds;
    }

    public List<ArmaStep> getSteps(){
        List<ArmaStep> steps = new ArrayList<>();
        getBackgrounds().forEach(background -> steps.addAll(background.getSteps()));
        getScenarios().forEach(scenario -> steps.addAll(scenario.getSteps()));
        return steps;
    }

    public List<ArmaStepDef> getNotUsedStepDefinitions(){
        List<ArmaStepDef> usedStepDefs =
            getSteps().stream().filter(ArmaStep::isImplemented).map(ArmaStep::getStepDef).collect(Collectors.toList());
        return new ArrayList<>(CollectionUtils.subtract(getStepDefinitions(), usedStepDefs));
    }

    public String getStatistics(){
        ArmaDataTable table = new ArmaDataTable();
        table.addRow("Features", String.valueOf(getFeatures().size()));
        table.addRow("Scenarios", String.valueOf(getScenarios().size()));
        table.addRow("Scenario Outlines", String.valueOf(getScenarios().stream().filter(scenario -> scenario instanceof ArmaScenarioOutline).count()));
        table.addRow("Backgrounds", String.valueOf(getBackgrounds().size()));
        table.addRow("Steps", String.valueOf(getSteps().size()));
        table.addRow("Implemented steps", String.valueOf(getSteps().stream().filter(ArmaStep::isImplemented).count()));
        table.addRow("Not implemented steps", String.valueOf(getSteps().stream().filter(step -> !step.isImplemented()).count()));
        table.addRow("Step Definitions", String.valueOf(getStepDefinitions().size()));
        table.addRow("Not used Step Definitions", String.valueOf(getNotUsedStepDefinitions().size()));
        table.addRow("Tags", String.valueOf(getTags().size()));

        //getNotUsedStepDefinitions().forEach(stepDef -> System.out.println(stepDef));

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
        table.addRow("Tests", String.valueOf(testsCount));


        return table.toString();
    }

    static {
        STEP_ANNOTATIONS.add("given");
        STEP_ANNOTATIONS.add("when");
        STEP_ANNOTATIONS.add("then");
        STEP_ANNOTATIONS.add("and");
        STEP_ANNOTATIONS.add("but");
    }

    private static List<ArmaStepDef> scanStepDefFile(File file){
        List<ArmaStepDef> steps = new ArrayList<>();

        CompilationUnit cu;
        try (FileInputStream in = new FileInputStream(file)){
            cu = JavaParser.parse(in);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } catch (ParseException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

        List<TypeDeclaration> types = cu.getTypes();
        for (TypeDeclaration type : types) {
            List<BodyDeclaration> members = type.getMembers();
            for (BodyDeclaration member : members) {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
                    ArmaStepDef step = createStep(method);
                    if (step != null){
                        ArmaStepDefLocation location = new ArmaStepDefLocation();
                        String path = file.getPath();
                        /*if (file.exists()){
                            try {
                                path = file.getCanonicalPath();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }*/
                        location.setFile(path);
                        location.setMethodName(method.getName());
                        location.setDeclaration(method.getDeclarationAsString());
                        location.setLine(method.getBegin().line);
                        step.setLocation(location);
                        steps.add(step);
                    }
                }
            }
        }
        return steps;
    }

    private static ArmaStepDef createStep(MethodDeclaration method){
        ArmaStepDef step = new ArmaStepDef();
        AnnotationExpr annotation = getStepAnnotation(method);
        if (annotation == null){
            return null;
        }
        if (annotation instanceof SingleMemberAnnotationExpr) {
            String stepText = ((SingleMemberAnnotationExpr) annotation).getMemberValue().toStringWithoutComments();
            step.setText(StringUtils.strip(stepText.trim(), "\"").replaceAll("\\\\+", "\\\\"));
        }

        JavadocComment javaDoc = method.getJavaDoc();
        if (javaDoc != null) {
            step.setDescription(javaDoc.toString());
        }
        //System.out.println(method.getParameters());
        step.setImplemented(true);
        return step;
    }

    private static AnnotationExpr getStepAnnotation(MethodDeclaration method){
        List<AnnotationExpr> annotations = method.getAnnotations();
        for (AnnotationExpr annotation: annotations){
            if (STEP_ANNOTATIONS.contains(annotation.getName().getName().toLowerCase())){
                return annotation;
            }
        }
        return null;
    }

    public static void main(String[] args) throws IOException, ParseException {
        Analyzer test = new Analyzer();
        test.collectStepDefs(STEP_DEF_ROOT);
        List<ArmaStepDef> steps = test.getStepDefinitions();
        //steps.forEach(System.out::println);

        test.collectFeatures(FEATURES_ROOT);
        test.mergeStepsAndStepDefs();

        test.getFeatures().forEach(feature -> {System.out.println(); System.out.println(feature);});

        System.out.println(test.getStatistics());

        /*List<List<ArmaScenario>> allDuplicates = test.findFullyEqialScenarios();
        allDuplicates.forEach(duplicates -> {
            System.out.println();
            duplicates.forEach(scenario -> System.out.println(scenario.getName() + " " + scenario.getLocation()));
        });*/
        //test.findDuplicatedScenarios();
        /*test.getScenarios().stream()
                .filter(scenario -> scenario instanceof ArmaScenarioOutline)
                .forEach(scenarioOutline -> ((ArmaScenarioOutline)scenarioOutline).optimizeExamples());*/
    }

    List<List<ArmaScenario>> findFullyEqialScenarios(){
        List<ArmaScenario> scenarios = getScenarios();
        List<List<ArmaScenario>> allDuplicates = new LinkedList<>();
        while (!scenarios.isEmpty()){
            List<ArmaScenario> duplicates = new LinkedList<>();
            Iterator<ArmaScenario> iterator = scenarios.iterator();

            ArmaScenario scenario = iterator.next();
            duplicates.add(scenario);
            iterator.remove();
            while (iterator.hasNext()){
                ArmaScenario anotherScenario = iterator.next();
                if (scenario.isFullyEqualTo(anotherScenario)){
                    duplicates.add(anotherScenario);
                    iterator.remove();
                }
            }

            if (duplicates.size() > 1){
                allDuplicates.add(duplicates);
            }
        }
        return allDuplicates;
    }

    void findDuplicatedScenarios(){
        List<ArmaScenario> scenarios = getScenarios();
        while (!scenarios.isEmpty()){
            ArmaDataTable duplicates = new ArmaDataTable();
            Iterator<ArmaScenario> iterator = scenarios.iterator();

            ArmaScenario scenario = iterator.next();
            duplicates.addRow(scenario.getName(), scenario.getLocation().toString(), "");
            iterator.remove();
            while (iterator.hasNext()){
                ArmaScenario anotherScenario = iterator.next();
                if (scenario.isFullyEqualTo(anotherScenario)){
                    duplicates.addRow(anotherScenario.getName(), anotherScenario.getLocation().toString(), "FULL");
                    iterator.remove();
                } else if (scenario.isEqualToIgnoreParameters(anotherScenario)){
                    duplicates.addRow(anotherScenario.getName(), anotherScenario.getLocation().toString(), "IGNORE PARAMETERS");
                    iterator.remove();
                }
            }

            if (!duplicates.isEmpty() && duplicates.getRows().size() > 1){
                System.out.println();
                System.out.println(duplicates.toString(" "));
            }
        }
    }

    static void applyStepDef(List<ArmaStepDef> stepDefs, ArmaStep step, String text){
        for (ArmaStepDef stepDef: stepDefs){
                Pattern pattern = Pattern.compile(stepDef.getText());
                Matcher matcher = pattern.matcher(text);
            if (matcher.matches()){
                step.setStepDef(stepDef);
                int paramsCount = matcher.groupCount();
                if (paramsCount > 0){
                    //System.out.println(text);
                    //System.out.println(paramsCount);
                    for (int i = 1; i <= paramsCount; i++){
                        //System.out.println(matcher.group(i));
                    }
                }

                return;
            }
        }
    }


}
