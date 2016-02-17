package com.deinlandel.eliquizer;

import com.deinlandel.eliquizer.entity.Recipe;
import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * @author Deinlandel
 */
public class Main {
    public static void main(String[] args) throws IOException, NotAuthorizedException, WrongCredentialsException {

        ElrClient c = new ElrClient();
        c.login("login", "pass");
        List<Recipe> whatCanIMake = c.whatCanIMake(4);
        System.out.println(whatCanIMake);

        Collection<Recipe> filtered = Collections2.filter(whatCanIMake, new Predicate<Recipe>() {
            @Override
            public boolean apply(Recipe input) {

                return input.containsFlavor("sour");
            }
        });

        System.out.println(">>>>>>>>>" + filtered);
    }

}
