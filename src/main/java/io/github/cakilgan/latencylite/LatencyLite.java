package io.github.cakilgan.latencylite;

import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.MinecraftForge;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.config.ModConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.Objects;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(LatencyLite.MODID)
public class LatencyLite {
    public static final String MODID = "latencylite";
    public static final String MOD_NAME= "LatencyLite";
    public static final String VERSION = "0.21";
    private static final Logger LOGGER = LogUtils.getLogger();

    public LatencyLite(FMLJavaModLoadingContext context){
        MinecraftForge.EVENT_BUS.register(new PingOverlay());
        MinecraftForge.EVENT_BUS.register(new PingCalc());
        context.registerConfig(ModConfig.Type.CLIENT,PingConfig.SPEC);
    }
    public static class PingCalc{
        private static final int AVR_PING_THRESHOLD = 3;
        public static int CURRENT_PING;
        public static int AVERAGE_PING;
        private static long LAST_REFRESH;
        int countAvr=0,valAvr=0;
        @SubscribeEvent
        public void calc(TickEvent.ClientTickEvent event){
            if (PingConfig.DISABLE_MOD.get()) return;
            if (event.side.isClient()){
            Minecraft mc = Minecraft.getInstance();
            if (mc.player==null||mc.getConnection()==null){
                return;
            }
            PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
            if (info!=null){
                int newPing = Objects.requireNonNull(info).getLatency();
                if (newPing != CURRENT_PING) {
                    CURRENT_PING = newPing;
                    valAvr+=CURRENT_PING;
                    countAvr++;
                    LAST_REFRESH = System.currentTimeMillis();
                }
                if (countAvr==AVR_PING_THRESHOLD+1){
                    AVERAGE_PING = valAvr/AVR_PING_THRESHOLD;
                    countAvr=0;
                    valAvr=0;
                }
            }
            }
        }
        public static String getLastRefreshTime() {
            long now = System.currentTimeMillis();
            long diff = now - LAST_REFRESH;
            if (diff>20000000){
                return formatTime(0);
            }
            return formatTime(diff);
        }

        private static String formatTime(long diff) {
            if (diff < 1000) {
                return diff + " ms ago";
            }
            long seconds = diff / 1000;
            if (seconds < 60) {
                return seconds + " seconds ago";
            }
            long minutes = seconds / 60;
            return minutes + " minutes ago";
        }
        public static int getPingColor(int ping) {
            if (ping < 100) return 0x00FF00;
            else if (ping < 200) return 0xFFFF00;
            else if (ping < 300) return 0xFFA500;
            else return 0xFF0000;
        }
    }
    public static class PingOverlay{
        public static final ResourceLocation PING_ICON_EMPTY = ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/ping_icon_empty.png");
        public static final ResourceLocation PING_ICON_LOADING_1 = ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/ping_icon_loading_1.png");
        public static final ResourceLocation PING_ICON_LOADING_2 = ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/ping_icon_loading_2.png");
        public static final ResourceLocation PING_ICON_LOADING_3 = ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/ping_icon_loading_3.png");
        public static final ResourceLocation PING_ICON_LOADING_4 = ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/ping_icon_loading_4.png");
        public static final ResourceLocation PING_ICON_LOW = ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/ping_icon_low.png");
        public static final ResourceLocation PING_ICON_MID = ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/ping_icon_mid.png");
        public static final ResourceLocation PING_ICON_HIGH = ResourceLocation.fromNamespaceAndPath(MODID,"textures/gui/ping_icon_high.png");

        private static int animationFrame = 0;
        private static long lastTime = 0;
        @SubscribeEvent
        public void overlayRender(RenderGuiOverlayEvent event) {
            if (PingConfig.DISABLE_MOD.get()) return;
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.getConnection() == null) return;

