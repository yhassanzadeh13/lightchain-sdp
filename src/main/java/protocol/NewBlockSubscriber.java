package protocol;

import model.lightchain.Identifier;

/**
 * Encapsulates logic of subscribing to the events of arrival of new blocks.
 */
public interface NewBlockSubscriber {
  /**
   * OnNewValidatedBlock is called whenever a new block arrives to the node that successfully pass validation.
   *
   * @param blockId identifier of block.
   */
  void onNewValidatedBlock(Identifier blockId);
}
