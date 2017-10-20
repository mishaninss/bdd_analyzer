package com.github.mishaninss.bddanalyzer.model;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

/**
 * Created by Sergey_Mishanin on 10/20/17.
 */
public interface HasTags {
    Set<ArmaTag> getTags();

    default boolean hasTag(ArmaTag tag){
        return getTags().contains(tag);
    }

    default boolean hasTag(String tag){
        return hasTag(new ArmaTag(tag));
    }

    default boolean hasAnyTag(ArmaTag... tags){
        for(ArmaTag tag: tags){
            if (hasTag(tag)){
                return true;
            }
        }
        return false;
    }

    default boolean hasAnyTag(String... tags){
        return hasAnyTag(Arrays.stream(tags).map(ArmaTag::new).toArray(ArmaTag[]::new));
    }

    default boolean hasAllTags(ArmaTag... tags){
        for(ArmaTag tag: tags){
            if (!hasTag(tag)){
                return false;
            }
        }
        return true;
    }

    default boolean hasAllTags(String... tags){
        return hasAllTags(Arrays.stream(tags).map(ArmaTag::new).toArray(ArmaTag[]::new));
    }

    default void addTag(ArmaTag tag){
        getTags().add(tag);
    }

    default void addTag(String tagName){
        addTag(new ArmaTag(tagName));
    }

    default void addTags(Iterable<ArmaTag> newTags){
        if (newTags == null){
            return;
        }
        newTags.forEach(this::addTag);
    }

    default void addTags(List<String> newTags){
        if (newTags == null){
            return;
        }
        newTags.forEach(this::addTag);
    }

    default void addTags(String... newTags){
        for (String newTag : newTags){
            addTag(newTag);
        }
    }

    default void setTags(Iterable<ArmaTag> newTags){
        Set<ArmaTag> tags = getTags();
        tags.clear();
        newTags.forEach(this::addTag);
    }

    default boolean acceptTagFilters(String... tagFilters){
        for (String tagFilter :tagFilters){
            if (!acceptTagFilter(tagFilter)){
                return false;
            }
        }
        return true;
    }

    default boolean acceptTagFilter(String tagFilter){
        if (StringUtils.isBlank(tagFilter)){
            return true;
        }
        String[] tags = StringUtils.stripAll(tagFilter.split(","));
        boolean accepted = false;
        for (String tag: tags){
            if (tag.startsWith("~")){
                accepted = accepted || !hasTag(tag);
            } else {
                accepted = accepted || hasTag(tag);
            }
        }
        return accepted;
    }

    default void removeTag(ArmaTag tag){
        getTags().remove(tag);
    }

    default void removeTag(String tag){
        removeTag(new ArmaTag(tag));
    }

    default void removeTags(Iterable<ArmaTag> tags){
        for (ArmaTag tag: tags){
            removeTag(tag);
        }
    }

    default void removeTags(ArmaTag... tags){
        for (ArmaTag tag: tags){
            removeTag(tag);
        }
    }

    default void removeTags(String... tags){
        for (String tag: tags){
            removeTag(tag);
        }
    }

    default boolean hasTags(){
        return CollectionUtils.isNotEmpty(getTags());
    }
}
