// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package org.jetbrains.idea.devkit.refactoring;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.io.FileUtilRt;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.refactoring.rename.UnresolvableCollisionUsageInfo;
import com.intellij.refactoring.rename.naming.AutomaticRenamer;
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory;
import com.intellij.refactoring.util.NonCodeUsageInfo;
import com.intellij.usageView.UsageInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.devkit.DevKitBundle;
import org.jetbrains.idea.devkit.inspections.DescriptionType;
import org.jetbrains.idea.devkit.inspections.DescriptionTypeResolver;
import org.jetbrains.idea.devkit.inspections.DescriptionTypeResolverKeys;
import org.jetbrains.idea.devkit.util.PsiUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

final class InspectionAutomaticRenamerFactory implements AutomaticRenamerFactory {
  private static final @NonNls String PROPERTY_RENAME_DESCRIPTION_AND_SHORT_NAME = "rename.inspection.description.and.short.name";
  private static final @NonNls String INSPECTION_CLASS_SUFFIX = "Inspection";

  @Override
  public boolean isApplicable(@NotNull PsiElement element) {
    if (!(element instanceof PsiClass inspectionClass)) {
      return false;
    }
    if (!PsiUtil.isPluginProject(element.getProject())) {
      return false;
    }
    String inspectionClassName = inspectionClass.getName();
    return inspectionClassName != null &&
           inspectionClassName.endsWith(INSPECTION_CLASS_SUFFIX) &&
           DescriptionType.INSPECTION.matches(inspectionClass) &&
           !isGetShortNameMethodOverridden(inspectionClass);
  }

  @Override
  public @Nls String getOptionName() {
    return DevKitBundle.message("inspection.renamer.option.name");
  }

  @Override
  public boolean isEnabled() {
    return PropertiesComponent.getInstance().getBoolean(PROPERTY_RENAME_DESCRIPTION_AND_SHORT_NAME, true);
  }

  @Override
  public void setEnabled(boolean enabled) {
    PropertiesComponent.getInstance().setValue(PROPERTY_RENAME_DESCRIPTION_AND_SHORT_NAME, enabled);
  }

  @Override
  public @NotNull AutomaticRenamer createRenamer(PsiElement element, String newName, Collection<UsageInfo> usages) {
    return new InspectionAutomaticRenamer((PsiClass)element, newName);
  }

  private static boolean isGetShortNameMethodOverridden(PsiClass inspectionClass) {
    Module module = ModuleUtilCore.findModuleForPsiElement(inspectionClass);
    if (module == null) {
      return false;
    }

    DescriptionTypeResolver resolver = DescriptionType.INSPECTION.createDescriptionTypeResolver(module, inspectionClass);
    return isGetShortNameMethodOverridden(resolver);
  }

  private static boolean isGetShortNameMethodOverridden(DescriptionTypeResolver resolver) {
    return resolver.getUserData(DescriptionTypeResolverKeys.INSPECTION_SHORT_NAME_METHOD) != null;
  }


