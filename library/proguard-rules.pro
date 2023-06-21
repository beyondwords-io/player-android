##---------------Begin: proguard configuration for Gson  ----------
# Gson uses generic type information stored in a class file when working with fields. Proguard
# removes such information by default, so configure it to keep all of it.
-keepattributes Signature

# For using GSON @Expose annotation
-keepattributes *Annotation*

# Gson specific classes
-dontwarn sun.misc.**
#-keep class com.google.gson.stream.** { *; }

# Prevent proguard from stripping interface information from TypeAdapter, TypeAdapterFactory,
# JsonSerializer, JsonDeserializer instances (so they can be used in @JsonAdapter)
-keep class * extends com.google.gson.TypeAdapter
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Prevent R8 from leaving Data object members always null
-keepclassmembers,allowobfuscation class * {
  @com.google.gson.annotations.SerializedName <fields>;
}

# Retain generic signatures of TypeToken and its subclasses with R8 version 3.0 and higher.
-keep,allowobfuscation,allowshrinking class com.google.gson.reflect.TypeToken
-keep,allowobfuscation,allowshrinking class * extends com.google.gson.reflect.TypeToken

##---------------End: proguard configuration for Gson  ----------

-keep class io.beyondwords.player.PlayerEvent { *; }
-keepclassmembers class io.beyondwords.player.PlayerEvent { *; }

-keep class io.beyondwords.player.PlayerSettings { *; }
-keepclassmembers class io.beyondwords.player.PlayerSettings { *; }

-keep class io.beyondwords.player.PlayerSettings$Identifier { *; }
-keepclassmembers class io.beyondwords.player.PlayerSettings$Identifier { *; }

-keep class io.beyondwords.player.PlayerSettings$Media { *; }
-keepclassmembers class io.beyondwords.player.PlayerSettings$Media { *; }

-keep class io.beyondwords.player.PlayerSettings$Segment { *; }
-keepclassmembers class io.beyondwords.player.PlayerSettings$Segment { *; }

-keep class io.beyondwords.player.PlayerSettings$Content { *; }
-keepclassmembers class io.beyondwords.player.PlayerSettings$Content { *; }

-keep class io.beyondwords.player.PlayerSettings$IntroOutro { *; }
-keepclassmembers class io.beyondwords.player.PlayerSettings$IntroOutro { *; }

-keep class io.beyondwords.player.PlayerSettings$Advert { *; }
-keepclassmembers class io.beyondwords.player.PlayerSettings$Advert { *; }

-keep class io.beyondwords.player.MediaSession$SeekToParams { *; }
-keepclassmembers class io.beyondwords.player.MediaSession$SeekToParams { *; }
