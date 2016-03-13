package com.deinlandel.eliquizer.entity;

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

    @SuppressWarnings("UnusedDeclaration")
    public String getId() {
        return id;
    }

    @SuppressWarnings("UnusedDeclaration")
    public String getName() {
        return name;
    }

    @SuppressWarnings("UnusedDeclaration")
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
        return flavors.stream().anyMatch(input -> input.name.toLowerCase().contains(lowerCase));
    }

    @Override
    public String toString() {

        StringBuilder b = new StringBuilder("RECIPE id='" + id + '\'' + ", name='" + name + '\'' + ", rating="
                + rating + ", flavors=\n");

        for (RecipeFlavor flavor : flavors) {
            b.append("   - ").append(flavor.name).append(" ").append(flavor.perc).append("%").append("\n");
        }
        b.append("\n");

        return b.toString();
    }
}
