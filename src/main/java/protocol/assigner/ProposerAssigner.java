package protocol.assigner;

import model.lightchain.Identifier;
import state.Snapshot;

/**
 * Proposer assigner abstracts the logic of assigning proposer of the next block given the current block.
 */
public interface ProposerAssigner {
  /**
   * Picks identifier of the proposer of the next block.
   *
   * @param currentBlockId identifier of the current block.
   * @param s              snapshot to pick proposer from.
   * @return identifier of the proposer of the next block.
   */
  Identifier nextBlockProposer(Identifier currentBlockId, Snapshot s) throws IllegalArgumentException, IllegalStateException;
}
