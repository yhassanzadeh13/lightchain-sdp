package modules.ads.merkletree;

import model.Entity;
import modules.ads.MembershipProof;

/**
 * An entity with its membership proof and type.
 */
public class MerkleTreeAuthenticatedEntity extends modules.ads.AuthenticatedEntity {
  private final MembershipProof membershipProof;
  private final String type;
  private final Entity entity;

  /**
   * Constructor of an authenticated entity.
   *
   * @param proof the membership proof
   * @param type  the type of the entity
   * @param e     the entity
   */
  public MerkleTreeAuthenticatedEntity(MerkleProof proof, String type, Entity e) {
    this.membershipProof = new MerkleProof(proof.getRoot(), proof.getMerklePath());
    this.type = type;
    this.entity = e;
  }

  /**
   * Gets the type of the entity.
   *
   * @return the type of the entity
   */
  @Override
  public String type() {
    return type;
  }

  /**
   * Gets the entity.
   *
   * @return the entity
   */
  @Override
  public Entity getEntity() {
    return entity;
  }

  /**
   * Gets the membership proof.
   *
   * @return the membership proof
   */
  @Override
  public MembershipProof getMembershipProof() {
    return new MerkleProof(membershipProof.getRoot(), membershipProof.getMerklePath());
  }
}
