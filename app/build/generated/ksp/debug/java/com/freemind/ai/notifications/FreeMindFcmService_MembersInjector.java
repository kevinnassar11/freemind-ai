package com.freemind.ai.notifications;

import com.freemind.ai.data.repository.AuthRepository;
import com.freemind.ai.data.repository.SupabaseRepository;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;

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
public final class FreeMindFcmService_MembersInjector implements MembersInjector<FreeMindFcmService> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SupabaseRepository> supabaseRepositoryProvider;

  private FreeMindFcmService_MembersInjector(Provider<AuthRepository> authRepositoryProvider,
      Provider<SupabaseRepository> supabaseRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.supabaseRepositoryProvider = supabaseRepositoryProvider;
  }

  @Override
  public void injectMembers(FreeMindFcmService instance) {
    injectAuthRepository(instance, authRepositoryProvider.get());
    injectSupabaseRepository(instance, supabaseRepositoryProvider.get());
  }

  public static MembersInjector<FreeMindFcmService> create(
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SupabaseRepository> supabaseRepositoryProvider) {
    return new FreeMindFcmService_MembersInjector(authRepositoryProvider, supabaseRepositoryProvider);
  }

  @InjectedFieldSignature("com.freemind.ai.notifications.FreeMindFcmService.authRepository")
  public static void injectAuthRepository(FreeMindFcmService instance,
      AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }

  @InjectedFieldSignature("com.freemind.ai.notifications.FreeMindFcmService.supabaseRepository")
  public static void injectSupabaseRepository(FreeMindFcmService instance,
      SupabaseRepository supabaseRepository) {
    instance.supabaseRepository = supabaseRepository;
  }
}
