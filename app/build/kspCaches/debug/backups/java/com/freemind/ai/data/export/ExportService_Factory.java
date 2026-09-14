package com.freemind.ai.data.export;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.Provider;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;

@ScopeMetadata("javax.inject.Singleton")
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
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
public final class ExportService_Factory implements Factory<ExportService> {
  private final Provider<Context> contextProvider;

  private ExportService_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public ExportService get() {
    return newInstance(contextProvider.get());
  }

  public static ExportService_Factory create(Provider<Context> contextProvider) {
    return new ExportService_Factory(contextProvider);
  }

  public static ExportService newInstance(Context context) {
    return new ExportService(context);
  }
}
