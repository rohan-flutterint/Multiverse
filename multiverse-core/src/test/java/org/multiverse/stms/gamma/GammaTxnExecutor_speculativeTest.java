package org.multiverse.stms.gamma;

import org.junit.Before;
import org.junit.Test;
import org.multiverse.api.Txn;
import org.multiverse.api.TxnExecutor;
import org.multiverse.api.LockMode;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.functions.Function;
import org.multiverse.api.lifecycle.TransactionListener;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactionalobjects.GammaRef;
import org.multiverse.stms.gamma.transactions.GammaTxn;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTxn;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTxn;
import org.multiverse.stms.gamma.transactions.lean.LeanFixedLengthGammaTxn;
import org.multiverse.stms.gamma.transactions.lean.LeanMonoGammaTxn;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.multiverse.TestUtils.assertInstanceof;
import static org.multiverse.api.TxnThreadLocal.clearThreadLocalTxn;
import static org.multiverse.api.TxnThreadLocal.getThreadLocalTxn;

public class GammaTxnExecutor_speculativeTest implements GammaConstants {

    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTxn();
    }

    @Test
    public void whenTransactionGrowing() {
        final GammaRef<Long>[] refs = new GammaRef[1000];
        for (int k = 0; k < refs.length; k++) {
            refs[k] = new GammaRef<Long>(stm, 0L);
        }

        final List<GammaTxn> transactions = new LinkedList<GammaTxn>();
        final AtomicInteger attempt = new AtomicInteger(1);

        TxnExecutor block = stm.newTransactionFactoryBuilder()
                .setSpeculative(true)
                .setControlFlowErrorsReused(false)
                .setDirtyCheckEnabled(false)
                .newTxnExecutor();

        block.atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                System.out.println(tx.getClass());
                assertSame(tx, getThreadLocalTxn());
                GammaTxn btx = (GammaTxn) tx;
                assertEquals(attempt.get(), tx.getAttempt());
                attempt.incrementAndGet();

                transactions.add(btx);

                for (GammaRef<Long> ref : refs) {
                    ref.set(1L);
                }
            }
        });

        for (GammaRef ref : refs) {
            assertEquals(1L, ref.atomicGet());
        }

        assertEquals(4, transactions.size());
        assertInstanceof(LeanMonoGammaTxn.class, transactions.get(0));
        assertInstanceof(LeanFixedLengthGammaTxn.class, transactions.get(1));
        assertInstanceof(FatVariableLengthGammaTxn.class, transactions.get(2));
        assertInstanceof(FatVariableLengthGammaTxn.class, transactions.get(3));
    }

    @Test
    public void whenCommute() {
        final List<GammaTxn> transactions = new LinkedList<GammaTxn>();
        final GammaRef<String> ref = new GammaRef<String>(stm);
        final Function<String> function = mock(Function.class);

        TxnExecutor block = stm.newTransactionFactoryBuilder()
                .setDirtyCheckEnabled(false)
                .setSpeculative(true)
                .newTxnExecutor();

        block.atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                transactions.add(btx);
                ref.commute(btx, function);
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTxn);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTxn);
    }

    @Test
    public void whenEnsure() {
        final List<GammaTxn> transactions = new LinkedList<GammaTxn>();
        final GammaRef<String> ref = new GammaRef<String>(stm);

        TxnExecutor block = stm.newTransactionFactoryBuilder()
                .setDirtyCheckEnabled(false)
                .setSpeculative(true)
                .newTxnExecutor();

        block.atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                transactions.add(btx);
                ref.get(btx);
                ref.ensure(btx);
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTxn);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTxn);
    }


    @Test
    public void whenConstructing() {
        final List<GammaTxn> transactions = new LinkedList<GammaTxn>();

        TxnExecutor block = stm.newTransactionFactoryBuilder()
                .setSpeculative(true)
                .setDirtyCheckEnabled(false)
                .newTxnExecutor();

        block.atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                transactions.add(btx);

                new GammaRef(btx);
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTxn);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTxn);
    }

    @Test
    public void whenNonRef() {
        final List<GammaTxn> transactions = new LinkedList<GammaTxn>();
        final GammaLongRef ref = new GammaLongRef(stm);

        TxnExecutor block = stm.newTransactionFactoryBuilder()
                .setSpeculative(true)
                .setDirtyCheckEnabled(false)
                .newTxnExecutor();

        block.atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                transactions.add(btx);
                ref.get(tx);
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTxn);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTxn);
    }

    @Test
    public void whenExplicitLocking() {
        final List<GammaTxn> transactions = new LinkedList<GammaTxn>();
        final GammaRef ref = new GammaRef(stm);

        TxnExecutor block = stm.newTransactionFactoryBuilder()
                .setSpeculative(true)
                .setDirtyCheckEnabled(false)
                .newTxnExecutor();

        block.atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                GammaTxn btx = (GammaTxn) tx;
                transactions.add(btx);
                ref.getLock().acquire(LockMode.Read);
            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTxn);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTxn);
    }

    @Test
    public void whenNormalListenerAdded() {
        final List<GammaTxn> transactions = new LinkedList<GammaTxn>();
        final AtomicBoolean added = new AtomicBoolean();
        final TransactionListener listener = mock(TransactionListener.class);

        TxnExecutor block = stm.newTransactionFactoryBuilder()
                .setSpeculative(true)
                .setDirtyCheckEnabled(false)
                .newTxnExecutor();

        block.atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                assertSame(tx, getThreadLocalTxn());
                GammaTxn btx = (GammaTxn) tx;
                transactions.add(btx);

                if (!added.get()) {
                    btx.register(listener);
                }

            }
        });

        assertEquals(2, transactions.size());
        assertTrue(transactions.get(0) instanceof LeanMonoGammaTxn);
        assertTrue(transactions.get(1) instanceof FatMonoGammaTxn);
    }

    @Test
    public void whenTimeoutAvailable_thenCopied() {
        final GammaLongRef ref1 = new GammaLongRef(stm);
        final GammaLongRef ref2 = new GammaLongRef(stm);

        final List<GammaTxn> transactions = new LinkedList<GammaTxn>();

        TxnExecutor block = stm.newTransactionFactoryBuilder()
                .setTimeoutNs(1000)
                .setSpeculative(true)
                .setDirtyCheckEnabled(false)
                .newTxnExecutor();

        block.atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                assertSame(tx, getThreadLocalTxn());
                GammaTxn btx = (GammaTxn) tx;
                transactions.add(btx);

                if (transactions.size() == 1) {
                    btx.remainingTimeoutNs = 500;
                } else {
                    assertEquals(500, btx.getRemainingTimeoutNs());
                }

                ref1.openForWrite(btx, LOCKMODE_NONE);
                ref2.openForWrite(btx, LOCKMODE_NONE);
            }
        });
    }
}
