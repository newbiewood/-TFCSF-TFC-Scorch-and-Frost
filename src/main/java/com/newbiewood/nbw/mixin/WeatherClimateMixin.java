package com.newbiewood.nbw.mixin;

import com.newbiewood.nbw.weather.WeatherEventManager;
import net.dries007.tfc.util.climate.OverworldClimateModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.LevelReader;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = OverworldClimateModel.class, remap = false)
public class WeatherClimateMixin
{
    @Inject(method = "getInstantTemperature", at = @At("RETURN"), cancellable = true, remap = false)
    private void addWeatherEventOffset(LevelReader level, BlockPos pos, long calendarTicks, int daysInMonth, CallbackInfoReturnable<Float> cir)
    {
        float offset = WeatherEventManager.getGlobalTemperatureOffset(level, pos);
        if (offset != 0f)
        {
            cir.setReturnValue(cir.getReturnValue() + offset);
        }
    }
}
