package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.DocString;
import lombok.Data;

/**
 * Objective representation of a Gherkin Doc String
 * Created by Sergey_Mishanin on 11/17/16.
 * @see DocString
 */
@Data
public class ArmaDocString {
    private String contentType;
    private String content;

    public ArmaDocString(){

    }

    public ArmaDocString(ArmaDocString docString){
        if (docString == null){
            return;
        }
        contentType = docString.getContentType();
        content = docString.getContent();
    }

    public ArmaDocString(DocString gherkinDocString){
        if (gherkinDocString == null){
            return;
        }
        contentType = gherkinDocString.getContentType();
        content = gherkinDocString.getContent();
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("\"\"\"");
        if (contentType != null){
            sb.append(contentType);
        }
        sb.append("\n").append(content).append("\n").append("\"\"\"");
        return sb.toString();
    }
}
