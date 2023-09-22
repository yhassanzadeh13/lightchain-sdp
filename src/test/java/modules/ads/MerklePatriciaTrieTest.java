package modules.ads;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import model.Entity;
import model.crypto.Sha3256Hash;
import model.lightchain.Identifier;
import modules.ads.merkletree.MerklePath;
import modules.ads.merkletree.MerkleProof;
import modules.ads.merkletree.MerkleTreeAuthenticatedEntity;
import modules.ads.merkletree.MerkleTreeAuthenticatedEntityVerifier;
import modules.ads.mtrie.MerklePatriciaTrie;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import unittest.fixtures.EntityFixture;
import unittest.fixtures.MerklePatriciaTrieFixture;
import unittest.fixtures.Sha3256HashFixture;

/**
 * Encapsulates tests for an authenticated and concurrent implementation of MerklePatriciaTree ADS.
 */
public class MerklePatriciaTrieTest {

  /**
   * A basic test for one sequential put and get operations.
   */
  @Test
  public void testVerification() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(5);
    Assertions.assertEquals(merklePatriciaTrie.size(), 5); // fixture sanity check.

    Entity entity = new EntityFixture();
    merklePatriciaTrie.put(entity);
    Assertions.assertEquals(merklePatriciaTrie.size(), 6);

