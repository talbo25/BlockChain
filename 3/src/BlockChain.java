// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.HashMap;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    TransactionPool txs_pool = new TransactionPool();

    private class BlockNode {
        Block block;
        ArrayList<BlockNode> children_nodes = new ArrayList<>();
        UTXOPool utxo_pool;
        int height;
        BlockNode parent_node;

        BlockNode(Block _block, UTXOPool _utxo_pool, BlockNode _parent) {
            this.block = _block;
            this.utxo_pool = _utxo_pool;
            if (_parent != null) {   // if not genesis block
                this.parent_node = _parent;
                this.height = _parent.height + 1;
                _parent.children_nodes.add(this);
            } else {
                this.parent_node = null;
                this.height = 1;
            }
        }
    }

    HashMap<ByteArrayWrapper, BlockNode> hash_chain = new HashMap<>();
    BlockNode max_height_node;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        UTXOPool utxo_pool = new UTXOPool();
        init_UTXOPool(genesisBlock,utxo_pool);

        BlockNode gen_node = new BlockNode(genesisBlock, utxo_pool, null);
        hash_chain.put(new ByteArrayWrapper(genesisBlock.getHash()), gen_node);

        max_height_node = gen_node;
    }

    public void init_UTXOPool(Block _block, UTXOPool _pool) {
        Transaction current_base = _block.getCoinbase();

        for (int i = 0; i < current_base.numOutputs(); ++i) {
            _pool.addUTXO((new UTXO(current_base.getHash(),i)),(current_base.getOutput(i)));
        }
    }

    /**
     * Get the maximum height block
     */
    public Block getMaxHeightBlock() {
        return max_height_node.block;
    }

    /**
     * Get the UTXOPool for mining a new block on top of max height block
     */
    public UTXOPool getMaxHeightUTXOPool() {
        return (new UTXOPool(max_height_node.utxo_pool));
    }

    /**
     * Get the transaction pool to mine a new block
     */
    public TransactionPool getTransactionPool() {
        return txs_pool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     *
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     *
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        //check if get a null block
        if (block == null) {
            return false;
        }

        //check if there is already a block with the same hash key
        if (block.getHash() == null) {
            return false;
        }

        //check if parent exist
        if (block.getPrevBlockHash()==null){
            return false;
        }
        BlockNode parent_node = hash_chain.get(new ByteArrayWrapper(block.getPrevBlockHash()));
        if (parent_node ==null) {
            return false;
        }

        // check if all transactions are valid
        TxHandler current_handler = new TxHandler(new UTXOPool(parent_node.utxo_pool));
        Transaction[] check_txs = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] valid_txs = current_handler.handleTxs(check_txs);
        if (check_txs.length != valid_txs.length) {
            return false;
        }

        // check the height
        if ((parent_node.height + 1) <= (max_height_node.height - CUT_OFF_AGE)) {
            return false;
        }
        UTXOPool temp_pool = current_handler.getUTXOPool();

        init_UTXOPool(block,temp_pool);

        BlockNode new_block_node = new BlockNode(block, temp_pool,parent_node);

        hash_chain.put(new ByteArrayWrapper(block.getHash()),new_block_node);

        if (new_block_node.height > max_height_node.height) {
            max_height_node = new_block_node;
        }

        return true;
    }
    /**
     * Add a transaction to the transaction pool
     */
    public void addTransaction(Transaction tx) {
        txs_pool.addTransaction(tx);
    }
}