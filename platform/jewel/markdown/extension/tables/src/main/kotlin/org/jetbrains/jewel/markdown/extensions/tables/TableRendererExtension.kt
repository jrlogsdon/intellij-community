package org.jetbrains.jewel.markdown.extensions.tables

import org.jetbrains.jewel.markdown.extensions.MarkdownBlockRendererExtension
import org.jetbrains.jewel.markdown.extensions.MarkdownRendererExtension
import org.jetbrains.jewel.markdown.rendering.MarkdownStyling

public class TableRendererExtension(
    alertStyling: TableStyling,
    rootStyling: MarkdownStyling,
) : MarkdownRendererExtension {

    override val blockRenderer: MarkdownBlockRendererExtension =
        TableBlockRenderer(alertStyling, rootStyling)
}
