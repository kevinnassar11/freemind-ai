package com.freemind.ai.data.repository;

import com.freemind.ai.data.remote.firebase.FirebaseAuthDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

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
public final class AuthRepositoryImpl_Factory implements Factory<AuthRepositoryImpl> {
  private final Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider;

  private AuthRepositoryImpl_Factory(
      Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider) {
    this.firebaseAuthDataSourceProvider = firebaseAuthDataSourceProvider;
  }

  @Override
  public AuthRepositoryImpl get() {
    return newInstance(firebaseAuthDataSourceProvider.get());
  }

  public static AuthRepositoryImpl_Factory create(
      Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider) {
    return new AuthRepositoryImpl_Factory(firebaseAuthDataSourceProvider);
  }

  public static AuthRepositoryImpl newInstance(FirebaseAuthDataSource firebaseAuthDataSource) {
    return new AuthRepositoryImpl(firebaseAuthDataSource);
  }
}
