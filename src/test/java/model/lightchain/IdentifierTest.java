package model.lightchain;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.ipfs.multibase.Multibase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.BitFixture;
import unittest.fixtures.IdentifierFixture;

class IdentifierTest {
  private static final Random random = new Random();

  /**
   * Tests the constructor of Identifier with a byte array. The constructor should return an Identifier with the same byte
   * array.
   */
  @Test
  void testIdentifierConstructorWithByteArray() {
    byte[] value = new byte[Identifier.Size];
    random.nextBytes(value);

    Identifier identifier = new Identifier(value);

    // the underlying byte array of the identifier should be the same as the input byte array.
    assertArrayEquals(value, identifier.getBytes());
  }

  /**
   * Tests the constructor of Identifier with a byte array. The constructor should throw an IllegalArgumentException if the
   * byte array is not 32 bytes long.
   */
  @Test
  void testIdentifierConstructorWithInvalidByteArraySize() {
    // tests 32 + 1 bytes
    byte[] value1 = new byte[Identifier.Size + 1];
    random.nextBytes(value1);

    // the constructor should throw an IllegalArgumentException if the byte array is not 32 bytes long.
    assertThrows(IllegalArgumentException.class, () -> new Identifier(value1));

    // tests 32 - 1 bytes
    byte[] value2 = new byte[Identifier.Size - 1];
    random.nextBytes(value2);

    // the constructor should throw an IllegalArgumentException if the byte array is not 32 bytes long.
    assertThrows(IllegalArgumentException.class, () -> new Identifier(value2));
  }

  /**
   * Tests the constructor of Identifier with a string. The constructor should return an Identifier with the same string.
   */
  @Test
  void testIdentifierConstructorWithString() {
    // creates a random identifier
    byte[] value = new byte[Identifier.Size];
    random.nextBytes(value);
    String identifierString = Multibase.encode(Multibase.Base.Base58BTC, value);

    // creates a new identifier with the string representation of the random identifier.
    Identifier identifier = new Identifier(identifierString);

    // the string representation of the identifier should be the same as the input string.
    assertEquals(identifierString, identifier.toString());
  }

  /**
   * Tests the constructor of Identifier with a string. The constructor should throw an IllegalArgumentException if the
   * string is not in Base58BTC format.
   */
  @Test
  void testIdentifierConstructorWithInvalidString() {
    // creates an invalid identifier string which is not in Base58BTC format.
    String invalidIdentifierString = "invalid identifier string";
    // the constructor should throw an IllegalArgumentException if the string is not in Base58BTC format.
    assertThrows(RuntimeException.class, () -> new Identifier(invalidIdentifierString));
  }

  /**
   * Tests that when two identifiers have the same underlying byte array, they are equal and have the same hash code.
   */
  @Test
  void testEqualsAndHashCodeBytes() {
    byte[] value1 = new byte[Identifier.Size];
    random.nextBytes(value1);
    Identifier identifier1 = new Identifier(value1);

    byte[] value2 = value1.clone();
    Identifier identifier2 = new Identifier(value2);

    assertTrue(identifier1.equals(identifier2) && identifier2.equals(identifier1));
    assertEquals(identifier1.hashCode(), identifier2.hashCode());
  }

  /**
   * Tests that when two identifiers have been initiated with the same string, they are equal and have the same hash code.
   */
  @Test
  void testEqualsAndHashCodeString() {
    byte[] value = new byte[Identifier.Size];
    random.nextBytes(value);
    String idStr = new Identifier(value).toString();

    Identifier id1 = new Identifier(idStr);
    Identifier id2 = new Identifier(idStr);

    assertTrue(id1.equals(id2) && id2.equals(id1));
    assertEquals(id1.hashCode(), id2.hashCode());
  }

  /**
   * Tests compareTo method of Identifier. The method should return 0 if the two identifiers are equal, a negative number
   * when the first identifier is smaller than the second identifier, and a positive number when the first identifier is
   * larger than the second identifier.
   */
  @Test
  void testCompareTo() {
    // creates identifier1 with value all 1s
    byte[] value1 = new byte[Identifier.Size];
    random.nextBytes(value1);
    Arrays.fill(value1, (byte) 1);
    Identifier identifier1 = new Identifier(value1);

    // creates identifier2 with value all 2s
    byte[] value2 = new byte[Identifier.Size];
    Arrays.fill(value2, (byte) 2);
    Identifier identifier2 = new Identifier(value2);

    // identifier1 should be equal to itself
    assertEquals(0, identifier1.comparedTo(identifier1));
    // identifier1 should be smaller than identifier2
    assertTrue(identifier1.comparedTo(identifier2) < 0);
    // identifier2 should be larger than identifier1
    assertTrue(identifier2.comparedTo(identifier1) > 0);
  }

  /**
   * Tests the round trip of Identifier. The method should return an Identifier with the same string representation as the
   * input string.
   */
  @Test
  void testByteStringRoundTrip() {
    // creates a random identifier with a random byte array.
    byte[] value = new byte[Identifier.Size];
    random.nextBytes(value);
    Identifier identifier = new Identifier(value);

    // creates a new identifier with the string representation of the random identifier.
    Identifier identifier2 = new Identifier(identifier.toString());
    // both identifiers should be equal regardless of the way they are created.
    assertEquals(identifier, identifier2);
    // the string representation of the two identifiers should be the same regardless of the way they are created.
    assertEquals(identifier.toString(), identifier2.toString());
  }

  /**
   * Tests the round trip of Identifier. The method should return an Identifier with the same bit string representation as
   * the input bit string.
   */
  @Test
  void testBitStringRoundTrip() {
    byte[] value = new byte[Identifier.Size];
    random.nextBytes(value);
    Identifier identifier = new Identifier(value);

    String bitString = identifier.getBitString();
    Identifier bitIdentifier = Identifier.bitStringToIdentifier(bitString);

    assertEquals(bitIdentifier.getBitString(), bitString);
    assertEquals(bitIdentifier.getBitString(), identifier.getBitString());
    assertArrayEquals(bitIdentifier.getBytes(), identifier.getBytes());
    assertEquals(bitIdentifier.comparedTo(identifier), 0);
  }

  /**
   * Tests that unique bit strings are converted to unique identifiers.
   */
  @Test
  void testRandomBitStrings() {
    HashSet<String> bitStrings = new HashSet<>();
    HashSet<Identifier> identifiers = new HashSet<>();

    for (int i = 0; i < 1_000_000; i++) {
      String bitString = BitFixture.newBitString(Identifier.Size * 8);
      Assertions.assertFalse(bitStrings.contains(bitString));
      bitStrings.add(bitString);
      Identifier identifier = Identifier.bitStringToIdentifier(bitString);
      Assertions.assertFalse(identifiers.contains(identifier));
      identifiers.add(identifier);
    }
  }

  /**
   * Tests that the bit string representation of an identifier is correct, i.e., it is a string of 0s and 1s with length 256 bits.
   */
  @Test
  void testBitStringCorrectness() {
    Identifier identifier = IdentifierFixture.newIdentifier();
    String bitString = identifier.getBitString();
    Assertions.assertEquals(bitString.length(), Identifier.Size * 8);
    for (int i = 0; i < bitString.length(); i++) {
      Assertions.assertTrue(bitString.charAt(i) == '0' || bitString.charAt(i) == '1');
    }
  }
}
