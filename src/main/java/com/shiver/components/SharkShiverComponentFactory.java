package com.shiver.components;

import com.shiver.ShiverGroupStorage;
import com.shiver.ShiverSecurity;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;

public class SharkShiverComponentFactory implements SharkComponentFactory {
    private ShiverGroupStorage shiverGroupStorage = null;
    private ShiverSecurity shiverSecurity = null;
    private SharkShiverComponentImpl instance = null;

    public SharkShiverComponentFactory(ShiverSecurity shiverSecurity, ShiverGroupStorage shiverGroupStorage) {
        this.shiverSecurity = shiverSecurity;
        this.shiverGroupStorage = shiverGroupStorage;
    }

    @Override
    public SharkComponent getComponent() {
        if (instance == null) {
            return new SharkShiverComponentImpl(shiverSecurity, shiverGroupStorage);
        } else {
            return instance;
        }
    }
}
