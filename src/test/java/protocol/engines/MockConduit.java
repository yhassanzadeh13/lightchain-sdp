package protocol.engines;

import java.util.ArrayList;
import java.util.HashMap;

import model.Entity;
import model.exceptions.LightChainDistributedStorageException;
import model.exceptions.LightChainNetworkingException;
import model.lightchain.Identifier;
import network.Conduit;

/**
 * MockConduit represents the Networking interface that is exposed to an Engine.
 */
public class MockConduit implements Conduit {
  private final HashMap<Identifier, Entity> sentEntities;

  public MockConduit() {
    this.sentEntities = new HashMap<>();
  }

  /**
   * Sends the Entity through the Network to the remote target.
   *
   * @param e      the Entity to be sent over the network.
   * @param target Identifier of the receiver.
   * @throws LightChainNetworkingException any unhappy path taken on sending the Entity.
   */
  @Override
  public void unicast(Entity e, Identifier target) throws LightChainNetworkingException {
    this.sentEntities.put(target, e);
  }

  /**
   * Stores given Entity on the underlying Distributed Hash Table (DHT) of nodes.
   *
   * @param e the Entity to be stored over the network.
   * @throws LightChainDistributedStorageException any unhappy path taken on storing the Entity.
   */
  @Override
  public void put(Entity e) throws LightChainDistributedStorageException {

  }

  /**
   * Retrieves the entity corresponding to the given identifier form the underlying Distributed Hash Table
   * (DHT) of nodes.
   *
   * @param identifier identifier of the entity to be retrieved.
   * @return the retrieved entity or null if it does not exist.
   * @throws LightChainDistributedStorageException any unhappy path taken on retrieving the Entity.
   */
  @Override
  public Entity get(Identifier identifier) throws LightChainDistributedStorageException {
    return null;
  }

  @Override
  public ArrayList<Entity> allEntities() throws LightChainDistributedStorageException {
    return null;
  }

}
