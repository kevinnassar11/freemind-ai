package com.freemind.ai.data.remote.firebase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import okhttp3.OkHttpClient;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata
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
public final class RoleStampClient_Factory implements Factory<RoleStampClient> {
  private final Provider<OkHttpClient> okHttpClientProvider;

  private final Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider;

  private RoleStampClient_Factory(Provider<OkHttpClient> okHttpClientProvider,
      Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider) {
    this.okHttpClientProvider = okHttpClientProvider;
    this.firebaseAuthDataSourceProvider = firebaseAuthDataSourceProvider;
  }

  @Override
  public RoleStampClient get() {
    return newInstance(okHttpClientProvider.get(), firebaseAuthDataSourceProvider.get());
  }

  public static RoleStampClient_Factory create(Provider<OkHttpClient> okHttpClientProvider,
      Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider) {
    return new RoleStampClient_Factory(okHttpClientProvider, firebaseAuthDataSourceProvider);
  }

  public static RoleStampClient newInstance(OkHttpClient okHttpClient,
      FirebaseAuthDataSource firebaseAuthDataSource) {
    return new RoleStampClient(okHttpClient, firebaseAuthDataSource);
  }
}
