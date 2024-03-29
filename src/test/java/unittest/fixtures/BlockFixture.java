package unittest.fixtures;

import java.util.Random;

import model.crypto.Signature;
import model.lightchain.Block;
import model.lightchain.BlockHeader;
import model.lightchain.BlockPayload;
import model.lightchain.BlockProposal;
import model.lightchain.Identifier;
import model.lightchain.ValidatedTransaction;
import model.local.Local;
import protocol.Parameters;

/**
 * Encapsulates creating random blocks for testing.
 */
public class BlockFixture {
  private static final Random random = new Random();

  /**
   * Creates a new random block payload with 100 transactions.
   *
   * @return a new random block payload with 100 transactions.
   */
  public static BlockPayload newBlockPayload() {
    return new BlockPayload(ValidatedTransactionFixture.newValidatedTransactions(100));
  }

  /**
   * Creates a new random block payload.
   *
   * @param txCount number of included random transactions.
   * @return new random block with random transactions.
   */
  public static BlockPayload newBlockPayload(int txCount) {
    return new BlockPayload(ValidatedTransactionFixture.newValidatedTransactions(txCount));
  }

  /**
   * Creates a new random block header.
   *
   * @return a new random block header.
   */
  public static BlockHeader newBlockHeader() {
    return newBlockHeader(IdentifierFixture.newIdentifier());
  }

  /**
   * Creates a new random block header.
   *
   * @param proposerId identifier of proposer.
   * @return a new random block header.
   */
  public static BlockHeader newBlockHeader(Identifier proposerId) {
    int height = Math.abs(random.nextInt(1_000_000));
    return new BlockHeader(
        height,
        IdentifierFixture.newIdentifier(),
        proposerId,
        IdentifierFixture.newIdentifier());
  }

  /**
   * Creates a new block proposal with random header and payload.
   *
   * @return a new block proposal with random header and payload.
   */
  public static BlockProposal newBlockProposal() {
    return newBlockProposal(IdentifierFixture.newIdentifier());
  }

  /**
   * Creates a new block proposal with random header and payload.
   *
   * @param proposerId identifier of proposer.
   * @return a new block proposal with random header and payload.
   */
  public static BlockProposal newBlockProposal(Identifier proposerId) {
    return new BlockProposal(newBlockHeader(proposerId), newBlockPayload(), SignatureFixture.newSignatureFixture());
  }

  /**
   * Creates a new block proposal with random header and payload.
   *
   * @param me local module of the node for signing the proposer signature.
   * @return a new block proposal with random header and payload.
   */
  public static BlockProposal newBlockProposal(Local me) {
    BlockHeader header = newBlockHeader(me.myId());
    return new BlockProposal(header, newBlockPayload(), me.signEntity(header));
  }

  /**
   * Creates a new block proposal with random header and payload.
   *
   * @param txCount total number of transactions.
   * @return a new block proposal with random header and payload.
   */
  public static BlockProposal newBlockProposal(int txCount) {
    return new BlockProposal(newBlockHeader(), newBlockPayload(txCount), SignatureFixture.newSignatureFixture());
  }

  /**
   * Returns a block with randomly generated values.
   *
   * @return a block with randomly generated values.
   */
  public static Block newBlock() {
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }

    return new Block(newBlockProposal(), certificates);
  }

  /**
   * Returns a block with randomly generated values and given validated transactions size.
   *
   * @param txCount number of included validated transactions.
   * @return a block with randomly generated values.
   */
  public static Block newBlock(int txCount) {
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }

    return new Block(
        new BlockProposal(
            newBlockHeader(),
            newBlockPayload(txCount),
            SignatureFixture.newSignatureFixture()),
        certificates);
  }

  /**
   * Returns a block with randomly generated values and given validated transactions.
   *
   * @param transactions validated transactions to be included in the block.
   * @return a block with randomly generated values.
   */
  public static Block newBlock(ValidatedTransaction[] transactions) {
    int certificatesSize = Parameters.SIGNATURE_THRESHOLD;
    Signature[] certificates = new Signature[certificatesSize];
    for (int i = 0; i < certificatesSize; i++) {
      certificates[i] = SignatureFixture.newSignatureFixture();
    }
    return new Block(
        new BlockProposal(
            newBlockHeader(),
            new BlockPayload(transactions),
            SignatureFixture.newSignatureFixture()),
        certificates);
  }

}
