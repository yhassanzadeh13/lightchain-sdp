package unittest.fixtures;

import java.util.Random;

/**
 * Bit Fixture class that creates random bit objects to be used in testing.
 */
public class BitFixture {
  /**
   * Random object used to generate random bits.
   */
  private static final Random random = new Random();

  /**
   * Generates a random bit string; a string of 1s and 0s.
   *
   * @param length length of bit string.
   * @return random bit string.
   */
  public static String newBitString(int length) {
    StringBuilder bitString = new StringBuilder();
    for (int i = 0; i < length; i++) {
      if (random.nextBoolean()) {
        bitString.append("1");
      } else {
        bitString.append("0");
      }
    }
    return bitString.toString();
  }
}
