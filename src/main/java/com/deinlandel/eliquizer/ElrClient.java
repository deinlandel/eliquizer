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
import rx.functions.Func0;
import rx.functions.FuncN;
import rx.schedulers.Schedulers;
import rx.util.async.Async;

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

    public List<Recipe> whatCanIMakePage(int page) throws IOException, NotAuthorizedException {
        final List<Recipe> result = new ArrayList<Recipe>();

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

        List<Observable<List<RecipeFlavor>>> requests = new ArrayList<Observable<List<RecipeFlavor>>>();

        ListIterator<Element> it = elements.listIterator(1);
        while (it.hasNext()) {
            Element row = it.next();
            if (!row.select("td[colspan=4]").isEmpty()) continue;  //skip ads
            Elements nameElement = row.select("td:eq(0)");
            String id = nameElement.select(".rinfo").attr("data-rid");
            String name = nameElement.select(".mlink").text();
            double rating = Double.parseDouble(row.select("td:eq(3)").select(".star").attr("data-score"));
            result.add(new Recipe(id, name, rating));
            requests.add(restApi.getFlavors(id));
        }

        //Request flavors for each recipe in list
        Observable.zip(requests, new FuncN<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object call(Object... args) {
                for (int i = 0; i < args.length; i++) {
                    result.get(i).setFlavors((List<RecipeFlavor>) args[i]);
                }
                return 1;
            }
        }).observeOn(Schedulers.io()).toBlocking().first();

        return result;
    }

    public List<Recipe> whatCanIMake(int pageLimit) throws IOException, NotAuthorizedException {
        final List<Recipe> result = new ArrayList<Recipe>();

        List<Observable<List<Recipe>>> requests = new ArrayList<Observable<List<Recipe>>>();
        for (int i = 1; i <= pageLimit; i++) {
            final int finalI = i;
            requests.add(Async.start(new Func0<List<Recipe>>() {
                @Override
                public List<Recipe> call() {
                    //System.out.println("Fetching page " + finalI);
                    try {
                        return whatCanIMakePage(finalI);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }));
        }

        Observable.zip(requests, new FuncN<Object>() {
            @SuppressWarnings("unchecked")
            @Override
            public Object call(Object... args) {
                for (Object arg : args) {
                    result.addAll((Collection<? extends Recipe>) arg);
                }
                return 1;
            }
        }).toBlocking().last();

        return result;
    }

    public List<RecipeFlavor> getFlavorsForRecipe(String recipeId) throws IOException {
       return restApi.getFlavors(recipeId).toBlocking().first();
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
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .build();
        return retrofit.create(RestApi.class);
    }
}
