package com.shiver.components;

import com.shiver.ShiverSecurity;
import com.shiver.GroupStorage;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;

public class SharkShiverComponentFactory implements SharkComponentFactory {
    private GroupStorage groupStorage = null;
    private ShiverSecurity shiverSecurity = null;
    private SharkShiverComponentImpl instance = null;

    public SharkShiverComponentFactory(GroupStorage groupStorage, ShiverSecurity shiverSecurity) {
        this.groupStorage = groupStorage;
        this.shiverSecurity = shiverSecurity;
    }

    @Override
    public SharkComponent getComponent() {
        if (instance == null) {
            return new SharkShiverComponentImpl(groupStorage, shiverSecurity);
        } else {
            return instance;
        }
    }
}
