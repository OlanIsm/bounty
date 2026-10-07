package id.ac.binus.bounty;

import android.content.Context;
import android.os.SystemClock;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.google.gson.Gson;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.ResponseBody;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import id.ac.binus.bounty.activities.MainActivity;
import id.ac.binus.bounty.activities.MainActivity.DemoUsersModel;
import id.ac.binus.bounty.models.User;
import id.ac.binus.bounty.network.RandomUserApi;
import id.ac.binus.bounty.network.RandomUserResponse;
import id.ac.binus.bounty.utils.SessionManager;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ExternalApiTest {
    @Test public void retrofitProfilesRetainLoadingAndRecoverFromFailuresWithoutChangingAccount() throws Exception {
        Context context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        SessionManager session = new SessionManager(context);
        assertTrue(session.login(SessionManager.DEMO_EMAIL));
        User local = session.getCurrentUser();
        AtomicInteger calls = new AtomicInteger(), status = new AtomicInteger(200);
        AtomicReference<String> body = new AtomicReference<>(profiles(10));
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        OkHttpClient http = new OkHttpClient.Builder().addInterceptor(chain -> {
            assertEquals("/api/", chain.request().url().encodedPath());
            assertEquals("10", chain.request().url().queryParameter("results"));
            if (calls.incrementAndGet() == 1) {
                entered.countDown();
                try { if (!release.await(10, TimeUnit.SECONDS)) throw new IOException("Test release timed out"); }
                catch (InterruptedException error) { Thread.currentThread().interrupt(); throw new IOException(error); }
            }
            if (status.get() == -1) throw new IOException("Offline fixture");
            return new okhttp3.Response.Builder().request(chain.request()).protocol(Protocol.HTTP_1_1)
                    .code(status.get()).message("Fixture")
                    .body(ResponseBody.create(MediaType.get("application/json"), body.get())).build();
        }).build();
        RandomUserApi api = new Retrofit.Builder().baseUrl("https://randomuser.me/").client(http)
                .addConverterFactory(GsonConverterFactory.create()).build().create(RandomUserApi.class);
        try (ActivityScenario<MainActivity> screen = ActivityScenario.launch(MainActivity.class)) {
            AtomicReference<DemoUsersModel> retained = new AtomicReference<>();
            screen.onActivity(activity -> {
                DemoUsersModel model = new ViewModelProvider(activity).get(DemoUsersModel.class);
                retained.set(model);
                model.load(api);
                model.load(api); // Duplicate clicks must not launch another request.
                activity.findViewById(R.id.nav_profile).performClick();
                assertEquals(View.VISIBLE, activity.findViewById(R.id.demo_users_loading).getVisibility());
            });
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            screen.recreate();
            screen.onActivity(activity -> {
                assertSame(retained.get(), new ViewModelProvider(activity).get(DemoUsersModel.class));
                assertTrue(retained.get().state.getValue().loading);
                activity.findViewById(R.id.nav_home).performClick();
                activity.findViewById(R.id.nav_profile).performClick();
            });
            assertEquals(1, calls.get());
            release.countDown();
            awaitResult(screen, false);
            screen.onActivity(activity -> {
                LinearLayout list = activity.findViewById(R.id.demo_users_list);
                assertEquals(10, list.getChildCount());
                assertEquals("Demo0 Hunter", ((TextView) list.getChildAt(0).findViewById(R.id.demo_user_name)).getText().toString());
                assertEquals("demo0@example.com", ((TextView) list.getChildAt(0).findViewById(R.id.demo_user_email)).getText().toString());
                assertEquals(View.GONE, activity.findViewById(R.id.demo_users_retry).getVisibility());
            });
            screen.recreate();
            screen.onActivity(activity -> assertEquals(10,
                    ((LinearLayout) activity.findViewById(R.id.demo_users_list)).getChildCount()));
            assertEquals(1, calls.get());

            for (String invalid : new String[]{"{\"results\":[]}", "{\"error\":\"Unavailable\"}", "not-json", "{\"results\":[null,{}]}"}) {
                body.set(invalid);
                screen.onActivity(activity -> retained.get().load(api));
                awaitResult(screen, true);
            }
            status.set(503);
            screen.onActivity(activity -> retained.get().load(api));
            awaitResult(screen, true);
            status.set(-1);
            screen.onActivity(activity -> retained.get().load(api));
            awaitResult(screen, true);
            screen.recreate();
            screen.onActivity(activity -> {
                LinearLayout list = activity.findViewById(R.id.demo_users_list);
                assertEquals(1, list.getChildCount());
                assertEquals("Bounty User", ((TextView) list.getChildAt(0).findViewById(R.id.demo_user_name)).getText().toString());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.demo_users_error).getVisibility());
                assertEquals(View.VISIBLE, activity.findViewById(R.id.demo_users_retry).getVisibility());
                assertEquals(local.name, ((TextView) activity.findViewById(R.id.profile_name)).getText().toString());
            });
            status.set(200); body.set(profiles(10));
            screen.onActivity(activity -> retained.get().load(api));
            awaitResult(screen, false);
            assertEquals(local.id, session.getCurrentUser().id);
            assertEquals(local.email, session.getCurrentUser().email);
            assertEquals(local.demoBalance, session.getCurrentUser().demoBalance, 0);

            RandomUserResponse limited = new Gson().fromJson(profiles(12), RandomUserResponse.class);
            assertEquals(10, limited.toUsers().size());
            RandomUserResponse unsafe = new Gson().fromJson(profiles(1).replace("https://example.com/avatar.png", "file:///secret.png"), RandomUserResponse.class);
            assertEquals("", unsafe.toUsers().get(0).avatarUrl);
        } finally {
            release.countDown();
            http.dispatcher().executorService().shutdownNow();
            http.connectionPool().evictAll();
            session.logout();
        }
    }
    private void awaitResult(ActivityScenario<MainActivity> screen, boolean failed) {
        long end = SystemClock.uptimeMillis() + 5000;
        AtomicReference<DemoUsersModel.State> state = new AtomicReference<>();
        do {
            screen.onActivity(activity -> state.set(new ViewModelProvider(activity).get(DemoUsersModel.class).state.getValue()));
            if (state.get() != null && !state.get().loading) {
                assertEquals(failed, state.get().failed);
                return;
            }
            SystemClock.sleep(16);
        } while (SystemClock.uptimeMillis() < end);
        fail("Demo API state timed out");
    }
    private String profiles(int count) {
        StringBuilder json = new StringBuilder("{\"results\":[");
        for (int i = 0; i < count; i++) {
            if (i > 0) json.append(',');
            json.append("{\"name\":{\"first\":\"Demo").append(i).append("\",\"last\":\"Hunter\"},")
                    .append("\"email\":\"demo").append(i).append("@example.com\",\"login\":{\"uuid\":\"demo-")
                    .append(i).append("\"},\"picture\":{\"large\":\"https://example.com/avatar.png\"}}");
        }
        return json.append("]}").toString();
    }
}
