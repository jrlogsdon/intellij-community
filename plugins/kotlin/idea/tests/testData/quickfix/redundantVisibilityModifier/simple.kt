// "Remove redundant 'public' modifier" "true"
<caret>public class C {
}

// FUS_QUICKFIX_NAME: org.jetbrains.kotlin.idea.quickfix.RemoveModifierFixBase
// FUS_K2_QUICKFIX_NAME: org.jetbrains.kotlin.idea.k2.codeinsight.inspections.diagnosticBased.RedundantVisibilityModifierInspection$createQuickFixes$1