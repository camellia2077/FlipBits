package com.bag.audioandroid.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.bag.audioandroid.ui.component.ActionButton

@Composable
fun AudioTabScreen(
    inputText: String,
    onInputTextChange: (String) -> Unit,
    generatedPcm: ShortArray,
    resultText: String,
    statusText: String,
    isPlaying: Boolean,
    playbackProgress: Float,
    onEncode: () -> Unit,
    onPlay: () -> Unit,
    onDecode: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Audio",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        OutlinedTextField(
            value = inputText,
            onValueChange = onInputTextChange,
            label = { Text("输入文本") },
            modifier = Modifier.fillMaxWidth()
        )

        ActionButton("文本转音频", onEncode)
        ActionButton("播放音频", onPlay)
        ActionButton("解析生成音频", onDecode)
        ActionButton("清空", onClear)

        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 2.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(148.dp)
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text("状态: $statusText", style = MaterialTheme.typography.bodyMedium)
                Text(
                    "播放进度: ${(playbackProgress * 100).toInt()}%${if (isPlaying) " (播放中)" else ""}",
                    style = MaterialTheme.typography.bodySmall
                )
                LinearProgressIndicator(
                    progress = { playbackProgress.coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth()
                )
                Text("样本数: ${generatedPcm.size}", style = MaterialTheme.typography.bodySmall)
                Text("解析结果: $resultText", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
