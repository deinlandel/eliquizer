package com.deinlandel.eliquizer;

import com.deinlandel.eliquizer.entity.Recipe;
import com.deinlandel.eliquizer.entity.RecipeFlavor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.io.IOException;
import java.util.*;

/**
 * @author Deinlandel
 */
public class ElrClient {
    private static final String BASE_URL = "http://e-liquid-recipes.com";

    private final Map<String, String> cookies = new HashMap<String, String>();
    private final RestApi restApi;

    public ElrClient() {
        restApi = setupRestApi();
    }

    public void login(String login, String password) throws IOException, WrongCredentialsException {
        Connection c = Jsoup.connect(BASE_URL + "/login").timeout(30000)
                .data("email", login)
                .data("passwd", password);
        Connection.Response response = c.method(Connection.Method.POST).execute();

        if(response.parse().getElementById("passwd") != null) throw new WrongCredentialsException();

        cookies.putAll(response.cookies());
    }

    public List<Recipe> whatCanIMake(int page) throws IOException, NotAuthorizedException {
        List<Recipe> result = new ArrayList<Recipe>();

        String url = BASE_URL + "/whatcanimake?exclsingle=1&sort=score&direction=desc&page=" + page;
        Connection.Response response = getResponseWithCookies(url);
        Document document = response.parse();
        Elements recipeList = document.getElementsByClass("recipelist");

        Elements elements = recipeList.select("tr");
        if (elements.isEmpty()) {
            if (!document.getElementsByAttributeValue("action", BASE_URL + "/login").isEmpty()) {
                throw new NotAuthorizedException();
            }
            return result;
        }
        ListIterator<Element> it = elements.listIterator(1);
        while (it.hasNext()) {
            Element row = it.next();
            if (!row.select("td[colspan=4]").isEmpty()) continue;  //skip ads
            Elements nameElement = row.select("td:eq(0)");
            String id = nameElement.select(".rinfo").attr("data-rid");
            String name = nameElement.select(".mlink").text();
            double rating = Double.parseDouble(row.select("td:eq(3)").select(".star").attr("data-score"));
            result.add(new Recipe(id, name, rating));
        }

        return result;
    }

    public List<RecipeFlavor> getFlavorsForRecipe(String recipeId) throws IOException {
       return restApi.getFlavors(recipeId).execute().body();
    }

    private Connection.Response getResponseWithCookies(String url) throws IOException {
        Connection c = Jsoup.connect(url).timeout(30000)
                .cookies(cookies);
        Connection.Response response = c.method(Connection.Method.GET).execute();
        cookies.putAll(response.cookies());
        return response;
    }

    private static RestApi setupRestApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        return retrofit.create(RestApi.class);
    }
}
