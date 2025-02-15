// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.siyeh.ig.bugs;

import com.intellij.pom.java.JavaFeature;
import com.intellij.psi.PsiAssertStatement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.util.PsiUtil;
import com.siyeh.InspectionGadgetsBundle;
import com.siyeh.ig.BaseInspection;
import com.siyeh.ig.BaseInspectionVisitor;
import com.siyeh.ig.psiutils.BoolUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class ConstantAssertConditionInspection extends BaseInspection {

  @Override
  protected @NotNull String buildErrorString(Object... infos) {
    return InspectionGadgetsBundle.message(
      "constant.assert.condition.problem.descriptor");
  }

  @Override
  public @NotNull Set<@NotNull JavaFeature> requiredFeatures() {
    return Set.of(JavaFeature.ASSERTIONS);
  }

  @Override
  public BaseInspectionVisitor buildVisitor() {
    return new ConstantAssertConditionVisitor();
  }

  private static class ConstantAssertConditionVisitor
    extends BaseInspectionVisitor {

    @Override
    public void visitAssertStatement(@NotNull PsiAssertStatement statement) {
      super.visitAssertStatement(statement);
      final PsiExpression assertCondition =
        statement.getAssertCondition();
      final PsiExpression expression =
        PsiUtil.skipParenthesizedExprDown(assertCondition);
      if (expression == null) {
        return;
      }
      if (BoolUtils.isFalse(expression)) {
        return;
      }
      if (!PsiUtil.isConstantExpression(expression)) {
        return;
      }
      registerError(expression);
    }
  }
}