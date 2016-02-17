package com.deinlandel.eliquizer;

import java.io.IOException;

/**
 * @author Deinlandel
 */
public class Main {
    public static void main(String[] args) throws IOException, NotAuthorizedException, WrongCredentialsException {

        ElrClient c = new ElrClient();
        c.login("login", "pass");
        System.out.println(c.whatCanIMake(1));

    }

}
