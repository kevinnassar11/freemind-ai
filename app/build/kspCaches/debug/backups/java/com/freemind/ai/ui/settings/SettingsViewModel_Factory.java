package com.freemind.ai.ui.settings;

import com.freemind.ai.data.export.ExportService;
import com.freemind.ai.data.repository.AuthRepository;
import com.freemind.ai.data.repository.PlacesRepository;
import com.freemind.ai.data.repository.SettingsRepository;
import com.freemind.ai.data.repository.SupabaseRepository;
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
public final class SettingsViewModel_Factory implements Factory<SettingsViewModel> {
  private final Provider<SettingsRepository> settingsRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<SupabaseRepository> supabaseRepositoryProvider;

  private final Provider<PlacesRepository> placesRepositoryProvider;

  private final Provider<ExportService> exportServiceProvider;

  private SettingsViewModel_Factory(Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SupabaseRepository> supabaseRepositoryProvider,
      Provider<PlacesRepository> placesRepositoryProvider,
      Provider<ExportService> exportServiceProvider) {
    this.settingsRepositoryProvider = settingsRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.supabaseRepositoryProvider = supabaseRepositoryProvider;
    this.placesRepositoryProvider = placesRepositoryProvider;
    this.exportServiceProvider = exportServiceProvider;
  }

  @Override
  public SettingsViewModel get() {
    return newInstance(settingsRepositoryProvider.get(), authRepositoryProvider.get(), supabaseRepositoryProvider.get(), placesRepositoryProvider.get(), exportServiceProvider.get());
  }

  public static SettingsViewModel_Factory create(
      Provider<SettingsRepository> settingsRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<SupabaseRepository> supabaseRepositoryProvider,
      Provider<PlacesRepository> placesRepositoryProvider,
      Provider<ExportService> exportServiceProvider) {
    return new SettingsViewModel_Factory(settingsRepositoryProvider, authRepositoryProvider, supabaseRepositoryProvider, placesRepositoryProvider, exportServiceProvider);
  }

  public static SettingsViewModel newInstance(SettingsRepository settingsRepository,
      AuthRepository authRepository, SupabaseRepository supabaseRepository,
      PlacesRepository placesRepository, ExportService exportService) {
    return new SettingsViewModel(settingsRepository, authRepository, supabaseRepository, placesRepository, exportService);
  }
}
