package storage.mapdb;

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.lightchain.Block;
import model.lightchain.Identifier;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.HTreeMap;
import org.mapdb.Serializer;
import storage.Blocks;

/**
 * Implementation of BlocksMapDb interface.
 */
public class BlocksMapDb implements Blocks {
  private static final String MAP_NAME_ID = "blocks_map_id";
  private static final String MAP_NAME_HEIGHT = "blocks_map_height";
  private final DB dbId;
  private final DB dbHeight;
  private final ReentrantReadWriteLock lock;
  private final HTreeMap blocksIdMap;
  private final HTreeMap<Integer, Identifier> blocksHeightMap;

  /**
   * Creates blocks mapdb.
   *
   * @param filePathId     of id,block mapdb.
   * @param filePathHeight of height,id mapdb.
   */
  @SuppressWarnings("unchecked")
  public BlocksMapDb(String filePathId, String filePathHeight) {
    // TODO: file paths consolidated.
    this.dbId = DBMaker.fileDB(filePathId).make();
    this.lock = new ReentrantReadWriteLock();
    blocksIdMap = this.dbId.hashMap(MAP_NAME_ID).keySerializer(Serializer.BYTE_ARRAY).createOrOpen();
    this.dbHeight = DBMaker.fileDB(filePathHeight).make();
    blocksHeightMap = (HTreeMap<Integer, Identifier>) this.dbHeight.hashMap(MAP_NAME_HEIGHT).createOrOpen();
  }

  /**
   * Checks existence of block on the database.
   *
   * @param blockId Identifier of block.
   * @return true if a block with that identifier exists, false otherwise.
   */
  @Override
  public boolean has(Identifier blockId) {
    boolean hasBoolean;
    try {
      lock.readLock().lock();
      hasBoolean = blocksIdMap.containsKey(blockId.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    return hasBoolean;
  }

  /**
   * Adds block to the database.
   *
   * @param block given block to be added.
   * @return true if block did not exist on the database,
   *     false if block is already in database.
   */
  @Override
  public boolean add(Block block) {
    boolean addBooleanId;
    // TODO: refactor to Long
    Integer height = (int) block.getHeight();
    try {
      lock.writeLock().lock();
      // if the block.id() key and the block was absent in IdMap, add it and return true
      addBooleanId = blocksIdMap.putIfAbsentBoolean(block.id().getBytes(), block);
      if (addBooleanId) {
        blocksHeightMap.compute(height, (key, value) -> (value == null) ? block.id() : null);   //TODO: implement else case
      }
    } finally {
      lock.writeLock().unlock();
    }
    return addBooleanId;
  }

  /**
   * Removes block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return true if block exists on database and removed successfully,
   *     false if block does not exist on database.
   */
  @Override
  public boolean remove(Identifier blockId) {
    boolean removeBoolean;
    try {
      lock.writeLock().lock();
      Block block = byId(blockId);
      removeBoolean = blocksIdMap.remove(blockId.getBytes(), block);
      if (removeBoolean) {
        blocksHeightMap.remove((int) block.getHeight(), blockId);
      }
    } finally {
      lock.writeLock().unlock();
    }
    return removeBoolean;
  }

  /**
   * Returns the block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return the block itself if exists and null otherwise.
   */
  @Override
  public Block byId(Identifier blockId) {
    Block block;
    try {
      lock.readLock().lock();
      block = (Block) blocksIdMap.get(blockId.getBytes());
    } finally {
      lock.readLock().unlock();
    }
    return block;
  }

  /**
   * Returns the block with the given height.
   *
   * @param height height of the block.
   * @return the block id itself if exists and null otherwise.
   */
  @Override
  public Block atHeight(long height) {
    Block block = null;
    try {
      lock.readLock().lock();
      // TODO: fix the height for long.
      final Identifier blockId = blocksHeightMap.get((int) height);
      if (blockId != null) {
        block = byId(blockId);
      }
    } finally {
      lock.readLock().unlock();
    }
    return block;
  }

  /**
   * Returns all blocks stored in database.
   *
   * @return all stored blocks in database.
   */
  @Override
  public ArrayList<Block> all() {
    ArrayList<Block> allBlocks = new ArrayList<>();
    for (Object block : blocksIdMap.values()) {
      allBlocks.add((Block) block);
    }
    return allBlocks;
  }

  /**
   * Close the db.
   */
  public void close() {
    dbId.close();
    dbHeight.close();
  }
}
