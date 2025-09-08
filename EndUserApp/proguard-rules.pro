########################################
# FIREBASE / FIRESTORE
########################################

# Keep Firestore model classes and their fields
-keepclassmembers class com.sowp.user.models.** {
    <fields>;
    <init>();
}
-keep class com.sowp.user.models.** { *; }

# Keep Firebase annotations
-keepattributes *Annotation*

# Keep Firestore classes
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**

########################################
# GSON / JSON (if you use it for parsing)
########################################
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**



########################################
# GENERAL ANDROIDX
########################################
-keep class androidx.** { *; }
-dontwarn androidx.**

########################################
# RETROFIT / OKHTTP (if used)
########################################
-keep class retrofit2.** { *; }
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}
-dontwarn retrofit2.**
-dontwarn okhttp3.**

