package model.lightchain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Tests the Direction data type.
 */
public class DirectionTest {

  @Test
  public void testIsRight() {
    assertTrue(Direction.RIGHT.isRight());
    assertFalse(Direction.LEFT.isRight());
  }

  @Test
  public void testIsLeft() {
    assertTrue(Direction.LEFT.isLeft());
    assertFalse(Direction.RIGHT.isLeft());
  }

  @Test
  public void testToString() {
    assertEquals("Right", Direction.RIGHT.toString());
    assertEquals("Left", Direction.LEFT.toString());
  }

  @Test
  public void testEquality() {
    assertEquals(Direction.LEFT, Direction.LEFT);
    assertEquals(Direction.RIGHT, Direction.RIGHT);
    assertNotEquals(Direction.LEFT, Direction.RIGHT);
    assertNotEquals(Direction.RIGHT, Direction.LEFT);
  }

  @Test
  public void testSameObject() {
    assertSame(Direction.LEFT, Direction.LEFT);
    assertSame(Direction.RIGHT, Direction.RIGHT);
    assertNotSame(Direction.LEFT, Direction.RIGHT);
    assertNotSame(Direction.RIGHT, Direction.LEFT);
  }
}

