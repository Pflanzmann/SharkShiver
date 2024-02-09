package com.shiver.components;

import com.shiver.ShiverGroupStorage;
import com.shiver.ShiverSecurity;
import com.shiver.ShiverGroupFactory;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;

public class SharkShiverComponentFactory implements SharkComponentFactory {
    private ShiverGroupStorage shiverGroupStorage = null;
    private ShiverSecurity shiverSecurity = null;
    private ShiverGroupFactory groupFactory = null;
    private SharkShiverComponentImpl instance = null;

    public SharkShiverComponentFactory(ShiverSecurity shiverSecurity, ShiverGroupStorage shiverGroupStorage, ShiverGroupFactory groupFactory) {
        this.shiverSecurity = shiverSecurity;
        this.shiverGroupStorage = shiverGroupStorage;
        this.groupFactory = groupFactory;
    }

    @Override
    public SharkComponent getComponent() {
        if (instance == null) {
            return new SharkShiverComponentImpl(shiverSecurity, shiverGroupStorage, groupFactory);
        } else {
            return instance;
        }
    }
}
