package com.freemind.ai.ui.maps;

import com.freemind.ai.data.repository.AuthRepository;
import com.freemind.ai.data.repository.PlacesRepository;
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
public final class MapsViewModel_Factory implements Factory<MapsViewModel> {
  private final Provider<PlacesRepository> placesRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private MapsViewModel_Factory(Provider<PlacesRepository> placesRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    this.placesRepositoryProvider = placesRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.settingsRepositoryProvider = settingsRepositoryProvider;
  }

  @Override
  public MapsViewModel get() {
    return newInstance(placesRepositoryProvider.get(), authRepositoryProvider.get(), settingsRepositoryProvider.get());
  }

  public static MapsViewModel_Factory create(Provider<PlacesRepository> placesRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SettingsRepository> settingsRepositoryProvider) {
    return new MapsViewModel_Factory(placesRepositoryProvider, authRepositoryProvider, settingsRepositoryProvider);
  }

  public static MapsViewModel newInstance(PlacesRepository placesRepository,
      AuthRepository authRepository, SettingsRepository settingsRepository) {
    return new MapsViewModel(placesRepository, authRepository, settingsRepository);
  }
}
