package com.freemind.ai;

import android.app.Activity;
import android.app.Service;
import android.view.View;
import androidx.datastore.core.DataStore;
import androidx.datastore.preferences.core.Preferences;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import com.freemind.ai.data.export.ExportService;
import com.freemind.ai.data.local.PendingFavoriteQueue;
import com.freemind.ai.data.remote.firebase.FirebaseAuthDataSource;
import com.freemind.ai.data.remote.firebase.RoleStampClient;
import com.freemind.ai.data.remote.places.PlacesDataSource;
import com.freemind.ai.data.remote.supabase.PlacesSupabaseDataSource;
import com.freemind.ai.data.remote.supabase.ProfileDataSource;
import com.freemind.ai.data.repository.AuthRepositoryImpl;
import com.freemind.ai.data.repository.PlacesRepositoryImpl;
import com.freemind.ai.data.repository.SettingsRepositoryImpl;
import com.freemind.ai.data.repository.SupabaseRepositoryImpl;
import com.freemind.ai.di.CoroutineScopeModule_ProvideApplicationScopeFactory;
import com.freemind.ai.di.DataStoreModule_ProvidePreferencesDataStoreFactory;
import com.freemind.ai.di.FirebaseModule_ProvideFirebaseAuthFactory;
import com.freemind.ai.di.NetworkModule_ProvideOkHttpClientFactory;
import com.freemind.ai.di.SupabaseModule_ProvideSupabaseClientFactory;
import com.freemind.ai.notifications.FreeMindFcmService;
import com.freemind.ai.notifications.FreeMindFcmService_MembersInjector;
import com.freemind.ai.ui.auth.SignInViewModel;
import com.freemind.ai.ui.auth.SignInViewModel_HiltModules;
import com.freemind.ai.ui.auth.SignInViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.freemind.ai.ui.auth.SignInViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.freemind.ai.ui.favorites.FavoritesViewModel;
import com.freemind.ai.ui.favorites.FavoritesViewModel_HiltModules;
import com.freemind.ai.ui.favorites.FavoritesViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.freemind.ai.ui.favorites.FavoritesViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.freemind.ai.ui.history.HistoryViewModel;
import com.freemind.ai.ui.history.HistoryViewModel_HiltModules;
import com.freemind.ai.ui.history.HistoryViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.freemind.ai.ui.history.HistoryViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.freemind.ai.ui.maps.MapsViewModel;
import com.freemind.ai.ui.maps.MapsViewModel_HiltModules;
import com.freemind.ai.ui.maps.MapsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.freemind.ai.ui.maps.MapsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.freemind.ai.ui.navigation.AuthGateViewModel;
import com.freemind.ai.ui.navigation.AuthGateViewModel_HiltModules;
import com.freemind.ai.ui.navigation.AuthGateViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.freemind.ai.ui.navigation.AuthGateViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.freemind.ai.ui.settings.SettingsViewModel;
import com.freemind.ai.ui.settings.SettingsViewModel_HiltModules;
import com.freemind.ai.ui.settings.SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey;
import com.freemind.ai.ui.settings.SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey;
import com.freemind.ai.util.ConnectivityObserver;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.google.firebase.auth.FirebaseAuth;
import dagger.hilt.android.ActivityRetainedLifecycle;
import dagger.hilt.android.ViewModelLifecycle;
import dagger.hilt.android.internal.builders.ActivityComponentBuilder;
import dagger.hilt.android.internal.builders.ActivityRetainedComponentBuilder;
import dagger.hilt.android.internal.builders.FragmentComponentBuilder;
import dagger.hilt.android.internal.builders.ServiceComponentBuilder;
import dagger.hilt.android.internal.builders.ViewComponentBuilder;
import dagger.hilt.android.internal.builders.ViewModelComponentBuilder;
import dagger.hilt.android.internal.builders.ViewWithFragmentComponentBuilder;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories;
import dagger.hilt.android.internal.lifecycle.DefaultViewModelFactories_InternalFactoryFactory_Factory;
import dagger.hilt.android.internal.managers.ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory;
import dagger.hilt.android.internal.managers.SavedStateHandleHolder;
import dagger.hilt.android.internal.modules.ApplicationContextModule;
import dagger.hilt.android.internal.modules.ApplicationContextModule_ProvideContextFactory;
import dagger.internal.DaggerGenerated;
import dagger.internal.DoubleCheck;
import dagger.internal.LazyClassKeyMap;
import dagger.internal.Preconditions;
import dagger.internal.Provider;
import io.github.jan.supabase.SupabaseClient;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;
import kotlinx.coroutines.CoroutineScope;
import okhttp3.OkHttpClient;

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
public final class DaggerFreeMindApplication_HiltComponents_SingletonC {
  private DaggerFreeMindApplication_HiltComponents_SingletonC() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static final class Builder {
    private ApplicationContextModule applicationContextModule;

    private Builder() {
    }

    public Builder applicationContextModule(ApplicationContextModule applicationContextModule) {
      this.applicationContextModule = Preconditions.checkNotNull(applicationContextModule);
      return this;
    }

    public FreeMindApplication_HiltComponents.SingletonC build() {
      Preconditions.checkBuilderRequirement(applicationContextModule, ApplicationContextModule.class);
      return new SingletonCImpl(applicationContextModule);
    }
  }

