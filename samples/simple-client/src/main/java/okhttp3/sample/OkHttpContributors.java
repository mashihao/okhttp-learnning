package okhttp3.sample;

import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.Types;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;

public class OkHttpContributors {
    private static final String ENDPOINT = "https://api.github.com/repos/square/okhttp/contributors";
    private static final Moshi MOSHI = new Moshi.Builder().build();
    private static final JsonAdapter<List<Contributor>> CONTRIBUTORS_JSON_ADAPTER = MOSHI.adapter(
            Types.newParameterizedType(List.class, Contributor.class));

    static class Contributor {
        String login;
        int contributions;
        String avatar_url;
    }

    public static void main(String... args) throws Exception {

        OkHttpClient.Builder builder = new OkHttpClient.Builder();

        builder.addInterceptor(new HttpLoggingInterceptor(message -> System.out.println(message)).setLevel(HttpLoggingInterceptor.Level.BASIC));

        builder.readTimeout(5, TimeUnit.SECONDS);

        OkHttpClient client = builder.build();

        // Create request for remote resource.
        Request request = new Request.Builder()
                .url(ENDPOINT)
                .build();


        Call call = client.newCall(request);

        // Execute the request and retrieve the response.


        // 添加 回调函数
//        call.enqueue(new Callback() {
//            @Override
//            public void onFailure(Call call, IOException e) {
//
//            }
//
//            @Override
//            public void onResponse(Call call, Response response) throws IOException {
//
//            }
//        });


        Response response = call.execute();
        // Deserialize HTTP response to concrete type.
        ResponseBody body = response.body();
        List<Contributor> contributors = CONTRIBUTORS_JSON_ADAPTER.fromJson(body.source());

        // Sort list by the most contributions.
        Collections.sort(contributors, (c1, c2) -> c2.contributions - c1.contributions);

        // Output list of contributors.
        for (Contributor contributor : contributors) {
            System.out.println(contributor.login + ": " + contributor.contributions + ": " + contributor.avatar_url);
        }

    }

    private OkHttpContributors() {
        // No instances.
    }
}
