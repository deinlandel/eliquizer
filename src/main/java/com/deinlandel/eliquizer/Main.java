package com.deinlandel.eliquizer;

import com.deinlandel.eliquizer.entity.Recipe;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Deinlandel
 */
public class Main {
    public static void main(String[] args) throws IOException, NotAuthorizedException, WrongCredentialsException {

        ElrClient c = new ElrClient();
        c.login("login", "password");
        List<Recipe> whatCanIMake = c.whatCanIMake(4);
        //System.out.println(whatCanIMake);

        List<Recipe> filtered = whatCanIMake.stream().filter(input ->
                !input.containsFlavor("strawberry") && input.getFlavors().size() > 2 && input.getFlavors().size() < 7)
                .collect(Collectors.toList());

        System.out.println(">>>>>>>>>" + filtered);
    }

}
