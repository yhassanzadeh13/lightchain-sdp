package storage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.lightchain.Block;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import storage.mapdb.BlocksMapDb;
import unittest.fixtures.BlockFixture;

/**
 * Encapsulates tests for block database.
 */
public class BlocksTest {

  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE_ID = "tempfileID.db";
  private static final String TEMP_FILE_HEIGHT = "tempfileHEIGHT.db";
  private Path tempdir;
  private ArrayList<Block> allBlocks;
  private BlocksMapDb db;

  /**
   * Initializes database.
   *
   * @throws IOException if creating temporary directory faces unhappy path.
   */
  @BeforeEach
  void setup() throws IOException {
    Path currentRelativePath = Paths.get("");
    tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    db = new BlocksMapDb(tempdir.toAbsolutePath() + "/" + TEMP_FILE_ID,
        tempdir.toAbsolutePath() + "/" + TEMP_FILE_HEIGHT);
    allBlocks = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      allBlocks.add(BlockFixture.newBlock());
    }
  }

  /**
   * Closes database.
   *
   * @throws IOException if deleting temporary directory faces unhappy path.
   */
  @AfterEach
  void cleanup() throws IOException {
    db.close();
    FileUtils.deleteDirectory(new File(tempdir.toString()));
  }

  /**
   * When adding 10 new blocks SEQUENTIALLY, the Add method must return true for all of them. Moreover, after
   * adding blocks is done, querying the Has method for each of the block should return true. After adding all blocks
   * are done, each block must be retrievable using both its id (byId) as well as its height (byHeight). Also, when
   * querying All method, list of all 10 block must be returned.
   */
  @Test
  void sequentialAddTest() {
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.add(block));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.has(block.id()));
    }
    for (Block block : allBlocks) {
      Assertions.assertEquals(block, db.atHeight(block.getHeight()));
      Assertions.assertEquals(block.id(), db.atHeight(block.getHeight()).id());
    }
    for (Block block : allBlocks) {
      Assertions.assertEquals(block, db.byId(block.id()));
      Assertions.assertEquals(block.id(), db.byId(block.id()).id());
    }
    ArrayList<Block> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Block block : all) {
      Assertions.assertTrue(allBlocks.contains(block));
    }
  }

  /**
   * When adding 10 new blocks CONCURRENTLY, the Add method must return true for all of them. Moreover, after
   * adding blocks is done, querying the Has method for each of the block should return true. After adding all blocks
   * are done, each block must be retrievable using both its id (byId) as well as its height (byHeight). Also, when
   * querying All method, list of all 10 block must be returned.
   */
  @Test
  void concurrentAddTest() {
    /*
    Adding all blocks concurrently.
    */
    this.addAllBlocksConcurrently(true);

    /*
    All blocks should be retrievable
    */
    this.checkForHasConcurrently(0);
    this.checkForByIdConcurrently(0);
    this.checkForByHeightConcurrently(0);
    this.checkForAllConcurrently(0);
  }

  /**
   * Add 10 new blocks SEQUENTIALLY, check that they are added correctly, i.e., while adding each block
   * Add must return
   * true, Has returns true for each of them, each block is retrievable by both its height and its identifier,
   * and All returns list of all of them. Then Remove the first 5 blocks sequentially.
   * While Removing each of them, the Remove should return true. Then query all 10 blocks using has, byId,
   * and byHeight.
   */
  @Test
  void removeFirstFiveTest() {
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.add(block));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.has(block.id()));
    }
    for (Block block : allBlocks) {
      Assertions.assertEquals(block, db.atHeight(block.getHeight()));
    }
    for (Block block : allBlocks) {
      Assertions.assertEquals(block, db.byId(block.id()));
    }
    ArrayList<Block> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Block block : all) {
      Assertions.assertTrue(allBlocks.contains(block));
    }
    for (int i = 0; i < 5; i++) {
      Assertions.assertTrue(db.remove(allBlocks.get(i).id()));
    }
    for (int i = 0; i < 10; i++) {
      Block block = allBlocks.get(i);
      if (i < 5) {
        Assertions.assertFalse(db.has(block.id()));
        Assertions.assertFalse(db.all().contains(block));
        Assertions.assertNull(db.byId(block.id()));
        Assertions.assertNull(db.atHeight(block.getHeight()));
      } else {
        Assertions.assertTrue(db.has(block.id()));
        Assertions.assertTrue(db.all().contains(allBlocks.get(i)));
        Assertions.assertEquals(block, db.atHeight(block.getHeight()));
        Assertions.assertEquals(block, db.byId(allBlocks.get(i).id()));
      }
    }
  }

  /**
   * Add 10 new blocks SEQUENTIALLY, check that they are added correctly, i.e., while adding each block
   * Add must return
   * true, Has returns true for each of them, each block is retrievable by both its height and its identifier,
   * and All returns list of all of them. Then Remove the first 5 blocks sequentially.
   * While Removing each of them, the Remove should return true. Then query all 10 blocks using has, byId,
   * and byHeight.
   */
  @Test
  void concurrentRemoveFirstFiveTest() {
    /*
    Adding all blocks concurrently.
    */
    this.addAllBlocksConcurrently(true);

    /*
    All blocks should be retrievable using their id or height.
    */
    this.checkForByIdConcurrently(0);
    this.checkForByHeightConcurrently(0);
    this.checkForHasConcurrently(0);
    this.checkForAllConcurrently(0);

    /*
    Removing first 5 concurrently
     */
    this.removeBlocksTill(5);

    /*
    first five blocks must not be retrievable,
    the rest must be.
     */
    this.checkForByHeightConcurrently(5);
    this.checkForByIdConcurrently(5);
    this.checkForHasConcurrently(5);
    this.checkForAllConcurrently(5);
  }

  /**
   * Add 10 new blocks SEQUENTIALLY and check that all of them are added correctly, i.e., while adding each block
   * Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
   * block is retrievable using its identifier (byId) and height (byHeight).
   * Then try Adding all of them again, and Add should return false for each of them,
   * while has should still return true, and byId and byHeight should be
   * able to retrieve the block.
   */
  @Test
  void duplicationTest() {
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.add(block));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.has(block.id()));
    }

    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.atHeight(block.getHeight())));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.byId(block.id())));
    }
    ArrayList<Block> all = db.all();
    Assertions.assertEquals(all.size(), 10);
    for (Block block : all) {
      Assertions.assertTrue(allBlocks.contains(block));
    }
    for (Block block : allBlocks) {
      Assertions.assertFalse(db.add(block));
    }
    /*
   After trying duplication, check again.
    */
    for (Block block : allBlocks) {
      Assertions.assertTrue(db.has(block.id()));
      Assertions.assertEquals(db.atHeight(block.getHeight()), block);
    }

    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.atHeight(block.getHeight())));
    }
    for (Block block : allBlocks) {
      Assertions.assertTrue(allBlocks.contains(db.byId(block.id())));
    }
  }

  /**
   * Add 10 new blocks CONCURRENTLY and check that all of them are added correctly, i.e., while adding each block
   * Add must return true, has returns true for each of them, and All returns list of all of them. Moreover, each
   * block is retrievable using its identifier (byId) and height (byHeight).
   * Then try Adding all of them again, and Add should return false for each of them,
   * while has should still return true, and byId and byHeight should be
   * able to retrieve the block.
   */
  @Test
  void concurrentDuplicationTest() {
    /*
    Adding all blocks concurrently.
     */
    this.addAllBlocksConcurrently(true);

    /*
    All blocks should be retrievable using their id or height.
    */
    this.checkForByIdConcurrently(0);
    this.checkForByHeightConcurrently(0);
    this.checkForHasConcurrently(0);
    this.checkForAllConcurrently(0);

    /*
    Adding all blocks again concurrently, all should fail due to duplication.
     */
    this.addAllBlocksConcurrently(false);

    /*
    Again, all blocks should be retrievable using their id or height.
    */
    this.checkForByIdConcurrently(0);
    this.checkForByHeightConcurrently(0);
    this.checkForHasConcurrently(0);
    this.checkForAllConcurrently(0);
  }

  /**
   * Removes blocks from blocks storage database till the given index concurrently.
   *
   * @param till exclusive index of the last block being removed.
   */
  private void removeBlocksTill(int till) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch doneRemove = new CountDownLatch(till);
    Thread[] removeThreads = new Thread[till];
    for (int i = 0; i < till; i++) {
      int finalI = i;
      removeThreads[i] = new Thread(() -> {
        if (!db.remove(allBlocks.get(finalI).id())) {
          threadError.getAndIncrement();
        }
        doneRemove.countDown();
      });
    }

    for (Thread t : removeThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = doneRemove.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Adds all blocks to the block storage database till the given index concurrently.
   *
   * @param expectedResult expected boolean result after each insertion; true means block added successfully,
   *                       false means block was not added successfully.
   */
  private void addAllBlocksConcurrently(boolean expectedResult) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch addDone = new CountDownLatch(allBlocks.size());
    Thread[] addThreads = new Thread[allBlocks.size()];
    /*
    Adding all blocks concurrently.
     */
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      addThreads[i] = new Thread(() -> {
        if (db.add(allBlocks.get(finalI)) != expectedResult) {
          threadError.getAndIncrement();
        }
        addDone.countDown();
      });
    }
    for (Thread t : addThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = addDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Checks existence of blocks in the block storage database starting from the given index.
   *
   * @param from inclusive index of the first block to check.
   */
  private void checkForHasConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch hasDone = new CountDownLatch(allBlocks.size());
    Thread[] hasThreads = new Thread[allBlocks.size()];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      Block block = allBlocks.get(i);

      hasThreads[i] = new Thread(() -> {
        if (finalI < from) {
          // blocks should not exist
          if (this.db.has(block.id())) {
            threadError.incrementAndGet();
          }
        } else {
          // block should exist
          if (!this.db.has(block.id())) {
            threadError.getAndIncrement();
          }
        }

        hasDone.countDown();
      });
    }

    for (Thread t : hasThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = hasDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Checks retrievability of blocks from the block storage database by identifier starting from the given index.
   *
   * @param from inclusive index of the first block to check.
   */
  private void checkForByIdConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch getDone = new CountDownLatch(allBlocks.size());
    Thread[] getThreads = new Thread[allBlocks.size()];
    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      Block block = allBlocks.get(i);

      getThreads[i] = new Thread(() -> {
        Block got = db.byId(block.id());
        if (finalI < from) {
          // blocks should not exist
          if (got != null) {
            threadError.incrementAndGet();
          }
        } else {
          // block should be retrievable
          if (!block.equals(got)) {
            threadError.getAndIncrement();
          }
          if (!block.id().equals(got.id())) {
            threadError.getAndIncrement();
          }
        }
        getDone.countDown();
      });
    }

    for (Thread t : getThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = getDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Checks retrievability of blocks from the block storage database by height starting from the given index.
   *
   * @param from inclusive index of the first block to check.
   */
  private void checkForByHeightConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch heightDone = new CountDownLatch(allBlocks.size());
    Thread[] heightThreats = new Thread[allBlocks.size()];
    for (int i = 0; i < allBlocks.size(); i++) {
      final int finalI = i;
      Block block = allBlocks.get(i);

      heightThreats[i] = new Thread(() -> {
        Block got = db.atHeight(block.getHeight());
        if (finalI < from) {
          // blocks should not exist
          if (got != null) {
            threadError.incrementAndGet();
          }
        } else {
          // block should be retrievable
          if (!block.equals(got)) {
            threadError.getAndIncrement();
          }
          if (!block.id().equals(got.id())) {
            threadError.getAndIncrement();
          }
        }

        heightDone.countDown();
      });
    }

    for (Thread t : heightThreats) {
      t.start();
    }
    try {
      boolean doneOneTime = heightDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Checks retrievability of blocks from the block storage database starting from the given index.
   *
   * @param from inclusive index of the first block to check.
   */
  private void checkForAllConcurrently(int from) {
    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch doneAll = new CountDownLatch(allBlocks.size());
    Thread[] allThreads = new Thread[allBlocks.size()];
    ArrayList<Block> all = db.all();

    for (int i = 0; i < allBlocks.size(); i++) {
      int finalI = i;
      final Block block = allBlocks.get(i);

      allThreads[i] = new Thread(() -> {
        if (finalI < from) {
          // blocks should not exist
          if (all.contains(block)) {
            threadError.incrementAndGet();
          }
        } else {
          // block should exist
          if (!all.contains(block)) {
            threadError.getAndIncrement();
          }
        }
        doneAll.countDown();
      });
    }

    for (Thread t : allThreads) {
      t.start();
    }
    try {
      boolean doneOneTime = doneAll.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    Assertions.assertEquals(0, threadError.get());
  }
}