            if (event.getOverlay().id() == VanillaGuiOverlay.DEBUG_TEXT.id() || mc.options.renderDebug){
                return;
            }
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastTime > 100) {
                animationFrame = (animationFrame + 1) % 4;
                lastTime = currentTime;
            }
                PlayerInfo info = mc.getConnection().getPlayerInfo(mc.player.getUUID());
                if (info != null) {
                    Font font = mc.font;
                    PoseStack pose = event.getGuiGraphics().pose();
                    pose.pushPose();
                    if (PingConfig.ENABLE_GLOBAL_SCALING.get()){
                    pose.scale(PingConfig.SCALE_MODIFIER_X.get().floatValue(),PingConfig.SCALE_MODIFIER_Y.get().floatValue(),1f);
                    }
                    if (PingConfig.SHOW_PING.get()){
                        String text1 = "Ping:";
                        if (!PingConfig.AVOID_LOCAL_SCALING.get()){
                         pose.scale(PingConfig.PING_SCALE_MODIFIER_X.get().floatValue(),PingConfig.PING_SCALE_MODIFIER_Y.get().floatValue(),1f);
                        }
                        event.getGuiGraphics().drawString(font,text1,PingConfig.PING_X_POS.get(),PingConfig.PING_Y_POS.get(),PingConfig.PING_COLOR.get());
                        event.getGuiGraphics().drawString(font,""+PingCalc.CURRENT_PING,PingConfig.PING_X_POS.get()+25,PingConfig.PING_Y_POS.get(),PingCalc.getPingColor(PingCalc.CURRENT_PING));
                        if (!PingConfig.AVOID_LOCAL_SCALING.get()){
                            pose.popPose();
                            pose.pushPose();
                        }
                    }
                    if (PingConfig.SHOW_AVERAGE_PING.get()){
                        String text2 = "AVR Ping: "+PingCalc.AVERAGE_PING;
                        if (!PingConfig.AVOID_LOCAL_SCALING.get()) {
                            pose.scale(PingConfig.AVERAGE_PING_SCALE_MODIFIER_X.get().floatValue(), PingConfig.AVERAGE_PING_SCALE_MODIFIER_Y.get().floatValue(), 1f);
                        }
                        event.getGuiGraphics().drawString(font,text2,PingConfig.AVERAGE_PING_X_POS.get(),PingConfig.AVERAGE_PING_Y_POS.get(),PingConfig.AVERAGE_PING_COLOR.get());
                        if (!PingConfig.AVOID_LOCAL_SCALING.get()){
                            pose.popPose();
                            pose.pushPose();
                        }
                    }
                    if (PingConfig.SHOW_LAST_REFRESH.get()){
                        String text3 = "Last Refresh: "+PingCalc.getLastRefreshTime();
                        if (!PingConfig.AVOID_LOCAL_SCALING.get()) {
                            pose.scale(PingConfig.LAST_REFRESH_SCALE_MODIFIER_X.get().floatValue(), PingConfig.LAST_REFRESH_SCALE_MODIFIER_Y.get().floatValue(), 1f);
                        }
                        event.getGuiGraphics().drawString(font,text3,PingConfig.LAST_REFRESH_X_POS.get(), PingConfig.LAST_REFRESH_Y_POS.get(),PingConfig.LAST_REFRESH_COLOR.get());
                        if (!PingConfig.AVOID_LOCAL_SCALING.get()){
                            pose.popPose();
                            pose.pushPose();
                        }
                    }
                    pose.popPose();
                    pose.pushPose();
                    if (PingCalc.CURRENT_PING==0){
                        pose.scale(PingConfig.ICON_SCALE_MODIFIER_X.get().floatValue(),PingConfig.ICON_SCALE_MODIFIER_Y.get().floatValue(),1f);
                        ResourceLocation currentIcon = switch (animationFrame) {
                            case 0 -> PING_ICON_LOADING_1;
                            case 1 -> PING_ICON_LOADING_2;
                            case 2 -> PING_ICON_LOADING_3;
                            case 3 -> PING_ICON_LOADING_4;
                            default -> PING_ICON_LOADING_1;
                        };
                        event.getGuiGraphics().blit(currentIcon,PingConfig.ICON_X_POS.get(), PingConfig.ICON_Y_POS.get(), 0, 0, 16, 16, 16, 16);
                    }else{
                        pose.scale(PingConfig.ICON_SCALE_MODIFIER_X.get().floatValue(),PingConfig.ICON_SCALE_MODIFIER_Y.get().floatValue(),1f);
                        ResourceLocation currentIcon = PING_ICON_EMPTY;
                        if (PingCalc.CURRENT_PING<100){
                            currentIcon = PING_ICON_HIGH;
                        } else if (PingCalc.CURRENT_PING<200) {
                            currentIcon = PING_ICON_MID;
                        } else if (PingCalc.CURRENT_PING<300) {
                            currentIcon = PING_ICON_LOW;
                        }
                        event.getGuiGraphics().blit(currentIcon,PingConfig.ICON_X_POS.get(), PingConfig.ICON_Y_POS.get(), 0, 0, 16, 16, 16, 16);
                    }
                    pose.popPose();
                }

        }
    }
    public static class PingConfig {
        public static final ForgeConfigSpec SPEC;

        public static final ForgeConfigSpec.BooleanValue DISABLE_MOD;
        public static final ForgeConfigSpec.BooleanValue ENABLE_GLOBAL_SCALING;
        public static final ForgeConfigSpec.BooleanValue AVOID_LOCAL_SCALING;
        public static final ForgeConfigSpec.DoubleValue SCALE_MODIFIER_X;
        public static final ForgeConfigSpec.DoubleValue SCALE_MODIFIER_Y;

        public static final ForgeConfigSpec.BooleanValue SHOW_ICON;
        public static final ForgeConfigSpec.IntValue ICON_X_POS;
        public static final ForgeConfigSpec.IntValue ICON_Y_POS;
        public static final ForgeConfigSpec.DoubleValue ICON_SCALE_MODIFIER_X;
        public static final ForgeConfigSpec.DoubleValue ICON_SCALE_MODIFIER_Y;


        public static final ForgeConfigSpec.BooleanValue SHOW_PING;
        public static final ForgeConfigSpec.IntValue PING_COLOR;
        public static final ForgeConfigSpec.IntValue PING_X_POS;
        public static final ForgeConfigSpec.IntValue PING_Y_POS;
        public static final ForgeConfigSpec.DoubleValue PING_SCALE_MODIFIER_X;
        public static final ForgeConfigSpec.DoubleValue PING_SCALE_MODIFIER_Y;


        public static final ForgeConfigSpec.BooleanValue SHOW_AVERAGE_PING;
        public static final ForgeConfigSpec.IntValue AVERAGE_PING_COLOR;
        public static final ForgeConfigSpec.IntValue AVERAGE_PING_X_POS;
        public static final ForgeConfigSpec.IntValue AVERAGE_PING_Y_POS;
        public static final ForgeConfigSpec.DoubleValue AVERAGE_PING_SCALE_MODIFIER_X;
        public static final ForgeConfigSpec.DoubleValue AVERAGE_PING_SCALE_MODIFIER_Y;


        public static final ForgeConfigSpec.BooleanValue SHOW_LAST_REFRESH;
        public static final ForgeConfigSpec.IntValue LAST_REFRESH_COLOR;
        public static final ForgeConfigSpec.IntValue LAST_REFRESH_X_POS;
        public static final ForgeConfigSpec.IntValue LAST_REFRESH_Y_POS;
        public static final ForgeConfigSpec.DoubleValue LAST_REFRESH_SCALE_MODIFIER_X;
        public static final ForgeConfigSpec.DoubleValue LAST_REFRESH_SCALE_MODIFIER_Y;



        static {
            ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
            builder.push("Core Config");
            DISABLE_MOD = builder
                    .comment("if it set true all functionality of mod will be disabled")
                    .define("disableMod",false);
            ENABLE_GLOBAL_SCALING = builder
                    .comment("if this config enabled, it will scale all the components of this mod")
                    .define("enable_global_scaling",false);
            AVOID_LOCAL_SCALING = builder
                    .comment("if this config enabled, it will avoid other local scale configs and use only Core scaling")
                    .define("avoid_local_scaling",false);
            SCALE_MODIFIER_X = builder
                    .comment("x scale of all text rendering.")
                    .defineInRange("x_scale",1.0,0,1);
            SCALE_MODIFIER_Y = builder
                    .comment("y scale of all text rendering.")
                    .defineInRange("y_scale",1.0,0,1);
            builder.pop();

            builder.push("Signal Icon Config");
            SHOW_ICON = builder
                    .comment("Show signal icon on screen")
                    .define("show_ping", true);
            ICON_X_POS = builder
                    .comment("X position of 'signal_icon'")
                    .defineInRange("signal_icon_x_pos", 50, 0, 1920);
            ICON_Y_POS = builder
                    .comment("Y position of 'signal_icon'")
                    .defineInRange("signal_icon_y_pos", 15, 0, 1080);
            ICON_SCALE_MODIFIER_X = builder
                    .comment("X scale of 'signal_icon'")
                    .defineInRange("signal_icon_x_scale",0.5,0,1);
            ICON_SCALE_MODIFIER_Y = builder
                    .comment("Y scale of 'signal_icon'")
                    .defineInRange("signal_icon_y_scale",0.5,0,1);
            builder.pop();

            builder.push("Ping Config");
            SHOW_PING = builder
                    .comment("Show ping as text on screen")
                    .define("show_ping", true);
            PING_COLOR = builder
                    .comment("Color of 'ping' text")
                    .defineInRange("ping_color", 16777215, 0, 0XFFFFFF);
            PING_X_POS = builder
                    .comment("X position of 'ping' text")
                    .defineInRange("ping_x_pos", 10, 0, 1920);
            PING_Y_POS = builder
                    .comment("Y position of 'ping' text")
                    .defineInRange("ping_y_pos", 20, 0, 1080);
            PING_SCALE_MODIFIER_X = builder
                    .comment("X scale of 'ping' text")
                    .defineInRange("ping_x_scale",0.5,0,1);
            PING_SCALE_MODIFIER_Y = builder
                    .comment("Y scale of 'ping' text")
                    .defineInRange("ping_y_scale",0.5,0,1);
            builder.pop();

            builder.push("Average Ping Config");
            SHOW_AVERAGE_PING = builder
                    .comment("Show average ping as text on screen")
                    .define("show_avr_ping", true);
            AVERAGE_PING_COLOR = builder
                    .comment("Color of 'average ping' text")
                    .defineInRange("average_ping_color", 16776960, 0, 0XFFFFFF);
            AVERAGE_PING_X_POS = builder
                    .comment("X position of 'average ping' text")
                    .defineInRange("average_ping_x_pos", 10, 0, 1920);
            AVERAGE_PING_Y_POS = builder
                    .comment("Y position of 'average ping' text")
                    .defineInRange("average_ping_y_pos", 40, 0, 1080);
            AVERAGE_PING_SCALE_MODIFIER_X = builder
                    .comment("X scale of 'average_ping' text")
                    .defineInRange("average_ping_x_scale",0.5,0,1);
            AVERAGE_PING_SCALE_MODIFIER_Y = builder
                    .comment("Y scale of 'average_ping' text")
                    .defineInRange("average_ping_y_scale",0.5,0,1);
            builder.pop();

            builder.push("Last Refresh Config");
            SHOW_LAST_REFRESH = builder
                    .comment("Show last refresh of ping as text on screen")
                    .define("show_last_refresh", true);
            LAST_REFRESH_COLOR = builder
                    .comment("Color of 'last_refresh' text")
                    .defineInRange("last_refresh_color", 12632256, 0, 0XFFFFFF);
            LAST_REFRESH_X_POS = builder
                    .comment("X position of 'last_refresh' text")
                    .defineInRange("last_refresh_x_pos", 10, 0, 1920);
            LAST_REFRESH_Y_POS = builder
                    .comment("Y position of 'last_refresh' text")
                    .defineInRange("last_refresh_y_pos", 60, 0, 1080);
            LAST_REFRESH_SCALE_MODIFIER_X = builder
                    .comment("X scale of 'last_refresh' text")
                    .defineInRange("last_refresh_x_scale",0.5,0,1);
            LAST_REFRESH_SCALE_MODIFIER_Y = builder
                    .comment("Y scale of 'last_refresh' text")
                    .defineInRange("last_refresh_y_scale",0.5,0,1);
            builder.pop();

            SPEC = builder.build();
        }
    }
}
