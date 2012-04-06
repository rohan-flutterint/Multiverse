package org.multiverse.collections;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.multiverse.api.Stm;
import org.multiverse.api.Txn;
import org.multiverse.api.closures.AtomicVoidClosure;

import static org.junit.Assert.*;
import static org.multiverse.api.GlobalStmInstance.getGlobalStmInstance;
import static org.multiverse.api.StmUtils.atomic;
import static org.multiverse.api.TxnThreadLocal.clearThreadLocalTxn;

public class NaiveTransactionalLinkedList_offerFirstTest {

    private Stm stm;

    @Before
    public void setUp() {
        stm = getGlobalStmInstance();
        clearThreadLocalTxn();
    }

    @Test
    public void whenNullItem_thenNullPointerException() {
        final NaiveTransactionalLinkedList<String> list = new NaiveTransactionalLinkedList<String>(stm);

        atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                try {
                    list.offerFirst(null);
                    fail();
                } catch (NullPointerException expected) {
                }

                assertEquals("[]", list.toString());
                assertEquals(0, list.size());
            }
        });
    }

    @Test
    public void whenEmpty() {
        final NaiveTransactionalLinkedList<String> list = new NaiveTransactionalLinkedList<String>(stm);

        atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                boolean result = list.offerFirst("1");

                assertTrue(result);
                assertEquals("[1]", list.toString());
                assertEquals(1, list.size());
            }
        });
    }

    @Test
    public void whenNotEmpty() {
        final NaiveTransactionalLinkedList<String> list = new NaiveTransactionalLinkedList<String>(stm);

        atomic(new AtomicVoidClosure() {
            @Override
            public void execute(Txn tx) throws Exception {
                list.offerFirst("1");
                boolean result = list.offerFirst("2");

                assertTrue(result);
                assertEquals("[2, 1]", list.toString());
                assertEquals(2, list.size());
            }
        });
    }

    @Test
    @Ignore
    public void whenFull() {

    }
}
