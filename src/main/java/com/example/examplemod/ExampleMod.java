package com.example.examplemod;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;

@Mod(ExampleMod.MODID)
public class ExampleMod
{
    public static final String MODID = "examplemod";
    private static final Logger LOGGER = LogUtils.getLogger();

    // Existing registers
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    // Existing blocks and items
    public static final RegistryObject<Block> EXAMPLE_BLOCK = BLOCKS.register("example_block", () -> new Block(BlockBehaviour.Properties.of().mapColor(MapColor.STONE)));
    public static final RegistryObject<Item> EXAMPLE_BLOCK_ITEM = ITEMS.register("example_block", () -> new BlockItem(EXAMPLE_BLOCK.get(), new Item.Properties()));
    public static final RegistryObject<Item> EXAMPLE_ITEM = ITEMS.register("example_item", () -> new Item(new Item.Properties().food(new FoodProperties.Builder()
            .alwaysEat().nutrition(1).saturationMod(2f).build())));
    public static final RegistryObject<CreativeModeTab> EXAMPLE_TAB = CREATIVE_MODE_TABS.register("example_tab", () -> CreativeModeTab.builder()
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> EXAMPLE_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(EXAMPLE_ITEM.get());
            }).build());

    // ===== KEY BINDING (K) =====
    private static final String KEY_CATEGORY = "key.category." + MODID;
    private static final String KEY_OPEN_MENU = "key." + MODID + ".open_menu";
    private static final KeyMapping OPEN_MENU_KEY = new KeyMapping(
            KEY_OPEN_MENU,
            KeyConflictContext.IN_GAME,
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_K,
            KEY_CATEGORY
    );

    public ExampleMod(FMLJavaModLoadingContext context)
    {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // Register key binding and input handler
        modEventBus.addListener(ExampleMod::registerKeyMapping);
        MinecraftForge.EVENT_BUS.register(ExampleMod.class);
    }

    public static void registerKeyMapping(RegisterKeyMappingsEvent event) {
        event.register(OPEN_MENU_KEY);
        LOGGER.info("Key K registered");
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (OPEN_MENU_KEY.consumeClick()) {
            Minecraft.getInstance().tell(() -> {
                Player player = Minecraft.getInstance().player;
                if (player != null) {
                    Minecraft.getInstance().setScreen(new RadialMenuScreen(player));
                    LOGGER.info("Radial menu opened");
                }
            });
        }
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        LOGGER.info("HELLO FROM COMMON SETUP");
        if (Config.logDirtBlock)
            LOGGER.info("DIRT BLOCK >> {}", ForgeRegistries.BLOCKS.getKey(Blocks.DIRT));
        LOGGER.info(Config.magicNumberIntroduction + Config.magicNumber);
        Config.items.forEach((item) -> LOGGER.info("ITEM >> {}", item.toString()));
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event)
    {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS)
            event.accept(EXAMPLE_BLOCK_ITEM);
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event)
    {
        LOGGER.info("HELLO from server starting");
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            LOGGER.info("HELLO FROM CLIENT SETUP");
            LOGGER.info("MINECRAFT NAME >> {}", Minecraft.getInstance().getUser().getName());
        }
    }

    // ===== RADIAL MENU =====
    public static class RadialMenuScreen extends Screen {
        private final Player player;
        private static final int RADIUS = 80;

        private final String[] commands = {
                "rtp", "Space", "naboo", "jeda", "kamino",
                "Mustafar", "alderaan", "hoth", "endor", "yavin4"
        };

        private final int[] colors = {
                0xFFFF5555, 0xFFFFAA55, 0xFFFFFF55, 0xFF55FF55, 0xFF55FFFF,
                0xFF5555FF, 0xFFAA55FF, 0xFFFF55FF, 0xFFFFAAAA, 0xFFAAAAFF
        };

        public RadialMenuScreen(Player player) {
            super(Component.literal("Radial Menu"));
            this.player = player;
        }

        @Override
        protected void init() {
            super.init();
            int centerX = this.width / 2;
            int centerY = this.height / 2;

            for (int i = 0; i < commands.length; i++) {
                double angle = (2 * Math.PI * i) / commands.length;
                int x = centerX + (int) (RADIUS * Math.cos(angle - Math.PI / 2));
                int y = centerY + (int) (RADIUS * Math.sin(angle - Math.PI / 2));

                String cmd = commands[i];
                int color = colors[i % colors.length];

                this.addRenderableWidget(new RadialButton(x, y, 30, Component.literal("/" + cmd), color,
                        () -> sendCommand(cmd)
                ));
            }

            this.addRenderableWidget(new RadialButton(centerX, centerY, 25, Component.literal("X"), 0xFF888888,
                    this::onClose
            ));
        }

        private void sendCommand(String command) {
            if (player != null) {
                player.connection.sendCommand(command);
                LOGGER.info("Command sent: /{}", command);
            }
            this.onClose();
        }

        @Override
        public void renderBackground(GuiGraphics graphics) {
            graphics.fillGradient(0, 0, this.width, this.height, 0xC0000000, 0xC0000000);
        }

        @Override
        public boolean isPauseScreen() {
            return false;
        }
    }

    // ===== CUSTOM RADIAL BUTTON =====
    public static class RadialButton extends AbstractWidget {
        private final int color;
        private final int hoverColor;
        private final Runnable onPress;

        public RadialButton(int x, int y, int radius, Component message, int color, Runnable onPress) {
            super(x - radius, y - radius, radius * 2, radius * 2, message);
            this.color = color;
            this.hoverColor = (color & 0x00FFFFFF) | 0xFF000000; // brighter
            this.onPress = onPress;
        }

        @Override
        public void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
            int currentColor = color;
            if (!this.active) {
                currentColor = 0xFF333333;
            } else if (this.isHovered()) {
                currentColor = hoverColor;
            }

            RenderSystem.enableBlend();
            graphics.fill(this.getX(), this.getY(), this.getX() + this.width, this.getY() + this.height, currentColor);
            RenderSystem.disableBlend();

            graphics.drawCenteredString(Minecraft.getInstance().font, this.getMessage(),
                    this.getX() + this.width / 2, this.getY() + this.height / 2 - 4, 0xFFFFFFFF);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            onPress.run();
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput narration) {
            this.defaultButtonNarrationText(narration);
        }
    }
}