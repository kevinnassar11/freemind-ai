package com.freemind.ai.data.repository;

import com.freemind.ai.data.local.PendingFavoriteQueue;
import com.freemind.ai.data.remote.places.PlacesDataSource;
import com.freemind.ai.data.remote.supabase.PlacesSupabaseDataSource;
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
public final class PlacesRepositoryImpl_Factory implements Factory<PlacesRepositoryImpl> {
  private final Provider<PlacesDataSource> placesDataSourceProvider;

  private final Provider<PlacesSupabaseDataSource> placesSupabaseDataSourceProvider;

  private final Provider<PendingFavoriteQueue> pendingFavoriteQueueProvider;

  private PlacesRepositoryImpl_Factory(Provider<PlacesDataSource> placesDataSourceProvider,
      Provider<PlacesSupabaseDataSource> placesSupabaseDataSourceProvider,
      Provider<PendingFavoriteQueue> pendingFavoriteQueueProvider) {
    this.placesDataSourceProvider = placesDataSourceProvider;
    this.placesSupabaseDataSourceProvider = placesSupabaseDataSourceProvider;
    this.pendingFavoriteQueueProvider = pendingFavoriteQueueProvider;
  }

  @Override
  public PlacesRepositoryImpl get() {
    return newInstance(placesDataSourceProvider.get(), placesSupabaseDataSourceProvider.get(), pendingFavoriteQueueProvider.get());
  }

  public static PlacesRepositoryImpl_Factory create(
      Provider<PlacesDataSource> placesDataSourceProvider,
      Provider<PlacesSupabaseDataSource> placesSupabaseDataSourceProvider,
      Provider<PendingFavoriteQueue> pendingFavoriteQueueProvider) {
    return new PlacesRepositoryImpl_Factory(placesDataSourceProvider, placesSupabaseDataSourceProvider, pendingFavoriteQueueProvider);
  }

  public static PlacesRepositoryImpl newInstance(PlacesDataSource placesDataSource,
      PlacesSupabaseDataSource placesSupabaseDataSource,
      PendingFavoriteQueue pendingFavoriteQueue) {
    return new PlacesRepositoryImpl(placesDataSource, placesSupabaseDataSource, pendingFavoriteQueue);
  }
}