  private static class InspectionAutomaticRenamer extends AutomaticRenamer {
    InspectionAutomaticRenamer(PsiClass inspectionClass, String newName) {
      super();

      Module module = ModuleUtilCore.findModuleForPsiElement(inspectionClass);
      if (module == null) {
        return;
      }

      DescriptionTypeResolver resolver = DescriptionType.INSPECTION.createDescriptionTypeResolver(module, inspectionClass);
      if (Boolean.FALSE == resolver.getUserData(DescriptionTypeResolverKeys.INSPECTION_SHORT_NAME_IN_XML) ||
          isGetShortNameMethodOverridden(resolver)) {
        return;
      }

      PsiFile descriptionFile = resolver.resolveDescriptionFile();
      if (descriptionFile == null) {
        return;
      }

      String inspectionClassName = inspectionClass.getName();
      if (inspectionClassName == null) {
        return;
      }
      String descriptionFileName = descriptionFile.getName();

      String defaultShortName = getDescriptionFileName(inspectionClassName);
      if (!newName.endsWith(INSPECTION_CLASS_SUFFIX) || !defaultShortName.equals(FileUtilRt.getNameWithoutExtension(descriptionFileName))) {
        return;
      }

      XmlAttribute shortNameAttribute = getInspectionShortNameAttribute(resolver);
      if (shortNameAttribute == null) {
        return;
      }
      if (!defaultShortName.equals(shortNameAttribute.getValue())) {
        return;
      }

      VirtualFile descriptionVirtualFile = descriptionFile.getVirtualFile();
      if (descriptionVirtualFile == null) {
        return;
      }

      String newDescriptionFileName = getDescriptionFileName(newName);
      if (newDescriptionFileName.equals(descriptionFileName)) {
        return;
      }
      String descriptionFileExtension = StringUtil.notNullize(descriptionVirtualFile.getExtension());

      myElements.add(descriptionFile);
      suggestAllNames(descriptionFileName, newDescriptionFileName + "." + descriptionFileExtension);
    }


    @Override
    public @Nls String getDialogTitle() {
      return DevKitBundle.message("inspection.renamer.dialog.title");
    }

    @Override
    public @Nls String getDialogDescription() {
      return DevKitBundle.message("inspection.renamer.dialog.description");
    }

    @Override
    public String entityName() {
      return DevKitBundle.message("inspection.renamer.entity.name");
    }

    @Override
    public void findUsages(List<UsageInfo> result,
                           boolean searchInStringsAndComments,
                           boolean searchInNonJavaFiles,
                           List<? super UnresolvableCollisionUsageInfo> unresolvedUsages,
                           Map<PsiElement, String> allRenames) {
      super.findUsages(result, searchInStringsAndComments, searchInNonJavaFiles, unresolvedUsages, allRenames);
      if (allRenames == null) {
        return;
      }

      for (Map.Entry<PsiElement, String> entry : allRenames.entrySet()) {
        PsiElement element = entry.getKey();
        if (!(element instanceof PsiClass inspectionClass)) {
          continue;
        }

        Module module = ModuleUtilCore.findModuleForPsiElement(element);
        if (module == null) {
          continue;
        }
        DescriptionTypeResolver resolver = DescriptionType.INSPECTION.createDescriptionTypeResolver(module, inspectionClass);
        PsiFile descriptionFile = resolver.resolveDescriptionFile();
        if (descriptionFile == null) {
          continue;
        }

        XmlAttribute shortNameAttribute = getInspectionShortNameAttribute(resolver);
        if (shortNameAttribute == null) {
          continue;
        }

        XmlAttributeValue shortNameValue = shortNameAttribute.getValueElement();
        if (shortNameValue == null) {
          continue;
        }

        PsiFile pluginXmlFile = shortNameAttribute.getContainingFile();
        if (pluginXmlFile == null) {
          continue;
        }

        String newName = getDescriptionFileName(entry.getValue());
        TextRange range = ElementManipulators.getValueTextRange(shortNameValue).shiftRight(shortNameValue.getTextRange().getStartOffset());
        result.add(NonCodeUsageInfo.create(pluginXmlFile,
                                           range.getStartOffset(), // quotes
                                           range.getEndOffset(),
                                           shortNameValue,
                                           newName));
        break;
      }
    }

    /**
     * @return inspection description file name without extension.
     */
    private static @NotNull String getDescriptionFileName(String inspectionClassName) {
      return StringUtil.trimEnd(inspectionClassName, INSPECTION_CLASS_SUFFIX);
    }

    private static @Nullable XmlAttribute getInspectionShortNameAttribute(DescriptionTypeResolver resolver) {
      return resolver.getUserData(DescriptionTypeResolverKeys.INSPECTION_SHORT_NAME_XML_ATTRIBUTE);
    }
  }
}
