package com.deinlandel.eliquizer.entity;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.util.List;

/**
 * @author Deinlandel
 */
public class Recipe {
    private final String id;
    private final String name;
    private final double rating;

    private List<RecipeFlavor> flavors;

    public Recipe(String id, String name, double rating) {
        this.id = id;
        this.name = name;
        this.rating = rating;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public double getRating() {
        return rating;
    }

    public List<RecipeFlavor> getFlavors() {
        return flavors;
    }

    public void setFlavors(List<RecipeFlavor> flavors) {
        this.flavors = flavors;
    }

    public boolean containsFlavor(String flavor) {
        if (flavors == null) return false;
        final String lowerCase = flavor.toLowerCase();
        return  !Collections2.filter(flavors, new Predicate<RecipeFlavor>() {
            @Override
            public boolean apply(RecipeFlavor input) {
                return input.name.toLowerCase().contains(lowerCase);
            }
        }).isEmpty();
    }

    @Override
    public String toString() {
        return "Recipe{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", rating=" + rating +
                ", flavors=\n" + flavors +
                "\n}";
    }
}