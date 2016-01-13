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
package gov.nasa.jpf.jvm;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.JPFException;
import gov.nasa.jpf.util.JPFLogger;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;


/**
 * native peer classes are part of MJI and contain the code that is
 * executed by the host VM (i.e. outside the state-tracked JPF JVM). Each
 * class executed by JPF that has native mehods must have a native peer class
 * (which is looked up and associated at class loadtime)
 */
public class NativePeer {

  static final String MODEL_PACKAGE = "<model>";
  static final String DEFAULT_PACKAGE = "<default>";

  static JPFLogger logger = JPF.getLogger("gov.nasa.jpf.jvm.NativePeer");

  static ClassLoader loader;
  static HashMap<String, NativePeer> peers;
  static Config config;
  static boolean noOrphanMethods;

  static String[] peerPackages;

  ClassInfo ci;
  Class<?> peerClass;
  HashMap<String, Method> methods;


  public static boolean init (Config conf) {
    loader = conf.getClassLoader();
    peers = new HashMap<String, NativePeer>();

    peerPackages = getPeerPackages(conf);

    config = conf;
    noOrphanMethods = conf.getBoolean("vm.no_orphan_methods", false);

    return true;
  }

  static String[] getPeerPackages (Config conf) {
    String[] defPeerPackages = { MODEL_PACKAGE, "gov.nasa.jpf.jvm", DEFAULT_PACKAGE };
    String[] packages = conf.getStringArray("peer_packages", defPeerPackages);

    // internalize
    for (int i=0; i<packages.length; i++) {
      if (packages[i].equals(MODEL_PACKAGE)) {
        packages[i] = MODEL_PACKAGE;
      } else if (packages[i].equals(DEFAULT_PACKAGE)) {
        packages[i] = DEFAULT_PACKAGE;
      }
    }

    return packages;
  }

  public NativePeer (Class<?> peerClass, ClassInfo ci) {
    initialize(peerClass, ci, true);
  }

  static Class<?> locatePeerCls (String clsName) {
    String cn = "JPF_" + clsName.replace('.', '_');

    for (int i=0; i<peerPackages.length; i++) {
      String pcn;
      String pkg = peerPackages[i];

      if (pkg == MODEL_PACKAGE) {
        int j = clsName.lastIndexOf('.');
        pcn = clsName.substring(0, j+1) + cn;
      } else if (pkg == DEFAULT_PACKAGE) {
        pcn = cn;
      } else {
        pcn = pkg + '.' + cn;
      }
     
      try {
        Class<?> peerCls = loader.loadClass(pcn);
        
        if ((peerCls.getModifiers() & Modifier.PUBLIC) == 0) {
          logger.warning("non-public peer class: " + peerCls.getName());
          continue; // pointless to use this one, it would just create IllegalAccessExceptions
        }
        
        return peerCls;
      } catch (ClassNotFoundException cnfx) {
        // try next one
      }
    }

    return null; // nothing found
  }

  /**
   * this becomes the factory method to load either a plain (slow)
   * reflection-based peer (a NativePeer object), or some speed optimized
   * derived class object.
   * Watch out - this gets called before the ClassInfo is fully initialized
   * (we shouldn't rely on more than just its name here)
   */
  static NativePeer getNativePeer (ClassInfo ci) {
    String     clsName = ci.getName();
    NativePeer peer = peers.get(clsName);
    Class<?>      peerCls = null;

    if (peer == null) {
      peerCls = locatePeerCls(clsName);

      if (peerCls != null) {

        // Method.invoke() got fast enough so that we don't need a peer dispatcher anymore
                
        if (logger.isLoggable(Level.INFO)) {
          logger.info("load peer: ", peerCls.getName());
        }

        peer = new NativePeer(peerCls, ci);
        peers.put(clsName, peer);
      }
    }

    return peer;
  }

  static String getPeerDispatcherClassName (String clsName) {
    return (clsName + '$');
  }

  public Class<?> getPeerClass() {
    return peerClass;
  }

  public String getPeerClassName() {
    return peerClass.getName();
  }

  void initialize (Class<?> peerClass, ClassInfo ci, boolean cacheMethods) {
    if ((this.ci != null) || (this.peerClass != null)) {
      throw new RuntimeException("cannot re-initialize NativePeer: " +
                                 peerClass.getName());
    }

    this.ci = ci;
    this.peerClass = peerClass;

    loadMethods(cacheMethods);

    initializePeerClass();
  }

