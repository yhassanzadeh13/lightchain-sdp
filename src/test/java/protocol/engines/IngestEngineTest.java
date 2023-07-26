package protocol.engines;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import model.Entity;
import model.codec.EntityType;
import model.crypto.PublicKey;
import model.crypto.Signature;
import model.lightchain.Account;
import model.lightchain.Assignment;
import model.lightchain.Block;
import model.lightchain.BlockProposal;
import model.lightchain.Identifier;
import model.lightchain.Transaction;
import model.lightchain.ValidatedTransaction;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.shaded.org.apache.commons.io.FileUtils;
import protocol.Parameters;
import protocol.assigner.AssignerInf;
import state.Snapshot;
import state.State;
import storage.Blocks;
import storage.Identifiers;
import storage.Transactions;
import storage.mapdb.BlocksMapDb;
import unittest.fixtures.AccountFixture;
import unittest.fixtures.BlockFixture;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.ValidatedTransactionFixture;
import unittest.storage.TempBlocksMapDB;

/**
 * Encapsulates tests for ingest engine implementation.
 */
public class IngestEngineTest {
  private static final String TEMP_DIR = "tempdir";
  private static final String TEMP_FILE_ID = "tempfileID.db";
  private static final String TEMP_FILE_HEIGHT = "tempfileHEIGHT.db";

  /**
   * Get the temporary path.
   *
   * @param label dir name, height or id.
   * @return temporary path.
   */
  private static String getTemporaryPath(String label) {
    Path temporaryDir = null;
    Path currentRelativePath = Paths.get("");
    try {
      temporaryDir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
    } catch (IOException e) {
      Assertions.fail();
    }
    return temporaryDir.toAbsolutePath() + "/" + label;
  }

  /**
   * Evaluates that when a new validated block arrives at ingest engine,
   * the engine adds the block to its mocked block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   * <p>
   * This test is using a mocked block storage database.
   */
  @Test
  public void testValidatedSingleBlock_MockedStorage() {
    Blocks blocks = mock(Blocks.class);
    Block block = BlockFixture.newBlock();
    when(blocks.has(block.id())).thenReturn(false);
    testValidatedSingleBlock(blocks, block);
    verify(blocks, times(1)).add(block);
  }

