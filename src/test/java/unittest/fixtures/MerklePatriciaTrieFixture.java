package unittest.fixtures;

import modules.ads.mtrie.MerklePatriciaTrie;

/**
 * Creates a new randomly looking MerklePatriciaTrie.
 */
public class MerklePatriciaTrieFixture {
  /**
   * Creates a new merkle patricia trie with n random elements.
   *
   * @param n number of elements to create
   * @return a new merkle tree with n random elements.
   */
  public static MerklePatriciaTrie createMerklePatriciaTree(int n) {
    MerklePatriciaTrie merklePatriciaTrie = new MerklePatriciaTrie();
    for (int i = 0; i < n; i++) {
      merklePatriciaTrie.put(new EntityFixture());
    }
    return merklePatriciaTrie;
  }
}
