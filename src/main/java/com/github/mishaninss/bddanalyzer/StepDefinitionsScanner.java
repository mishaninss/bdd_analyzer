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
import com.github.mishaninss.bddanalyzer.model.ArmaStepDef;
import com.github.mishaninss.bddanalyzer.model.ArmaStepDefLocation;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

/**
 * Created by Sergey_Mishanin on 9/29/17.
 */
public class StepDefinitionsScanner {
    private static final Logger LOG = LoggerFactory.getLogger(StepDefinitionsScanner.class);
    private final String stepDefsRoot;
    private static final String STEP_DEF_FILE_EXTENSION = "java";
    private static final Set<String> STEP_ANNOTATIONS = new HashSet<>();

    static {
        STEP_ANNOTATIONS.add("given");
        STEP_ANNOTATIONS.add("when");
        STEP_ANNOTATIONS.add("then");
        STEP_ANNOTATIONS.add("and");
        STEP_ANNOTATIONS.add("but");
    }

    public StepDefinitionsScanner(String stepDefsRoot){
        this.stepDefsRoot = stepDefsRoot;
    }

    public List<ArmaStepDef> collectStepDefinitions(){
        if (StringUtils.isBlank(stepDefsRoot)){
            throw new IllegalArgumentException("step definition files root directory is not defined");
        }
        File stepDefDir = new File(stepDefsRoot);
        if (!stepDefDir.exists()){
            throw new IllegalArgumentException("step definition files root directory [" + stepDefsRoot + "] doesn't exist");
        }
        Collection<File> stepDefFiles = FileUtils.listFiles(stepDefDir, new String[] {STEP_DEF_FILE_EXTENSION}, true);
        if (CollectionUtils.isEmpty(stepDefFiles)){
            throw new IllegalArgumentException("step definition files root directory [" + stepDefsRoot + "] doesn't contain files with [" + STEP_DEF_FILE_EXTENSION + "] extension");
        }
        List<ArmaStepDef> stepDefs = new LinkedList<>();

        stepDefFiles.forEach(file -> {
                LOG.info("Parsing step definition file {}", file);
                try {
                    stepDefs.addAll(scanStepDefFile(file));
                } catch (Exception ex){
                    LOG.error("Couldn't parse [" + file + "] step definition file", ex);
                }
            });
        return stepDefs;
    }

    private static List<ArmaStepDef> scanStepDefFile(File file) throws IOException, ParseException {
        List<ArmaStepDef> steps = new ArrayList<>();

        CompilationUnit cu;
        try (FileInputStream in = new FileInputStream(file)){
            cu = JavaParser.parse(in);
        }

        List<TypeDeclaration> types = cu.getTypes();
        types.forEach(type -> {
            List<BodyDeclaration> members = type.getMembers();
            members.forEach(member -> {
                if (member instanceof MethodDeclaration) {
                    MethodDeclaration method = (MethodDeclaration) member;
                    ArmaStepDef step = createStepDef(method);
                    if (step != null){
                        ArmaStepDefLocation location = new ArmaStepDefLocation();
                        String path = file.getPath();
                        location.setFile(path);
                        location.setMethodName(method.getName());
                        location.setDeclaration(method.getDeclarationAsString());
                        location.setLine(method.getBegin().line);
                        step.setLocation(location);
                        steps.add(step);
                    }
                }
            });
        });
        return steps;
    }

    private static ArmaStepDef createStepDef(MethodDeclaration method){
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
}
