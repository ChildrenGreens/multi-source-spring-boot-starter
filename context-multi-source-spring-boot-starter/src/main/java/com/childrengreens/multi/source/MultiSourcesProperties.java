package com.childrengreens.multi.source;

import java.util.Map;

public class MultiSourcesProperties<T> {

    private Map<String, T> sources;

    private String primaryKey;

    public Map<String,T> getSources() {
        return sources;
    }

    public void setSources(Map<String,T> sources) {
        this.sources = sources;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }
}
