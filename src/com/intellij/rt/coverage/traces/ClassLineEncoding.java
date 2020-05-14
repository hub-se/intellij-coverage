/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.rt.coverage.traces;

import com.intellij.rt.coverage.data.ClassData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class ClassLineEncoding {

  // TODO these maps need to be either provided by the IDE executing the agent OR
  //  need to be provided to the executing IDE in the end; we don't want to store Strings or Objects in the trace.

  // maps class data to respecive integer ID
  public static Map<ClassData, Integer> classDataToIdMap = new HashMap<ClassData, Integer>();
  // maps integer ID to respective class data
  public static Map<Integer, ClassData> idToClassDataMap = new HashMap<Integer, ClassData>();
  // current class data ID
  private static AtomicInteger currentId = new AtomicInteger(0);

  /**
   * @param classData class coverage data object
   * @param line line number
   * @return a unique long value that represents the respective class + line number
   */
  public static long encode(ClassData classData, int line) {
    Integer id = classDataToIdMap.get(classData);
    if (id == null) {
      id = currentId.getAndIncrement();
      classDataToIdMap.put(classData, id);
      idToClassDataMap.put(id, classData);
    }
    // 32 bits class id | 32 bits line number
    return ((long)id << INTEGER_BITS) | line;
  }

//  private static final long UPPER_BITMASK = 0xFFFFFFFF00000000L;
//  private static final long LOWER_BITMASK = 0x00000000FFFFFFFFL;

  private static final int INTEGER_BITS = 32;

  /**
   * @param encodedStatement an encoded line reference
   * @return the class ID
   */
  public static int getClassId(long encodedStatement) {
    // push everything to the right (fills up with 0s)
    return (int) (encodedStatement >>> INTEGER_BITS);
  }

  /**
   * @param encodedStatement an encoded line reference
   * @return the class data coverage object
   */
  public static ClassData getClassData(long encodedStatement) {
    // TODO actually, this should probably return either the class name or the respective PSI class
    return idToClassDataMap.get(getClassId(encodedStatement));
  }

  /**
   * @param encodedStatement  an encoded line reference
   * @return the respective line number
   */
  public static int getLineNUmber(long encodedStatement) {
    // push out the class id and then push everything back (fills up with 0s)
    return (int) ((encodedStatement << INTEGER_BITS) >>> (INTEGER_BITS));
  }

}