  void initializePeerClass() {
    // they are all static, so we can't use polymorphism
    try {
      Method m = peerClass.getDeclaredMethod("init", Config.class );
      try {
        m.invoke(null, config);
      } catch (IllegalArgumentException iax){
        // can't happen - static method
      } catch (IllegalAccessException iacx) {
        throw new RuntimeException("peer initialization method not accessible: "
                                   + peerClass.getName());
      } catch (InvocationTargetException itx){
        throw new RuntimeException("initialization of peer " +
                                   peerClass.getName() + " failed: " + itx.getCause());

      }
    } catch (NoSuchMethodException nsmx){
      // nothing to do
    }
  }

  
  static final int MJI_MODIFIERS = Modifier.PUBLIC | Modifier.STATIC;
  static final int MJI_TEST = Modifier.PUBLIC | Modifier.STATIC | Modifier.FINAL;
  
  private static boolean isMJICandidate (Method mth) {
    // only the public static ones are supposed to be native method impls
    // if there is a public static method with MJIEnv argument that we DO NOT
    // want to be considered, we have to add a 'final' modifier (which is pointless
    // for statics but accepted by the compiler)
    if ((mth.getModifiers() & MJI_TEST) != MJI_MODIFIERS) {
      return false;
    }

    // native method always have a MJIEnv and int as the first parameters
    Class<?>[] argTypes = mth.getParameterTypes();
    if ((argTypes.length >= 2) && (argTypes[0] == MJIEnv.class) && (argTypes[1] == int.class) ) {
      return true;
    } else {
      return false;
    }
  }


  private Method getMethod (MethodInfo mi) {
    return getMethod(null, mi);
  }

  private Method getMethod (String prefix, MethodInfo mi) {
    String name = mi.getUniqueName();

    if (prefix != null) {
      name = prefix + name;
    }

    return methods.get(name);
  }

  /**
   * look at all public static methods in the peer and set their
   * corresponding model class MethodInfo attributes
   * <2do> pcm - this is too long, break it down
   */
  private void loadMethods (boolean cacheMethods) {
    Method[] m = peerClass.getDeclaredMethods();
    methods = new HashMap<String, Method>(m.length);

    Map<String,MethodInfo> methodInfos = ci.getDeclaredMethods();
    MethodInfo[] mis = null;

    for (int i = 0; i < m.length; i++) {
      Method  mth = m[i];

      if (isMJICandidate(mth)) {
        // Note that we can't mangle the name automatically, since we loose the
        // object type info (all mapped to int). This has to be handled
        // the same way like with overloaded JNI methods - you have to
        // mangle them manually
        String mn = mth.getName();

        // JNI doesn't allow <clinit> or <init> to be native, but MJI does
        // (you should know what you are doing before you use that, really)
        if (mn.startsWith("$clinit")) {
          mn = "<clinit>";
        } else if (mn.startsWith("$init")) {
          mn = "<init>" + mn.substring(5);
        }

        String mname = Types.getJNIMethodName(mn);
        String sig = Types.getJNISignature(mn);

        if (sig != null) {
          mname += sig;
        }

        // now try to find a corresponding MethodInfo object and mark it
        // as 'peer-ed'
        // <2do> in case of <clinit>, it wouldn't be strictly required to
        // have a MethodInfo upfront (we could create it). Might be handy
        // for classes where we intercept just a few methods, but need
        // to init before
        MethodInfo mi = methodInfos.get(mname);

        if ((mi == null) && (sig == null)) {
          // nothing found, we have to do it the hard way - check if there is
          // a single method with this name (still unsafe, but JNI behavior)
          // Note there's no point in doing that if we do have a signature
          if (mis == null) { // cache it for subsequent lookup
            mis = new MethodInfo[methodInfos.size()];
            methodInfos.values().toArray(mis);
          }

          mi = searchMethod(mname, mis);
        }

        if (mi != null) {
          logger.info("load MJI method: ", mname);

          NativeMethodInfo miNative = new NativeMethodInfo(mi, mth, this);
          miNative.replace(mi);

        } else {
          // we have an orphan method, i.e. a peer method that does not map into any model method
          // (this is usually a signature typo or an out-of-sync peer)
          String message = "orphan NativePeer method: " + ci.getName() + '.' + mname;
           
          if (noOrphanMethods){
            throw new JPFException(message);
          }

          // issue a warning if we have a NativePeer native method w/o a corresponding
          // method in the model class (this might happen due to compiler optimizations
          // silently skipping empty methods)
          logger.warning(message);
        }
      }
    }
  }

  private static MethodInfo searchMethod (String mname, MethodInfo[] methods) {
    int idx = -1;

    for (int j = 0; j < methods.length; j++) {
      if (methods[j].getName().equals(mname)) {
        // if this is actually a overloaded method, and the first one
        // isn't the right choice, we would get an IllegalArgumentException,
        // hence we have to go on and make sure it's not overloaded

        if (idx == -1) {
          idx = j;
        } else {
          throw new JPFException("overloaded native method without signature: " + mname);
        }
      }
    }

    if (idx >= 0) {
      return methods[idx];
    } else {
      return null;
    }
  }
}

