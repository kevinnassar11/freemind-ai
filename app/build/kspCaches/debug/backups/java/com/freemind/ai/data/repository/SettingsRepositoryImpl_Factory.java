package com.freemind.ai.data.repository;

import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
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
public final class SettingsRepositoryImpl_Factory implements Factory<SettingsRepositoryImpl> {
  private final Provider<DataStore<Preferences>> dataStoreProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SupabaseRepository> supabaseRepositoryProvider;

  private SettingsRepositoryImpl_Factory(Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SupabaseRepository> supabaseRepositoryProvider) {
    this.dataStoreProvider = dataStoreProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.supabaseRepositoryProvider = supabaseRepositoryProvider;
  }

  @Override
  public SettingsRepositoryImpl get() {
    return newInstance(dataStoreProvider.get(), authRepositoryProvider.get(), supabaseRepositoryProvider.get());
  }

  public static SettingsRepositoryImpl_Factory create(
      Provider<DataStore<Preferences>> dataStoreProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SupabaseRepository> supabaseRepositoryProvider) {
    return new SettingsRepositoryImpl_Factory(dataStoreProvider, authRepositoryProvider, supabaseRepositoryProvider);
  }

  public static SettingsRepositoryImpl newInstance(DataStore<Preferences> dataStore,
      AuthRepository authRepository, SupabaseRepository supabaseRepository) {
    return new SettingsRepositoryImpl(dataStore, authRepository, supabaseRepository);
  }
}
