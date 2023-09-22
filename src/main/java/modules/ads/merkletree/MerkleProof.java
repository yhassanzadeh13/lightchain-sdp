package modules.ads.merkletree;

import java.util.Objects;

import model.crypto.Sha3256Hash;
import modules.ads.MembershipProof;

/**
 * A proof of membership in a Merkle tree.
 */
public class MerkleProof implements MembershipProof {
  private final MerklePath merklePath;
  private final Sha3256Hash root;

  /**
   * Constructs a proof from a list of hashes and a root.
   *
   * @param root       the root
   * @param merklePath the merkle path of the proof and their isLeft booleans
   */
  public MerkleProof(Sha3256Hash root, MerklePath merklePath) {
    this.root = root;
    this.merklePath = new MerklePath(merklePath);
  }

  /**
   * Return the root of the Merkle tree.
   *
   * @return the root of the Merkle tree.
   */
  public Sha3256Hash getRoot() {
    return root;
  }

  /**
   * Returns the merkle path of the proof of membership.
   *
   * @return merkle path of the proof of membership.
   */
  @Override
  public MerklePath getMerklePath() {
    return new MerklePath(merklePath);
  }

  /**
   * Checks if two MerkleProofs are equal.
   *
   * @param o the other MerkleProof
   * @return true if the MerkleProofs are equal, false otherwise
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MerkleProof that = (MerkleProof) o;
    return merklePath.equals(that.merklePath) && root.equals(that.root);
  }

  /**
   * Returns the hash code of the MerkleProof.
   *
   * @return the hash code of the MerkleProof.
   */
  @Override
  public int hashCode() {
    return Objects.hash(merklePath, root);
  }
}
