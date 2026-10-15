// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
}

// OneDrive locks/denies deletes in synced folders, which breaks Gradle's build outputs
// (AccessDeniedException / "Unable to delete directory"). When the project lives inside OneDrive,
// put build directories in %LOCALAPPDATA% instead, which is not synced.
val localAppData: String? = System.getenv("LOCALAPPDATA")
if (localAppData != null && rootDir.absolutePath.contains("OneDrive", ignoreCase = true)) {
    val buildRoot = File(localAppData, "GradleBuilds/${rootProject.name}")
    allprojects {
        layout.buildDirectory.set(File(buildRoot, if (this == rootProject) "root" else name))
    }
}
