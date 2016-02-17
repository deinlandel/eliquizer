package com.deinlandel.eliquizer.entity;

/**
 * @author: Deinlandel
 */
public class RecipeFlavor {
    public String name;
    public double perc;

    @Override
    public String toString() {
        return "RecipeFlavor{" +
                "name='" + name + '\'' +
                ", perc=" + perc +
                '}';
    }
}
