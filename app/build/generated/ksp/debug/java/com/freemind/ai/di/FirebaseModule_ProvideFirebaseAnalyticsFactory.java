package com.freemind.ai.di;

import com.google.firebase.analytics.FirebaseAnalytics;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Preconditions;
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
public final class FirebaseModule_ProvideFirebaseAnalyticsFactory implements Factory<FirebaseAnalytics> {
  @Override
  public FirebaseAnalytics get() {
    return provideFirebaseAnalytics();
  }

  public static FirebaseModule_ProvideFirebaseAnalyticsFactory create() {
    return InstanceHolder.INSTANCE;
  }

  public static FirebaseAnalytics provideFirebaseAnalytics() {
    return Preconditions.checkNotNullFromProvides(FirebaseModule.INSTANCE.provideFirebaseAnalytics());
  }

  private static final class InstanceHolder {
    static final FirebaseModule_ProvideFirebaseAnalyticsFactory INSTANCE = new FirebaseModule_ProvideFirebaseAnalyticsFactory();
  }
}
