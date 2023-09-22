package modules.ads.merkletree;

import java.util.ArrayList;
import java.util.Arrays;

import crypto.Sha3256Hasher;
import model.crypto.Sha3256Hash;
import modules.ads.AuthenticatedEntity;
import modules.ads.MembershipProof;

/**
 * Verifies the AuthenticatedEntity against its self-contained proof.
 */
public class MerkleTreeAuthenticatedEntityVerifier implements modules.ads.AuthenticatedEntityVerifier {

  /**
   * Verifies the AuthenticatedEntity against its self-contained proof.
   *
   * @param authenticatedEntity the AuthenticatedEntity to verify.
   *
   * @return true if entity contains a valid Merkle Proof against its root identifier, false otherwise.
   */
  @Override
  public boolean verify(AuthenticatedEntity authenticatedEntity) {
    Sha3256Hasher hasher = new Sha3256Hasher();
    MembershipProof proof = authenticatedEntity.getMembershipProof();
    MerklePath path = proof.getMerklePath();
    ArrayList<Boolean> isLeftNode = path.getIsLeftNode();
    ArrayList<Sha3256Hash> proofPath = path.getPath();

    Sha3256Hash initialHash = hasher.computeHash(authenticatedEntity.getEntity().id());
    Sha3256Hash currentHash;
    if (isLeftNode.get(0)) {
      currentHash = hasher.computeHash(initialHash, proofPath.get(0));
    } else {
      currentHash = hasher.computeHash(proofPath.get(0), initialHash);
    }
    for (int i = 1; i < proofPath.size(); i++) {
      if (isLeftNode.get(i)) {
        currentHash = hasher.computeHash(currentHash, proofPath.get(i));
      } else {
        currentHash = hasher.computeHash(proofPath.get(i), currentHash);
      }
    }
    return Arrays.equals(proof.getRoot().getBytes(), currentHash.getBytes());
  }
}