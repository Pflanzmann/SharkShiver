package com.shiver.logic;

import com.shiver.exceptions.ShiverDHKeyGenerationException;
import com.shiver.exceptions.ShiverGroupSizeException;
import com.shiver.exceptions.ShiverPeerNotVerifiedException;
import com.shiver.models.GroupCredentialMessage;
import net.sharksystem.asap.ASAPException;
import net.sharksystem.asap.ASAPPeer;

import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

/**
 * This class represents the logic of this library
 */
public interface ShiverSecurity {

    /**
     * This method is used like the onStart method of {@link net.sharksystem.SharkComponent} and
     * gets called by the owner of the object
     *
     * @param asapPeer - The own peer from ASAP
     */
    void onStart(ASAPPeer asapPeer);

    /**
     * Adds a event listener
     *
     * @param shiverEventListener
     */
    void addShiverEventListener(ShiverEventListener shiverEventListener);

    /**
     * Remove a certain event listener
     *
     * @param shiverEventListener
     */
    void removeShiverEventListener(ShiverEventListener shiverEventListener);

    /**
     * Starts the process of creating a group key.
     * Sends out a GroupCredentialMessage
     * When a key is ready to use every event listener gets notified
     *
     * @param peers - the list of peers to do the exchange with
     * @throws ShiverGroupSizeException       - Gets thrown if the group size is 0 or if the only member of the group is the owner
     * @throws ShiverPeerNotVerifiedException - Gets thrown if not all peers are verifiable
     * @throws ShiverDHKeyGenerationException - Gets thrown if something with the DH-key generation fails
     * @throws IOException
     * @throws ASAPException
     */
    void startKeyExchangeWithPeers(List<CharSequence> peers) throws ShiverGroupSizeException, ShiverPeerNotVerifiedException, ShiverDHKeyGenerationException, IOException, ASAPException;

    /**
     * When receiving a {@link GroupCredentialMessage} the end user should accept this manually to prevent unwanted group key creations.
     * This message accepts the groupCredentialMessage and continues the key creation process that someone else started
     * The groupCredentialMessage does get the user from the {@link ShiverEventListener}.
     * The groupCredentialMessage should never get manipulated before accepting it.
     *
     * @param groupCredentialMessage - original groupCredentialMessage that got received via the {@link ShiverEventListener}
     * @throws IOException
     * @throws ASAPException
     * @throws ShiverDHKeyGenerationException - Gets thrown if something with the DH-key generation fails
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeyException
     * @throws InvalidKeySpecException
     */
    void acceptGroupCredentialMessage(GroupCredentialMessage groupCredentialMessage) throws IOException, ASAPException, ShiverDHKeyGenerationException, NoSuchAlgorithmException, InvalidKeyException, InvalidKeySpecException;
}
