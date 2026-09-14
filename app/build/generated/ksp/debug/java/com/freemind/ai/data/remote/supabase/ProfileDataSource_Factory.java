package com.freemind.ai.data.remote.supabase;

import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
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
public final class ProfileDataSource_Factory implements Factory<ProfileDataSource> {
  private final Provider<SupabaseClient> supabaseClientProvider;

  private ProfileDataSource_Factory(Provider<SupabaseClient> supabaseClientProvider) {
    this.supabaseClientProvider = supabaseClientProvider;
  }

  @Override
  public ProfileDataSource get() {
    return newInstance(supabaseClientProvider.get());
  }

  public static ProfileDataSource_Factory create(Provider<SupabaseClient> supabaseClientProvider) {
    return new ProfileDataSource_Factory(supabaseClientProvider);
  }

  public static ProfileDataSource newInstance(SupabaseClient supabaseClient) {
    return new ProfileDataSource(supabaseClient);
  }
}
