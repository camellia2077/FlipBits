package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.model.PaletteOption
import com.bag.audioandroid.ui.theme.MaterialPalettes

@Composable
fun ConfigTabScreen(
    selectedPalette: PaletteOption,
    onPaletteSelected: (PaletteOption) -> Unit,
    onOpenAboutPage: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Config",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = "颜色选择（Material3）",
            style = MaterialTheme.typography.bodyMedium
        )

        MaterialPalettes.forEach { option ->
            val selected = option.id == selectedPalette.id
            Surface(
                tonalElevation = if (selected) 6.dp else 1.dp,
                shadowElevation = if (selected) 2.dp else 0.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPaletteSelected(option) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(0.86f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(option.previewColor, CircleShape)
                        )
                        Column(
                            modifier = Modifier.padding(start = 12.dp)
                        ) {
                            Text(option.title, fontWeight = FontWeight.Medium)
                            Text(option.subtitle, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    if (selected) {
                        Text("已选", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenAboutPage() }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("About", fontWeight = FontWeight.SemiBold)
                    Text("作者、项目链接与开源许可证入口", style = MaterialTheme.typography.bodySmall)
                }
                Text(">", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}
