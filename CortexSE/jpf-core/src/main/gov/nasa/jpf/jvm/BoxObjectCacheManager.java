package gov.nasa.jpf.jvm;

/**
 * @author Nastaran
 * 
 *         Cache management implementation for the types Boolean, Byte,
 *         Character, Short, Integer, Long. The references to the caches are in
 *         the class classes/gov/nasa/jpf/BoxObjectCaches.
 * 
 *         All the caches, except Boolean, are initialized on the first
 *         invocation of valueOf(), and they all exempt from garbage collection.
 */
public class BoxObjectCacheManager {
  private static String boxObjectCaches = "gov.nasa.jpf.BoxObjectCaches";

  // cache default bounds
  private static int defLow = -128;
  private static int defHigh = 127;

  public static int valueOfBoolean(ThreadInfo ti, boolean b) {
    ClassInfo cls = ClassInfo.getResolvedClassInfo("java.lang.Boolean");

    int boolObj;
    if (b) {
      boolObj = cls.getStaticElementInfo().getReferenceField("TRUE");
    } else {
      boolObj = cls.getStaticElementInfo().getReferenceField("FALSE");
    }

    return boolObj;
  }

  // Byte cache bounds
  private static byte byteLow;
  private static byte byteHigh;

  public static int initByteCache(ThreadInfo ti) {
    byteLow = (byte) ti.getVM().getConfig().getInt("vm.cache.low_byte", defLow);
    byteHigh = (byte) ti.getVM().getConfig().getInt("vm.cache.high_byte", defHigh);

    int n = (byteHigh - byteLow) + 1;
    int aRef = ti.getHeap().newArray("Ljava/lang/Byte", n, ti);
    ElementInfo ei = ti.getElementInfo(aRef);

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Byte");
    byte val = byteLow;
    for (int i = 0; i < n; i++) {
      int byteObj = ti.getHeap().newObject(ci, ti);
      ti.getElementInfo(byteObj).setByteField("value", val++);
      ei.setReferenceElement(i, byteObj);
    }

    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    cacheClass.getStaticElementInfo().setReferenceField("byteCache", aRef);
    return aRef;
  }

  public static int valueOfByte(ThreadInfo ti, byte b) {
    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    int byteCache = cacheClass.getStaticElementInfo().getReferenceField("byteCache");

    if (byteCache == MJIEnv.NULL) { // initializing the cache on demand
      byteCache = initByteCache(ti);
    }

    if (b >= byteLow && b <= byteHigh) {
      return ti.getElementInfo(byteCache).getReferenceElement(b - byteLow);
    }

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Byte");
    int byteObj = ti.getHeap().newObject(ci, ti);
    ti.getElementInfo(byteObj).setByteField("value", b);
    return byteObj;
  }

  // Character cache bound
  private static int charHigh;

  public static int initCharCache(ThreadInfo ti) {
    charHigh = ti.getVM().getConfig().getInt("vm.cache.high_char", defHigh);

    int n = charHigh + 1;
    int aRef = ti.getHeap().newArray("Ljava/lang/Character", n, ti);
    ElementInfo ei = ti.getElementInfo(aRef);

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Character");
    for (int i = 0; i < n; i++) {
      int charObj = ti.getHeap().newObject(ci, ti);
      ti.getElementInfo(charObj).setCharField("value", (char) i);
      ei.setReferenceElement(i, charObj);
    }

    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    cacheClass.getStaticElementInfo().setReferenceField("charCache", aRef);
    return aRef;
  }

  public static int valueOfCharacter(ThreadInfo ti, char c) {
    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    int charCache = cacheClass.getStaticElementInfo().getReferenceField("charCache");

    if (charCache == MJIEnv.NULL) { // initializing the cache on demand
      charCache = initCharCache(ti);
    }

    if (c >= 0 && c <= charHigh) {
      return ti.getElementInfo(charCache).getReferenceElement(c);
    }

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Character");
    int charObj = ti.getHeap().newObject(ci, ti);
    ti.getElementInfo(charObj).setCharField("value", c);
    return charObj;
  }

  // Short cache bounds
  private static short shortLow;
  private static short shortHigh;

