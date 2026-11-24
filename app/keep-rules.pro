# WorkManager (pulled in by Glance) builds its Room database by reflection on the no-arg constructor of
# WorkDatabase_Impl. Room's bundled rule keeps the class but, under R8 full mode, not its constructor,
# so the release build crashed at startup in androidx.startup.InitializationProvider.
-keep class * extends androidx.room.RoomDatabase { <init>(); }
