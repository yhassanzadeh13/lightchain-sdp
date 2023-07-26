package unittest.storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import model.lightchain.Block;
import model.lightchain.Identifier;
import storage.Blocks;
import storage.mapdb.BlocksMapDb;

public class TempBlocksMapDB implements Blocks {
  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE_ID = "tempfileID.db";
  private static final String TEMP_FILE_HEIGHT = "tempfileHEIGHT.db";

  private final Blocks blocks;
  private final Path tempDir;

  public TempBlocksMapDB() {
    Path currentRelativePath = Paths.get("");
    try {
      this.tempDir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    this.blocks = new BlocksMapDb(getTemporaryPath(TEMP_FILE_ID), getTemporaryPath(TEMP_FILE_HEIGHT));
  }

  @Override
  public boolean has(Identifier blockId) {
    return this.blocks.has(blockId);
  }

  @Override
  public boolean add(Block block) {
    return this.blocks.add(block);
  }

  @Override
  public boolean remove(Identifier blockId) {
    return this.blocks.remove(blockId);
  }

  @Override
  public Block byId(Identifier blockId) {
    return this.blocks.byId(blockId);
  }

  @Override
  public Block atHeight(long height) {
    return this.blocks.atHeight(height);
  }

  @Override
  public ArrayList<Block> all() {
    return this.blocks.all();
  }

  public void close() {
    this.blocks.close();
    try {
      Files.deleteIfExists(this.tempDir);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private String getTemporaryPath(String label) {
    Path temporaryDir;
    Path currentRelativePath = Paths.get("");
    try {
      temporaryDir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return temporaryDir.toAbsolutePath() + "/" + label;
  }
}
