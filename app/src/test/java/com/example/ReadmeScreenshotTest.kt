package com.example

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class ReadmeScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun capture_app_dashboard() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        Scaffold(
          modifier = Modifier.fillMaxSize(),
          topBar = {
            CenterAlignedTopAppBar(
              title = {
                Text(
                  "VV Pixel Enhancer",
                  fontWeight = FontWeight.Bold,
                  fontFamily = FontFamily.SansSerif
                )
              },
              colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp)
              )
            )
          }
        ) { innerPadding ->
          DashboardScreen(
            modifier = Modifier.padding(innerPadding),
            sensorX = 2.4f,
            sensorY = 0.3f,
            sensorZ = 9.8f
          )
        }
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "../docs/screenshots/app_dashboard.png")
  }

  @Test
  fun capture_volume_overlay() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        VolumeBarOverlayScreen(
          currentVolume = 10,
          maxVolume = 15,
          isMuted = false,
          onVolumeChange = {},
          onMuteToggle = {},
          onDismiss = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "../docs/screenshots/app_volume_overlay.png")
  }

  @Test
  fun capture_ringer_dialog() {
    composeTestRule.setContent {
      MyApplicationTheme(darkTheme = true, dynamicColor = false) {
        RingerModeDialogScreen(onDismiss = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "../docs/screenshots/app_ringer_dialog.png")
  }
}
