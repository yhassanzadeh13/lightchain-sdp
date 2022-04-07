package unittest.fixtures;

import java.util.ArrayList;

import model.lightchain.ValidatedBlock;
import state.Snapshot;

public class BlockchainFixture {
  /**
   * It then creates a genesis empty validated block, and keeps extending the chain of blocks with new validated blocks
   * till it creates the specified number of blocks.
   * Each block points to the previous block as its previousBlockId.
   * Each block contains random number of transactions between man and max parameters.
   * Each transaction gets validated by running the assignment on that as well as the transaction validator, signing the
   * validation certification on behalf of its validators using their private keys, and including it in a block.
   * Each block has a randomly chosen proposer, goes through validation and gets validation signature by its validators.
   *
   * @param rootSnapshot Root snapshot for genesis block that contains at least as many validator threshold as
   *                     protocol parameter says. Each account is staked, and has enough balance to make a transaction
   *                     per block (though it may not really do that).
   * @param blockNum     number of blocks generated by this chain.
   * @throws IllegalStateException if any block or transaction fails validation.
   * @return chain of validated blocks.
   */
  public static ArrayList<ValidatedBlock> newValidChain(Snapshot rootSnapshot, int blockNum)
      throws IllegalStateException{
    return null;
  }
}
