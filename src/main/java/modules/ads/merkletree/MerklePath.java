package modules.ads.merkletree;

import java.util.ArrayList;
import java.util.Objects;

import model.crypto.Sha3256Hash;

/**
 * A MerklePath is a list of hashes that represents the path from the node to the root and an arraylist which
 * contains which child (left or right) the nodes in the path is.
 */
public class MerklePath {
  private ArrayList<Sha3256Hash> path;
  private ArrayList<Boolean> isLeftNode;

  /**
   * Default constructor for a MerklePath.
   */
  public MerklePath() {
    this.path = new ArrayList<>();
    this.isLeftNode = new ArrayList<>();
  }

  /**
   * Constructor for a MerklePath from another MerklePath.
   *
   * @param merklePath the MerklePath to copy.
   */
  public MerklePath(MerklePath merklePath) {
    this.path = new ArrayList<>(merklePath.path);
    this.isLeftNode = new ArrayList<>(merklePath.isLeftNode);
  }

  /**
   * Constructor with path and isLeftNode.
   *
   * @param path       the path of the proof.
   * @param isLeftNode the isLeftNode of the MerklePath.
   */
  public MerklePath(ArrayList<Sha3256Hash> path, ArrayList<Boolean> isLeftNode) {
    this.path = new ArrayList<>(path);
    this.isLeftNode = new ArrayList<>(isLeftNode);
  }

  /**
   * Adds a new node and its isLeft boolean to the merklePath.
   *
   * @param hash       the hash of the node.
   * @param isLeftNode the isLeftNode of the node.
   */
  public void add(Sha3256Hash hash, boolean isLeftNode) {
    this.path.add(hash);
    this.isLeftNode.add(isLeftNode);
  }

  /**
   * Returns the path of the MerklePath.
   *
   * @return the path of the MerklePath.
   */
  public ArrayList<Sha3256Hash> getPath() {
    return new ArrayList<>(path);
  }

  /**
   * Returns the isLeftNode of the MerklePath.
   *
   * @return the isLeftNode of the MerklePath.
   */
  public ArrayList<Boolean> getIsLeftNode() {
    return new ArrayList<>(isLeftNode);
  }

  /**
   * Checks if two MerklePaths are equal.
   *
   * @param o the other MerklePath.
   *
   * @return true if the MerklePaths are equal, false otherwise.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    MerklePath that = (MerklePath) o;
    return path.equals(that.path) && isLeftNode.equals(that.isLeftNode);
  }

  /**
   * Returns the hashcode of the MerklePath.
   *
   * @return the hashcode of the MerklePath.
   */
  @Override
  public int hashCode() {
    return Objects.hash(path, isLeftNode);
  }
}
