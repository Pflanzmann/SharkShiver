package com.shiver.components;

import com.shiver.logic.ShiverSecurity;
import com.shiver.storager.ShiverDHKeyPairStorage;
import com.shiver.storager.ShiverKeyStorage;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;

public class SharkShiverComponentFactory implements SharkComponentFactory {
    private final SharkShiverComponentImpl instance;

    public SharkShiverComponentFactory(ShiverSecurity shiverSecurity, ShiverKeyStorage shiverKeyStorage, ShiverDHKeyPairStorage shiverDHKeyPairStorage) {
        instance = new SharkShiverComponentImpl(shiverSecurity, shiverKeyStorage, shiverDHKeyPairStorage);
    }

    @Override
    public SharkComponent getComponent() {
        return instance;
    }
}
