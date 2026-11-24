# Room and WorkManager (pulled in by Glance) create these classes by reflection. Their bundled rules keep the
# classes, but R8 full mode then drops the constructors, and release builds crashed at startup (WorkDatabase_Impl)
# or left widgets loading forever (OverwritingInputMerger).
-keep class * extends androidx.room.RoomDatabase { <init>(); }
-keep class * extends androidx.work.InputMerger { <init>(); }
-keep class * extends androidx.work.ListenableWorker { <init>(android.content.Context, androidx.work.WorkerParameters); }
