//
// Copyright (C) 2011 United States Government as represented by the
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

package gov.nasa.jpf.util.json;

import gov.nasa.jpf.jvm.ElementInfo;
import gov.nasa.jpf.jvm.MJIEnv;

import java.util.HashMap;

public class CreatorsFactory {

  static private final HashMap<String, Creator> creatorsTable = new HashMap<String, Creator>();

  static {
    creatorsTable.put("java.lang.Boolean", new BoxedBoolCreator());
    creatorsTable.put("java.lang.Byte", new BoxedByteCreator());
    creatorsTable.put("java.lang.Short", new BoxedShortCreator());
    creatorsTable.put("java.lang.Integer", new BoxedIntCreator());
    creatorsTable.put("java.lang.Long", new BoxedLongCreator());
    creatorsTable.put("java.lang.Float", new BoxedFloatCreator());
    creatorsTable.put("java.lang.Double", new BoxedDoubleCreator());
    creatorsTable.put("java.lang.String", new StringCreator());
  }

  public static Creator getCreator(String typeName) {

    return creatorsTable.get(typeName);
  }
}


class BoxedBoolCreator implements Creator {
  public int create(MJIEnv env, String typeName, Value value) {
    Boolean read = value.getBoolean();
    int boolRef = MJIEnv.NULL;

    if (read != null) {
      boolRef = env.newObject("java.lang.Boolean");
      ElementInfo intEI = env.getElementInfo(boolRef);
      intEI.setIntField("value", (read == true) ? 1 : 0);
    }

    return boolRef;
  }
}

class BoxedByteCreator implements Creator {
  public int create(MJIEnv env, String typeName, Value value) {
    Double read = value.getDouble();
    int byteRef = MJIEnv.NULL;

    if (read != null) {
      byteRef = env.newObject("java.lang.Byte");
      ElementInfo intEI = env.getElementInfo(byteRef);
      intEI.setIntField("value", read.byteValue());
    }

    return byteRef;
  }
}

class BoxedShortCreator implements Creator {
  public int create(MJIEnv env, String typeName, Value value) {
    Double read = value.getDouble();
    int shortRef = MJIEnv.NULL;

    if (read != null) {
      shortRef = env.newObject("java.lang.Short");
      ElementInfo intEI = env.getElementInfo(shortRef);
      intEI.setIntField("value", read.shortValue());
    }

    return shortRef;
  }
}

class BoxedIntCreator implements Creator {
  public int create(MJIEnv env, String typeName, Value value) {
    Double read = value.getDouble();
    int intRef = MJIEnv.NULL;

    if (read != null) {
      intRef = env.newObject("java.lang.Integer");
      ElementInfo intEI = env.getElementInfo(intRef);
      intEI.setIntField("value", read.intValue());
    }

    return intRef;
  }
}

class BoxedLongCreator implements Creator {
  public int create(MJIEnv env, String typeName, Value value) {
    Double read = value.getDouble();
    int longRef = MJIEnv.NULL;

    if (read != null) {
      longRef = env.newObject("java.lang.Long");
      ElementInfo intEI = env.getElementInfo(longRef);
      intEI.setLongField("value", read.longValue());
    }

    return longRef;
  }
}

class BoxedFloatCreator implements Creator {
  public int create(MJIEnv env, String typeName, Value value) {
    Double read = value.getDouble();
    int floatRef = MJIEnv.NULL;

    if (read != null) {
      floatRef = env.newObject("java.lang.Float");
      ElementInfo intEI = env.getElementInfo(floatRef);

      Float flt = read.floatValue();


      intEI.setIntField("value", flt.floatToIntBits(flt));
    }

    return floatRef;
  }
}

class BoxedDoubleCreator implements Creator {
  public int create(MJIEnv env, String typeName, Value value) {
    Double read = value.getDouble();
    int doubleRef = MJIEnv.NULL;

    if (read != null) {
      doubleRef = env.newObject("java.lang.Double");
      ElementInfo intEI = env.getElementInfo(doubleRef);
      intEI.setDoubleField("value", read.doubleValue());
    }

    return doubleRef;
  }
}

class StringCreator implements Creator {

  public int create(MJIEnv env, String typeName, Value value) {
    String strVal = value.getString();
    int stringRef = MJIEnv.NULL;

    if (strVal != null) {
      stringRef = env.newString(strVal);
    }

    return stringRef;
  }
}
