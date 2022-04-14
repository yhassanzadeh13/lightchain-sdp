package storage.mapdb;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import model.lightchain.Block;
import model.lightchain.Identifier;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.mapdb.Serializer;
import org.mapdb.serializer.SerializerArrayTuple;
import storage.Blocks;


/**
 * Implementation of Transactions interface.
 */
public class BlocksMapDb implements Blocks {
  private final DB db;
  private final ReentrantReadWriteLock lock;
  private static final String MAP_NAME = "blocks_map";
  private final BTreeMap<Object[],Block> blocksMap;

  public BlocksMapDb(String filePath) {
    this.db = DBMaker.fileDB(filePath).make();
    this.lock = new ReentrantReadWriteLock();
    blocksMap = (BTreeMap<Object[], Block>) this.db.treeMap(MAP_NAME)
        .keySerializer(new SerializerArrayTuple(Serializer.BYTE_ARRAY,Serializer.INTEGER))
        .createOrOpen();
  }

  /**
   * Checks existence of block on the database.
   *
   * @param blockId Identifier of block.
   * @return true if a block with that identifier exists, false otherwise.
   */
  @Override
  public boolean has(Identifier blockId) {
for(Map.Entry<Object[],Block> entry : blocksMap.entrySet()){
  Object[] objects= entry.getKey();
  byte[] bytes = (byte[]) objects[0];
  if(Arrays.equals(bytes,blockId.getBytes())){
    return true;
  }
}return false;
  }

  /**
   * Adds block to the database.
   *
   * @param block given block to be added.
   * @return true if block did not exist on the database, false if block is already in
   * database.
   */
  @Override
  public boolean add(Block block) {

    return blocksMap.putIfAbsentBoolean(new Object[]{block.id().getBytes(),block.getHeight()},block);
  }

  /**
   * Removes block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return true if block exists on database and removed successfully, false if block does not exist on
   * database.
   */
  @Override
  public boolean remove(Identifier blockId) {
    for(Object[] objects : blocksMap.keySet()){
      if(objects[0] == blockId.getBytes()){
        return blocksMap.remove(objects,blocksMap.get(objects));
      }
    }
    return false;
  }

  /**
   * Returns the block with given identifier.
   *
   * @param blockId identifier of the block.
   * @return the block itself if exists and null otherwise.
   */
  @Override
  public Block byId(Identifier blockId) {
    for(Object[] objects : blocksMap.keySet()){
      if(objects[0] == blockId.getBytes()){
        return blocksMap.get(objects);
      }
    }
    return null;
  }

  /**
   * Returns the block with the given height.
   *
   * @param height height of the block.
   * @return the block itself if exists and null otherwise.
   */
  @Override
  public Block atHeight(int height) {
    for(Object[] objects : blocksMap.keySet()){
      if((Integer) objects[1] == height){
        return blocksMap.get(objects);
      }
    }
    return null;
  }

  /**
   * Returns all blocks stored in database.
   *
   * @return all stored blocks in database.
   */
  @Override
  public ArrayList<Block> all() {
    ArrayList<Block> allBlocks =new ArrayList<>();
    allBlocks.addAll(blocksMap.getValues());
    return allBlocks;
  }
}
