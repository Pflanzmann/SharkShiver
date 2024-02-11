package com.shiver.components;

import com.shiver.ShiverGroupFactory;
import com.shiver.ShiverGroupStorage;
import com.shiver.ShiverMediator;
import com.shiver.ShiverSecurity;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;

public class SharkShiverComponentFactory implements SharkComponentFactory {
    private final SharkShiverComponentImpl instance;

    public SharkShiverComponentFactory(ShiverSecurity shiverSecurity, ShiverGroupStorage shiverGroupStorage, ShiverGroupFactory groupFactory, ShiverMediator shiverMediator) {
        instance = new SharkShiverComponentImpl(shiverSecurity, shiverGroupStorage, groupFactory, shiverMediator);
    }

    @Override
    public SharkComponent getComponent() {
        return instance;
    }
}
