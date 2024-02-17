package com.shiver.components;

import com.shiver.logic.ShiverPkiSecurity;
import com.shiver.logic.ShiverSecurity;
import com.shiver.storage.ShiverDHKeyPairStorage;
import com.shiver.storage.ShiverDHKeyPairStorageInMemo;
import com.shiver.storage.ShiverKeyStorage;
import com.shiver.storage.ShiverKeyStoreInMemo;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;
import net.sharksystem.pki.SharkPKIComponent;

public class ShiverComponentFactory implements SharkComponentFactory {
    private final ShiverComponentImpl instance;

    /**
     * A constructor for more controllable dependency resolution
     *
     * @param shiverSecurity
     * @param shiverKeyStorage
     * @param shiverDHKeyPairStorage
     */
    public ShiverComponentFactory(ShiverSecurity shiverSecurity, ShiverDHKeyPairStorage shiverDHKeyPairStorage, ShiverKeyStorage shiverKeyStorage) {
        instance = new ShiverComponentImpl(shiverSecurity, shiverKeyStorage, shiverDHKeyPairStorage);
    }

    /**
     * The constructor that should be used by default
     *
     * @param sharkPKIComponent
     */
    public ShiverComponentFactory(SharkPKIComponent sharkPKIComponent) {
        ShiverKeyStorage shiverKeyStorage = new ShiverKeyStoreInMemo();
        ShiverDHKeyPairStorage shiverDHKeyPairStorage = new ShiverDHKeyPairStorageInMemo();
        ShiverSecurity shiverSecurity = new ShiverPkiSecurity(sharkPKIComponent, shiverDHKeyPairStorage, shiverKeyStorage);

        instance = new ShiverComponentImpl(shiverSecurity, shiverKeyStorage, shiverDHKeyPairStorage);
    }

    @Override
    public SharkComponent getComponent() {
        return instance;
    }
}