  /**
   * Evaluates that when a new validated block arrives at ingest engine,
   * the engine adds the block to its real block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testValidatedSingleBlock_RealStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();
      Block block = BlockFixture.newBlock();
      testValidatedSingleBlock(blocks, block);
      Assertions.assertTrue(blocks.has(block.id()));
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }
  }

  /**
   * Evaluates that when two validated blocks arrive at ingest engine SEQUENTIALLY,
   * the engine adds the blocks to its mocked block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   * <p>
   * This test is using a mocked blocks storage database.
   */
  @Test
  public void testValidatedTwoBlocks_MockedStorage() {
    Blocks blocks = mock(Blocks.class);

    Block block1 = BlockFixture.newBlock();
    Block block2 = BlockFixture.newBlock();

    when(blocks.has(block1.id())).thenReturn(false);
    when(blocks.has(block2.id())).thenReturn(false);

    testValidatedTwoBlocks(blocks, block1, block2);

    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);
  }

  /**
   * Evaluates that when two validated blocks arrive at ingest engine SEQUENTIALLY,
   * the engine adds the blocks to its real block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testValidatedTwoBlocks_RealStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();
      Block block1 = BlockFixture.newBlock();
      Block block2 = BlockFixture.newBlock();
      testValidatedTwoBlocks(blocks, block1, block2);

      Assertions.assertTrue(blocks.has(block1.id()));
      Assertions.assertTrue(blocks.has(block2.id()));
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }

  }

  /**
   * Test validated single block for mocked and real versions.
   *
   * @param blocks mocked or real block.
   */
  // TODO: this method should be refactored with parameters option.
  public void testValidatedTwoBlocks(Blocks blocks, Block block1, Block block2) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(block1, block2)), seenEntities, transactionIds, pendingTransactions, blocks);

    ingestEngine.process(block1);
    ingestEngine.process(block2);

    verifyBlockHappyPathCalled(block1, blocks, pendingTransactions, transactionIds, seenEntities);

    verifyBlockHappyPathCalled(block2, blocks, pendingTransactions, transactionIds, seenEntities);
  }

  /**
   * Evaluates that when two validated blocks arrive at ingest engine concurrently,
   * the engine adds the blocks to its mocked block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   * <p>
   * This test is using a mocked block storage database.
   */
  @Test
  public void testValidatedTwoBlocks_Concurrently_MockedBlocks() {
    Blocks blocks = mock(Blocks.class);
    Block block1 = BlockFixture.newBlock();
    Block block2 = BlockFixture.newBlock();
    when(blocks.has(block1.id())).thenReturn(false);
    when(blocks.has(block2.id())).thenReturn(false);
    testValidatedTwoBlocks_Concurrently(blocks, block1, block2);

    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);

  }

  /**
   * Evaluates that when two validated blocks arrive at ingest engine concurrently,
   * the engine adds the blocks to its real block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testValidatedTwoBlocks_Concurrently_RealBlocks() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();

      Block block1 = BlockFixture.newBlock();
      Block block2 = BlockFixture.newBlock();

      testValidatedTwoBlocks_Concurrently(blocks, block1, block2);
      Assertions.assertTrue(blocks.has(block1.id()));
      Assertions.assertTrue(blocks.has(block2.id()));
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }
  }

  /**
   * Test validated two blocks for mocked and real versions concurrently.
   *
   * @param blocks mocked or real block.
   */
  // TODO: this method should be refactored with parameters option.
  public void testValidatedTwoBlocks_Concurrently(Blocks blocks, Block block1, Block block2) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(block1, block2)), seenEntities, transactionIds, pendingTransactions, blocks);

    this.processEntitiesConcurrently(ingestEngine, new ArrayList<>(Arrays.asList(block1, block2)));

    // verification for block 1
    verifyBlockHappyPathCalled(block1, blocks, pendingTransactions, transactionIds, seenEntities);

    // verification for block 2
    verifyBlockHappyPathCalled(block2, blocks, pendingTransactions, transactionIds, seenEntities);
  }

  /**
   * Evaluates that when two same validated blocks arrive at ingest engine (second one should be ignored),
   * the engine adds the blocks to its mocked block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   * <p>
   * This test is using a mocked block storage database.
   */
  @Test
  public void testValidated_SameTwoBlocks_MockedStorage() {
    Blocks blocks = mock(Blocks.class);
    Block block = BlockFixture.newBlock();
    when(blocks.has(block.id())).thenReturn(false);
    testValidated_SameTwoBlocks(blocks, block);
    verify(blocks, times(1)).add(block);
  }

  /**
   * Evaluates that when two same validated blocks arrive at ingest engine (second one should be ignored),
   * the engine adds the blocks to its real block storage database.
   * The engine also adds hash of all the transactions of blocks into its "transactions" database.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testValidated_SameTwoBlocks_RealStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();
      Block block = BlockFixture.newBlock();
      testValidated_SameTwoBlocks(blocks, block);
      Assertions.assertTrue(blocks.has(block.id()));
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }
  }

  /**
   * Test validated two blocks for mocked and real versions concurrently.
   *
   * @param blocks mocked or real blocks.
   */
  // TODO: this method should be refactored with parameters option.
  public void testValidated_SameTwoBlocks(Blocks blocks, Block block) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    when(seenEntities.has(block.id())).thenReturn(false);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(List.of(block)), seenEntities, transactionIds, pendingTransactions, blocks);

    ingestEngine.process(block);
    when(seenEntities.has(block.id())).thenReturn(true); // block is already seen
    ingestEngine.process(block);

    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
    verify(seenEntities, times(2)).has(block.id());
  }

  /**
   * Evaluates that when a new validated block (with shared transactions in pendingTx) arrive at ingest engine,
   * the engine adds the blocks to its real block storage database.
   * The engine also removes hash of the transactions of blocks from pendingTransactions.
   * <p>
   * This test is using a mocked block storage database.
   */
  @Test
  public void testValidatedBlock_PendingTransaction_MockedStorage() {
    Blocks blocks = mock(Blocks.class);
    Block block = BlockFixture.newBlock();
    when(blocks.has(block.id())).thenReturn(false);
    testValidatedBlock_PendingTransaction(blocks, block);
    verify(blocks, times(1)).add(block);
  }

  /**
   * Evaluates that when a new validated block (with shared transactions in pendingTx) arrive at ingest engine,
   * the engine adds the blocks to its real block storage database.
   * The engine also removes hash of the transactions of blocks from pendingTransactions.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testValidatedBlock_PendingTransaction_RealStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();
      Block block = BlockFixture.newBlock();
      testValidatedBlock_PendingTransaction(blocks, block);
      Assertions.assertTrue(blocks.has(block.id()));
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }
  }

  /**
   * Test validated blocks containing pending transactions for mocked and real versions concurrently.
   *
   * @param blocks mocked or real block.
   */
  // TODO: this method should be refactored with parameters option.
  public void testValidatedBlock_PendingTransaction(Blocks blocks, Block block) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(List.of(block)), seenEntities, transactionIds, pendingTransactions, blocks);

    for (Transaction tx : block.getTransactions()) {
      when(pendingTransactions.has(tx.id())).thenReturn(true);
    }

    ingestEngine.process(block);

    // verification
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
    for (Transaction tx : block.getTransactions()) {
      // all transactions included in the block must be removed from pending transactions.
      verify(pendingTransactions, times(1)).remove(tx.id());
    }
  }

  /**
   * Evaluates that when two new validated blocks (with a shared transactions in pendingTx, disjoint set)
   * arrive at ingest engine, the engine adds the blocks to its real block storage database.
   * The engine also removes the hash of the single shared transaction among blocks from pending transactions.
   * <p>
   * This test is using a mocked block storage database.
   */
  @Test
  public void testConcurrentBlock_DisjointSet_MockedStorage() {
    Blocks blocks = mock(Blocks.class);
    Block block1 = BlockFixture.newBlock();
    Block block2 = BlockFixture.newBlock();
    testConcurrentBlock_SeenTransaction_DisjointSet(blocks, block1, block2);
    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);
  }

  /**
   * Evaluates that when two new validated blocks (with a shared transactions in pendingTx, disjoint set)
   * arrive at ingest engine, the engine adds the blocks to its real block storage database.
   * The engine also removes the hash of the single shared transaction among blocks from pending transactions.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testConcurrentBlock_DisjointSet_RealStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();

      Block block1 = BlockFixture.newBlock();
      Block block2 = BlockFixture.newBlock();
      testConcurrentBlock_SeenTransaction_DisjointSet(blocks, block1, block2);
      Assertions.assertTrue(blocks.has(block1.id()));
      Assertions.assertTrue(blocks.has(block2.id()));
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }
  }

  /**
   * Test concurrent block ingestion containing seen transaction disjoint set mocked and real versions.
   *
   * @param blocks mocked or real block.
   */
  // TODO: this method should be refactored with parameters option.
  private void testConcurrentBlock_SeenTransaction_DisjointSet(Blocks blocks, Block block1, Block block2) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ArrayList<Account> accounts = new ArrayList<>(AccountFixture.newAccounts(10, 10).values());

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(block1, block2)), seenEntities, transactionIds, pendingTransactions, blocks);

    // simulates 1 shared transaction for each block
    when(pendingTransactions.has(block1.getTransactions()[0].id())).thenReturn(true);
    when(pendingTransactions.has(block2.getTransactions()[0].id())).thenReturn(true);

    processEntitiesConcurrently(ingestEngine, new ArrayList<>(Arrays.asList(block1, block2)));

    // verification for block1
    verifyBlockHappyPathCalled(block1, blocks, pendingTransactions, transactionIds, seenEntities);
    // shared transaction should be removed from pending transactions
    verify(pendingTransactions, times(1)).remove(block1.getTransactions()[0].id());

    // verification for block2
    verifyBlockHappyPathCalled(block2, blocks, pendingTransactions, transactionIds, seenEntities);
    // shared transaction should be removed from pending transactions
    verify(pendingTransactions, times(1)).remove(block2.getTransactions()[0].id());
  }

  /**
   * Test concurrent block ingestion containing seen transaction overlapping set mocked and real versions.
   *
   * @param blocks mocked or real block.
   */
  // TODO: this method should be refactored with parameters option.
  private void testConcurrentBlock_OverlappingSet(Blocks blocks, Block block1, Block block2) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(block1, block2)), seenEntities, transactionIds, pendingTransactions, blocks);

    // simulates an overlapping set of shared transactions
    when(pendingTransactions.has(any(Identifier.class))).thenReturn(true);

    processEntitiesConcurrently(ingestEngine, new ArrayList<>(Arrays.asList(block1, block2)));

    // verification for block1
    verifyBlockHappyPathCalled(block1, blocks, pendingTransactions, transactionIds, seenEntities);
    for (Transaction tx : block1.getTransactions()) {
      verify(pendingTransactions, times(1)).remove(tx.id());
    }

    // verification for block2
    verifyBlockHappyPathCalled(block2, blocks, pendingTransactions, transactionIds, seenEntities);
    for (Transaction tx : block2.getTransactions()) {
      verify(pendingTransactions, times(1)).remove(tx.id());
    }
  }

  /**
   * Evaluates that when two new validated blocks (with shared transactions in pendingTx, overlapping set)
   * arrive at ingest engine, the engine adds the blocks to its mocked block storage database.
   * The engine also removes hash of the transactions of blocks from pendingTransactions.
   * <p>
   * This test is using a mocked block storage database.
   */
  @Test
  public void testConcurrentBlock_OverlappingSet_MockedStorage() {
    Blocks blocks = mock(Blocks.class);
    Block block1 = BlockFixture.newBlock();
    Block block2 = BlockFixture.newBlock();
    when(blocks.has(block1.id())).thenReturn(false);
    when(blocks.has(block2.id())).thenReturn(false);
    testConcurrentBlock_OverlappingSet(blocks, block1, block2);
    verify(blocks, times(1)).add(block1);
    verify(blocks, times(1)).add(block2);
  }

  /**
   * Evaluates that when two new validated blocks (with shared transactions in pendingTx, overlapping set)
   * arrive at ingest engine, the engine adds the blocks to its real block storage database.
   * The engine also removes hash of the transactions of blocks from pendingTransactions.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testConcurrentBlock_OverlappingSet_RealStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();

      Block block1 = BlockFixture.newBlock();
      Block block2 = BlockFixture.newBlock();
      testConcurrentBlock_OverlappingSet(blocks, block1, block2);
      Assertions.assertTrue(blocks.has(block1.id()));
      Assertions.assertTrue(blocks.has(block2.id()));
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }

  }

  // TODO: this method should be refactored with parameters option.
  private void testValidated_AlreadyIngestedBlock(Blocks blocks, Block block) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(List.of(block)), seenEntities, transactionIds, pendingTransactions, blocks);

    when(seenEntities.has(block.id())).thenReturn(true); // block is already ingested

    ingestEngine.process(block);

    verify(seenEntities, times(0)).add(block.id());
    verify(seenEntities, times(1)).has(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(pendingTransactions, times(0)).has(tx.id());
      verify(transactionIds, times(0)).add(tx.id());
    }
  }

  /**
   * Evaluates that when an already ingested validated block arrives at ingest engine,
   * the engine discards the block right away.
   * <p>
   * This test is using a mocked block storage database.
   */
  @Test
  public void testValidated_AlreadyIngestedBlock_MockedStorage() {
    Blocks blocks = mock(Blocks.class);
    Block block = BlockFixture.newBlock();
    when(blocks.has(block.id())).thenReturn(false);
    testValidated_AlreadyIngestedBlock(blocks, block);
  }

  /**
   * Evaluates that when an already ingested validated block arrives at ingest engine,
   * the engine discards the block right away.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testValidated_AlreadyIngestedBlock_RealStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();
      Block block = BlockFixture.newBlock();
      testValidated_AlreadyIngestedBlock(blocks, block);
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }

  }

  /**
   * Test validated transaction for mocked and real blokcs storage.
   *
   * @param blocks mocked or real block.
   */
  private void testValidatedTransaction(Blocks blocks) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction tx = ValidatedTransactionFixture.newValidatedTransaction();

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(List.of(tx)), seenEntities, transactionIds, pendingTransactions, blocks);

    ingestEngine.process(tx);

    verifyTransactionHappyPathCalled(tx, seenEntities, transactionIds, pendingTransactions);
  }

  /**
   * Evaluates that when a new validated transaction arrives at ingest engine, with mocked blocks storage,
   * the engine adds hash of the transaction into its pending transactions' database.
   * <p>
   * This test is using a mocked block storage database.
   */
  @Test
  public void testValidatedTransaction_MockedStorage() {
    Blocks blocks = mock(Blocks.class);
    testValidatedTransaction(blocks);
  }

  /**
   * Evaluates that when a new validated transaction arrives at ingest engine, with real blocks storage,
   * the engine adds hash of the transaction into its pending transactions' database.
   * <p>
   * This test is using a real block storage database.
   */
  @Test
  public void testValidatedTransaction_RealStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();
      testValidatedTransaction(blocks);
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }
  }

  /**
   * Test validated two transactions for mocked and real blocks storage.
   *
   * @param blocks mocked or real block.
   */
  // TODO: this method should be refactored with parameters option.
  private void testValidated_TwoTransactions(Blocks blocks) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction tx1 = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedTransaction tx2 = ValidatedTransactionFixture.newValidatedTransaction();
    when(seenEntities.has(any(Identifier.class))).thenReturn(false);
    when(transactionIds.has(any(Identifier.class))).thenReturn(false);
    when(pendingTransactions.has(any(Identifier.class))).thenReturn(false);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(tx1, tx2)), seenEntities, transactionIds, pendingTransactions, blocks);

    ingestEngine.process(tx1);
    ingestEngine.process(tx2);

    verifyTransactionHappyPathCalled(tx1, seenEntities, transactionIds, pendingTransactions);

    verifyTransactionHappyPathCalled(tx2, seenEntities, transactionIds, pendingTransactions);
  }

  /**
   * Evaluates that when two validated transactions arrives at ingest engine sequentially,
   * the engine adds the hashes of the transactions into its pending transactions' database.
   */
  @Test
  public void testValidated_TwoTransactions_MockedBlocksStorage() {
    Blocks blocks = mock(Blocks.class);
    testValidated_TwoTransactions(blocks);
  }

  /**
   * Evaluates that when two validated transactions arrives at ingest engine sequentially,
   * the engine adds the hashes of the transactions into its pending transactions' database.
   */
  @Test
  public void testValidated_TwoTransactions_RealBlocksStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();
      testValidated_TwoTransactions(blocks);
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }
  }

  /**
   * Test validated two transactions concurrently for mocked and real blocks storage.
   *
   * @param blocks mocked or real block.
   */
  // TODO: this method should be refactored with parameters option.
  private void testConcurrent_ValidatedTwoTransactions(Blocks blocks) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction tx1 = ValidatedTransactionFixture.newValidatedTransaction();
    ValidatedTransaction tx2 = ValidatedTransactionFixture.newValidatedTransaction();

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(tx1, tx2)), seenEntities, transactionIds, pendingTransactions, blocks);

    processEntitiesConcurrently(ingestEngine, new ArrayList<>(List.of(tx1, tx2)));

    // verification of tx1
    verifyTransactionHappyPathCalled(tx1, seenEntities, transactionIds, pendingTransactions);

    // verification of tx2
    verifyTransactionHappyPathCalled(tx2, seenEntities, transactionIds, pendingTransactions);
  }

  /**
   * Evaluates that when two validated transactions arrive at ingest engine concurrently, with mocked blocks storage,
   * the engine adds the hashes of the transactions into its pending transactions' database.
   */
  @Test
  public void testConcurrent_TwoTransactions_MockedBlockStorage() {
    Blocks blocks = mock(Blocks.class);
    testConcurrent_ValidatedTwoTransactions(blocks);
  }

  /**
   * Evaluates that when two validated transactions arrive at ingest engine concurrently, with real blocks storage,
   * the engine adds the hashes of the transactions into its pending transactions' database.
   */
  @Test
  public void testConcurrent_TwoTransactions_RealBlockStorage() {
    Blocks blocks = null;
    try {
      blocks = new TempBlocksMapDB();
      testConcurrent_ValidatedTwoTransactions(blocks);
    } finally {
      if (blocks != null) {
        blocks.close();
      }
    }
  }

  /**
   * Test validated same two transactions for mocked and real blocks storage.
   *
   * @param blocks mocked or real blocks
   */
  private void testValidated_SameTwoTransactions(Blocks blocks) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction tx = ValidatedTransactionFixture.newValidatedTransaction();
    when(seenEntities.has(tx.id())).thenReturn(false);
    when(transactionIds.has(tx.id())).thenReturn(false);
    when(pendingTransactions.has(tx.id())).thenReturn(false);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(List.of(tx)), seenEntities, transactionIds, pendingTransactions, blocks);

    ingestEngine.process(tx);
    when(seenEntities.has(tx.id())).thenReturn(true);
    when(pendingTransactions.has(tx.id())).thenReturn(true);

    ingestEngine.process(tx);

    verifyTransactionHappyPathCalled(tx, seenEntities, transactionIds, pendingTransactions);
    verify(seenEntities, times(2)).has(tx.id());
  }

  /**
   * Evaluates that when two same validated transactions arrive at ingest engine sequentially, with mocked blocks,
   * the engine adds the hash of the first transaction into its pending transactions' database.
   * The engine also discards the second transaction right away.
   */
  @Test
  public void testValidated_SameTwoTransactions_MockedBlocks() {
    Blocks blocks = mock(Blocks.class);
    testValidated_SameTwoTransactions(blocks);
  }

  /**
   * Evaluates that when two same validated transactions arrive at ingest engine sequentially, with real blocks,
   * the engine adds the hash of the first transaction into its pending transactions' database.
   * The engine also discards the second transaction right away.
   */
  @Test
  public void testValidated_SameTwoTransactions_RealBlocks() {
    Path tempdir = null;
    BlocksMapDb db = null;
    try {
      Path currentRelativePath = Paths.get("");
      tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
      db = new BlocksMapDb(getTemporaryPath(TEMP_FILE_ID), getTemporaryPath(TEMP_FILE_HEIGHT));
      Blocks blocks = db;
      testValidated_SameTwoTransactions(blocks);
    } catch (IOException e) {
      Assertions.fail();
    } finally {
      db.close();
      try {
        FileUtils.deleteDirectory(new File(tempdir.toString()));
      } catch (IOException e) {
        Assertions.fail();
      }
    }
  }

  /**
   * Test validated transactions already in transaction id storage for mocked and real blocks.
   *
   * @param blocks mocked or real blocks.
   */
  private void testAlreadyInTransaction_IdStorage(Blocks blocks) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction tx = ValidatedTransactionFixture.newValidatedTransaction();

    final IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(List.of(tx)), seenEntities, transactionIds, pendingTransactions, blocks);

    // transaction is not seen, but it is in transaction ids storage (as the result of processing a validated block).
    when(seenEntities.has(tx.id())).thenReturn(false);
    when(transactionIds.has(tx.id())).thenReturn(true);
    when(pendingTransactions.has(tx.id())).thenReturn(false);

    ingestEngine.process(tx);

    verify(seenEntities, times(1)).add(tx.id());
    verify(pendingTransactions, times(1)).has(tx.id());
    verify(transactionIds, times(1)).has(tx.id());
    verify(pendingTransactions, times(0)).add(tx);
  }

  /**
   * Evaluates that when a validated transaction which is already in transaction identifiers arrives at ingest engine,
   * with mocked blocks storage, the engine discards the transaction.
   */
  @Test
  public void testAlreadyInTransaction_IdStorage_MockedBlocks() {
    Blocks blocks = mock(Blocks.class);
    testAlreadyInTransaction_IdStorage(blocks);
  }

  /**
   * Evaluates that when a validated transaction which is already in transaction identifiers arrives at ingest engine,
   * with real blocks storage, the engine discards the transaction.
   */
  @Test
  public void testAlreadyInTransaction_IdStorage_RealBlocks() {
    Path tempdir = null;
    BlocksMapDb db = null;
    try {
      Path currentRelativePath = Paths.get("");
      tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
      db = new BlocksMapDb(getTemporaryPath(TEMP_FILE_ID), getTemporaryPath(TEMP_FILE_HEIGHT));
      Blocks blocks = db;
      testAlreadyInTransaction_IdStorage(blocks);
    } catch (IOException e) {
      Assertions.fail();
    } finally {
      db.close();
      try {
        FileUtils.deleteDirectory(new File(tempdir.toString()));
      } catch (IOException e) {
        Assertions.fail();
      }
    }
  }

  /**
   * Evaluates that when an entity that is neither a validated block nor a validated transaction
   * arrives at ingest engine, with mocked block storage, the engine throws IllegalArgumentException.
   */
  @Test
  public void testNeitherBlockNorTransaction_MockedBlocks() {
    Blocks blocks = mock(Blocks.class);
    testNeitherBlockNorTransaction(blocks);
  }

  /**
   * Evaluates that when an entity that is neither a validated block nor a validated transaction
   * arrives at ingest engine, with real block storage, the engine throws IllegalArgumentException.
   */
  @Test
  public void testNeitherBlockNorTransaction_RealBlocks() {
    Path tempdir = null;
    BlocksMapDb db = null;
    try {
      Path currentRelativePath = Paths.get("");
      tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
      db = new BlocksMapDb(getTemporaryPath(TEMP_FILE_ID), getTemporaryPath(TEMP_FILE_HEIGHT));
      Blocks blocks = db;
      testNeitherBlockNorTransaction(blocks);
    } catch (IOException e) {
      Assertions.fail();
    } finally {
      db.close();
      try {
        FileUtils.deleteDirectory(new File(tempdir.toString()));
      } catch (IOException e) {
        Assertions.fail();
      }
    }
  }

  /**
   * TestNeitherBlockNorTransaction for mocked and real versions.
   *
   * @param blocks mocked or real blocks
   */
  public void testNeitherBlockNorTransaction(Blocks blocks) {
    Transactions pendingTransactions = mock(Transactions.class);
    AssignerInf assigner = mock(AssignerInf.class);
    Identifiers transactionIds = mock(Identifiers.class);
    State state = mock(State.class);
    Identifiers seenEntities = mock(Identifiers.class);

    Entity e = new EntityFixture(); // not a block nor a transaction
    IngestEngine ingestEngine = new IngestEngine(state, blocks, transactionIds, pendingTransactions, seenEntities, assigner);
    Assertions.assertThrows(IllegalArgumentException.class, () -> ingestEngine.process(e));
  }

  /**
   * Test concurrent validated transaction and block nonoverlapping for mocked and real versions.
   *
   * @param blocks mocked or real blocks
   */
  private void testConcurrent_TransactionAndBlock_NonOverlapping(Blocks blocks) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction validatedTx = ValidatedTransactionFixture.newValidatedTransaction();
    Block block = BlockFixture.newBlock();

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(block, validatedTx)), seenEntities, transactionIds, pendingTransactions, blocks);

    processEntitiesConcurrently(ingestEngine, new ArrayList<>(List.of(block, validatedTx)));

    // verification for block
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);

    // verification for transaction
    verifyTransactionHappyPathCalled(validatedTx, seenEntities, transactionIds, pendingTransactions);
    // no transaction should be removed from pending ones
    verify(pendingTransactions, times(0)).remove(any(Identifier.class));
  }

  /**
   * Evaluates that when a validated block and a validated transaction arrives at ingest engine concurrently,
   * the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block
   * and the hash of the validated transaction into its "transactions" database.
   */
  @Test
  public void testConcurrent_TransactionAndBlock_NonOverlapping_MockedBlocks() {
    Blocks blocks = mock(Blocks.class);
    testConcurrent_TransactionAndBlock_NonOverlapping(blocks);
  }

  /**
   * Evaluates that when a validated block and a validated transaction arrives at ingest engine concurrently,
   * the engine adds the block to its block storage database.
   * The engine also adds hash of all the transactions of block
   * and the hash of the validated transaction into its "transactions" database.
   */
  @Test
  public void testConcurrent_TransactionAndBlock_NonOverlapping_RealBlocks() {
    Path tempdir = null;
    BlocksMapDb db = null;
    try {
      Path currentRelativePath = Paths.get("");
      tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
      db = new BlocksMapDb(getTemporaryPath(TEMP_FILE_ID), getTemporaryPath(TEMP_FILE_HEIGHT));
      Blocks blocks = db;
      testConcurrent_TransactionAndBlock_NonOverlapping(blocks);
    } catch (IOException e) {
      Assertions.fail();
    } finally {
      db.close();
      try {
        FileUtils.deleteDirectory(new File(tempdir.toString()));
      } catch (IOException e) {
        Assertions.fail();
      }
    }
  }

  /**
   * Tests processBlockAndIncludedTransaction_BlockFirst for mocked and real block storages.
   *
   * @param blocks mocked or real blocks.
   */
  private void testIncludedTransaction_BlockFirst(Blocks blocks, Block block) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);
    ValidatedTransaction validatedTx = block.getTransactions()[0]; // the transaction is in the block

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(block, validatedTx)), seenEntities, transactionIds, pendingTransactions, blocks);

    // process block
    ingestEngine.process(block);
    // as result of processing block, the validated transaction that also included in the block is
    // added to transaction ids.
    when(transactionIds.has(validatedTx.id())).thenReturn(true);
    // process transaction
    ingestEngine.process(validatedTx);

    verify(seenEntities, times(1)).add(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(transactionIds, times(1)).add(tx.id());
    }

    // verification for transaction
    verify(seenEntities, times(1)).add(validatedTx.id());
    verify(transactionIds, times(1)).has(validatedTx.id());
    verify(pendingTransactions, times(0)).add(validatedTx);
  }

  /**
   * Evaluates that when a validated block and a validated transaction (which the block contains)
   * arrive at ingest engine (block first), the engine adds the block to its mocked block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   * Hence, when the transaction that also included in the block comes next, it is never added to pending
   * transactions' database.
   */
  @Test
  public void testIncludedTransaction_BlockFirst_MockedBlocks() {
    Blocks blocks = mock(Blocks.class);
    Block block = BlockFixture.newBlock();
    when(blocks.has(block.id())).thenReturn(false);
    testIncludedTransaction_BlockFirst(blocks, block);
    verify(blocks, times(1)).add(block);
  }

  /**
   * Evaluates that when a validated block and a validated transaction (which the block contains)
   * arrive at ingest engine (block first), the engine adds the block to its real block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   * Hence, when the transaction that also included in the block comes next, it is never added to pending
   * transactions' database.
   */
  @Test
  public void testIncludedTransaction_BlockFirst_RealBlocks() {
    Path tempdir = null;
    BlocksMapDb db = null;
    try {
      Path currentRelativePath = Paths.get("");
      tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
      db = new BlocksMapDb(getTemporaryPath(TEMP_FILE_ID), getTemporaryPath(TEMP_FILE_HEIGHT));
      Blocks blocks = db;
      Block block = BlockFixture.newBlock();
      testIncludedTransaction_BlockFirst(blocks, block);
      Assertions.assertTrue(blocks.has(block.id()));
    } catch (IOException e) {
      Assertions.fail();
    } finally {
      db.close();
      try {
        FileUtils.deleteDirectory(new File(tempdir.toString()));
      } catch (IOException e) {
        Assertions.fail();
      }
    }
  }

  /**
   * Tests process block and included transaction and transaction first for mocked and real blocks.
   *
   * @param blocks real or mocked blocks.
   */
  private void testIncludedTransaction_TransactionFirst(Blocks blocks, Block block) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    ValidatedTransaction validatedTx = block.getTransactions()[0]; // the transaction is in the block

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(Arrays.asList(block, validatedTx)), seenEntities, transactionIds, pendingTransactions, blocks);

    // process transaction first.
    ingestEngine.process(validatedTx);
    // as result of processing transaction, the transaction must also be added to transaction ids.
    when(transactionIds.has(validatedTx.id())).thenReturn(true);
    // after processing transaction it should be added to pending transaction
    when(pendingTransactions.has(validatedTx.id())).thenReturn(true);
    // process block next.
    ingestEngine.process(block);

    verify(seenEntities, times(1)).add(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(transactionIds, times(1)).add(tx.id());
    }

    // verification for transaction
    verify(seenEntities, times(1)).add(validatedTx.id());
    verify(transactionIds, times(1)).has(validatedTx.id());
    // transaction must be added to pending ones, and then removed.
    verify(pendingTransactions, times(1)).add(validatedTx);
    verify(pendingTransactions, times(1)).remove(validatedTx.id());
  }

  /**
   * Evaluates that when a validated block and a validated transaction (which the block contains)
   * arrive at ingest engine (transaction first), the engine adds the block to its mocked block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   * Hence, when the transaction that also included in the block comes next, it is never added to pending
   * transactions' database.
   */
  @Test
  public void testIncludedTransaction_TransactionFirst_MockedBlocks() {
    Blocks blocks = mock(Blocks.class);
    Block block = BlockFixture.newBlock();
    when(blocks.has(block.id())).thenReturn(false);
    testIncludedTransaction_TransactionFirst(blocks, block);
    verify(blocks, times(1)).add(block);
  }

  /**
   * Evaluates that when a validated block and a validated transaction (which the block contains)
   * arrive at ingest engine (transaction first), the engine adds the block to its real block storage database.
   * The engine also adds hash of all the transactions of block into its "transactions" database.
   * Hence, when the transaction that also included in the block comes next, it is never added to pending
   * transactions' database.
   */
  @Test
  public void testIncludedTransaction_TransactionFirst_RealBlocks() {
    Path tempdir = null;
    BlocksMapDb db = null;
    try {
      Path currentRelativePath = Paths.get("");
      tempdir = Files.createTempDirectory(currentRelativePath, TEMP_DIR);
      db = new BlocksMapDb(getTemporaryPath(TEMP_FILE_ID), getTemporaryPath(TEMP_FILE_HEIGHT));
      Blocks blocks = db;
      Block block = BlockFixture.newBlock();
      testIncludedTransaction_TransactionFirst(blocks, block);
      Assertions.assertTrue(blocks.has(block.id()));
    } catch (IOException e) {
      Assertions.fail();
    } finally {
      db.close();
      try {
        FileUtils.deleteDirectory(new File(tempdir.toString()));
      } catch (IOException e) {
        Assertions.fail();
      }
    }

  }

  /**
   * Verifies mocked storage components have been called with expected parameters on an
   * expected number of times for block happy path, i.e., block is added to the block's storage, and its id is
   * added to seenEntities storage. Also, all its transactions ids are added to the pendingTx and
   * txIds.
   *
   * @param block        the block itself.
   * @param blocks       the blocks storage component.
   * @param pendingTx    the pending transactions identifiers.
   * @param txIds        the transaction identifiers.
   * @param seenEntities identifiers of processed entities by engine.
   */
  private void verifyBlockHappyPathCalled(Block block, Blocks blocks, Transactions pendingTx, Identifiers txIds, Identifiers seenEntities) {
    verify(seenEntities, times(1)).add(block.id());
    for (Transaction tx : block.getTransactions()) {
      verify(pendingTx, times(1)).has(tx.id());
      verify(txIds, times(1)).add(tx.id());
    }
  }

  /**
   * Verifies mocked storage components have been called with the expected parameters on an
   * expected number of times for transaction happy path.
   *
   * @param transaction  the transaction itself.
   * @param seenEntities identifiers of processed entities by engine.
   * @param txIds        the transaction identifiers.
   * @param pendingTx    the pending transactions identifiers.
   */
  private void verifyTransactionHappyPathCalled(Transaction transaction, Identifiers seenEntities, Identifiers txIds, Transactions pendingTx) {

    verify(seenEntities, times(1)).add(transaction.id());
    verify(txIds, times(1)).has(transaction.id());
    verify(pendingTx, times(1)).add(transaction);
  }

  /**
   * Sets up the mocks for ingest engine for processing given list of entities.
   *
   * @param entities     list of entities.
   * @param seenEntities identifiers of processed entities by engine.
   * @param txIds        identifiers of transactions.
   * @param pendingTx    identifiers of pending transactions.
   * @param blocks       the blocks storage component.
   * @return mocked ingest engine with mocked components.
   */
  private IngestEngine mockIngestEngineForEntities(ArrayList<Entity> entities, Identifiers seenEntities, Identifiers txIds, Transactions pendingTx, Blocks blocks) {

    Snapshot snapshot = mock(Snapshot.class);
    AssignerInf assigner = mock(AssignerInf.class);
    State state = mock(State.class);

    for (Entity e : entities) {
      // mocks entity as new
      when(seenEntities.has(e.id())).thenReturn(false);

      if (e.type().equals(EntityType.TYPE_VALIDATED_TRANSACTION)) {

        ValidatedTransaction tx = (ValidatedTransaction) e;
        when(state.atBlockId(tx.getReferenceBlockId())).thenReturn(snapshot);
        when(pendingTx.has(tx.id())).thenReturn(false);
        when(txIds.has(tx.id())).thenReturn(false);

        // mocks assignment
        mockAssignment(assigner, tx, snapshot);

      } else if (e.type().equals(EntityType.TYPE_BLOCK)) {

        Block block = (Block) e;
        when(state.atBlockId(block.getPreviousBlockId())).thenReturn(snapshot);
        for (Transaction tx : block.getTransactions()) {
          when(pendingTx.has(tx.id())).thenReturn(false);
        }

        // mocks assignment
        mockAssignment(assigner, block.getProposal(), snapshot);
      }

    }

    return new IngestEngine(state, blocks, txIds, pendingTx, seenEntities, assigner);
  }

  /**
   * Processes entities concurrently thorough ingest engine.
   *
   * @param ingestEngine ingest engine.
   * @param entities     the entities to be processed.
   */
  private void processEntitiesConcurrently(IngestEngine ingestEngine, ArrayList<Entity> entities) {

    AtomicInteger threadError = new AtomicInteger();
    int concurrencyDegree = entities.size();
    CountDownLatch threadsDone = new CountDownLatch(concurrencyDegree);
    Thread[] threads = new Thread[concurrencyDegree];

    for (int i = 0; i < concurrencyDegree; i++) {
      int finalI = i;
      threads[i] = new Thread(() -> {
        try {
          ingestEngine.process(entities.get(finalI));
          threadsDone.countDown();
        } catch (IllegalStateException e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : threads) {
      t.start();
    }
    try {
      boolean doneOneTime = threadsDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTime);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
  }

  /**
   * Mocks assignment of for an entity.
   *
   * @param assigner the validator assigner.
   * @param e        the entity to be assigned.
   * @param snapshot the snapshot.
   */
  private void mockAssignment(AssignerInf assigner, Entity e, Snapshot snapshot) {
    Assignment assignment = mock(Assignment.class);
    when(assigner.assign(e.id(), snapshot, Parameters.VALIDATOR_THRESHOLD)).thenReturn(assignment);
    when(assignment.has(any(Identifier.class))).thenReturn(true); // returns true for all identifiers
    PublicKey pubKey = mock(PublicKey.class); // mock public key
    Account account = mock(Account.class); // mock account
    when(account.getPublicKey()).thenReturn(pubKey); // returns the mocked public key for all accounts

    // returns true for all signatures
    when(pubKey.verifySignature(any(BlockProposal.class), any(Signature.class))).thenReturn(true);
    when(pubKey.verifySignature(any(Transaction.class), any(Signature.class))).thenReturn(true);
    // returns the mock account for all identifiers
    when(snapshot.getAccount(any(Identifier.class))).thenReturn(account);
  }

  /**
   * Test validated single block for mocked and real versions.
   *
   * @param blocks mocked or real block storage.
   * @param block  the real block
   */
  private void testValidatedSingleBlock(Blocks blocks, Block block) {
    Identifiers seenEntities = mock(Identifiers.class);
    Identifiers transactionIds = mock(Identifiers.class);
    Transactions pendingTransactions = mock(Transactions.class);

    when(seenEntities.has(block.id())).thenReturn(false);

    IngestEngine ingestEngine = this.mockIngestEngineForEntities(new ArrayList<>(List.of(block)), seenEntities, transactionIds, pendingTransactions, blocks);
    ingestEngine.process(block);
    verifyBlockHappyPathCalled(block, blocks, pendingTransactions, transactionIds, seenEntities);
  }
}