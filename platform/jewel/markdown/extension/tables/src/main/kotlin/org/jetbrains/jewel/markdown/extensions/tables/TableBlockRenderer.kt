package org.jetbrains.jewel.markdown.extensions.tables

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.jetbrains.jewel.markdown.MarkdownBlock.CustomBlock
import org.jetbrains.jewel.markdown.extensions.MarkdownBlockRendererExtension
import org.jetbrains.jewel.markdown.rendering.InlineMarkdownRenderer
import org.jetbrains.jewel.markdown.rendering.MarkdownBlockRenderer
import org.jetbrains.jewel.markdown.rendering.MarkdownStyling
import org.jetbrains.jewel.ui.component.Text

public class TableBlockRenderer(
    private val styling: TableStyling,
    private val rootStyling: MarkdownStyling,
) : MarkdownBlockRendererExtension {

    override fun canRender(block: CustomBlock): Boolean =
        block is Table.TableBlock

    @Composable
    override fun render(
        block: CustomBlock,
        blockRenderer: MarkdownBlockRenderer,
        inlineRenderer: InlineMarkdownRenderer,
        enabled: Boolean,
        onUrlClick: (String) -> Unit,
        onTextClick: () -> Unit,
    ) {
        block as Table.TableBlock
        Column(Modifier.fillMaxSize().padding(16.dp)) {
            Row(Modifier.background(styling.headColor)) {
                block.header.headerRow.cells.forEach {
                    TableHeaderCells(it, inlineRenderer)
                }
            }
            block.body?.rows?.forEachIndexed { index, row ->
                Row(Modifier.background(styling.headColor)) {
                    row.cells.forEach {
                        TableCells(it, inlineRenderer, index % 2 == 0)
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.TableHeaderCells(
        cell: Table.TableCell,
        inlineRenderer: InlineMarkdownRenderer,
    ) {
        Text(
            text = inlineRenderer.renderAsAnnotatedString(
                inlineMarkdown = cell.inline,
                enabled = false,
                styling = rootStyling.paragraph.inlinesStyling,
                onUrlClicked = null
            ),
            Modifier
                .border(1.dp, styling.borderColor)
                .weight(1.0f)
                .padding(8.dp),
            fontWeight = FontWeight.Bold // headers are bold
        )
    }

    @Composable
    private fun RowScope.TableCells(
        cell: Table.TableCell,
        inlineRenderer: InlineMarkdownRenderer,
        everyOther: Boolean
    ) {
        val background = if (everyOther) Color.Gray else Color.White
        Text(
            text = inlineRenderer.renderAsAnnotatedString(
                inlineMarkdown = cell.inline,
                enabled = false,
                styling = rootStyling.paragraph.inlinesStyling,
                onUrlClicked = null
            ),
            Modifier
                .border(1.dp, styling.borderColor)
                .weight(1.0f)
                .padding(8.dp)
                .background(background)
        )
    }
}
