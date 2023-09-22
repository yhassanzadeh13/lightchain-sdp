package model.lightchain;

import java.io.Serializable;

/**
 * Represents the semantic of direction.
 * A Direction can be either LEFT or RIGHT.
 */
public class Direction implements Serializable {
  /**
   * The value of the direction; either "Left" or "Right".
   */
  private final String value;

  /**
   * The public static instances of Left direction.
   */
  public static final Direction LEFT = new Direction("Left");

  /**
   * The public static instances of Right direction.
   */
  public static final Direction RIGHT = new Direction("Right");

  /**
   * Constructor.
   *
   * @param value The value of the direction; either "Left" or "Right".
   */
  private Direction(String value) {
    this.value = value;
  }

  /**
   * Returns true if the direction is RIGHT.
   *
   * @return true if the direction is RIGHT.
   */
  public boolean isRight() {
    return this == RIGHT;
  }

  /**
   * Returns true if the direction is LEFT.
   *
   * @return true if the direction is LEFT.
   */
  public boolean isLeft() {
    return this == LEFT;
  }

  @Override
  public String toString() {
    return value;
  }
}