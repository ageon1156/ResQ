
import com.android.build.api.dsl.LibraryExtension

plugins { alias(libs.plugins.meshtastic.android.library) }

configure<LibraryExtension> {
    buildFeatures { aidl = true }
    namespace = "org.meshtastic.core.service"

    testOptions { unitTests.isReturnDefaultValues = true }
}

dependencies {
    implementation(projects.core.database)
    implementation(projects.core.model)
    implementation(projects.core.proto)
    implementation(libs.javax.inject)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kermit)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

