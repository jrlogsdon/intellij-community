package org.jetbrains.jewel.markdown.extensions.tables

import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import org.commonmark.ext.gfm.tables.TableBlock
import org.jetbrains.jewel.markdown.InlineMarkdown
import org.jetbrains.jewel.markdown.MarkdownBlock
import org.jetbrains.jewel.markdown.processing.MarkdownProcessor
import org.junit.Assert.assertThrows
import org.junit.Test

class TableProcessorExtensionTest {

    private val processor = MarkdownProcessor(listOf(TableProcessorExtension))

    //Make sure we can parse examples from [https://github.github.com/gfm/#tables-extension](gfm tables) correctly
    @Test
    fun `GFM table example 198`() {
        val parsed = processor.processMarkdownDocument(
            """
            | foo | bar |
            | --- | --- |
            | baz | bim |
        """.trimIndent()
        )
        /*
         * Expected HTML:
         * <table>
         *  <thead>
         *      <tr>
         *          <th>foo</th>
         *          <th>bar</th>
         *      </tr>
         *  </thead>
         *  <tbody>
         *      <tr>
         *          <td>baz</td>
         *          <td>bim</td>
         *      </tr>
         *  </tbody>
         * </table>
         */
        val table = parsed[0] as Table.TableBlock
        val expectedHeaders = listOf("foo", "bar")
        val expectedBody = listOf("baz", "bim")
        val headers = expectedHeaders.map { createCell(it, true) }
        val body = expectedBody.map { createCell(it) }
        val expectedTable = Table.TableBlock(
            Table.TableHead(Table.TableRow(headers)),
            Table.TableBody(listOf(Table.TableRow(body)))
        )
        assertEquals(expectedTable, table)
    }

    @Test
    fun `GFM table example 199`() {
        val parsed = processor.processMarkdownDocument(
            """
            | abc | defghi |
            :-: | -----------:
            bar | baz
        """.trimIndent()
        )
        /*
        * <table>
        *   <thead>
        *       <tr>
        *           <th align="center">abc</th>
        *           <th align="right">defghi</th>
        *       </tr>
        *   </thead>
        *   <tbody>
        *       <tr>
        *           <td align="center">bar</td>
        *           <td align="right">baz</td>
        *       </tr>
        *   </tbody>
        * </table>
        *
         */
        val table = parsed[0] as Table.TableBlock
        val header1 = createCell("abc", isHeader = true, Table.Alignment.CENTER)
        val header2 = createCell("defghi", isHeader = true, Table.Alignment.RIGHT)
        val body1 = createCell("bar", isHeader = false, Table.Alignment.CENTER)
        val body2 = createCell("baz", isHeader = false, Table.Alignment.RIGHT)

        val expectedTable = Table.TableBlock(
            Table.TableHead(Table.TableRow(listOf(header1, header2))),
            Table.TableBody(listOf(Table.TableRow(listOf(body1, body2))))
        )
        assertEquals(expectedTable, table)
    }

    @Test
    fun `GFM table example 200`() {
        val parsed = processor.processMarkdownDocument(
            """
            | f\|oo  |
            | ------ |
            | b `\|` az |
            | b **\|** im |
        """.trimIndent()
        )
        /* <table>
         *  <thead>
         *      <tr>
         *          <th>f|oo</th>
         *      </tr>
         *  </thead>
         *  <tbody>
         *      <tr>
         *          <td>b <code>|</code> az</td>
         *      </tr>
         *      <tr>
         *          <td>b <strong>|</strong> im</td>
         *      </tr>
         *   </tbody>
         *</table>

         *
         */
        val table = parsed[0] as Table.TableBlock
        val header1 = createCell("f|oo", isHeader = true)
        val body1 = Table.TableCell(
            listOf(
                InlineMarkdown.Text("b "),
                InlineMarkdown.Code("|"),
                InlineMarkdown.Text(" az")
            )
        )
        val body2 = Table.TableCell(
            listOf(
                InlineMarkdown.Text("b "),
                InlineMarkdown.StrongEmphasis("**", listOf(InlineMarkdown.Text("|"))),
                InlineMarkdown.Text(" im")
            )
        )
        val expectedTable = Table.TableBlock(
            Table.TableHead(Table.TableRow(listOf(header1))),
            Table.TableBody(listOf(Table.TableRow(listOf(body1)), Table.TableRow(listOf(body2))))
        )
        assertEquals(expectedTable, table)
    }

    @Test
    fun `GFM table example 201`() {
        val parsed = processor.processMarkdownDocument(
            """
            | abc | def |
            | --- | --- |
            | bar | baz |
            > bar
        """.trimIndent()
        )
        /* <table>
         *  <thead>
         *      <tr>
         *          <th>abc</th>
         *          <th>def</th>
         *      </tr>
         *  </thead>
         *  <tbody>
         *      <tr>
         *          <td>bar</td>
         *          <td>baz</td>
         *      </tr>
         *  </tbody>
         * </table>
         * <blockquote>
         *  <p>bar</p>
         * </blockquote>
         */

        val table = parsed[0] as Table.TableBlock
        // next block after new line
        val blockQuote = parsed[1] as MarkdownBlock.BlockQuote

        val expectedHeaders = listOf("abc", "def")
        val expectedBody = listOf("bar", "baz")
        val headers = expectedHeaders.map { createCell(it, true) }
        val body = expectedBody.map { createCell(it) }
        val expectedTable = Table.TableBlock(
            Table.TableHead(Table.TableRow(headers)),
            Table.TableBody(listOf(Table.TableRow(body)))
        )

        val expectedBlockQuote = MarkdownBlock.BlockQuote(
            listOf(
                MarkdownBlock.Paragraph(InlineMarkdown.Text("bar"))
            )
        )
        assertEquals(expectedTable, table)
        assertEquals(expectedBlockQuote, blockQuote)
    }

