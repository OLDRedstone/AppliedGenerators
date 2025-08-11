package io.github.sapporo1101.appgen.network;

import com.glodblock.github.glodium.network.NetworkHandler;
import io.github.sapporo1101.appgen.AppliedGenerators;
import io.github.sapporo1101.appgen.network.packet.CAGGenericPacket;

public class AGNetworkHandler extends NetworkHandler {

    public static final AGNetworkHandler INSTANCE = new AGNetworkHandler();

    public AGNetworkHandler() {
        super(AppliedGenerators.MODID);
        registerPacket(CAGGenericPacket::new);
    }
}
