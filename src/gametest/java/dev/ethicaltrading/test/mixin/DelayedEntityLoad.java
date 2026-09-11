package dev.ethicaltrading.test.mixin;
import dev.ethicaltrading.test.EntityLoadGate;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.entity.EntityPersistentStorage;
import net.minecraft.world.level.entity.PersistentEntitySectionManager;
import java.util.concurrent.CompletableFuture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
@Mixin(PersistentEntitySectionManager.class)
public abstract class DelayedEntityLoad {
    @Redirect(method="requestChunkLoad", at=@At(value="INVOKE", target="Lnet/minecraft/world/level/entity/EntityPersistentStorage;loadEntities(Lnet/minecraft/world/level/ChunkPos;)Ljava/util/concurrent/CompletableFuture;"))
    private CompletableFuture<?> ethicalTest$delay(EntityPersistentStorage<?> storage, ChunkPos pos) {
        var original=storage.loadEntities(pos);
        if (!EntityLoadGate.armed || pos.x()!=64 || pos.z()!=64) return original;
        EntityLoadGate.intercepted=true;
        return original.thenCombine(EntityLoadGate.ready, (entities,ignored)->entities);
    }
}
