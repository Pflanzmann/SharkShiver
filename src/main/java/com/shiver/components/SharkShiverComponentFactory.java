package com.shiver.components;

import com.shiver.ShiverGroupStorage;
import com.shiver.ShiverMediator;
import com.shiver.ShiverSecurity;
import com.shiver.ShiverGroupFactory;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;

public class SharkShiverComponentFactory implements SharkComponentFactory {
    private ShiverGroupStorage shiverGroupStorage = null;
    private ShiverSecurity shiverSecurity = null;
    private ShiverGroupFactory groupFactory = null;
    private ShiverMediator shiverMediator = null;
    private SharkShiverComponentImpl instance = null;

    public SharkShiverComponentFactory(ShiverSecurity shiverSecurity, ShiverGroupStorage shiverGroupStorage, ShiverGroupFactory groupFactory, ShiverMediator shiverMediator) {
        this.shiverSecurity = shiverSecurity;
        this.shiverGroupStorage = shiverGroupStorage;
        this.groupFactory = groupFactory;
        this.shiverMediator = shiverMediator;
    }

    @Override
    public SharkComponent getComponent() {
        if (instance == null) {
            return new SharkShiverComponentImpl(shiverSecurity, shiverGroupStorage, groupFactory, shiverMediator);
        } else {
            return instance;
        }
    }
}
