package com.example.scraperremus;

import java.util.List;

public class OptionGroup {
    private String name;
    private List<OptionValue> values;

    public OptionGroup(String name, List<OptionValue> values) {
        this.name = name;
        this.values = values;
    }

    public String getName() {
        return name;
    }

    public List<OptionValue> getValues() {
        return values;
    }
}
