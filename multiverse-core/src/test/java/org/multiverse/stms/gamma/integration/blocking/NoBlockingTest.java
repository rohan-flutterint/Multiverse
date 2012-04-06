package org.multiverse.stms.gamma.integration.blocking;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.TxnExecutor;
import org.multiverse.api.Txn;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.RetryNotAllowedException;
import org.multiverse.api.exceptions.RetryNotPossibleException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.references.LongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTxn;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.*;
import static org.multiverse.api.TxnThreadLocal.clearThreadLocalTxn;

public class NoBlockingTest {

    @Before
    public void setUp() {
        clearThreadLocalTxn();
    }

    @Test
    public void whenNothingRead_thenNoRetryPossibleException() {
        try {
            atomic(new AtomicVoidClosure() {
                @Override
                public void execute(Txn tx) throws Exception {
                    retry();
                }
            });
            fail();
        } catch (RetryNotPossibleException expected) {
        }
    }

    @Test
    public void whenContainsCommute_thenNoRetryPossibleException() {
        final LongRef ref = newLongRef();

        try {
            atomic(new AtomicVoidClosure() {
                @Override
                public void execute(Txn tx) throws Exception {
                    ref.commute(Functions.incLongFunction());
                    retry();
                }
            });
            fail();
        } catch (RetryNotPossibleException expected) {
        }
    }

    @Test
    public void whenContainsConstructing_thenNoRetryPossibleException() {
        try {
            atomic(new AtomicVoidClosure() {
                @Override
                public void execute(Txn tx) throws Exception {
                    GammaTxn btx = (GammaTxn) tx;
                    GammaLongRef ref = new GammaLongRef(btx);
                    retry();
                }
            });
            fail();
        } catch (RetryNotPossibleException expected) {
        }
    }

    @Test
    public void whenBlockingNotAllowed_thenNoBlockingRetryAllowedException() {
        final LongRef ref = newLongRef();

        TxnExecutor block = getGlobalStmInstance()
                .newTransactionFactoryBuilder()
                .setBlockingAllowed(false)
                .newTxnExecutor();

        try {
            block.atomic(new AtomicVoidClosure() {
                @Override
                public void execute(Txn tx) throws Exception {
                    ref.set(1);
                    retry();
                }
            });
            fail();
        } catch (RetryNotAllowedException expected) {
        }

        assertEquals(0, ref.atomicGet());
    }
}
