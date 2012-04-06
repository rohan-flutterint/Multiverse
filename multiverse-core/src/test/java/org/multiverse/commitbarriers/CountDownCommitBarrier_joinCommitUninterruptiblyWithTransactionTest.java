package org.multiverse.commitbarriers;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.TestThread;
import org.multiverse.api.Txn;
import org.multiverse.api.TxnStatus;
import org.multiverse.api.closures.AtomicVoidClosure;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.references.IntRef;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaIntRef;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.TxnThreadLocal.clearThreadLocalTxn;

public class CountDownCommitBarrier_joinCommitUninterruptiblyWithTransactionTest {
    private CountDownCommitBarrier barrier;
    private GammaStm stm;

    @Before
    public void setUp() {
        stm = new GammaStm();
        clearThreadLocalTxn();
        clearCurrentThreadInterruptedStatus();
    }

    @After
    public void tearDown() {
        clearCurrentThreadInterruptedStatus();
    }

    @Test
    public void whenTransactionNull_thenFailWithNullPointerException() {
        barrier = new CountDownCommitBarrier(1);

        try {
            barrier.joinCommitUninterruptibly(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionFailsToPrepare() {
        barrier = new CountDownCommitBarrier(1);
        Txn tx = mock(Txn.class);

        when(tx.getStatus()).thenReturn(TxnStatus.Active);
        doThrow(new RuntimeException()).when(tx).prepare();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail("Expecting Runtime Exception thrown on Txn preparation");
        } catch (RuntimeException ex) {

        }
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionAborted_thenDeadTransactionException() {
        barrier = new CountDownCommitBarrier(1);

        Txn tx = stm.newDefaultTransaction();
        tx.abort();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail("Should have thrown DeadTransactionException");
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenTransactionCommitted_thenDeadTransactionException() {
        barrier = new CountDownCommitBarrier(1);

        Txn tx = stm.newDefaultTransaction();
        tx.commit();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail("Should have thrown DeadTransactionException");
        } catch (DeadTransactionException expected) {
        }

        assertIsCommitted(tx);
        assertTrue(barrier.isClosed());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    @Ignore
    public void whenStartingInterrupted() throws InterruptedException {
    }

    @Test
    public void whenInterruptedWhileWaiting_thenNoInterruption() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final IntRef ref = stm.getDefaultRefFactory().newIntRef(10);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                stm.getDefaultTxnExecutor().atomic(new AtomicVoidClosure() {
                    @Override
                    public void execute(Txn tx) throws Exception {
                        ref.incrementAndGet(tx, 1);
                        barrier.joinCommitUninterruptibly(tx);
                    }
                });
            }
        };

        t.setPrintStackTrace(false);
        t.start();
        sleepMs(500);
        t.interrupt();
        sleepMs(500);

        assertAlive(t);
        assertTrue(barrier.isClosed());

        //todo
        //assertTrue(t.isInterrupted());
    }

    @Test
    public void whenCommittedWhileWaiting() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final GammaIntRef ref = stm.getDefaultRefFactory().newIntRef(0);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                stm.getDefaultTxnExecutor().atomic(new AtomicVoidClosure() {
                    @Override
                    public void execute(Txn tx) throws Exception {
                        ref.incrementAndGet(tx, 1);
                        barrier.joinCommitUninterruptibly(tx);
                    }
                });
            }
        };

        t.setPrintStackTrace(false);
        t.start();
        sleepMs(500);

        barrier.countDown();
        sleepMs(500);

        t.join();
        assertNothingThrown(t);
        assertTrue(barrier.isCommitted());
        assertEquals(1, ref.atomicGet());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAbortedWhileWaiting_() throws InterruptedException {
        barrier = new CountDownCommitBarrier(2);

        final GammaIntRef ref = stm.getDefaultRefFactory().newIntRef(0);

        TestThread t = new TestThread() {
            @Override
            public void doRun() throws Exception {
                stm.getDefaultTxnExecutor().atomic(new AtomicVoidClosure() {
                    @Override
                    public void execute(Txn tx) throws Exception {
                        ref.getAndIncrement(tx, 1);
                        barrier.joinCommitUninterruptibly(tx);
                    }
                });
            }
        };

        t.setPrintStackTrace(false);
        t.start();
        sleepMs(500);

        barrier.abort();
        sleepMs(500);

        t.join();
        t.assertFailedWithException(IllegalStateException.class);
        assertTrue(barrier.isAborted());
        assertEquals(0, ref.atomicGet());
        assertEquals(0, barrier.getNumberWaiting());
    }

    @Test
    public void whenAborted_thenCommitBarrierOpenException() {
        barrier = new CountDownCommitBarrier(1);
        barrier.abort();

        Txn tx = stm.newDefaultTransaction();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail("Expecting CommitBarrierOpenException");
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isAborted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsAborted(tx);
    }

    @Test
    public void whenCommitted_thenCommitBarrierOpenException() {
        barrier = new CountDownCommitBarrier(0);

        Txn tx = stm.newDefaultTransaction();
        try {
            barrier.joinCommitUninterruptibly(tx);
            fail("Expecting CommitBarrierOpenException");
        } catch (CommitBarrierOpenException expected) {
        }

        assertTrue(barrier.isCommitted());
        assertEquals(0, barrier.getNumberWaiting());
        assertIsAborted(tx);
    }
}
