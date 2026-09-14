package com.freemind.ai.di;

import com.freemind.ai.data.remote.firebase.FirebaseAuthDataSource;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import io.github.jan.supabase.SupabaseClient;
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
public final class SupabaseModule_ProvideSupabaseClientFactory implements Factory<SupabaseClient> {
  private final Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider;

  private SupabaseModule_ProvideSupabaseClientFactory(
      Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider) {
    this.firebaseAuthDataSourceProvider = firebaseAuthDataSourceProvider;
  }

  @Override
  public SupabaseClient get() {
    return provideSupabaseClient(firebaseAuthDataSourceProvider.get());
  }

  public static SupabaseModule_ProvideSupabaseClientFactory create(
      Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider) {
    return new SupabaseModule_ProvideSupabaseClientFactory(firebaseAuthDataSourceProvider);
  }

  public static SupabaseClient provideSupabaseClient(
      FirebaseAuthDataSource firebaseAuthDataSource) {
    return Preconditions.checkNotNullFromProvides(SupabaseModule.INSTANCE.provideSupabaseClient(firebaseAuthDataSource));
  }
}
