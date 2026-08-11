package com.ble.signal.analyzer

import org.junit.Assert.assertEquals
import org.junit.Test

class InformationNavigationTest {
    @Test
    fun nestedInformationPages_returnInLogicalOrder() {
        val about = AppUiState(destination = AppDestination.Settings)
            .openInformationDestination(AppDestination.About)
        val privacy = about.openInformationDestination(AppDestination.PrivacyPolicy)

        val backToAbout = privacy.navigateBackFromInformation()
        val backToSettings = backToAbout.navigateBackFromInformation()

        assertEquals(AppDestination.About, backToAbout.destination)
        assertEquals(AppDestination.Settings, backToSettings.destination)
        assertEquals(emptyList<AppDestination>(), backToSettings.informationBackStack)
    }

    @Test
    fun emptyInformationHistory_fallsBackToSettings() {
        val result = AppUiState(destination = AppDestination.HowBleSignalsWork)
            .navigateBackFromInformation()

        assertEquals(AppDestination.Settings, result.destination)
    }
}
