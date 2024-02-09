package com.shiver.components;

import com.shiver.GroupStorage;
import com.shiver.ShiverSecurity;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;

public class SharkShiverComponentFactory implements SharkComponentFactory {
    private GroupStorage groupStorage = null;
    private ShiverSecurity shiverSecurity = null;
    private SharkShiverComponentImpl instance = null;

    public SharkShiverComponentFactory(ShiverSecurity shiverSecurity, GroupStorage groupStorage) {
        this.shiverSecurity = shiverSecurity;
        this.groupStorage = groupStorage;
    }

    @Override
    public SharkComponent getComponent() {
        if (instance == null) {
            return new SharkShiverComponentImpl(shiverSecurity, groupStorage);
        } else {
            return instance;
        }
    }
}
