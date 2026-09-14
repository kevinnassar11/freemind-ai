package com.freemind.ai.data.repository;

import com.freemind.ai.data.remote.supabase.ProfileDataSource;
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
public final class SupabaseRepositoryImpl_Factory implements Factory<SupabaseRepositoryImpl> {
  private final Provider<ProfileDataSource> profileDataSourceProvider;

  private SupabaseRepositoryImpl_Factory(Provider<ProfileDataSource> profileDataSourceProvider) {
    this.profileDataSourceProvider = profileDataSourceProvider;
  }

  @Override
  public SupabaseRepositoryImpl get() {
    return newInstance(profileDataSourceProvider.get());
  }

  public static SupabaseRepositoryImpl_Factory create(
      Provider<ProfileDataSource> profileDataSourceProvider) {
    return new SupabaseRepositoryImpl_Factory(profileDataSourceProvider);
  }

  public static SupabaseRepositoryImpl newInstance(ProfileDataSource profileDataSource) {
    return new SupabaseRepositoryImpl(profileDataSource);
  }
}