  public static int initShortCache(ThreadInfo ti) {
    shortLow = (short) ti.getVM().getConfig().getInt("vm.cache.low_short", defLow);
    shortHigh = (short) ti.getVM().getConfig().getInt("vm.cache.high_short", defHigh);

    int n = (shortHigh - shortLow) + 1;
    int aRef = ti.getHeap().newArray("Ljava/lang/Short", n, ti);
    ElementInfo ei = ti.getElementInfo(aRef);

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Short");
    short val = shortLow;
    for (int i = 0; i < n; i++) {
      int shortObj = ti.getHeap().newObject(ci, ti);
      ti.getElementInfo(shortObj).setShortField("value", val++);
      ei.setReferenceElement(i, shortObj);
    }

    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    cacheClass.getStaticElementInfo().setReferenceField("shortCache", aRef);
    return aRef;
  }

  public static int valueOfShort(ThreadInfo ti, short s) {
    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    int shortCache = cacheClass.getStaticElementInfo().getReferenceField("shortCache");

    if (shortCache == MJIEnv.NULL) { // initializing the cache on demand
      shortCache = initShortCache(ti);
    }

    if (s >= shortLow && s <= shortHigh) {
      return ti.getElementInfo(shortCache).getReferenceElement(s - shortLow);
    }

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Short");
    int shortObj = ti.getHeap().newObject(ci, ti);
    ti.getElementInfo(shortObj).setShortField("value", s);
    return shortObj;
  }

  // Integer cache bounds
  private static int intLow;
  private static int intHigh;

  public static int initIntCache(ThreadInfo ti) {
    intLow = ti.getVM().getConfig().getInt("vm.cache.low_int", defLow);
    intHigh = ti.getVM().getConfig().getInt("vm.cache.high_int", defHigh);

    int n = (intHigh - intLow) + 1;
    int aRef = ti.getHeap().newArray("Ljava/lang/Integer", n, ti);
    ElementInfo ei = ti.getElementInfo(aRef);

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Integer");
    for (int i = 0; i < n; i++) {
      int intObj = ti.getHeap().newObject(ci, ti);
      ti.getElementInfo(intObj).setIntField("value", i + intLow);
      ei.setReferenceElement(i, intObj);
    }

    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    cacheClass.getStaticElementInfo().setReferenceField("intCache", aRef);
    return aRef;
  }

  public static int valueOfInteger(ThreadInfo ti, int i) {
    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    int intCache = cacheClass.getStaticElementInfo().getReferenceField("intCache");

    if (intCache == MJIEnv.NULL) { // initializing the cache on demand
      intCache = initIntCache(ti);
    }

    if (i >= intLow && i <= intHigh) {
      return ti.getElementInfo(intCache).getReferenceElement(i - intLow);
    }

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Integer");
    int intObj = ti.getHeap().newObject(ci, ti);
    ti.getElementInfo(intObj).setIntField("value", i);
    return intObj;
  }

  // Long cache bounds
  private static int longLow;
  private static int longHigh;

  public static int initLongCache(ThreadInfo ti) {
    longLow = ti.getVM().getConfig().getInt("vm.cache.low_long", defLow);
    longHigh = ti.getVM().getConfig().getInt("vm.cache.high_long", defHigh);

    int n = (longHigh - longLow) + 1;
    int aRef = ti.getHeap().newArray("Ljava/lang/Long", n, ti);
    ElementInfo ei = ti.getElementInfo(aRef);

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Long");
    for (int i = 0; i < n; i++) {
      int longObj = ti.getHeap().newObject(ci, ti);
      ti.getElementInfo(longObj).setLongField("value", i + longLow);
      ei.setReferenceElement(i, longObj);
    }

    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    cacheClass.getStaticElementInfo().setReferenceField("longCache", aRef);
    return aRef;
  }

  public static int valueOfLong(ThreadInfo ti, long l) {
    ClassInfo cacheClass = ClassInfo.getResolvedClassInfo(boxObjectCaches);
    int longCache = cacheClass.getStaticElementInfo().getReferenceField("longCache");

    if (longCache == MJIEnv.NULL) { // initializing the cache on demand
      longCache = initLongCache(ti);
    }

    if (l >= longLow && l <= longHigh) {
      return ti.getElementInfo(longCache).getReferenceElement((int) l - longLow);
    }

    ClassInfo ci = ClassInfo.getResolvedClassInfo("java.lang.Long");
    int longObj = ti.getHeap().newObject(ci, ti);
    ti.getElementInfo(longObj).setLongField("value", l);
    return longObj;
  }
}
