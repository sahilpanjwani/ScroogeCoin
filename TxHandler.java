import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
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
        ArrayList<Transaction.Input> txInputs = tx.getInputs();
        ArrayList<Transaction.Output> txOutputs = tx.getOutputs();
        ArrayList<UTXO> allCurrentUTXOs = this.utxoPool.getAllUTXO();
        double totalOfInputValues = 0.0;
        double totalOfOutputValues = 0.0;
        for (int i=0; i < txInputs.size(); i++) {
            Transaction.Input currentInput = tx.getInput(i);
            UTXO currentUTXO = new UTXO(currentInput.prevTxHash, currentInput.outputIndex);
            if (!allCurrentUTXOs.contains(currentUTXO))
                return false;
            allCurrentUTXOs.remove(currentUTXO);
            Transaction.Output prevOutput = this.utxoPool.getTxOutput(currentUTXO);
            if (!Crypto.verifySignature(prevOutput.address, tx.getRawDataToSign(i), currentInput.signature))
                return false;
            totalOfInputValues += prevOutput.value;
        }
        for (Transaction.Output currentOutput : txOutputs) {
            if (currentOutput.value < 0)
                return false;
            totalOfOutputValues += currentOutput.value;
        }
        if (totalOfInputValues < totalOfOutputValues)
            return false;
        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> validTxns = new ArrayList<>();
        ArrayList<Transaction> tempInvalidTxns = new ArrayList<>(Arrays.asList(possibleTxs));

        boolean txnTurnedValid = true;
        while (txnTurnedValid) {
            txnTurnedValid = false;
            List<Transaction> toBeRemoved = new ArrayList<>();
            for (Transaction txn : tempInvalidTxns) {
                if (isValidTx(txn)) {
                    validTxns.add(txn);
                    adjustTxnPool(txn);
                    txnTurnedValid = true;
                    toBeRemoved.add(txn);
                }
            }
            tempInvalidTxns.removeAll(toBeRemoved);
        }

        Transaction[] finalTxns = new Transaction[validTxns.size()];
        finalTxns = validTxns.toArray(finalTxns);
        return finalTxns;
    }

    private void adjustTxnPool(Transaction txn) {
        ArrayList<Transaction.Input> inputs = txn.getInputs();

        for (Transaction.Input input : inputs) {
            UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
            this.utxoPool.removeUTXO(utxo);
        }

        for (int i=0; i < txn.numOutputs(); i++) {
            Transaction.Output output = txn.getOutput(i);
            this.utxoPool.addUTXO(new UTXO(txn.getHash(), i), output);
        }
    }

}
