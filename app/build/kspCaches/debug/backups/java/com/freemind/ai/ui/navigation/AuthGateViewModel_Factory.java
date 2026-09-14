package com.freemind.ai.ui.navigation;

import com.freemind.ai.data.repository.AuthRepository;
import com.freemind.ai.data.repository.SettingsRepository;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata
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
public final class AuthGateViewModel_Factory implements Factory<AuthGateViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private AuthGateViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public AuthGateViewModel get() {
    return newInstance(authRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static AuthGateViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new AuthGateViewModel_Factory(authRepositoryProvider, settingsRepositoryProvider);
  }

  public static AuthGateViewModel newInstance(AuthRepository authRepository,
      SettingsRepository settingsRepository) {
    return new AuthGateViewModel(authRepository, settingsRepository);
  }
}
