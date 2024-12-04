package org.jetbrains.jewel.markdown.extensions.tables

import org.commonmark.ext.gfm.tables.TablesExtension
import org.commonmark.node.CustomBlock
import org.commonmark.parser.Parser.ParserExtension
import org.jetbrains.jewel.markdown.MarkdownBlock
import org.jetbrains.jewel.markdown.extensions.MarkdownBlockProcessorExtension
import org.jetbrains.jewel.markdown.extensions.MarkdownProcessorExtension
import org.jetbrains.jewel.markdown.processing.MarkdownProcessor
import org.jetbrains.jewel.markdown.processing.readInlineContent
import org.commonmark.ext.gfm.tables.TableBlock as CMTableBlock
import org.commonmark.ext.gfm.tables.TableBody as CMTableBody
import org.commonmark.ext.gfm.tables.TableCell as CMTableCell
import org.commonmark.ext.gfm.tables.TableHead as CMTableHead
import org.commonmark.ext.gfm.tables.TableRow as CMTableRow

public object TableProcessorExtension : MarkdownProcessorExtension {
    private val tableExtension = TablesExtension.create()
    override val parserExtension: ParserExtension = tableExtension as ParserExtension
    override val blockProcessorExtension: MarkdownBlockProcessorExtension = TableBlockProcessor

    private object TableBlockProcessor : MarkdownBlockProcessorExtension {
        override fun canProcess(block: CustomBlock): Boolean =
            block is CMTableBlock

        override fun processMarkdownBlock(
            block: CustomBlock,
            processor: MarkdownProcessor,
        ): MarkdownBlock.CustomBlock {
            if (block !is CMTableBlock)
                error("Unsupported block of type ${block.javaClass.name} cannot be processed")
            return processTableBlock(block, processor)
        }

        fun processTableBlock(table: CMTableBlock, processor: MarkdownProcessor): Table.TableBlock {
            val head = (table.firstChild as CMTableHead).getTableHeader(processor)
            val body = getTableBody(table.firstChild.next as? CMTableBody, processor)
            return Table.TableBlock(head, body)
        }

        fun CMTableHead.getTableHeader(processor: MarkdownProcessor): Table.TableHead =
            Table.TableHead(getTableRow(processor))

        fun getTableBody(tableBody: CMTableBody?, processor: MarkdownProcessor): Table.TableBody? {
            return if (tableBody != null) {
                return Table.TableBody(tableBody.addChildrenSequentially(processor))
            } else {
                null
            }
        }

        fun CMTableHead.getTableRow(processor: MarkdownProcessor): Table.TableRow =
            Table.TableRow((this.firstChild as CMTableRow).getCells(processor))

        private fun CMTableBody.addChildrenSequentially(processor: MarkdownProcessor): List<Table.TableRow> {
            val rows = mutableListOf<Table.TableRow>()
            var child = firstChild as CMTableRow?
            while (child != null) {
                val cells = child.getCells(processor)
                rows.add(Table.TableRow(cells))
                child = child.next as? CMTableRow
            }
            return rows.toList()
        }

        private fun CMTableRow.getCells(processor: MarkdownProcessor): List<Table.TableCell> {
            val cells = mutableListOf<Table.TableCell>()
            var child = firstChild as CMTableCell?
            while (child != null) {
                val inlineContent = child.readInlineContent(processor, processor.extensions)

                cells.add(Table.TableCell(inlineContent, this.parent is CMTableHead, getAlignment(child.alignment)))
                child = child.next as? CMTableCell
            }
            return cells.toList()
        }

        fun getAlignment(cmAlignment: CMTableCell.Alignment?): Table.Alignment {
            return when (cmAlignment) {
                CMTableCell.Alignment.CENTER -> Table.Alignment.CENTER
                CMTableCell.Alignment.RIGHT -> Table.Alignment.RIGHT
                else -> Table.Alignment.LEFT
            }
        }
    }
}
