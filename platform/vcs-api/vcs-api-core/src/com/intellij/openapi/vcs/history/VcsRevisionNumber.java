// Copyright 2000-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license.
package com.intellij.openapi.vcs.history;

import com.intellij.openapi.util.NlsSafe;
import org.jetbrains.annotations.NotNull;

public interface VcsRevisionNumber extends Comparable<VcsRevisionNumber>{
  VcsRevisionNumber NULL = new VcsRevisionNumber() {
    @Override
    public @NotNull String asString() {
      return "";
    }

    @Override public int compareTo(@NotNull VcsRevisionNumber vcsRevisionNumber) {
      return 0;
    }

    @Override public String toString() {
      return "NULL";
    }
  };

  class Int implements VcsRevisionNumber, LongRevisionNumber {
    private final int myValue;

    public Int(int value) {
      myValue = value;
    }

    @Override
    public long getLongRevisionNumber() {
      return myValue;
    }

    @Override
    public @NotNull String asString() {
      return String.valueOf(myValue);
    }

    @Override
    public int compareTo(VcsRevisionNumber vcsRevisionNumber) {
      if (vcsRevisionNumber instanceof VcsRevisionNumber.Int){
        return myValue - ((Int)vcsRevisionNumber).myValue;
      }
      return 0;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final Int anInt = (Int)o;

      if (myValue != anInt.myValue) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return myValue;
    }

    public int getValue() {
      return myValue;
    }

    @Override
    public String toString() {
      return asString();
    }
  }

  class Long implements VcsRevisionNumber, LongRevisionNumber {
    private final long myValue;

    public Long(long value) {
      myValue = value;
    }

    @Override
    public long getLongRevisionNumber() {
      return myValue;
    }

    @Override
    public @NotNull String asString() {
      return String.valueOf(myValue);
    }

    @Override
    public int compareTo(@NotNull VcsRevisionNumber vcsRevisionNumber) {
      if (vcsRevisionNumber instanceof VcsRevisionNumber.Long){
        return java.lang.Long.signum(myValue - ((Long)vcsRevisionNumber).myValue);
      }
      return 0;
    }

    public long getLongValue() {
      return myValue;
    }

    @Override
    public boolean equals(final Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      final Long aLong = (Long)o;

      if (myValue != aLong.myValue) return false;

      return true;
    }

    @Override
    public int hashCode() {
      return java.lang.Long.hashCode(myValue);
    }

    @Override
    public String toString() {
      return asString();
    }
  }

  @NlsSafe
  @NotNull
  String asString();
}
