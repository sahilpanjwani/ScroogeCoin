public class Test {
    public static void main(String[] args) {
        System.out.println("here");
        Transaction txn = new Transaction();
        TxHandler txHandler = new TxHandler(new UTXOPool());
        txHandler.isValidTx(txn);
        Transaction[] transactions = new Transaction[1];
        transactions[0] = txn;
        txHandler.handleTxs(transactions);
    }
}
