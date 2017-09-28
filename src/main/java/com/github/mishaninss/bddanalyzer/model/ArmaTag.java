package com.github.mishaninss.bddanalyzer.model;

import gherkin.ast.Tag;
import lombok.Data;

import java.util.Objects;

/**
 * Created by Sergey_Mishanin on 11/16/16.
 */
@Data
public class ArmaTag {
    private String name;

    public ArmaTag(){

    }

    public ArmaTag(ArmaTag tag){
        if (tag == null){
            return;
        }
        name = tag.getName();
    }

    public ArmaTag(String name){
        this.name = name.trim();
        if (!this.name.startsWith("@")){
            this.name = "@" + this.name;
        }
    }

    public ArmaTag(Tag gherkinTag){
        name = gherkinTag.getName();
    }

    @Override
    public String toString(){
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ArmaTag)) return false;
        ArmaTag armaTag = (ArmaTag) o;
        return Objects.equals(name, armaTag.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
