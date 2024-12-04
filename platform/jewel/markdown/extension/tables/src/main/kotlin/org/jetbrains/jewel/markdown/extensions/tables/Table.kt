package org.jetbrains.jewel.markdown.extensions.tables

import org.jetbrains.jewel.markdown.InlineMarkdown
import org.jetbrains.jewel.markdown.MarkdownBlock

public sealed interface Table : MarkdownBlock.CustomBlock {

    public data class TableBlock(val header: TableHead, val body: TableBody?) : Table

    public data class TableHead(val headerRow: TableRow)

    public data class TableBody(val rows: List<TableRow>)

    public data class TableRow(val cells: List<TableCell>)

    public data class TableCell(
        val inline: List<InlineMarkdown> = listOf(),
        val isHeader: Boolean = false,
        val alignment: Alignment = Alignment.LEFT,
    )

    public enum class Alignment {
        LEFT,
        CENTER,
        RIGHT
    }
}