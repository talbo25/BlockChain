import java.util.ArrayList;
import java.util.List;

public class TxHandler {

    private UTXOPool utxoPool;
    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
        this.utxoPool= new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        double sum_inputs=0;
        double sum_outputs =0;

        UTXOPool temp_pool=new UTXOPool(); //create a temporary pool to verify for no double claiming

        for (Transaction.Input input : tx.getInputs()) {
            UTXO check_utxo = new UTXO(input.prevTxHash,input.outputIndex); // a temporary utxo object to check validation

            if (!utxoPool.contains(check_utxo)) return false; // (1)

            Transaction.Output match_output=utxoPool.getTxOutput(check_utxo);   //  get the match output from the UTXOpool

            if (!Crypto.verifySignature(match_output.address,tx.getRawDataToSign(tx.getInputs().indexOf(input)),input.signature))
                return false;   // (2)

            if (temp_pool.contains(check_utxo)) return false;   //(3)

            temp_pool.addUTXO(check_utxo,match_output);     //  insert the new match output to the temporary UTXOpool

            sum_inputs+=match_output.value;     //  add the value of the input (according to the match output) to the total inputs' sum
        }

        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value<0) return false;   //(4)

            sum_outputs+=output.value;  //  sum the outputs
        }

        if (sum_inputs<sum_outputs) return false;   //(5)


        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        List<Transaction> new_Trxs= new ArrayList<>();
        for (Transaction possibleTx : possibleTxs) {
            if(isValidTx(possibleTx)) {     //  check if current transaction is valid
                new_Trxs.add(possibleTx);   //  if found valid - the transaction is added

                for (Transaction.Input input : possibleTx.getInputs()) {    // matching the inputs to the output from the UTXO pool
                    UTXO matched_utxo = new UTXO(input.prevTxHash,input.outputIndex);
                    utxoPool.removeUTXO(matched_utxo);      //  remove the matched output from the UTXO pool
                }
                for (Transaction.Output output : possibleTx.getOutputs()) {     // add the new output to UTXO the pool
                    UTXO matched_utxo = new UTXO(possibleTx.getHash(),possibleTx.getOutputs().indexOf(output));
                    utxoPool.addUTXO(matched_utxo,output);
                }
            }
        }

        return new_Trxs.toArray(new Transaction[new_Trxs.size()]);

    }

    public UTXOPool getUTXOPool(){
        return new UTXOPool(utxoPool);
    }

}
