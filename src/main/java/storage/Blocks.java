package storage;

import java.util.ArrayList;

import model.lightchain.Block;
import model.lightchain.Identifier;

/**
 * Persistent module for storing blocks on the disk.
 */
public interface Blocks {
  /**
   * Checks existence of block on the database.
   *
   * @param blockId Identifier of block.
   * @return true if a block with that identifier exists, false otherwise.
   */
  boolean has(Identifier blockId);

  /**
   * Adds block to the database.
   *
   * @param block given block to be added.
   * @return true if block did not exist on the database, false if block is already in
   *     database.
   */
  boolean add(Block block);

  /**
   * Removes block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return true if block exists on database and removed successfully, false if block does not exist on
   *     database.
   */
  boolean remove(Identifier blockId);

  /**
   * Returns the block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return the block itself if exists and null otherwise.
   */
  Block byId(Identifier blockId);

  /**
   * Returns the block with the given height.
   *
   * @param height height of the block.
   * @return the block itself if exists and null otherwise.
   */
  Block atHeight(long height);

  /**
   * Returns all blocks stored in database.
   *
   * @return all stored blocks in database.
   */
  ArrayList<Block> all();

  /**
   * Closes the database.
   */
  void close();
}
