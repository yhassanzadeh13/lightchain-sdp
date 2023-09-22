package modules.ads.mtrie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import crypto.Sha3256Hasher;
import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Direction;
import model.lightchain.Identifier;
import modules.ads.AuthenticatedEntity;
import modules.ads.merkletree.*;

/**
 * Implementation of an in-memory Merkle Patricia Tree
 * that is capable of storing and retrieval of LightChain entities.
 */
public class MerklePatriciaTrie extends MerkleTree {
  private static final Sha3256Hasher hasher = new Sha3256Hasher();
  private final Map<Identifier, Entity> entityHashTable;
  private final ReentrantReadWriteLock lock;
  private final MerkleNode rootNode;
  private int size;

  /**
   * Default constructor for a Merkle Patricia Trie.
   */
  public MerklePatriciaTrie() {
    this.size = 0;
    this.rootNode = new MerkleNode();
    this.entityHashTable = new HashMap<>();
    this.lock = new ReentrantReadWriteLock();
  }

  /**
   * Adds an entity to the merkle patricia tree.
   *
   * @param e the entity to add
   * @return AuthenticatedEntity containing the entity and its membership proof
   */
  @Override
  public AuthenticatedEntity put(Entity e) {
    try {
      lock.writeLock().lock();
      if (e == null) {
        throw new IllegalArgumentException("attempting to put a null entity in merkle patricia trie");
      }
      MerkleNode currentNode = rootNode;

      Identifier id = e.id();
      String path = id.getBitString();

      for (int i = 0; i < path.length() - 1; i++) {
        if (path.charAt(i) == '0') {
          if (currentNode.getLeft() == null) {
            currentNode.setLeftNode(new MerkleNode(currentNode, Direction.LEFT));
          }
          currentNode = currentNode.getLeft();
        } else {
          if (currentNode.getRight() == null) {
            currentNode.setRightNode(new MerkleNode(currentNode, Direction.RIGHT));
          }
          currentNode = currentNode.getRight();
        }
      }
      MerkleNode newNode = null;
      if (path.charAt(path.length() - 1) == '0') {
        if (currentNode.getLeft() == null) {
          newNode = new MerkleNode(currentNode, Direction.LEFT, hasher.computeHash(id));
          currentNode.setLeftNode(newNode);
          entityHashTable.put(id, e);
          size++;
        }
      } else {
        if (currentNode.getRight() == null) {
          newNode = new MerkleNode(currentNode, Direction.RIGHT, hasher.computeHash(id));
          currentNode.setRightNode(newNode);
          entityHashTable.put(id, e);
          size++;
        }
      }
      for (int i = 0; i < path.length() - 1; i++) {
        currentNode.updateHash();
        currentNode = currentNode.getParent();
      }
      currentNode.updateHash();
      return get(id);
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Returns the AuthenticatedEntity corresponding to the given identifier.
   *
   * @param id the identifier of the entity to retrieve
   * @return the AuthenticatedEntity corresponding to the given identifier
   */
  @Override
  public AuthenticatedEntity get(Identifier id) throws IllegalArgumentException {
    try {
      lock.readLock().lock();
      if (id == null) {
        throw new IllegalArgumentException("identifier cannot be null");
      }

      ArrayList<Sha3256Hash> pathList = new ArrayList<>();
      ArrayList<Boolean> isLeftNode = new ArrayList<>();
      MerkleNode currNode = rootNode;
      String path = id.getBitString();
      for (int i = 0; i < path.length(); i++) {
        if (path.charAt(i) == '0') {
          currNode = currNode.getLeft();
          isLeftNode.add(true);
        } else {
          currNode = currNode.getRight();
          isLeftNode.add(false);
        }
        if (currNode == null) {
          throw new IllegalArgumentException("identifier not found");
        }
        pathList.add(currNode.getSibling() == null ? new Sha3256Hash(new byte[32]) : currNode.getSibling().getRootHash());
      }
      Collections.reverse(pathList);
      Collections.reverse(isLeftNode);
      Entity e = entityHashTable.get(id);
      MerklePath merklePath = new MerklePath(pathList, isLeftNode);
      MerkleProof proof = new MerkleProof(rootNode.getRootHash(), merklePath);
      return new MerkleTreeAuthenticatedEntity(proof, e.type(), e);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Returns the size of the merkle patricia trie.
   *
   * @return the size of the merkle patricia trie
   */
  @Override
  public int size() {
    return size;
  }
}
