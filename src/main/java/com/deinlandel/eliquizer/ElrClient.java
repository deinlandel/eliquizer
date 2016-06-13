package com.deinlandel.eliquizer;

import com.deinlandel.eliquizer.entity.Recipe;
import com.deinlandel.eliquizer.entity.RecipeFlavor;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.*;

/**
 * Provides methods to retrieve various data from e-liquid-recipes.com.
 * Note that it class is stateful - it stores cookies (in memory for now) between requests. You will need to authorize
 * using {@link #login(String, String)} method before retrieving any account-related data, such as "What can I make", etc.
 * @author Deinlandel
 */
public class ElrClient {
    public static final String BASE_URL = "http://e-liquid-recipes.com";
    public static final String WHAT_CAN_I_MAKE_PATH = "whatcanimake?exclsingle=1&sort=score&direction=desc";
    public static final int TIMEOUT_MILLIS = 30000;

    private final Map<String, String> cookies = new HashMap<>();
    private final RestApi restApi;

    public ElrClient() {
        restApi = setupRestApi();
    }

    /**
     * Create user session at e-liquid-recipes and store it in cookies of ElrClient.
     * @param login login (e-mail)
     * @param password password
     * @throws IOException whenever any html loading error or network error occurs
     * @throws WrongCredentialsException if authorization failed because of wrong login/password
     */
    public void login(@Nonnull String login, @Nonnull String password) throws IOException, WrongCredentialsException {
        Connection c = Jsoup.connect(BASE_URL + "/login").timeout(TIMEOUT_MILLIS)
                .data("email", login)
                .data("passwd", password);
        Connection.Response response = c.method(Connection.Method.POST).execute();

        if(response.parse().getElementById("passwd") != null) throw new WrongCredentialsException();

        cookies.putAll(response.cookies());
    }

    /**
     * Fetch and parse single recipe list page.
     * @param page page number, starting at 1
     * @param path page path, for example, "whatcanimake?exclsingle=1&sort=score&direction=desc"
     * @return parsed list of recipes with flavors
     * @throws IOException whenever any html loading error or network error occurs
     * @throws NotAuthorizedException if current ElrClient cookies do not contain valid e-liquid-recipes.com user session
     */
    @SuppressWarnings("unchecked")
    public List<Recipe> getRecipesPage(@Nonnegative int page, @Nonnull String path) throws IOException, NotAuthorizedException {
        String url = BASE_URL + "/" + path + "&page=" + page;
        final List<Recipe> result = new ArrayList<>();

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

        List<Observable<List<RecipeFlavor>>> requests = new ArrayList<>();

        ListIterator<Element> it = elements.listIterator(1);
        while (it.hasNext()) {
            Element row = it.next();
            if (!row.select("td[colspan=4]").isEmpty()) continue;  //skip ads
            Elements nameElement = row.select("td:eq(0)");
            String id = nameElement.select(".rinfo").attr("data-rid");
            String name = nameElement.select(".mlink").text();
            Elements ratingCell = row.select("td:eq(3)");
            double rating = Double.parseDouble(ratingCell.select(".star").attr("data-score"));
            String voteString = ratingCell.select(".novo").text();
            int votes = Integer.parseInt(voteString.substring(1, voteString.length() - 1));
            result.add(new Recipe(id, name, rating, votes));
            requests.add(restApi.getFlavors(id));
        }

        //Request flavors for each recipe in list
        Observable.zip(requests, args -> {
            for (int i = 0; i < args.length; i++) {
                result.get(i).setFlavors((List<RecipeFlavor>) args[i]);
            }
            return 1;
        }).observeOn(Schedulers.io()).toBlocking().first();

        return result;
    }

    public List<Recipe> whatCanIMake(@Nonnegative int pageLimit) {
        return getRecipes(pageLimit, WHAT_CAN_I_MAKE_PATH);
    }

    @SuppressWarnings("unchecked")
    public List<Recipe> getRecipes(int pageLimit, String path) {
        final List<Recipe> result = new ArrayList<>();

        List<Observable<List<Recipe>>> requests = new ArrayList<>();
        for (int i = 1; i <= pageLimit; i++) {
            final int finalI = i;
            requests.add(Async.start(() -> {
                //System.out.println("Fetching page " + finalI);
                try {
                    return getRecipesPage(finalI, path);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        Observable.zip(requests, args -> {
            for (Object arg : args) {
                result.addAll((Collection<? extends Recipe>) arg);
            }
            return 1;
        }).toBlocking().last();

        return result;
    }

    @SuppressWarnings("UnusedDeclaration")
    public List<RecipeFlavor> getFlavorsForRecipe(@Nonnull String recipeId) {
       return restApi.getFlavors(recipeId).toBlocking().first();
    }

    private Connection.Response getResponseWithCookies(@Nonnull String url) throws IOException {
        Connection c = Jsoup.connect(url).timeout(TIMEOUT_MILLIS)
                .cookies(cookies);
        Connection.Response response = c.method(Connection.Method.GET).execute();
        cookies.putAll(response.cookies());
        return response;
    }

    private static RestApi setupRestApi() {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        return retrofit.create(RestApi.class);
    }
}