    AuthenticatedEntity authenticatedEntity = merklePatriciaTrie.get(entity.id());
    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
  }

  /**
   * Tests both putting and getting the same entity gives same proof
   * and putting another entity gives different proofs.
   */
  @Test
  public void testPutGetSameProof() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(5);
    Assertions.assertEquals(merklePatriciaTrie.size(), 5); // fixture sanity check.
    Entity e1 = new EntityFixture();

    // putting e1
    AuthenticatedEntity authenticatedEntityPut = merklePatriciaTrie.put(e1);
    MembershipProof proofPutE1 = authenticatedEntityPut.getMembershipProof();
    Assertions.assertEquals(merklePatriciaTrie.size(), 6);

    // getting e1
    AuthenticatedEntity authE1Get = merklePatriciaTrie.get(e1.id());
    MembershipProof proofGetE1 = authE1Get.getMembershipProof();

    // putting e2
    Entity e2 = new EntityFixture();
    AuthenticatedEntity authE2Put = merklePatriciaTrie.put(e2);
    Assertions.assertEquals(merklePatriciaTrie.size(), 7);

    // getting e2
    MembershipProof proofPutE2 = authE2Put.getMembershipProof();

    // proofs for putting and getting e1 should be the same.
    Assertions.assertEquals(proofPutE1, proofGetE1);

    // proofs for putting e1 and e2 must be different.
    Assertions.assertNotEquals(proofPutE1, proofPutE2);
  }

  /**
   * Tests putting an existing entity does not change the proof.
   */
  @Test
  public void testPutExistingEntity() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(5);
    Assertions.assertEquals(merklePatriciaTrie.size(), 5); // fixture sanity check.
    Entity entity = new EntityFixture();

    // first time put
    AuthenticatedEntity authenticatedEntityPut = merklePatriciaTrie.put(entity);
    MembershipProof proofPut = authenticatedEntityPut.getMembershipProof();
    Assertions.assertEquals(merklePatriciaTrie.size(), 6);

    // second attempt
    AuthenticatedEntity authenticatedEntityPutAgain = merklePatriciaTrie.put(entity);
    MembershipProof proofPutAgain = authenticatedEntityPutAgain.getMembershipProof();

    // proofs must be equal.
    Assertions.assertEquals(proofPut, proofPutAgain);
    Assertions.assertEquals(merklePatriciaTrie.size(), 6); // duplicate entity should not change the size.
  }

  /**
   * Concurrently puts and gets entities and checks their proofs are correct (thread safety check).
   */
  @Test
  public void testConcurrentPutGet() {
    int concurrencyDegree = 100;
    ArrayList<Entity> entities = new ArrayList<>();
    ArrayList<Identifier> ids = new ArrayList<>();

    AtomicInteger threadError = new AtomicInteger();
    CountDownLatch putDone = new CountDownLatch(concurrencyDegree);
    CountDownLatch getDone = new CountDownLatch(concurrencyDegree);

    Thread[] putThreads = new Thread[concurrencyDegree];
    Thread[] getThreads = new Thread[concurrencyDegree];

    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(0);
    Assertions.assertEquals(merklePatriciaTrie.size(), 0); // fixture sanity check.

    for (int i = 0; i < concurrencyDegree; i++) {
      Entity entity = new EntityFixture();
      entities.add(entity);
      ids.add(entity.id());
    }

    // put
    for (int i = 0; i < concurrencyDegree; i++) {
      Entity entity = entities.get(i);
      putThreads[i] = new Thread(() -> {
        try {
          merklePatriciaTrie.put(entity);
          putDone.countDown();
        } catch (Exception e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : putThreads) {
      t.start();
    }
    try {
      boolean doneOneTimePut = putDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTimePut);
    } catch (InterruptedException e) {
      Assertions.fail();
    }

    // get
    for (int i = 0; i < concurrencyDegree; i++) {
      Identifier id = ids.get(i);
      getThreads[i] = new Thread(() -> {
        try {
          AuthenticatedEntity authenticatedEntity = merklePatriciaTrie.get(id);
          MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
          if (!verifier.verify(authenticatedEntity)) {
            threadError.getAndIncrement();
          }
          getDone.countDown();
        } catch (Exception e) {
          threadError.getAndIncrement();
        }
      });
    }
    for (Thread t : getThreads) {
      t.start();
    }
    try {
      boolean doneOneTimeGet = getDone.await(60, TimeUnit.SECONDS);
      Assertions.assertTrue(doneOneTimeGet);
    } catch (InterruptedException e) {
      Assertions.fail();
    }
    Assertions.assertEquals(0, threadError.get());
    Assertions.assertEquals(concurrencyDegree, merklePatriciaTrie.size());
  }

  /**
   * Tests getting an entity that does not exist in the merkle patricia trie throws IllegalArgumentException.
   */
  @Test
  public void testGetNonExistingEntity() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(5);
    Assertions.assertEquals(merklePatriciaTrie.size(), 5); // fixture sanity check.
    Entity entity = new EntityFixture();

    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      merklePatriciaTrie.get(entity.id());
    });
  }

  /**
   * Tests inserting null throws IllegalArgumentException.
   */
  @Test
  public void testNullInsertion() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(5);
    Assertions.assertEquals(merklePatriciaTrie.size(), 5); // fixture sanity check.
    Assertions.assertThrows(IllegalArgumentException.class, () -> {
      merklePatriciaTrie.put(null);
    });
  }

  /**
   * Tests the proof verification fails when root is changed.
   */
  @Test
  public void testManipulatedRoot() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(5);
    Assertions.assertEquals(merklePatriciaTrie.size(), 5); // fixture sanity check.
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merklePatriciaTrie.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();

    // creates a tampered proof with random root.
    MerkleProof tamperedProof = new MerkleProof(new Sha3256Hash(new byte[32]), proof.getMerklePath());
    AuthenticatedEntity tamperedAuthenticatedEntity = new MerkleTreeAuthenticatedEntity(
            tamperedProof,
            authenticatedEntity.type(),
            authenticatedEntity.getEntity());

    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    // authenticated entity must be verified.
    Assertions.assertTrue(verifier.verify(authenticatedEntity));
    // tampered authenticated entity must be failed.
    Assertions.assertFalse(verifier.verify(tamperedAuthenticatedEntity));
  }

  /**
   * Tests the proof verification fails when entity is changed.
   */
  @Test
  public void testManipulatedEntity() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(5);
    Assertions.assertEquals(merklePatriciaTrie.size(), 5); // fixture sanity check.
    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merklePatriciaTrie.put(entity);

    AuthenticatedEntity tamperedEntity = new MerkleTreeAuthenticatedEntity(
            (MerkleProof) authenticatedEntity.getMembershipProof(),
            authenticatedEntity.type(),
            new EntityFixture());

    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity)); // original authenticated entity passes verification.
    Assertions.assertFalse(verifier.verify(tamperedEntity)); // tampered entity fails verification.
  }

  /**
   * Tests the proof fails verification when proof part of authenticated entity is changed.
   */
  @Test
  public void testManipulatedProof() {
    MerklePatriciaTrie merklePatriciaTrie = MerklePatriciaTrieFixture.createMerklePatriciaTree(5);
    Assertions.assertEquals(merklePatriciaTrie.size(), 5); // fixture sanity check.

    Entity entity = new EntityFixture();
    AuthenticatedEntity authenticatedEntity = merklePatriciaTrie.put(entity);
    MembershipProof proof = authenticatedEntity.getMembershipProof();
    MerklePath merklePath = proof.getMerklePath();
    ArrayList<Sha3256Hash> path = merklePath.getPath();
    ArrayList<Boolean> isLeft = merklePath.getIsLeftNode();
    ArrayList<Sha3256Hash> newPath = Sha3256HashFixture.newSha3256HashArrayList(path.size());
    MerklePath manipulatedMerklePath = new MerklePath(newPath, isLeft);
    AuthenticatedEntity tamperedEntity = new MerkleTreeAuthenticatedEntity(
            new MerkleProof(proof.getRoot(), manipulatedMerklePath),
            authenticatedEntity.type(),
            authenticatedEntity.getEntity());

    MerkleTreeAuthenticatedEntityVerifier verifier = new MerkleTreeAuthenticatedEntityVerifier();
    Assertions.assertTrue(verifier.verify(authenticatedEntity)); // original authenticated entity passes verification.
    Assertions.assertFalse(verifier.verify(tamperedEntity)); // tampered entity fails verification.
  }
}
