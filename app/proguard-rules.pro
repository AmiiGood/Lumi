# Hilt
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.HiltAndroidApp
-keepclasseswithmembers class * { @dagger.hilt.android.AndroidEntryPoint <fields>; }
-keepclasseswithmembers class * { @javax.inject.Inject <init>(...); }

# Room
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class *
-keep @androidx.room.Dao class *
-dontwarn androidx.room.paging.**

# Junrar (CBR)
-keep class com.github.junrar.** { *; }
-dontwarn com.github.junrar.**
-dontwarn java.beans.**

# Coroutines
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}

# Compose
-keep class androidx.compose.runtime.** { *; }
-keepnames class * extends androidx.compose.runtime.Composer

# Conservar nuestras data classes (DataStore + Room)
-keep class io.github.amiigood.lumi.data.model.** { *; }
-keep class io.github.amiigood.lumi.data.local.** { *; }

-dontwarn org.slf4j.impl.StaticLoggerBinder