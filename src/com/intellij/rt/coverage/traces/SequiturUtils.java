package com.intellij.rt.coverage.traces;

import com.sun.istack.internal.Nullable;
import de.unisb.cs.st.sequitur.input.InputSequence;
import de.unisb.cs.st.sequitur.input.SharedInputGrammar;
import de.unisb.cs.st.sequitur.output.OutputSequence;
import de.unisb.cs.st.sequitur.output.SharedOutputGrammar;

import java.io.*;

public class SequiturUtils {

  // ============================================
  // ========= Loading from byte arrays =========
  // ============================================

  /**
   * Gets an iterable {@link InputSequence} from a previously stored {@link OutputSequence}.
   * @param bytes output sequence stored as byte array
   * @param inGrammar a grammar to use, if it is not stored together with the output sequence; may be null
   * @param clazz for instance checking
   * @param <T> type of the elements
   * @return an iterable sequence
   * @throws IOException if something goes wrong with reading the sequence
   * @throws ClassNotFoundException if a class can't be found
   */
  public static <T> InputSequence<T> getInputSequenceFromByteArray(
      byte[] bytes, @Nullable SharedInputGrammar<T> inGrammar, Class<T> clazz)
      throws IOException, ClassNotFoundException {
    ByteArrayInputStream byteIn = new ByteArrayInputStream(bytes);
    InputStream buffer = new BufferedInputStream(byteIn);
    ObjectInputStream objIn = new ObjectInputStream(buffer);
    try {
      if (inGrammar == null) {
        return InputSequence.readFrom(objIn, clazz);
      } else {
        return InputSequence.readFrom(objIn, inGrammar);
      }
    } finally {
      objIn.close();
    }
  }

  /**
   * Gets an iterable {@link InputSequence} from a previously stored {@link OutputSequence}.
   * Assumes that the grammar has been stored together with the sequence.
   * Calls {@link SequiturUtils#getInputSequenceFromByteArray(byte[], SharedInputGrammar, Class)}.
   * @param bytes output sequence stored as byte array
   * @param clazz for instance checking
   * @param <T> type of the elements
   * @return an iterable sequence
   * @throws IOException if something goes wrong with reading the sequence
   * @throws ClassNotFoundException if a class can't be found
   */
  public static <T> InputSequence<T> getInputSequenceFromByteArray(byte[] bytes, Class<T> clazz)
      throws IOException, ClassNotFoundException {
    return getInputSequenceFromByteArray(bytes, null, clazz);
  }

  /**
   * Gets a {@link SharedInputGrammar} from a previously stored {@link SharedOutputGrammar}.
   * @param storedGrammar output grammar stored as byte array
   * @param clazz for instance checking
   * @param <T> type of the elements
   * @return an input grammar
   * @throws IOException if something goes wrong with reading the grammar
   * @throws ClassNotFoundException if a class can't be found
   */
  public static <T> SharedInputGrammar<T> getInputGrammarFromByteArray(byte[] storedGrammar, Class<T> clazz)
      throws IOException, ClassNotFoundException {
    if (storedGrammar == null) {
      return null;
    }
    ByteArrayInputStream byteIn = new ByteArrayInputStream(storedGrammar);
    InputStream buffer = new BufferedInputStream(byteIn);
    ObjectInputStream objIn = new ObjectInputStream(buffer);
    try {
      return SharedInputGrammar.readFrom(objIn, clazz);
    } finally {
      objIn.close();
    }
  }



  // ==========================================
  // ========= Storing in byte arrays =========
  // ==========================================

  /**
   * Stores a {@link SharedOutputGrammar} in a byte array.
   * @param outputGrammar the grammar
   * @return a byte array, holding the grammar in compressed form
   * @throws IOException if something goes wrong with storing the grammar
   */
  public static byte[] convertToByteArray(SharedOutputGrammar<?> outputGrammar)
      throws IOException {
    if (outputGrammar == null) {
      return null;
    }
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    OutputStream buffer = new BufferedOutputStream(byteOut);
    ObjectOutputStream objOut = new ObjectOutputStream(buffer);
    try {
      outputGrammar.writeOut(objOut);
      objOut.flush();
      return byteOut.toByteArray();
    } finally {
      objOut.close();
    }
  }

  /**
   * Stores an {@link OutputSequence} in a byte array.
   * @param outSeq the sequence
   * @param includeGrammar whether to include the sequence's grammar in the array
   * @return a byte array, holding the sequence (+ grammar, if included) in compressed form
   * @throws IOException if something goes wrong with storing the sequence (and grammar, if included)
   */
  public static byte[] convertToByteArray(OutputSequence<?> outSeq, final boolean includeGrammar)
      throws IOException {
    ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
    OutputStream buffer = new BufferedOutputStream(byteOut);
    ObjectOutputStream objOut = new ObjectOutputStream(buffer);
    try {
      outSeq.writeOut(objOut, includeGrammar);
      objOut.flush();
      return byteOut.toByteArray();
    } finally {
      objOut.close();
    }
  }



  // =======================================
  // ========= Convenience methods =========
  // =======================================

  /**
   * Generates a {@link SharedInputGrammar} from a {@link SharedOutputGrammar} for convenience. Creates a
   * temporary byte array in the process.
   * @param outputGrammar the grammar to transform
   * @param clazz for instance checking
   * @param <T> type of the elements
   * @return the respective input grammar
   * @throws IOException if something goes wrong with storing or reading
   * @throws ClassNotFoundException if a class can't be found
   */
  public static <T> SharedInputGrammar<T> convertToInputGrammar(SharedOutputGrammar<T> outputGrammar, Class<T> clazz)
      throws IOException, ClassNotFoundException {
    return getInputGrammarFromByteArray(convertToByteArray(outputGrammar), clazz);
  }

  /**
   * Generates an {@link InputSequence} from an {@link OutputSequence} for convenience. Creates a
   * temporary byte array in the process.
   * @param outSeq the sequence to transform
   * @param inputGrammar a shared grammar to use, if existing
   * @param clazz for instance checking
   * @param <T> type of the elements
   * @return the respective input sequence
   * @throws IOException if something goes wrong with storing or reading
   * @throws ClassNotFoundException if a class can't be found
   */
  public static <T> InputSequence<T> convertToInputSequence(
      OutputSequence<T> outSeq, @Nullable SharedInputGrammar<T> inputGrammar, Class<T> clazz)
      throws IOException, ClassNotFoundException {
    if (inputGrammar == null) {
      return getInputSequenceFromByteArray(convertToByteArray(outSeq, true), null, clazz);
    } else {
      return getInputSequenceFromByteArray(convertToByteArray(outSeq, false), inputGrammar, clazz);
    }
  }

  /**
   * Generates an {@link InputSequence} from an {@link OutputSequence} for convenience. Creates a
   * temporary byte array in the process. Calls
   * {@link SequiturUtils#getInputSequenceFromByteArray(byte[], SharedInputGrammar, Class)}.
   * @param outSeq the sequence to transform
   * @param clazz for instance checking
   * @param <T> type of the elements
   * @return the respective input sequence
   * @throws IOException if something goes wrong with storing or reading
   * @throws ClassNotFoundException if a class can't be found
   */
  public static <T> InputSequence<T> convertToInputSequence(OutputSequence<T> outSeq, Class<T> clazz)
      throws IOException, ClassNotFoundException {
    return getInputSequenceFromByteArray(convertToByteArray(outSeq, false), null, clazz);
  }

}
