

package org.meshtastic.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.ApplicationProductFlavor
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.ProductFlavor

@Suppress("EnumEntryName")
enum class FlavorDimension {
    marketplace
}

@Suppress("EnumEntryName")
enum class MeshtasticFlavor(val dimension: FlavorDimension, val default: Boolean = false) {
    google(FlavorDimension.marketplace, default = true),
}

fun configureFlavors(
    commonExtension: CommonExtension,
    flavorConfigurationBlock: ProductFlavor.(flavor: MeshtasticFlavor) -> Unit = {},
) {
    (commonExtension as? ApplicationExtension)?.apply {
        FlavorDimension.entries.forEach { flavorDimension ->
            flavorDimensions += flavorDimension.name
        }

        productFlavors {
            MeshtasticFlavor.entries.forEach { meshtasticFlavor ->
                register(meshtasticFlavor.name) {
                    dimension = meshtasticFlavor.dimension.name
                    flavorConfigurationBlock(this, meshtasticFlavor)
                    if (meshtasticFlavor.default) {
                        isDefault = true
                    }
                }
            }
        }
    }
    (commonExtension as? LibraryExtension)?.apply {
        FlavorDimension.entries.forEach { flavorDimension ->
            flavorDimensions += flavorDimension.name
        }

        productFlavors {
            MeshtasticFlavor.entries.forEach { meshtasticFlavor ->
                register(meshtasticFlavor.name) {
                    dimension = meshtasticFlavor.dimension.name
                    flavorConfigurationBlock(this, meshtasticFlavor)
                    if (this@apply is ApplicationExtension && this is ApplicationProductFlavor) {
                        if (meshtasticFlavor.default) {
                            isDefault = true
                        }
                    }
                }
            }
        }
    }
}

