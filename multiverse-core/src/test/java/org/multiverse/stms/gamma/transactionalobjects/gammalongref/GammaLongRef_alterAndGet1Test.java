package org.multiverse.stms.gamma.transactionalobjects.gammalongref;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.multiverse.api.TxnFactory;
import org.multiverse.api.exceptions.DeadTransactionException;
import org.multiverse.api.exceptions.PreparedTransactionException;
import org.multiverse.api.exceptions.TransactionMandatoryException;
import org.multiverse.api.functions.Functions;
import org.multiverse.api.functions.LongFunction;
import org.multiverse.stms.gamma.GammaStm;
import org.multiverse.stms.gamma.transactionalobjects.GammaLongRef;
import org.multiverse.stms.gamma.transactions.GammaTxn;
import org.multiverse.stms.gamma.transactions.GammaTxnFactory;
import org.multiverse.stms.gamma.transactions.fat.FatFixedLengthGammaTxnFactory;
import org.multiverse.stms.gamma.transactions.fat.FatMonoGammaTxnFactory;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTxnFactory;

import java.util.Collection;

import static java.util.Arrays.asList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.*;
import static org.multiverse.TestUtils.*;
import static org.multiverse.api.TxnThreadLocal.*;
import static org.multiverse.api.functions.Functions.identityLongFunction;
import static org.multiverse.api.functions.Functions.incLongFunction;
import static org.multiverse.stms.gamma.GammaTestUtils.*;

@RunWith(Parameterized.class)
public class GammaLongRef_alterAndGet1Test {
    private final GammaTxnFactory transactionFactory;
    private final GammaStm stm;

    public GammaLongRef_alterAndGet1Test(GammaTxnFactory transactionFactory) {
        this.transactionFactory = transactionFactory;
        this.stm = transactionFactory.getConfiguration().getStm();
    }

    @Before
    public void setUp() {
        clearThreadLocalTxn();
    }

    @Parameterized.Parameters
    public static Collection<TxnFactory[]> configs() {
        return asList(
                new TxnFactory[]{new FatVariableLengthGammaTxnFactory(new GammaStm())},
                new TxnFactory[]{new FatFixedLengthGammaTxnFactory(new GammaStm())},
                new TxnFactory[]{new FatMonoGammaTxnFactory(new GammaStm())}
        );
    }

    @Test
    public void whenActiveTransactionAvailableAndNullFunction_thenNullPointerException() {
        GammaLongRef ref = new GammaLongRef(stm);

        GammaTxn tx = transactionFactory.newTransaction();
        setThreadLocalTxn(tx);

        try {
            ref.alterAndGet(null);
            fail();
        } catch (NullPointerException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTxn());
    }

    @Test
    public void whenFunctionCausesException() {
        GammaLongRef ref = new GammaLongRef(stm);

        LongFunction function = mock(LongFunction.class);
        RuntimeException ex = new RuntimeException();
        when(function.call(anyLong())).thenThrow(ex);

        GammaTxn tx = transactionFactory.newTransaction();
        setThreadLocalTxn(tx);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (RuntimeException found) {
            assertSame(ex, found);
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTxn());
    }

    @Test
    public void whenActiveTransactionAvailable() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTxn tx = transactionFactory.newTransaction();
        setThreadLocalTxn(tx);

        LongFunction function = Functions.incLongFunction();
        ref.alterAndGet(function);
        assertEquals(initialValue + 1, ref.get());
        assertVersionAndValue(ref, initialVersion, initialValue);
        tx.commit();

        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }

    @Test
    public void whenActiveTransactionAvailableButNoChange_thenNoWrite() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        GammaTxn tx = transactionFactory.newTransaction();
        setThreadLocalTxn(tx);

        LongFunction function = identityLongFunction();
        ref.alterAndGet(function);
        assertEquals(initialValue, ref.get());
        assertVersionAndValue(ref, initialVersion, initialValue);
        tx.commit();

        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenPreparedTransactionAvailable_thenPreparedTransactionException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongFunction function = mock(LongFunction.class);
        GammaTxn tx = transactionFactory.newTransaction();
        tx.prepare();
        setThreadLocalTxn(tx);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (PreparedTransactionException expected) {

        }

        assertIsAborted(tx);
        verifyZeroInteractions(function);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenNoTransactionAvailable_thenNoTransactionFoundException() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();
        LongFunction function = Functions.incLongFunction(1);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (TransactionMandatoryException expected) {

        }

        assertVersionAndValue(ref, initialVersion, initialValue);
        assertNull(getThreadLocalTxn());
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
    }

    @Test
    public void whenCommittedTransactionAvailable_thenDeadTransactionException() {
        GammaTxn tx = transactionFactory.newTransaction();
        setThreadLocalTxn(tx);
        tx.commit();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();
        LongFunction function = Functions.incLongFunction(1);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (DeadTransactionException expected) {

        }

        assertIsCommitted(tx);
        assertSame(tx, getThreadLocalTxn());
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenAbortedTransactionAvailable_thenDeadTransactionException() {
        GammaTxn tx = transactionFactory.newTransaction();
        setThreadLocalTxn(tx);
        tx.abort();

        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();
        LongFunction function = incLongFunction(1);

        try {
            ref.alterAndGet(function);
            fail();
        } catch (DeadTransactionException expected) {
        }

        assertIsAborted(tx);
        assertSame(tx, getThreadLocalTxn());
        assertSurplus(ref, 0);
        assertRefHasNoLocks(ref);
        assertVersionAndValue(ref, initialVersion, initialValue);
    }

    @Test
    public void whenListenersAvailable_thenTheyAreNotified() {
        long initialValue = 10;
        GammaLongRef ref = new GammaLongRef(stm, initialValue);
        long initialVersion = ref.getVersion();

        LongRefAwaitThread thread = new LongRefAwaitThread(ref, initialValue + 1);
        thread.start();

        sleepMs(500);

        GammaTxn tx = stm.newDefaultTransaction();
        setThreadLocalTxn(tx);
        ref.alterAndGet(Functions.incLongFunction());
        tx.commit();

        joinAll(thread);

        assertVersionAndValue(ref, initialVersion + 1, initialValue + 1);
    }
}
