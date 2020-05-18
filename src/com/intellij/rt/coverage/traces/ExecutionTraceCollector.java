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
import de.unisb.cs.st.sequitur.output.OutputSequence;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ExecutionTraceCollector {

  public static final String TRACE_FILE_NAME = "traces.ser";

  private static final transient Lock globalExecutionTraceCollectorLock = new ReentrantLock();

  // shouldn't need to be thread-safe, as each thread only accesses its own trace (thread id -> sequence of sub trace ids)
  private static Map<Long, OutputSequence<Long>> executionTraces = new HashMap<Long, OutputSequence<Long>>();

  // holds the current active threads that we have seen so far
  private static Set<Thread> currentThreads = new HashSet<Thread>();

  /**
   * @return the collection of execution traces for all executed threads;
   * the statements in the traces are encoded as "class_id:line";
   * also resets the internal map and collects potentially remaining sub traces.
   */
  public static Map<Long, byte[]> getAndResetExecutionTraces() {
    globalExecutionTraceCollectorLock.lock();
    try {
      processAllRemainingSubTraces();

      // reset map for next run
      Map<Long, OutputSequence<Long>> temp = executionTraces;
      executionTraces = new HashMap<Long, OutputSequence<Long>>();

      Map<Long, byte[]> traces = new HashMap<Long, byte[]>();
      for (Map.Entry<Long, OutputSequence<Long>> entry : temp.entrySet()) {
        byte[] bytes = SequiturUtils.convertToByteArray(entry.getValue(), true);

        traces.put(entry.getKey(), bytes);
      }
      return traces;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    } finally {
      globalExecutionTraceCollectorLock.unlock();
    }
  }

  private static void processAllRemainingSubTraces() {
    // TODO necessary? Tries to wait for any threads that are still alive...
    //  If this section makes the execution deadlock, we need to rethink this.
    for (Thread thread : currentThreads) {
      if (thread.equals(Thread.currentThread())) {
        continue;
      }
      boolean done = false;
      while (!done) {
        if (thread.isAlive()) {
          System.err.println("Thread " + thread.getId() + " is still alive. Waiting 10 seconds for it to die...");
          try {
            thread.join(10000); // wait 10 seconds for threads to die... TODO
            if (thread.isAlive()) {
              System.err.println("(At least) thread " + thread.getId() + " remains alive...");
              //						thread.interrupt();
              break;
            }
            done = true;
          } catch (InterruptedException e) {
            // try again
          }
        } else {
          break;
        }
      }
    }

    currentThreads.clear();
  }

  public static void addLineToExecutionTrace(ClassData classData, int line) {
//    if (++counter % 1000000 == 0) {
//      System.out.print('.');
//      if (counter % 100000000 == 0)
//        System.out.println(String.format("%,d", counter));
//    }

    // add the line to the execution trace
    getOutputSequence().append(ClassLineEncoding.encode(classData, line));
  }

  /**
   * @return output sequence for the current thread
   */
  private static OutputSequence<Long> getOutputSequence() {
    Thread currentThread = Thread.currentThread();
    currentThreads.add(currentThread);
    long threadId = currentThread.getId();
    // get the thread's execution trace
    OutputSequence<Long> trace = executionTraces.get(threadId);
    if (trace == null) {
      trace = getNewCollector(threadId);
      executionTraces.put(threadId, trace);
    }
    return trace;
  }

  private static OutputSequence<Long> getNewCollector(long threadId) {
    return new OutputSequence<Long>();
  }


}
