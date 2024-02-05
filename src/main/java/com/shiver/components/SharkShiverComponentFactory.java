package com.shiver.components;

import com.shiver.SingleGroupStorage;
import net.sharksystem.SharkComponent;
import net.sharksystem.SharkComponentFactory;
import net.sharksystem.pki.SharkPKIComponent;

public class SharkShiverComponentFactory implements SharkComponentFactory {

    private SharkPKIComponent sharkPKIComponent = null;
    private SingleGroupStorage singleGroupStorage = null;
    private SharkShiverComponentImpl instance = null;

    public SharkShiverComponentFactory(SharkPKIComponent sharkPKIComponent, SingleGroupStorage singleGroupStorage) {
        this.sharkPKIComponent = sharkPKIComponent;
        this.singleGroupStorage = singleGroupStorage;
    }

    @Override
    public SharkComponent getComponent() {
        if (instance == null) {
            return new SharkShiverComponentImpl(sharkPKIComponent, singleGroupStorage);
        } else {
            return instance;
        }
    }
}
