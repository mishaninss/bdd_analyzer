package com.github.mishaninss.bddanalyzer.model;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

/**
 * Objective representation of a Gherkin Node
 * Created by Sergey_Mishanin on 11/16/16.
 * @see gherkin.ast.Node
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArmaNode)) return false;
        ArmaNode armaNode = (ArmaNode) o;
        return Objects.equals(keyword, armaNode.keyword) &&
                Objects.equals(name, armaNode.name) &&
                Objects.equals(description, armaNode.description);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), keyword, name, description);
    }
}
