package com.github.mishaninss.bddanalyzer;

import com.github.mishaninss.bddanalyzer.model.ArmaFeature;
import gherkin.AstBuilder;
import gherkin.Parser;
import gherkin.ast.GherkinDocument;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * Provides methods to parse feature files and build a data model
 * Created by Sergey_Mishanin on 9/29/17.
 */
public class GherkinScanner {
    private static final Logger LOG = LoggerFactory.getLogger(GherkinScanner.class);
    private final String featuresRoot;
    private static final String FEATURE_FILE_EXTENSION = "feature";

    public GherkinScanner(String featuresRoot){
        this.featuresRoot = featuresRoot;
    }

    public List<ArmaFeature> collectFeatures(){
        if (StringUtils.isBlank(featuresRoot)){
            throw new IllegalArgumentException("feature files root directory is not defined");
        }
        File featuresRootDir = new File(featuresRoot);
        if (!featuresRootDir.exists()){
            throw new IllegalArgumentException("feature files root directory [" + featuresRoot + "] doesn't exist");
        }
        Collection<File> featureFiles = FileUtils.listFiles(featuresRootDir, new String[] {FEATURE_FILE_EXTENSION}, true);
        if (CollectionUtils.isEmpty(featureFiles)){
            throw new IllegalArgumentException("feature files root directory [" + featuresRoot + "] doesn't contain files with [" + FEATURE_FILE_EXTENSION + "] extension");
        }

        List<ArmaFeature> features = new LinkedList<>();
        Parser<GherkinDocument> parser = new Parser<>(new AstBuilder());
        featureFiles.forEach(featureFile ->
        {
            try {
                LOG.info("Parsing feature file {}", featureFile);
                GherkinDocument gherkinDocument = parser.parse(FileUtils.readFileToString(featureFile));
                ArmaFeature feature = new ArmaFeature(gherkinDocument.getFeature());
                feature.setLocation(featureFile);
                features.add(feature);
            } catch (Exception e) {
                LOG.error("Couldn't parse [" + featureFile + "] feature file", e);
            }
        });
        return features;
    }
}
