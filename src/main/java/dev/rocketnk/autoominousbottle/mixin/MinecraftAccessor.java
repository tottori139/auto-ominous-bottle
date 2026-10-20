package dev.rocketnk.autoominousbottle.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	/** Пока > 0, зажатая ПКМ не начинает новое использование предмета. */
	@Accessor("rightClickDelay")
	void aob$setRightClickDelay(int delay);
}
