package com.shiver.logic;

import com.shiver.exceptions.ShiverGroupSizeException;
import com.shiver.exceptions.ShiverPeerNotVerifiedException;
import com.shiver.exceptions.ShiverDHKeyGenerationException;
import com.shiver.models.GroupCredentialMessage;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

public interface ShiverSecurity {

    void onStart(ASAPPeer asapPeer);

    void addShiverMessageReceiver(ShiverCredentialReceiver shiverMessageReceiver);

    void removeShiverMessageReceiver(ShiverCredentialReceiver shiverMessageReceiver);

    boolean verifyPeer(CharSequence userId) throws ShiverPeerNotVerifiedException;

    void startKeyExchangeWithPeers(List<CharSequence> charSequences) throws ShiverGroupSizeException, ShiverPeerNotVerifiedException, ShiverDHKeyGenerationException, IOException, ASAPException;

    void acceptGroupCredentialMessage(GroupCredentialMessage groupCredentialMessage) throws IOException, ASAPException, ShiverDHKeyGenerationException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException;
}
