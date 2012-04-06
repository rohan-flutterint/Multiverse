package org.multiverse.stms.gamma.integration.blocking;

import org.junit.Test;
import org.multiverse.api.TransactionExecutor;
import org.multiverse.api.LockMode;
import org.multiverse.stms.gamma.LeanGammaTransactionExecutor;
import org.multiverse.stms.gamma.transactions.GammaTxnConfiguration;
import org.multiverse.stms.gamma.transactions.fat.FatVariableLengthGammaTxnFactory;

public class QueueWithCapacity_FatVariableLengthGammaTransaction_StressTest extends QueueWithCapacity_AbstractTest {

    private LockMode lockMode;

    @Test
    public void testNoLock() {
        lockMode = LockMode.None;
        run();
    }

    @Test
    public void testReadLock() {
        lockMode = LockMode.Read;
        run();
    }

    @Test
    public void testWriteLock() {
        lockMode = LockMode.Write;
        run();
    }

    @Test
    public void testExclusiveLock() {
        lockMode = LockMode.Exclusive;
        run();
    }

    @Override
    protected TransactionExecutor newPopBlock() {
        GammaTxnConfiguration config = new GammaTxnConfiguration(stm)
                .setReadLockMode(lockMode);
        return new LeanGammaTransactionExecutor(new FatVariableLengthGammaTxnFactory(config));
    }

    @Override
    protected TransactionExecutor newPushBlock() {
        GammaTxnConfiguration config = new GammaTxnConfiguration(stm)
                .setReadLockMode(lockMode);
        return new LeanGammaTransactionExecutor(new FatVariableLengthGammaTxnFactory(config));
    }
}
