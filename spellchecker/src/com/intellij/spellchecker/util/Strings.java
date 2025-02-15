// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.spellchecker.util;

import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.List;

public final class Strings {
  private Strings() {
  }

  public static boolean isCapitalized(String word) {
    if (word.isEmpty()) return false;

    boolean lowCase = true;
    for (int i = 1; i < word.length() && lowCase; i++) {
      lowCase = Character.isLowerCase(word.charAt(i));
    }
    return Character.isUpperCase(word.charAt(0)) && lowCase;
  }


  public static boolean isCapitalized(@NotNull String text, @NotNull TextRange range) {
    if (range.getLength() == 0) return false;
    CharacterIterator it = new StringCharacterIterator(text, range.getStartOffset() + 1, range.getEndOffset(), range.getStartOffset() + 1);
    boolean lowCase = true;
    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      lowCase = Character.isLowerCase(c);
    }

    return Character.isUpperCase(text.charAt(range.getStartOffset())) && lowCase;
  }

  public static boolean isUpperCased(@NotNull String text, @NotNull TextRange range) {
    if (range.getLength() == 0) return false;
    CharacterIterator it = new StringCharacterIterator(text, range.getStartOffset(), range.getEndOffset(), range.getStartOffset());

    for (char c = it.first(); c != CharacterIterator.DONE; c = it.next()) {
      if (!Character.isUpperCase(c)) {
        return false;
      }
    }

    return true;
  }

  public static boolean isUpperCase(String word) {
    boolean upperCase = true;
    for (int i = 0; i < word.length() && upperCase; i++) {
      upperCase = Character.isUpperCase(word.charAt(i));
    }

    return upperCase;
  }

  public static boolean isMixedCase(String word) {
    if (word.length() < 2) return false;

    String tail = word.substring(1);
    String lowerCase = StringUtil.toLowerCase(tail);
    return !tail.equals(lowerCase) && !isUpperCase(word);
  }

  public static void capitalize(List<String> words) {
    words.replaceAll(StringUtil::capitalize);
  }

  public static void upperCase(List<String> words) {
    words.replaceAll(StringUtil::toUpperCase);
  }
}
