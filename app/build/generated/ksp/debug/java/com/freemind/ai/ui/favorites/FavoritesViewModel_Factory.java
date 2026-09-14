package com.freemind.ai.ui.favorites;

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
public final class FavoritesViewModel_Factory implements Factory<FavoritesViewModel> {
  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<PlacesRepository> placesRepositoryProvider;

  private FavoritesViewModel_Factory(Provider<AuthRepository> authRepositoryProvider,
      Provider<PlacesRepository> placesRepositoryProvider) {
    this.authRepositoryProvider = authRepositoryProvider;
    this.placesRepositoryProvider = placesRepositoryProvider;
  }

  @Override
  public FavoritesViewModel get() {
    return newInstance(authRepositoryProvider.get(), placesRepositoryProvider.get());
  }

  public static FavoritesViewModel_Factory create(Provider<AuthRepository> authRepositoryProvider,
      Provider<PlacesRepository> placesRepositoryProvider) {
    return new FavoritesViewModel_Factory(authRepositoryProvider, placesRepositoryProvider);
  }

  public static FavoritesViewModel newInstance(AuthRepository authRepository,
      PlacesRepository placesRepository) {
    return new FavoritesViewModel(authRepository, placesRepository);
  }
}
