package com.example.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MarkdownViewer(
    markdownText: String,
    modifier: Modifier = Modifier,
    textColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val blocks = remember(markdownText) { parseMarkdownBlocks(markdownText) }

    Column(modifier = modifier.animateContentSize()) {
        for (block in blocks) {
            when (block) {
                is MdBlock.Heading -> {
                    val fontSize = when (block.level) {
                        1 -> 22.sp
                        2 -> 19.sp
                        3 -> 17.sp
                        else -> 15.sp
                    }
                    Text(
                        text = block.text,
                        fontSize = fontSize,
                        fontWeight = FontWeight.Bold,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                is MdBlock.Paragraph -> {
                    Text(
                        text = formatInlineMarkdown(block.text, textColor),
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = textColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
                is MdBlock.CodeFence -> {
                    CodeBlock(
                        language = block.language,
                        code = block.code
                    )
                }
                is MdBlock.BulletItem -> {
                    Row(
                        modifier = Modifier.padding(vertical = 2.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(
                            text = "•",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Text(
                            text = formatInlineMarkdown(block.text, textColor),
                            fontSize = 15.sp,
                            lineHeight = 22.sp,
                            color = textColor
                        )
                    }
                }
                is MdBlock.Table -> {
                    TableBlock(rows = block.rows)
                }
            }
        }
    }
}

@Composable
fun CodeBlock(
    language: String,
    code: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = language.ifBlank { "code" },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(code))
                        copied = true
                        scope.launch {
                            delay(2000)
                            copied = false
                        }
                    },
                    modifier = Modifier.height(32.dp)
                ) {
                    Icon(
                        imageVector = if (copied) Icons.Default.Check else Icons.Default.ContentCopy,
                        contentDescription = "Copy code",
                        tint = if (copied) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(12.dp)
            ) {
                Text(
                    text = code,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
fun TableBlock(rows: List<List<String>>) {
    if (rows.isEmpty()) return

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(8.dp)
        ) {
            rows.forEachIndexed { rowIndex, row ->
                val isHeader = rowIndex == 0
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isHeader) MaterialTheme.colorScheme.surfaceVariant
                            else Color.Transparent
                        )
                        .padding(vertical = 6.dp, horizontal = 4.dp)
                ) {
                    row.forEach { cell ->
                        Text(
                            text = cell,
                            fontWeight = if (isHeader) FontWeight.Bold else FontWeight.Normal,
                            fontSize = 13.sp,
                            modifier = Modifier
                                .width(120.dp)
                                .padding(horizontal = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

sealed interface MdBlock {
    data class Heading(val level: Int, val text: String) : MdBlock
    data class Paragraph(val text: String) : MdBlock
    data class CodeFence(val language: String, val code: String) : MdBlock
    data class BulletItem(val text: String) : MdBlock
    data class Table(val rows: List<List<String>>) : MdBlock
}

fun parseMarkdownBlocks(raw: String): List<MdBlock> {
    val lines = raw.lines()
    val blocks = mutableListOf<MdBlock>()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // Code block
        if (line.trimStart().startsWith("```")) {
            val lang = line.trimStart().removePrefix("```").trim()
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            blocks.add(MdBlock.CodeFence(language = lang, code = codeLines.joinToString("\n")))
            i++
            continue
        }

        // Table detection
        if (line.trim().startsWith("|") && line.trim().endsWith("|")) {
            val tableRows = mutableListOf<List<String>>()
            while (i < lines.size && lines[i].trim().startsWith("|")) {
                val rowLine = lines[i].trim()
                if (!rowLine.contains("---")) {
                    val cells = rowLine.split("|")
                        .map { it.trim() }
                        .filterIndexed { idx, _ -> idx > 0 && idx < rowLine.split("|").size - 1 }
                    if (cells.isNotEmpty()) {
                        tableRows.add(cells)
                    }
                }
                i++
            }
            if (tableRows.isNotEmpty()) {
                blocks.add(MdBlock.Table(tableRows))
            }
            continue
        }

        // Headings
        if (line.startsWith("#")) {
            val level = line.takeWhile { it == '#' }.length
            val headingText = line.drop(level).trim()
            blocks.add(MdBlock.Heading(level, headingText))
            i++
            continue
        }

        // Bullet items
        if (line.trimStart().startsWith("- ") || line.trimStart().startsWith("* ")) {
            val bulletText = line.trimStart().drop(2).trim()
            blocks.add(MdBlock.BulletItem(bulletText))
            i++
            continue
        }

        // Empty line
        if (line.isBlank()) {
            i++
            continue
        }

        // Normal paragraph
        val paragraphLines = mutableListOf(line)
        i++
        while (i < lines.size && lines[i].isNotBlank() && !lines[i].startsWith("#") &&
            !lines[i].trimStart().startsWith("```") && !lines[i].trimStart().startsWith("- ") &&
            !lines[i].trimStart().startsWith("* ") && !lines[i].trim().startsWith("|")
        ) {
            paragraphLines.add(lines[i])
            i++
        }
        blocks.add(MdBlock.Paragraph(paragraphLines.joinToString(" ")))
    }

    return blocks
}

fun formatInlineMarkdown(text: String, defaultColor: Color): AnnotatedString {
    return buildAnnotatedString {
        var cursor = 0
        val len = text.length

        while (cursor < len) {
            // Bold **text**
            if (text.startsWith("**", cursor) && text.indexOf("**", cursor + 2) != -1) {
                val end = text.indexOf("**", cursor + 2)
                val boldContent = text.substring(cursor + 2, end)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                append(boldContent)
                pop()
                cursor = end + 2
                continue
            }

            // Inline code `code`
            if (text.startsWith("`", cursor) && text.indexOf("`", cursor + 1) != -1) {
                val end = text.indexOf("`", cursor + 1)
                val codeContent = text.substring(cursor + 1, end)
                pushStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = Color(0x33888888),
                        fontSize = 13.sp
                    )
                )
                append(codeContent)
                pop()
                cursor = end + 1
                continue
            }

            // Italic *text*
            if (text.startsWith("*", cursor) && text.indexOf("*", cursor + 1) != -1) {
                val end = text.indexOf("*", cursor + 1)
                val italicContent = text.substring(cursor + 1, end)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                append(italicContent)
                pop()
                cursor = end + 1
                continue
            }

            append(text[cursor])
            cursor++
        }
    }
}
