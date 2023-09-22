package unittest.fixtures;

import modules.ads.merkletree.MerkleTree;

/**
 * Creates a new randomly looking MerkleTree.
 */
public class MerkleTreeFixture {
  /**
   * Creates a new merkle tree with n random elements.
   *
   * @param n number of elements to create
   * @return a new merkle tree with n random elements.
   */
  public static MerkleTree createMerkleTree(int n) {
    MerkleTree merkleTree = new MerkleTree();
    for (int i = 0; i < n; i++) {
      merkleTree.put(new EntityFixture());
    }
    return merkleTree;
  }
}
