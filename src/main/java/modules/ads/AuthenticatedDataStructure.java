package modules.ads;

import model.Entity;
import model.lightchain.Identifier;

/**
 * Models AuthenticatedDataStructure (ADS) and a key-value store of entities supported with membership proofs.
 */
public interface AuthenticatedDataStructure {
  /**
   * Adds an entity to the ADS.
   *
   * @param e the entity to add
   *
   * @return AuthenticatedEntity containing the entity and its membership proof
   */
  AuthenticatedEntity put(Entity e);

  /**
   * Returns the AuthenticatedEntity corresponding to the given identifier.
   *
   * @param id the identifier of the entity to retrieve
   *
   * @return the AuthenticatedEntity corresponding to the given identifier
   */
  AuthenticatedEntity get(Identifier id);

  /**
   * Returns the size of the ADS.
   *
   * @return the size of the ADS
   */
  int size();
}
