package com.shiver.components;

import com.shiver.logic.SharkPkiSecurity;
import com.shiver.logic.ShiverSecurity;
import com.shiver.storager.ShiverDHKeyPairStorage;
import com.shiver.storager.ShiverDHKeyPairStorageInMemo;
import com.shiver.storager.ShiverKeyStorage;
import com.shiver.storager.ShiverKeyStoreInMemo;
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
    public ShiverComponentFactory(ShiverSecurity shiverSecurity, ShiverKeyStorage shiverKeyStorage, ShiverDHKeyPairStorage shiverDHKeyPairStorage) {
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
        ShiverSecurity shiverSecurity = new SharkPkiSecurity(sharkPKIComponent, shiverDHKeyPairStorage, shiverKeyStorage);

        instance = new ShiverComponentImpl(shiverSecurity, shiverKeyStorage, shiverDHKeyPairStorage);
    }

    @Override
    public SharkComponent getComponent() {
        return instance;
    }
}
