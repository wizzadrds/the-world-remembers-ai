package com.wizzadrds.theworldremembers;

import com.wizzadrds.theworldremembers.chronicle.ChronicleScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
public final class ChronicleClientGameTest implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        try (TestSingleplayerContext ignored = context.worldBuilder().create()) {
            context.runOnClient(client -> client.gui.setScreen(new ChronicleScreen(List.of(
                "TIMELINE", "test event", "PEOPLE", "Villager", "RELATIONSHIPS",
                "trust=50", "FAMILIES", "PARENT", "VILLAGES", "population=2"
            ))));
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof ChronicleScreen)) {
                    throw new AssertionError("Chronicle screen did not open");
                }
            });
            context.takeScreenshot("chronicle-screen");
            context.runOnClient(client -> client.gui.setScreen(null));
        }
    }
}
