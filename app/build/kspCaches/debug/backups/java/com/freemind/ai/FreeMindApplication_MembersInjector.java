package com.freemind.ai;

import com.freemind.ai.data.repository.AuthRepository;
import com.freemind.ai.data.repository.PlacesRepository;
import com.freemind.ai.di.ApplicationScope;
import com.freemind.ai.util.ConnectivityObserver;
import dagger.MembersInjector;
import dagger.internal.DaggerGenerated;
import dagger.internal.InjectedFieldSignature;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;

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
public final class FreeMindApplication_MembersInjector implements MembersInjector<FreeMindApplication> {
  private final Provider<PlacesRepository> placesRepositoryProvider;

  private final Provider<AuthRepository> authRepositoryProvider;

  private final Provider<ConnectivityObserver> connectivityObserverProvider;

  private final Provider<CoroutineScope> applicationScopeProvider;

  private FreeMindApplication_MembersInjector(Provider<PlacesRepository> placesRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<ConnectivityObserver> connectivityObserverProvider,
      Provider<CoroutineScope> applicationScopeProvider) {
    this.placesRepositoryProvider = placesRepositoryProvider;
    this.authRepositoryProvider = authRepositoryProvider;
    this.connectivityObserverProvider = connectivityObserverProvider;
    this.applicationScopeProvider = applicationScopeProvider;
  }

  @Override
  public void injectMembers(FreeMindApplication instance) {
    injectPlacesRepository(instance, placesRepositoryProvider.get());
    injectAuthRepository(instance, authRepositoryProvider.get());
    injectConnectivityObserver(instance, connectivityObserverProvider.get());
    injectApplicationScope(instance, applicationScopeProvider.get());
  }

  public static MembersInjector<FreeMindApplication> create(
      Provider<PlacesRepository> placesRepositoryProvider,
      Provider<AuthRepository> authRepositoryProvider,
      Provider<ConnectivityObserver> connectivityObserverProvider,
      Provider<CoroutineScope> applicationScopeProvider) {
    return new FreeMindApplication_MembersInjector(placesRepositoryProvider, authRepositoryProvider, connectivityObserverProvider, applicationScopeProvider);
  }

  @InjectedFieldSignature("com.freemind.ai.FreeMindApplication.placesRepository")
  public static void injectPlacesRepository(FreeMindApplication instance,
      PlacesRepository placesRepository) {
    instance.placesRepository = placesRepository;
  }

  @InjectedFieldSignature("com.freemind.ai.FreeMindApplication.authRepository")
  public static void injectAuthRepository(FreeMindApplication instance,
      AuthRepository authRepository) {
    instance.authRepository = authRepository;
  }

  @InjectedFieldSignature("com.freemind.ai.FreeMindApplication.connectivityObserver")
  public static void injectConnectivityObserver(FreeMindApplication instance,
      ConnectivityObserver connectivityObserver) {
    instance.connectivityObserver = connectivityObserver;
  }

  @InjectedFieldSignature("com.freemind.ai.FreeMindApplication.applicationScope")
  @ApplicationScope
  public static void injectApplicationScope(FreeMindApplication instance,
      CoroutineScope applicationScope) {
    instance.applicationScope = applicationScope;
  }
}
