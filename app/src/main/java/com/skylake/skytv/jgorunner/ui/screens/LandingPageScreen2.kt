package com.skylake.skytv.jgorunner.ui.screens

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@SuppressLint("NewApi")

@Composable
fun LandingPageScreen2() {
        Column(
            modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                Text(
                    text = "JTV-GO",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onBackground,
                )
            }
            LoadingIndicatorsPreview()
        }
    }

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Preview(showBackground = true)
@Composable
fun LoadingIndicatorsPreview() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LoadingIndicator()

        Spacer(Modifier.height(32.dp))

        LoadingIndicator(
            polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.take(2),
            color = Color.Red
        )

        Spacer(Modifier.height(32.dp))

        ContainedLoadingIndicator()

        Spacer(Modifier.height(32.dp))

        ContainedLoadingIndicator(
            containerColor = Color.Cyan
        )
    }
}
