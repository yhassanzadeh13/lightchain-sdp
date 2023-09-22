package modules.ads.merkletree;

import crypto.Sha3256Hasher;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Direction;

/**
 * A node in the Merkle tree.
 */
public class MerkleNode {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private MerkleNode left;
  private MerkleNode right;
  private MerkleNode parent;
  private Direction direction;
  private Sha3256Hash rootHash;

  /**
   * Default constructor.
   */
  public MerkleNode() {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.rootHash = new Sha3256Hash(new byte[32]);
  }

  /**
   * Constructor with entity and isLeft.
   *
   * @param e         input entity
   * @param direction whether the node is left child or right child of its parent.
   */
  public MerkleNode(Entity e, Direction direction) {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.direction = direction;
    this.rootHash = hasher.computeHash(e.id());
  }

  /**
   * Constructor with parent and isLeft.
   *
   * @param parent    the parent of the node
   * @param direction boolean that specifies if the node is left child or not
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "parent is intentionally mutable externally")
  public MerkleNode(MerkleNode parent, Direction direction) {
    this.left = null;
    this.right = null;
    this.parent = parent;
    this.direction = direction;
    this.rootHash = new Sha3256Hash(new byte[32]);
  }

  /**
   * Constructor with parent, isLeft and hash.
   *
   * @param parent    the parent of the node
   * @param direction whether the node is left child or right child of its parent.
   * @param hash      input hash of the entity corresponding to that node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "parent is intentionally mutable externally")
  public MerkleNode(MerkleNode parent, Direction direction, Sha3256Hash hash) {
    this.left = null;
    this.right = null;
    this.parent = parent;
    this.direction = direction;
    this.rootHash = hash;
  }

  /**
   * Constructor with hash of the entity.
   *
   * @param hash input hash of the entity corresponding to that node
   */
  public MerkleNode(Sha3256Hash hash) {
    this.left = null;
    this.right = null;
    this.parent = null;
    this.direction = null;
    this.rootHash = hash;
  }

  /**
   * Constructor of a parent node.
   *
   * @param hash  input hash of the entity corresponding to that node
   * @param left  left child of the node
   * @param right right child of the node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "left and right are intentionally mutable externally")
  public MerkleNode(Sha3256Hash hash, MerkleNode left, MerkleNode right) {
    this.left = left;
    this.right = right;
    this.parent = null;
    this.direction = null;
    this.rootHash = hash;
  }

  /**
   * Returns the left child of the node.
   *
   * @return the left child of the node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public MerkleNode getLeft() {
    return left;
  }

  /**
   * Returns the right child of the node.
   *
   * @return the right child of the node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public MerkleNode getRight() {
    return right;
  }

  /**
   * Returns the parent of the node.
   *
   * @return the parent of the node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP", justification = "internal representation is intentionally returned")
  public MerkleNode getParent() {
    return parent;
  }

  /**
   * Sets the parent of the node.
   *
   * @param parent the parent of the node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "parent is intentionally mutable externally")
  public void setParent(MerkleNode parent) {
    this.parent = parent;
  }

  /**
   * Returns the hash corresponding to the node.
   *
   * @return the hash corresponding to the node
   */
  public Sha3256Hash getRootHash() {
    return rootHash;
  }

  /**
   * Returns true if the node is a left child, false otherwise.
   *
   * @return true if the node is a left child, false otherwise
   */
  public boolean isLeft() {
    return direction.isLeft();
  }

  /**
   * Sets if the node is a left child.
   *
   * @param direction whether the node is left child or right child of its parent.
   */
  public void setDirection(Direction direction) {
    this.direction = direction;
  }

  /**
   * Returns the sibling of the node.
   *
   * @return the sibling of the node
   */
  public MerkleNode getSibling() {
    if (isLeft()) {
      return parent.getRight();
    } else {
      return parent.getLeft();
    }
  }

  /**
   * Sets the left child of the node.
   *
   * @param left the left child of the node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "left is intentionally mutable externally")
  public void setLeftNode(MerkleNode left) {
    this.left = left;
  }

  /**
   * Sets the left child of the node.
   *
   * @param right the right child of the node
   */
  @SuppressFBWarnings(value = "EI_EXPOSE_REP2", justification = "right is intentionally mutable externally")
  public void setRightNode(MerkleNode right) {
    this.right = right;
  }

  /**
   * Updates the hash of the node.
   */
  public void updateHash() {
    Sha3256Hash leftHash = left == null ? new Sha3256Hash(new byte[32]) : left.getRootHash();
    Sha3256Hash rightHash = right == null ? new Sha3256Hash(new byte[32]) : right.getRootHash();
    this.rootHash = hasher.computeHash(leftHash, rightHash);
  }
}
