package com.freemind.ai.data.remote.firebase;

import com.google.firebase.auth.FirebaseAuth;
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
public final class FirebaseAuthDataSource_Factory implements Factory<FirebaseAuthDataSource> {
  private final Provider<FirebaseAuth> firebaseAuthProvider;

  private FirebaseAuthDataSource_Factory(Provider<FirebaseAuth> firebaseAuthProvider) {
    this.firebaseAuthProvider = firebaseAuthProvider;
  }

  @Override
  public FirebaseAuthDataSource get() {
    return newInstance(firebaseAuthProvider.get());
  }

  public static FirebaseAuthDataSource_Factory create(Provider<FirebaseAuth> firebaseAuthProvider) {
    return new FirebaseAuthDataSource_Factory(firebaseAuthProvider);
  }

  public static FirebaseAuthDataSource newInstance(FirebaseAuth firebaseAuth) {
    return new FirebaseAuthDataSource(firebaseAuth);
  }
}
