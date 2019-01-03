package pogo.assistance.data.di;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import dagger.Module;
import dagger.Provides;
import java.util.ServiceLoader;

@Module
public class PersistenceModule {

    private static final Gson GSON = buildGson();

    @Provides
    public static Gson provideGson() {
        return GSON;
    }

    private static Gson buildGson() {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.setPrettyPrinting();
        gsonBuilder.setLenient();

        // Register adapters for GSON ser-de (https://immutables.github.io/json.html#type-adapter-registration)
        ServiceLoader.load(TypeAdapterFactory.class).forEach(gsonBuilder::registerTypeAdapterFactory);

        return gsonBuilder.create();
    }

}
