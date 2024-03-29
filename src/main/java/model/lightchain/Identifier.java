package model.lightchain;

import java.io.Serializable;
import java.util.Arrays;

import io.ipfs.multibase.Multibase;

/**
 * Represents a 32-byte unique identifier for an entity. Normally is computed as the hash value of the entity.
 */
public class Identifier implements Serializable {
  public static final int Size = 32;
  private final byte[] value;

  /**
   * Returns the byte representation of the identifier.
   *
   * @param value identifier in byte representation.
   */
  public Identifier(byte[] value) {
    if (value.length != Size) {
      throw new IllegalArgumentException("Identifier must be 32 bytes long");
    }
    this.value = value.clone();
  }

  /**
   * Returns the byte representation of the identifier.
   *
   * @param identifierString identifier in Base58BTC format.
   */
  public Identifier(String identifierString) {
    try {
      Multibase.decode(identifierString);
    } catch (IllegalStateException e) {
      throw new IllegalArgumentException(String.format("Identifier must be in Base58BTC format: %s", identifierString), e);
    }
    byte[] decodedValue = Multibase.decode(identifierString);
    if (decodedValue.length != Size) {
      throw new IllegalArgumentException("Identifier must be 32 bytes long");
    }
    this.value = decodedValue;
  }

  /**
   * Converts identifier from its byte representation to Base58BTC.
   *
   * @param identifier input identifier in byte representation.
   * @return Base58BTC representation of identifier.
   */
  private static String pretty(byte[] identifier) {
    if (identifier.length != Size) {
      throw new IllegalArgumentException("Identifier must be 32 bytes long");
    }
    return Multibase.encode(Multibase.Base.Base58BTC, identifier);
  }

  /**
   * Returns if objects equal.
   *
   * @param o an identifier object.
   * @return true if objcets equal.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    Identifier that = (Identifier) o;

    if (value.length != that.value.length) {
      return false;
    }
    return Arrays.equals(value, that.value);
  }

  /**
   * Return the hashCode.
   *
   * @return hashCode.
   */
  @Override
  public int hashCode() {
    return Arrays.hashCode(value);
  }

  public byte[] getBytes() {
    return this.value.clone();
  }

  /**
   * Returns string representation of identifier in Base58BTC.
   *
   * @return string representation of identifier in Base58BTC.
   */
  public String toString() {
    return pretty(this.value);
  }

  /**
   * Compares this identifier with the other identifier.
   *
   * @param other represents other identifier to compared to.
   * @return 0 if two identifiers are equal, 1 if this identifier is greater than other,
   *     -1 if other identifier is greater than this.
   */
  public int comparedTo(Identifier other) {
    int result = Arrays.compare(this.value, other.value);
    return Integer.compare(result, 0);
  }
}
