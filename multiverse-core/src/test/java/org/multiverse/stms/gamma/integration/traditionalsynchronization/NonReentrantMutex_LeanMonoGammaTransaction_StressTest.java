package org.multiverse.stms.gamma.integration.traditionalsynchronization;

import org.junit.Test;
import org.multiverse.api.TransactionExecutor;
import org.multiverse.stms.gamma.LeanGammaTransactionExecutor;
import org.multiverse.stms.gamma.transactions.GammaTxnConfiguration;
import org.multiverse.stms.gamma.transactions.lean.LeanMonoGammaTxnFactory;

public class NonReentrantMutex_LeanMonoGammaTransaction_StressTest extends NonReentrantMutex_AbstractTest {

    @Test
    public void test() {
        run();
    }

    @Override
    protected TransactionExecutor newLockBlock() {
        GammaTxnConfiguration config = new GammaTxnConfiguration(stm)
                .setMaxRetries(10000);
        return new LeanGammaTransactionExecutor(new LeanMonoGammaTxnFactory(config));
    }

    @Override
    protected TransactionExecutor newUnlockBlock() {
        GammaTxnConfiguration config = new GammaTxnConfiguration(stm)
                .setMaxRetries(10000);
        return new LeanGammaTransactionExecutor(new LeanMonoGammaTxnFactory(config));
    }
}
