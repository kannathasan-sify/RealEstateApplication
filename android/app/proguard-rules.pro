-keep class com.realestate.app.data.models.** { *; }
-keep interface com.realestate.app.data.api.ApiService { *; }
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn okhttp3.**
-dontwarn retrofit2.**

# Hilt
-keep class com.google.dagger.hilt.** { *; }
-keep class dagger.hilt.** { *; }
-keep @dagger.hilt.android.HiltAndroidApp class *
-keep @dagger.hilt.android.AndroidEntryPoint class *
-keep class * implements dagger.hilt.components.SingletonComponent
-keep class * extends androidx.lifecycle.ViewModel

# DataStore
-keep class androidx.datastore.** { *; }
-keep class com.realestate.app.data.local.DataStoreManager { *; }

# Kotlin Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.coroutines.android.HandlerContext$Main {
    public <init>(android.os.Handler, java.lang.String, boolean);
}

