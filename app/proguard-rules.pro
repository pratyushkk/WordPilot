# WordPilot ProGuard Rules

# Keep Room entities
-keep class com.wordpilot.assistant.data.database.** { *; }

# Keep Correction model
-keep class com.wordpilot.assistant.domain.models.** { *; }

# Keep engine interfaces
-keep interface com.wordpilot.assistant.engine.** { *; }
