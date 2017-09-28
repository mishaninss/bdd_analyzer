package com.github.mishaninss.bddanalyzer.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
@Data
public class ArmaNode {
    protected String keyword;
    protected String name;
    protected String description;

    public void setKeyword(String keyword){
        this.keyword = StringUtils.capitalize(StringUtils.lowerCase(StringUtils.trim(keyword)));
    }

    public void setName(String name){
        //this.name = StringUtils.capitalize(StringUtils.lowerCase(StringUtils.trim(name)));
        this.name = StringUtils.normalizeSpace(StringUtils.trim(name));
    }

    public void setDescription(String description){
        this.description = StringUtils.capitalize(StringUtils.lowerCase(StringUtils.trim(description)));
    }
}
