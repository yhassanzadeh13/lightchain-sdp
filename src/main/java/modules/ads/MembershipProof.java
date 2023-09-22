package modules.ads;

import model.crypto.Sha3256Hash;
import modules.ads.merkletree.MerklePath;

/**
 * Represents a Merkle Proof of membership against a certain root identifier.
 */
public interface MembershipProof {
  /**
   * Root of the authenticated data structure that this proof belongs to.
   *
   * @return hash value of the root node.
   */
  Sha3256Hash getRoot();

  /**
   * Returns the merkle path of the proof of membership.
   *
   * @return merkle path of the proof of membership.
   */
  MerklePath getMerklePath();
}