  private static final class ActivityRetainedCBuilder implements FreeMindApplication_HiltComponents.ActivityRetainedC.Builder {
    private final SingletonCImpl singletonCImpl;

    private SavedStateHandleHolder savedStateHandleHolder;

    private ActivityRetainedCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ActivityRetainedCBuilder savedStateHandleHolder(
        SavedStateHandleHolder savedStateHandleHolder) {
      this.savedStateHandleHolder = Preconditions.checkNotNull(savedStateHandleHolder);
      return this;
    }

    @Override
    public FreeMindApplication_HiltComponents.ActivityRetainedC build() {
      Preconditions.checkBuilderRequirement(savedStateHandleHolder, SavedStateHandleHolder.class);
      return new ActivityRetainedCImpl(singletonCImpl, savedStateHandleHolder);
    }
  }

  private static final class ActivityCBuilder implements FreeMindApplication_HiltComponents.ActivityC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private Activity activity;

    private ActivityCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ActivityCBuilder activity(Activity activity) {
      this.activity = Preconditions.checkNotNull(activity);
      return this;
    }

    @Override
    public FreeMindApplication_HiltComponents.ActivityC build() {
      Preconditions.checkBuilderRequirement(activity, Activity.class);
      return new ActivityCImpl(singletonCImpl, activityRetainedCImpl, activity);
    }
  }

  private static final class FragmentCBuilder implements FreeMindApplication_HiltComponents.FragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private Fragment fragment;

    private FragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public FragmentCBuilder fragment(Fragment fragment) {
      this.fragment = Preconditions.checkNotNull(fragment);
      return this;
    }

    @Override
    public FreeMindApplication_HiltComponents.FragmentC build() {
      Preconditions.checkBuilderRequirement(fragment, Fragment.class);
      return new FragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragment);
    }
  }

  private static final class ViewWithFragmentCBuilder implements FreeMindApplication_HiltComponents.ViewWithFragmentC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private View view;

    private ViewWithFragmentCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;
    }

    @Override
    public ViewWithFragmentCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public FreeMindApplication_HiltComponents.ViewWithFragmentC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewWithFragmentCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl, view);
    }
  }

  private static final class ViewCBuilder implements FreeMindApplication_HiltComponents.ViewC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private View view;

    private ViewCBuilder(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
    }

    @Override
    public ViewCBuilder view(View view) {
      this.view = Preconditions.checkNotNull(view);
      return this;
    }

    @Override
    public FreeMindApplication_HiltComponents.ViewC build() {
      Preconditions.checkBuilderRequirement(view, View.class);
      return new ViewCImpl(singletonCImpl, activityRetainedCImpl, activityCImpl, view);
    }
  }

  private static final class ViewModelCBuilder implements FreeMindApplication_HiltComponents.ViewModelC.Builder {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private SavedStateHandle savedStateHandle;

    private ViewModelLifecycle viewModelLifecycle;

    private ViewModelCBuilder(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
    }

    @Override
    public ViewModelCBuilder savedStateHandle(SavedStateHandle handle) {
      this.savedStateHandle = Preconditions.checkNotNull(handle);
      return this;
    }

    @Override
    public ViewModelCBuilder viewModelLifecycle(ViewModelLifecycle viewModelLifecycle) {
      this.viewModelLifecycle = Preconditions.checkNotNull(viewModelLifecycle);
      return this;
    }

    @Override
    public FreeMindApplication_HiltComponents.ViewModelC build() {
      Preconditions.checkBuilderRequirement(savedStateHandle, SavedStateHandle.class);
      Preconditions.checkBuilderRequirement(viewModelLifecycle, ViewModelLifecycle.class);
      return new ViewModelCImpl(singletonCImpl, activityRetainedCImpl, savedStateHandle, viewModelLifecycle);
    }
  }

  private static final class ServiceCBuilder implements FreeMindApplication_HiltComponents.ServiceC.Builder {
    private final SingletonCImpl singletonCImpl;

    private Service service;

    private ServiceCBuilder(SingletonCImpl singletonCImpl) {
      this.singletonCImpl = singletonCImpl;
    }

    @Override
    public ServiceCBuilder service(Service service) {
      this.service = Preconditions.checkNotNull(service);
      return this;
    }

    @Override
    public FreeMindApplication_HiltComponents.ServiceC build() {
      Preconditions.checkBuilderRequirement(service, Service.class);
      return new ServiceCImpl(singletonCImpl, service);
    }
  }

  private static final class ViewWithFragmentCImpl extends FreeMindApplication_HiltComponents.ViewWithFragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl;

    private final ViewWithFragmentCImpl viewWithFragmentCImpl = this;

    ViewWithFragmentCImpl(SingletonCImpl singletonCImpl,
        ActivityRetainedCImpl activityRetainedCImpl, ActivityCImpl activityCImpl,
        FragmentCImpl fragmentCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;
      this.fragmentCImpl = fragmentCImpl;


    }
  }

  private static final class FragmentCImpl extends FreeMindApplication_HiltComponents.FragmentC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final FragmentCImpl fragmentCImpl = this;

    FragmentCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, Fragment fragmentParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return activityCImpl.getHiltInternalFactoryFactory();
    }

    @Override
    public ViewWithFragmentComponentBuilder viewWithFragmentComponentBuilder() {
      return new ViewWithFragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl, fragmentCImpl);
    }
  }

  private static final class ViewCImpl extends FreeMindApplication_HiltComponents.ViewC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl;

    private final ViewCImpl viewCImpl = this;

    ViewCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        ActivityCImpl activityCImpl, View viewParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;
      this.activityCImpl = activityCImpl;


    }
  }

  private static final class ActivityCImpl extends FreeMindApplication_HiltComponents.ActivityC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ActivityCImpl activityCImpl = this;

    ActivityCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        Activity activityParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;


    }

    ImmutableMap keySetMapOfClassOfObjectAndBooleanBuilder() {
      ImmutableMap.Builder mapBuilder = ImmutableMap.<String, Boolean>builderWithExpectedSize(6);
      mapBuilder.put(AuthGateViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, AuthGateViewModel_HiltModules.KeyModule.provide());
      mapBuilder.put(FavoritesViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, FavoritesViewModel_HiltModules.KeyModule.provide());
      mapBuilder.put(HistoryViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, HistoryViewModel_HiltModules.KeyModule.provide());
      mapBuilder.put(MapsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, MapsViewModel_HiltModules.KeyModule.provide());
      mapBuilder.put(SettingsViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SettingsViewModel_HiltModules.KeyModule.provide());
      mapBuilder.put(SignInViewModel_HiltModules_KeyModule_Provide_LazyMapKey.lazyClassKeyName, SignInViewModel_HiltModules.KeyModule.provide());
      return mapBuilder.build();
    }

    @Override
    public void injectMainActivity(MainActivity arg0) {
    }

    @Override
    public DefaultViewModelFactories.InternalFactoryFactory getHiltInternalFactoryFactory() {
      return DefaultViewModelFactories_InternalFactoryFactory_Factory.newInstance(getViewModelKeys(), new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl));
    }

    @Override
    public Map<Class<?>, Boolean> getViewModelKeys() {
      return LazyClassKeyMap.<Boolean>of(keySetMapOfClassOfObjectAndBooleanBuilder());
    }

    @Override
    public ViewModelComponentBuilder getViewModelComponentBuilder() {
      return new ViewModelCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public FragmentComponentBuilder fragmentComponentBuilder() {
      return new FragmentCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }

    @Override
    public ViewComponentBuilder viewComponentBuilder() {
      return new ViewCBuilder(singletonCImpl, activityRetainedCImpl, activityCImpl);
    }
  }

  private static final class ViewModelCImpl extends FreeMindApplication_HiltComponents.ViewModelC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl;

    private final ViewModelCImpl viewModelCImpl = this;

    Provider<AuthGateViewModel> authGateViewModelProvider;

    Provider<FavoritesViewModel> favoritesViewModelProvider;

    Provider<HistoryViewModel> historyViewModelProvider;

    Provider<MapsViewModel> mapsViewModelProvider;

    Provider<SettingsViewModel> settingsViewModelProvider;

    Provider<SignInViewModel> signInViewModelProvider;

    ViewModelCImpl(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
        SavedStateHandle savedStateHandleParam, ViewModelLifecycle viewModelLifecycleParam) {
      this.singletonCImpl = singletonCImpl;
      this.activityRetainedCImpl = activityRetainedCImpl;

      initialize(savedStateHandleParam, viewModelLifecycleParam);

    }

    ImmutableMap hiltViewModelMapMapOfClassOfObjectAndProviderOfViewModelBuilder() {
      ImmutableMap.Builder mapBuilder = ImmutableMap.<String, javax.inject.Provider<ViewModel>>builderWithExpectedSize(6);
      mapBuilder.put(AuthGateViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (authGateViewModelProvider)));
      mapBuilder.put(FavoritesViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (favoritesViewModelProvider)));
      mapBuilder.put(HistoryViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (historyViewModelProvider)));
      mapBuilder.put(MapsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (mapsViewModelProvider)));
      mapBuilder.put(SettingsViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (settingsViewModelProvider)));
      mapBuilder.put(SignInViewModel_HiltModules_BindsModule_Binds_LazyMapKey.lazyClassKeyName, ((Provider) (signInViewModelProvider)));
      return mapBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandle savedStateHandleParam,
        final ViewModelLifecycle viewModelLifecycleParam) {
      this.authGateViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 0);
      this.favoritesViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 1);
      this.historyViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 2);
      this.mapsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 3);
      this.settingsViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 4);
      this.signInViewModelProvider = new SwitchingProvider<>(singletonCImpl, activityRetainedCImpl, viewModelCImpl, 5);
    }

    @Override
    public Map<Class<?>, javax.inject.Provider<ViewModel>> getHiltViewModelMap() {
      return LazyClassKeyMap.<javax.inject.Provider<ViewModel>>of(hiltViewModelMapMapOfClassOfObjectAndProviderOfViewModelBuilder());
    }

    @Override
    public Map<Class<?>, Object> getHiltViewModelAssistedMap() {
      return ImmutableMap.<Class<?>, Object>of();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final ViewModelCImpl viewModelCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          ViewModelCImpl viewModelCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.viewModelCImpl = viewModelCImpl;
        this.id = id;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T get() {
        switch (id) {
          case 0: // com.freemind.ai.ui.navigation.AuthGateViewModel
          return (T) new AuthGateViewModel(singletonCImpl.authRepositoryImplProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get());

          case 1: // com.freemind.ai.ui.favorites.FavoritesViewModel
          return (T) new FavoritesViewModel(singletonCImpl.authRepositoryImplProvider.get(), singletonCImpl.placesRepositoryImplProvider.get());

          case 2: // com.freemind.ai.ui.history.HistoryViewModel
          return (T) new HistoryViewModel(singletonCImpl.authRepositoryImplProvider.get(), singletonCImpl.placesRepositoryImplProvider.get());

          case 3: // com.freemind.ai.ui.maps.MapsViewModel
          return (T) new MapsViewModel(singletonCImpl.placesRepositoryImplProvider.get(), singletonCImpl.authRepositoryImplProvider.get(), singletonCImpl.settingsRepositoryImplProvider.get());

          case 4: // com.freemind.ai.ui.settings.SettingsViewModel
          return (T) new SettingsViewModel(singletonCImpl.settingsRepositoryImplProvider.get(), singletonCImpl.authRepositoryImplProvider.get(), singletonCImpl.supabaseRepositoryImplProvider.get(), singletonCImpl.placesRepositoryImplProvider.get(), singletonCImpl.exportServiceProvider.get());

          case 5: // com.freemind.ai.ui.auth.SignInViewModel
          return (T) new SignInViewModel(singletonCImpl.authRepositoryImplProvider.get(), singletonCImpl.supabaseRepositoryImplProvider.get(), singletonCImpl.roleStampClientProvider.get(), singletonCImpl.provideApplicationScopeProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ActivityRetainedCImpl extends FreeMindApplication_HiltComponents.ActivityRetainedC {
    private final SingletonCImpl singletonCImpl;

    private final ActivityRetainedCImpl activityRetainedCImpl = this;

    Provider<ActivityRetainedLifecycle> provideActivityRetainedLifecycleProvider;

    ActivityRetainedCImpl(SingletonCImpl singletonCImpl,
        SavedStateHandleHolder savedStateHandleHolderParam) {
      this.singletonCImpl = singletonCImpl;

      initialize(savedStateHandleHolderParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final SavedStateHandleHolder savedStateHandleHolderParam) {
      this.provideActivityRetainedLifecycleProvider = DoubleCheck.provider(new SwitchingProvider<ActivityRetainedLifecycle>(singletonCImpl, activityRetainedCImpl, 0));
    }

    @Override
    public ActivityComponentBuilder activityComponentBuilder() {
      return new ActivityCBuilder(singletonCImpl, activityRetainedCImpl);
    }

    @Override
    public ActivityRetainedLifecycle getActivityRetainedLifecycle() {
      return provideActivityRetainedLifecycleProvider.get();
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final ActivityRetainedCImpl activityRetainedCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, ActivityRetainedCImpl activityRetainedCImpl,
          int id) {
        this.singletonCImpl = singletonCImpl;
        this.activityRetainedCImpl = activityRetainedCImpl;
        this.id = id;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T get() {
        switch (id) {
          case 0: // dagger.hilt.android.ActivityRetainedLifecycle
          return (T) ActivityRetainedComponentManager_LifecycleModule_ProvideActivityRetainedLifecycleFactory.provideActivityRetainedLifecycle();

          default: throw new AssertionError(id);
        }
      }
    }
  }

  private static final class ServiceCImpl extends FreeMindApplication_HiltComponents.ServiceC {
    private final SingletonCImpl singletonCImpl;

    private final ServiceCImpl serviceCImpl = this;

    ServiceCImpl(SingletonCImpl singletonCImpl, Service serviceParam) {
      this.singletonCImpl = singletonCImpl;


    }

    @Override
    public void injectFreeMindFcmService(FreeMindFcmService arg0) {
      injectFreeMindFcmService2(arg0);
    }

    @CanIgnoreReturnValue
    private FreeMindFcmService injectFreeMindFcmService2(FreeMindFcmService instance) {
      FreeMindFcmService_MembersInjector.injectAuthRepository(instance, singletonCImpl.authRepositoryImplProvider.get());
      FreeMindFcmService_MembersInjector.injectSupabaseRepository(instance, singletonCImpl.supabaseRepositoryImplProvider.get());
      return instance;
    }
  }

  private static final class SingletonCImpl extends FreeMindApplication_HiltComponents.SingletonC {
    private final ApplicationContextModule applicationContextModule;

    private final SingletonCImpl singletonCImpl = this;

    Provider<OkHttpClient> provideOkHttpClientProvider;

    Provider<PlacesDataSource> placesDataSourceProvider;

    Provider<FirebaseAuth> provideFirebaseAuthProvider;

    Provider<FirebaseAuthDataSource> firebaseAuthDataSourceProvider;

    Provider<SupabaseClient> provideSupabaseClientProvider;

    Provider<PlacesSupabaseDataSource> placesSupabaseDataSourceProvider;

    Provider<DataStore<Preferences>> providePreferencesDataStoreProvider;

    Provider<PendingFavoriteQueue> pendingFavoriteQueueProvider;

    Provider<PlacesRepositoryImpl> placesRepositoryImplProvider;

    Provider<AuthRepositoryImpl> authRepositoryImplProvider;

    Provider<ConnectivityObserver> connectivityObserverProvider;

    Provider<CoroutineScope> provideApplicationScopeProvider;

    Provider<ProfileDataSource> profileDataSourceProvider;

    Provider<SupabaseRepositoryImpl> supabaseRepositoryImplProvider;

    Provider<SettingsRepositoryImpl> settingsRepositoryImplProvider;

    Provider<ExportService> exportServiceProvider;

    Provider<RoleStampClient> roleStampClientProvider;

    SingletonCImpl(ApplicationContextModule applicationContextModuleParam) {
      this.applicationContextModule = applicationContextModuleParam;
      initialize(applicationContextModuleParam);

    }

    @SuppressWarnings("unchecked")
    private void initialize(final ApplicationContextModule applicationContextModuleParam) {
      this.provideOkHttpClientProvider = DoubleCheck.provider(new SwitchingProvider<OkHttpClient>(singletonCImpl, 2));
      this.placesDataSourceProvider = DoubleCheck.provider(new SwitchingProvider<PlacesDataSource>(singletonCImpl, 1));
      this.provideFirebaseAuthProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuth>(singletonCImpl, 6));
      this.firebaseAuthDataSourceProvider = DoubleCheck.provider(new SwitchingProvider<FirebaseAuthDataSource>(singletonCImpl, 5));
      this.provideSupabaseClientProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseClient>(singletonCImpl, 4));
      this.placesSupabaseDataSourceProvider = DoubleCheck.provider(new SwitchingProvider<PlacesSupabaseDataSource>(singletonCImpl, 3));
      this.providePreferencesDataStoreProvider = DoubleCheck.provider(new SwitchingProvider<DataStore<Preferences>>(singletonCImpl, 8));
      this.pendingFavoriteQueueProvider = DoubleCheck.provider(new SwitchingProvider<PendingFavoriteQueue>(singletonCImpl, 7));
      this.placesRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<PlacesRepositoryImpl>(singletonCImpl, 0));
      this.authRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<AuthRepositoryImpl>(singletonCImpl, 9));
      this.connectivityObserverProvider = DoubleCheck.provider(new SwitchingProvider<ConnectivityObserver>(singletonCImpl, 10));
      this.provideApplicationScopeProvider = DoubleCheck.provider(new SwitchingProvider<CoroutineScope>(singletonCImpl, 11));
      this.profileDataSourceProvider = DoubleCheck.provider(new SwitchingProvider<ProfileDataSource>(singletonCImpl, 14));
      this.supabaseRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SupabaseRepositoryImpl>(singletonCImpl, 13));
      this.settingsRepositoryImplProvider = DoubleCheck.provider(new SwitchingProvider<SettingsRepositoryImpl>(singletonCImpl, 12));
      this.exportServiceProvider = DoubleCheck.provider(new SwitchingProvider<ExportService>(singletonCImpl, 15));
      this.roleStampClientProvider = DoubleCheck.provider(new SwitchingProvider<RoleStampClient>(singletonCImpl, 16));
    }

    @Override
    public void injectFreeMindApplication(FreeMindApplication freeMindApplication) {
      injectFreeMindApplication2(freeMindApplication);
    }

    @Override
    public Set<Boolean> getDisableFragmentGetContextFix() {
      return ImmutableSet.<Boolean>of();
    }

    @Override
    public ActivityRetainedComponentBuilder retainedComponentBuilder() {
      return new ActivityRetainedCBuilder(singletonCImpl);
    }

    @Override
    public ServiceComponentBuilder serviceComponentBuilder() {
      return new ServiceCBuilder(singletonCImpl);
    }

    @CanIgnoreReturnValue
    private FreeMindApplication injectFreeMindApplication2(FreeMindApplication instance) {
      FreeMindApplication_MembersInjector.injectPlacesRepository(instance, placesRepositoryImplProvider.get());
      FreeMindApplication_MembersInjector.injectAuthRepository(instance, authRepositoryImplProvider.get());
      FreeMindApplication_MembersInjector.injectConnectivityObserver(instance, connectivityObserverProvider.get());
      FreeMindApplication_MembersInjector.injectApplicationScope(instance, provideApplicationScopeProvider.get());
      return instance;
    }

    private static final class SwitchingProvider<T> implements Provider<T> {
      private final SingletonCImpl singletonCImpl;

      private final int id;

      SwitchingProvider(SingletonCImpl singletonCImpl, int id) {
        this.singletonCImpl = singletonCImpl;
        this.id = id;
      }

      @Override
      @SuppressWarnings("unchecked")
      public T get() {
        switch (id) {
          case 0: // com.freemind.ai.data.repository.PlacesRepositoryImpl
          return (T) new PlacesRepositoryImpl(singletonCImpl.placesDataSourceProvider.get(), singletonCImpl.placesSupabaseDataSourceProvider.get(), singletonCImpl.pendingFavoriteQueueProvider.get());

          case 1: // com.freemind.ai.data.remote.places.PlacesDataSource
          return (T) new PlacesDataSource(singletonCImpl.provideOkHttpClientProvider.get(), ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 2: // okhttp3.OkHttpClient
          return (T) NetworkModule_ProvideOkHttpClientFactory.provideOkHttpClient();

          case 3: // com.freemind.ai.data.remote.supabase.PlacesSupabaseDataSource
          return (T) new PlacesSupabaseDataSource(singletonCImpl.provideSupabaseClientProvider.get());

          case 4: // io.github.jan.supabase.SupabaseClient
          return (T) SupabaseModule_ProvideSupabaseClientFactory.provideSupabaseClient(singletonCImpl.firebaseAuthDataSourceProvider.get());

          case 5: // com.freemind.ai.data.remote.firebase.FirebaseAuthDataSource
          return (T) new FirebaseAuthDataSource(singletonCImpl.provideFirebaseAuthProvider.get());

          case 6: // com.google.firebase.auth.FirebaseAuth
          return (T) FirebaseModule_ProvideFirebaseAuthFactory.provideFirebaseAuth();

          case 7: // com.freemind.ai.data.local.PendingFavoriteQueue
          return (T) new PendingFavoriteQueue(singletonCImpl.providePreferencesDataStoreProvider.get());

          case 8: // androidx.datastore.core.DataStore<androidx.datastore.preferences.core.Preferences>
          return (T) DataStoreModule_ProvidePreferencesDataStoreFactory.providePreferencesDataStore(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 9: // com.freemind.ai.data.repository.AuthRepositoryImpl
          return (T) new AuthRepositoryImpl(singletonCImpl.firebaseAuthDataSourceProvider.get());

          case 10: // com.freemind.ai.util.ConnectivityObserver
          return (T) new ConnectivityObserver(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 11: // @com.freemind.ai.di.ApplicationScope kotlinx.coroutines.CoroutineScope
          return (T) CoroutineScopeModule_ProvideApplicationScopeFactory.provideApplicationScope();

          case 12: // com.freemind.ai.data.repository.SettingsRepositoryImpl
          return (T) new SettingsRepositoryImpl(singletonCImpl.providePreferencesDataStoreProvider.get(), singletonCImpl.authRepositoryImplProvider.get(), singletonCImpl.supabaseRepositoryImplProvider.get());

          case 13: // com.freemind.ai.data.repository.SupabaseRepositoryImpl
          return (T) new SupabaseRepositoryImpl(singletonCImpl.profileDataSourceProvider.get());

          case 14: // com.freemind.ai.data.remote.supabase.ProfileDataSource
          return (T) new ProfileDataSource(singletonCImpl.provideSupabaseClientProvider.get());

          case 15: // com.freemind.ai.data.export.ExportService
          return (T) new ExportService(ApplicationContextModule_ProvideContextFactory.provideContext(singletonCImpl.applicationContextModule));

          case 16: // com.freemind.ai.data.remote.firebase.RoleStampClient
          return (T) new RoleStampClient(singletonCImpl.provideOkHttpClientProvider.get(), singletonCImpl.firebaseAuthDataSourceProvider.get());

          default: throw new AssertionError(id);
        }
      }
    }
  }
}
