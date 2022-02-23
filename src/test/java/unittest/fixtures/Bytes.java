package unittest.fixtures;

import java.util.Random;

public class Bytes {
  private final static Random random = new Random();

  /**
   * Generates a random byte array.
   * @param length length of byte array.
   * @return random byte array.
   */
  public static byte[] ByteArrayFixture(int length){
    byte[] arr = new byte[length];
    random.nextBytes(arr);
    return arr;
  }


  /**
   * Returns a byte fixture. Note that in Java an 8-bit byte represents
   * a value between -128 (10000000) to 127 (01111111).
   *
   * @return a randomly generated byte.
   */
  public static byte ByteFixture() {
    StringBuilder bStr = new StringBuilder();
    for (int i = 0; i < 8; i++) {
      if (random.nextBoolean()) {
        bStr.append("1");
      } else {
        bStr.append("0");
      }
    }

    return Byte.parseByte(bStr.toString(), 2);
  }
}