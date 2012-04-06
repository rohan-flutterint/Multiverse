package org.multiverse.stms.gamma.integration.traditionalsynchronization;

import org.junit.Test;
import org.multiverse.api.TransactionExecutor;
import org.multiverse.stms.gamma.LeanGammaTransactionExecutor;
import org.multiverse.stms.gamma.transactions.GammaTxnConfiguration;
import org.multiverse.stms.gamma.transactions.lean.LeanFixedLengthGammaTxnFactory;

/**
 * @author Peter Veentjer
 */
public class Semaphore_LeanFixedLengthGammaTransaction_StressTest extends Semaphore_AbstractTest {

    @Test
    public void test() {
        run();
    }

    @Override
    protected TransactionExecutor newDownBlock() {
        GammaTxnConfiguration config = new GammaTxnConfiguration(stm);
        return new LeanGammaTransactionExecutor(new LeanFixedLengthGammaTxnFactory(config));
    }

    @Override
    protected TransactionExecutor newUpBlock() {
        GammaTxnConfiguration config = new GammaTxnConfiguration(stm);
        return new LeanGammaTransactionExecutor(new LeanFixedLengthGammaTxnFactory(config));
    }
}


