package net.cetendo.catutility.mixin;

import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler implements ClientConnectionInvoker {

    @Shadow
    public abstract ClientConnection getConnection();

    public void sendImmediately(Packet<?> packet) {
        this.getConnection().send(packet);
    }

    @Inject(method = "sendPacket(Lnet/minecraft/network/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void onSendPacket(Packet<?> packet, CallbackInfo ci) {
        this.sendImmediately(packet); // Invoke the packet send immediately
        ci.cancel(); // Cancel the original sendPacket method
    }
}
