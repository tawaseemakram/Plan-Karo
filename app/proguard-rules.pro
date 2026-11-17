# Keep source file and line numbers for readable crash stack traces
-keepattributes SourceFile,LineNumberTable,Signature,*Annotation*,InnerClasses,EnclosingMethod
-renamesourcefileattribute SourceFile

# Entry points & Android components
-keep class com.example.MainActivity { *; }
-keep class com.example.ui.alarm.TaskAlarmActivity { *; }
-keep class com.example.receiver.TaskReminderReceiver { *; }
-keep class com.example.service.TaskAlarmService { *; }

# Keep data model classes used by Room
-keep class com.example.data.model.** { *; }
-keep class com.example.data.local.** { *; }

# Room generated implementations & TypeConverters
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-keepclassmembers class * {
    @androidx.room.TypeConverter <methods>;
}

# Google Play Services Ads & UMP Consent SDK
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }
-keep class com.google.android.ump.** { *; }

# Silence harmless warnings for optional compile-time annotations
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**
