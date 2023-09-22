package crypto;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import model.codec.EncodedEntity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;

/**
 * Implements SHA3-256 hashing functionality.
 */
public class Sha3256Hasher implements Hasher {
  private static final String HASH_ALG_SHA_3_256 = "SHA3-256";

  private static byte[] concat(final byte[] e1, final byte[] e2) {
    byte[] result = new byte[e1.length + e2.length];
    System.arraycopy(e1, 0, result, 0, e1.length);
    System.arraycopy(e2, 0, result, e1.length, e2.length);
    return result;
  }

  /**
   * Computes hash of the given encoded entity.
   *
   * @param e input encoded entity.
   * @return SHA3-256 hash object of the entity.
   */
  @Override
  public Sha3256Hash computeHash(EncodedEntity e) {
    return this.computeHash(e.getBytes());
  }

  /**
   * Computes hash of the given identifier.
   *
   * @param id input identifier
   * @return SHA3-256 hash object of the entity
   */
  public Sha3256Hash computeHash(Identifier id) {
    return this.computeHash(id.getBytes());
  }

  /**
   * Computes hash of the given bytes.
   *
   * @param bytes input bytes.
   * @return SHA3-256 hash object of the given bytes.
   */
  public Sha3256Hash computeHash(byte[] bytes) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      byte[] hashValue = md.digest(bytes);
      return new Sha3256Hash(hashValue);
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }

  /**
   * Hashing of two given byte arrays.
   *
   * @param b1 first byte array.
   * @param b2 second byte array.
   * @return SHA3-256 hash object of the commutative concatenation of the two byte arrays.
   */
  public Sha3256Hash computeHash(byte[] b1, byte[] b2) {
    try {
      MessageDigest md = MessageDigest.getInstance(HASH_ALG_SHA_3_256);
      return new Sha3256Hash(md.digest(concat(b1, b2)));
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException(HASH_ALG_SHA_3_256 + "algorithm not found.", ex);
    }
  }

  /**
   * Hashing of two SHA3-256 hash objects.
   *
   * @param h1 first SHA3-256 hash object.
   * @param h2 second SHA3-256 hash object.
   * @return SHA3-256 hash object of the concatenation of the two SHA3-256 hash objects.
   */
  public Sha3256Hash computeHash(Sha3256Hash h1, Sha3256Hash h2) {
    return computeHash(h1.getBytes(), h2.getBytes());
  }
}
