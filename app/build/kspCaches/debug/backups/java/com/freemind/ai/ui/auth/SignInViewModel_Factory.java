package com.freemind.ai.ui.auth;

import com.freemind.ai.data.remote.firebase.RoleStampClient;
import com.freemind.ai.data.repository.AuthRepository;
import com.freemind.ai.data.repository.SupabaseRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

@ScopeMetadata
@QualifierMetadata("com.freemind.ai.di.ApplicationScope")
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
public final class SignInViewModel_Factory implements Factory<SignInViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SupabaseRepository> supabaseRepositoryProvider;

  private final Provider<RoleStampClient> roleStampClientProvider;

  private final Provider<CoroutineScope> applicationScopeProvider;

  private SignInViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<SupabaseRepository> supabaseRepositoryProvider,
      Provider<RoleStampClient> roleStampClientProvider,
      Provider<CoroutineScope> applicationScopeProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.supabaseRepositoryProvider = supabaseRepositoryProvider;
    this.roleStampClientProvider = roleStampClientProvider;
    this.applicationScopeProvider = applicationScopeProvider;
  }

  @Override
  public SignInViewModel get() {
    return newInstance(authRepositoryProvider.get(), supabaseRepositoryProvider.get(), roleStampClientProvider.get(), applicationScopeProvider.get());
  }

  public static SignInViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<SupabaseRepository> supabaseRepositoryProvider,
      Provider<RoleStampClient> roleStampClientProvider,
      Provider<CoroutineScope> applicationScopeProvider) {
    return new SignInViewModel_Factory(authRepositoryProvider, supabaseRepositoryProvider, roleStampClientProvider, applicationScopeProvider);
  }

  public static SignInViewModel newInstance(AuthRepository authRepository,
      SupabaseRepository supabaseRepository, RoleStampClient roleStampClient,
      CoroutineScope applicationScope) {
    return new SignInViewModel(authRepository, supabaseRepository, roleStampClient, applicationScope);
  }
}