    @Test
    fun `GFM table example 202`() {
        val parsed = processor.processMarkdownDocument(
            """
            | abc | def |
            | --- | --- |
            | bar | baz |
            bar

            bar
        """.trimIndent()
        )
        /*
         * <table>
         *  <thead>
         *      <tr>
         *          <th>abc</th>
         *          <th>def</th>
         *      </tr>
         *  </thead>
         *  <tbody>
         *      <tr>
         *          <td>bar</td>
         *          <td>baz</td>
         *      </tr>
         *      <tr>
         *          <td>bar</td>
         *          <td></td>
         *      </tr>
         *  </tbody>
         * </table>
         * <p>bar</p>
         */
        val table = parsed[0] as Table.TableBlock
        val paragraph = parsed[1] as MarkdownBlock.Paragraph

        val expectedHeaders = listOf("abc", "def")
        val expectedBody = listOf("bar", "baz")
        val headers = expectedHeaders.map { createCell(it, true) }
        val body = expectedBody.map { createCell(it) }
        val expectedTable = Table.TableBlock(
            Table.TableHead(Table.TableRow(headers)),
            Table.TableBody(
                listOf(
                    Table.TableRow(body),
                    Table.TableRow(listOf(createCell("bar"), Table.TableCell(listOf())))
                )
            )
        )
        val expectedParagraph = MarkdownBlock.Paragraph(listOf(InlineMarkdown.Text("bar")))

        assertEquals(expectedTable, table)
        assertEquals(expectedParagraph, paragraph)
    }

    @Test
    fun `GFM table example 203`() {
        val parsed = processor.processMarkdownDocument(
            """
            | abc | def |
            | --- |
            | bar |
        """.trimIndent()
        )
        /*
         * <p>| abc | def |
         * | --- |
         * | bar |</p>
        */
        assertThrows(ClassCastException::class.java) { parsed[0] as Table.TableBlock }

        val paragraph = parsed[0] as MarkdownBlock.Paragraph

        val expectedParagraph = MarkdownBlock.Paragraph(
            listOf(
                InlineMarkdown.Text("| abc | def |"),
                InlineMarkdown.SoftLineBreak,
                InlineMarkdown.Text("| --- |"),
                InlineMarkdown.SoftLineBreak,
                InlineMarkdown.Text("| bar |")
            )
        )

        assertEquals(expectedParagraph, paragraph)
    }

    @Test
    fun `GFM table example 204`() {
        val parsed = processor.processMarkdownDocument(
            """
            | abc | def |
            | --- | --- |
            | bar |
            | bar | baz | boo |
        """.trimIndent()
        )
        /*
         * <table>
         *  <thead>
         *      <tr>
         *          <th>abc</th>
         *          <th>def</th>
         *      </tr>
         *  </thead>
         *  <tbody>
         *      <tr>
         *          <td>bar</td>
         *          <td></td>
         *      </tr>
         *      <tr>
         *          <td>bar</td>
         *          <td>baz</td>
         *      </tr>
         *  </tbody>
         * </table>
        */
        val table = parsed[0] as Table.TableBlock

        val expectedHeaders = listOf("abc", "def")
        val headers = expectedHeaders.map { createCell(it, true) }

        val expectedTable = Table.TableBlock(
            Table.TableHead(Table.TableRow(headers)),
            Table.TableBody(
                listOf(
                    Table.TableRow(listOf(createCell("bar"), Table.TableCell(listOf()))),
                    Table.TableRow(listOf(createCell("bar"), createCell("baz")))
                )
            )
        )
        assertEquals(expectedTable, table)
    }

    @Test
    fun `GFM table example 205`() {
        val parsed = processor.processMarkdownDocument(
            """
            | abc | def |
            | --- | --- |
        """.trimIndent()
        )
        /*
        * <table>
        *   <thead>
        *       <tr>
        *           <th>abc</th>
        *           <th>def</th>
        *       </tr>
        *   </thead>
        * </table>
        */
        val table = parsed[0] as Table.TableBlock
        val expectedHeaders = listOf("abc", "def")
        val headers = expectedHeaders.map { createCell(it, true) }

        val expectedTable = Table.TableBlock(Table.TableHead(Table.TableRow(headers)), null)
        assertEquals(expectedTable, table)
    }

    private fun createCell(
        content: String,
        isHeader: Boolean = false,
        alignment: Table.Alignment = Table.Alignment.LEFT,
    ) =
        Table.TableCell(listOf(InlineMarkdown.Text(content)), isHeader, alignment)

    @Test
    fun `github table processor can process common mark TableBlocks`() {
        assertTrue(TableProcessorExtension.blockProcessorExtension.canProcess(TableBlock()))
    }
}
