//
// Copyright (C) 2006 United States Government as represented by the
// Administrator of the National Aeronautics and Space Administration
// (NASA).  All Rights Reserved.
// 
// This software is distributed under the NASA Open Source Agreement
// (NOSA), version 1.3.  The NOSA has been approved by the Open Source
// Initiative.  See the file NOSA-1.3-JPF at the top of the distribution
// directory tree for the complete NOSA document.
// 
// THE SUBJECT SOFTWARE IS PROVIDED "AS IS" WITHOUT ANY WARRANTY OF ANY
// KIND, EITHER EXPRESSED, IMPLIED, OR STATUTORY, INCLUDING, BUT NOT
// LIMITED TO, ANY WARRANTY THAT THE SUBJECT SOFTWARE WILL CONFORM TO
// SPECIFICATIONS, ANY IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR
// A PARTICULAR PURPOSE, OR FREEDOM FROM INFRINGEMENT, ANY WARRANTY THAT
// THE SUBJECT SOFTWARE WILL BE ERROR FREE, OR ANY WARRANTY THAT
// DOCUMENTATION, IF PROVIDED, WILL CONFORM TO THE SUBJECT SOFTWARE.
//
package gov.nasa.jpf.util;

import java.io.PrintWriter;


/**
 * logger used to print debugging messages based on the required debug level and
 * message category.
 * <2do> - replace this by log4j !
 */
public class Debug {
  public static final int  ERROR = 0;
  public static final int  WARNING = 1;
  public static final int  MESSAGE = 2;
  public static final int  DEBUG = 3;
  private static final int LAST_LEVEL = 4;
  public static final int  DEFAULT = 0;
  public static final int  RACE = 1;
  public static final int  LOCK_ORDER = 2;
  public static final int  DEPEND = 3;
  public static final int  DISTRIBUTED = 4;
  public static final int  SEARCH = 5;
  public static final int  TRACE = 6;
  private static final int LAST_KIND = 7;
  private static int[]     enabled = new int[LAST_KIND];
  private static String[]  levels = { "error", "warning", "message", "debug" };
  private static String[]  kinds = {
    "default", "race", "lock-order", "depend", "distributed", "search", "trace"
  };

  public static void setDebugLevel (int l) {
    if ((l < 0) || (l >= LAST_LEVEL)) {
      throw new IllegalArgumentException("0 <= level < " + LAST_LEVEL);
    }

    enabled[DEFAULT] = l;
  }

  public static void setDebugLevel (String ls) {
    int l = mapLevel(ls);

    if (l == -1) {
      throw new IllegalArgumentException(ls + " is not a valid level");
    }

    enabled[DEFAULT] = l;
  }

  public static void setDebugLevel (int l, int k) {
    if ((l < 0) || (l >= LAST_LEVEL)) {
      throw new IllegalArgumentException("0 <= level < " + LAST_LEVEL);
    }

    if ((k < 0) || (k >= LAST_KIND)) {
      throw new IllegalArgumentException("0 <= kind < " + LAST_KIND);
    }

    enabled[k] = l;
  }

  public static void setDebugLevel (int l, String ks) {
    if ((l < 0) || (l >= LAST_LEVEL)) {
      throw new IllegalArgumentException("0 <= level < " + LAST_LEVEL);
    }

    int k = mapKind(ks);

    if (k == -1) {
      throw new IllegalArgumentException(ks + " is not a valid kind");
    }

    enabled[k] = l;
  }

  public static void setDebugLevel (String ls, int k) {
    if ((k < 0) || (k >= LAST_KIND)) {
      throw new IllegalArgumentException("0 <= kind < " + LAST_KIND);
    }

    int l = mapLevel(ls);

    if (l == -1) {
      throw new IllegalArgumentException(ls + " is not a valid level");
    }

    enabled[k] = l;
  }

  public static void setDebugLevel (String ls, String ks) {
    int l = mapLevel(ls);

    if (l == -1) {
      throw new IllegalArgumentException(ls + " is not a valid level");
    }

    int k = mapKind(ks);

    if (k == -1) {
      throw new IllegalArgumentException(ks + " is not a valid kind");
    }

    enabled[k] = l;
  }

  public static int getDebugLevel () {
    return enabled[DEFAULT];
  }

  public static int getDebugLevel (int k) {
    return enabled[k];
  }

  public static int getDebugLevel (String ks) {
    int k = mapKind(ks);

    if (k == -1) {
      throw new IllegalArgumentException(ks + " is not a valid kind");
    }

    return enabled[k];
  }

  public static int mapKind (String ks) {
    for (int k = 0; k < LAST_KIND; k++) {
      if (ks.equals(kinds[k])) {
        return k;
      }
    }

    return -1;
  }

  public static int mapLevel (String ls) {
    for (int l = 0; l < LAST_LEVEL; l++) {
      if (ls.equals(levels[l])) {
        return l;
      }
    }

    return -1;
  }

  public static void print (int l, Object o) {
    if (l <= enabled[DEFAULT]) {
      System.err.print(o);
    }
  }

  public static void print (int l, String s) {
    if (l <= enabled[DEFAULT]) {
      System.err.print(s);
    }
  }

  public static void print (int l, int k, Object o) {
    if (l <= enabled[k]) {
      System.err.print(o);
    }
  }

  public static void print (int l, int k, String s) {
    if (l <= enabled[k]) {
      System.err.print(s);
    }
  }

  public static void print (int l, int k, Printable p) {
    if (l <= enabled[k]) {
      PrintWriter pw = new PrintWriter(System.err, true);
      p.printOn(pw);
    }
  }

  public static void print (int l, Printable p) {
    print(l, DEFAULT, p);
  }

  public static void println (int l, int k, Printable p) {
    if (l <= enabled[k]) {
      PrintWriter pw = new PrintWriter(System.err, true);
      p.printOn(pw);
      System.err.println();
    }
  }

  public static void println (int l, Printable p) {
    println(l, DEFAULT, p);
  }

  public static void println (int l) {
    if (l <= enabled[DEFAULT]) {
      System.err.println();
    }
  }

  public static void println (int l, Object o) {
    if (l <= enabled[DEFAULT]) {
      System.err.println(o);
    }
  }

  public static void println (int l, String s) {
    if (l <= enabled[DEFAULT]) {
      System.err.println(s);
    }
  }

  public static void println (int l, int k) {
    if (l <= enabled[k]) {
      System.err.println();
    }
  }

  public static void println (int l, int k, Object o) {
    if (l <= enabled[k]) {
      System.err.println(o);
    }
  }

  public static void println (int l, int k, String s) {
    if (l <= enabled[k]) {
      System.err.println(s);
    }
  }

  public static String status () {
    StringBuilder sb = new StringBuilder();

    for (int k = 1; k < LAST_KIND; k++) {
      int l = enabled[k];

      if (l != ERROR) {
        if (sb.length() != 0) {
          sb.append(',');
        }

        sb.append(kinds[k]);
        sb.append('=');
        sb.append(levels[l]);
      }
    }

    return sb.toString();
  }
}
