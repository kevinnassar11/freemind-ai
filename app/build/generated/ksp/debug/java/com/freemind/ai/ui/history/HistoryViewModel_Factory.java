package com.freemind.ai.ui.history;

import com.freemind.ai.data.repository.AuthRepository;
import com.freemind.ai.data.repository.PlacesRepository;
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
public final class HistoryViewModel_Factory implements Factory<HistoryViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<PlacesRepository> placesRepositoryProvider;

  private HistoryViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<PlacesRepository> placesRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.placesRepositoryProvider = placesRepositoryProvider;
  }

  @Override
  public HistoryViewModel get() {
    return newInstance(authRepositoryProvider.get(), placesRepositoryProvider.get());
  }

  public static HistoryViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<PlacesRepository> placesRepositoryProvider) {
    return new HistoryViewModel_Factory(authRepositoryProvider, placesRepositoryProvider);
  }

  public static HistoryViewModel newInstance(AuthRepository authRepository,
      PlacesRepository placesRepository) {
    return new HistoryViewModel(authRepository, placesRepository);
  }
}
