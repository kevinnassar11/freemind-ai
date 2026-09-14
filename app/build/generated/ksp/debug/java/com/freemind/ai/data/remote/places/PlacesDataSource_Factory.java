package com.freemind.ai.data.remote.places;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast",
    "deprecation",
    "nullness:initialization.field.uninitialized"
})
public final class PlacesDataSource_Factory implements Factory<PlacesDataSource> {
  private final Provider<OkHttpClient> httpClientProvider;

  private final Provider<Context> contextProvider;

  private PlacesDataSource_Factory(Provider<OkHttpClient> httpClientProvider,
      Provider<Context> contextProvider) {
    this.httpClientProvider = httpClientProvider;
    this.contextProvider = contextProvider;
  }

  @Override
  public PlacesDataSource get() {
    return newInstance(httpClientProvider.get(), contextProvider.get());
  }

  public static PlacesDataSource_Factory create(Provider<OkHttpClient> httpClientProvider,
      Provider<Context> contextProvider) {
    return new PlacesDataSource_Factory(httpClientProvider, contextProvider);
  }

  public static PlacesDataSource newInstance(OkHttpClient httpClient, Context context) {
    return new PlacesDataSource(httpClient, context);
  }
}
