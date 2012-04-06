package org.multiverse.stms.gamma.transactionalobjects.txnlong;

import org.multiverse.TestThread;
import org.multiverse.api.Txn;
import org.multiverse.api.closures.TxnVoidClosure;
import org.multiverse.api.predicates.LongPredicate;
import org.multiverse.stms.gamma.transactionalobjects.GammaTxnLong;

public class TxnLongAwaitThread extends TestThread {
    private final GammaTxnLong ref;
    private final LongPredicate predicate;

    public TxnLongAwaitThread(GammaTxnLong ref, final long awaitValue) {
        this(ref, new LongPredicate() {
            @Override
            public boolean evaluate(long current) {
                return current == awaitValue;
            }
        });
    }

    public TxnLongAwaitThread(GammaTxnLong ref, LongPredicate predicate) {
        this.ref = ref;
        this.predicate = predicate;
    }

    @Override
    public void doRun() throws Exception {
        ref.getStm().getDefaultTxnExecutor().atomic(new TxnVoidClosure() {
            @Override
            public void call(Txn tx) throws Exception {
                System.out.println("Starting wait and ref.value found: " + ref.get());
                ref.await(predicate);
                System.out.println("Finished wait and ref.value found: " + ref.get());
            }
        });
    }
}
