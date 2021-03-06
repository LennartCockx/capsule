package capsule.command;

import capsule.Config;
import capsule.Helpers;
import capsule.StructureSaver;
import capsule.items.CapsuleItem;
import capsule.loot.CapsuleLootEntry;
import capsule.loot.CapsuleLootTableHook;
import capsule.structure.CapsuleTemplate;
import capsule.structure.CapsuleTemplateManager;
import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.command.*;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.event.ClickEvent;
import net.minecraft.util.text.event.ClickEvent.Action;
import net.minecraft.util.text.event.HoverEvent;
import net.minecraft.world.WorldServer;
import net.minecraft.world.gen.structure.template.Template;
import net.minecraft.world.gen.structure.template.TemplateManager;
import net.minecraft.world.storage.loot.LootContext;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * @author Lythom
 */
public class CapsuleCommand extends CommandBase {

    public static String[] COMMAND_LIST = new String[]{
            "giveEmpty", "exportHeldItem", "exportSeenBlock", "fromExistingReward", "fromHeldCapsule", "fromStructure", "giveRandomLoot", "reloadLootList", "setAuthor", "setBaseColor", "setMaterialColor"
    };

    /*
     * (non-Javadoc)
     *
     * @see net.minecraft.command.ICommand#getName()
     */
    @Override
    public String getName() {
        return "capsule";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 2;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * net.minecraft.command.ICommand#getUsage(net.minecraft.command.
     * ICommandSender)
     */
    @Override
    public String getUsage(ICommandSender sender) {

        TextComponentString msg = new TextComponentString(
                "see Capsule commands usages at " + TextFormatting.UNDERLINE + "https://bitbucket.org/Lythom/mccapsule/wiki/Commands");
        msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_URL, "https://bitbucket.org/Lythom/mccapsule/wiki/Commands"));
        sender.sendMessage(msg);
        return "/capsule <" + Joiner.on("|").join(COMMAND_LIST) + ">";
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, BlockPos pos) {

        EntityPlayerMP player = null;
        switch (args.length) {
            case 1:
                return getListOfStringsMatchingLastWord(args, COMMAND_LIST);
            case 2:
                switch (args[0]) {
                    case "giveRandomLoot":
                        return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());

                    case "setBaseColor":
                        return getListOfStringsMatchingLastWord(args, CapsuleLootEntry.COLOR_PALETTE);

                    case "setMaterialColor":
                        return getListOfStringsMatchingLastWord(args, CapsuleLootEntry.COLOR_PALETTE);

                    case "fromStructure":
                        try {
                            player = getCommandSenderAsPlayer(sender);
                        } catch (PlayerNotFoundException ignored) {
                        }
                        if (player != null) {
                            String[] structuresList = (new File(player.getServerWorld().getSaveHandler().getWorldDirectory(), "structures")).list();
                            if (structuresList == null) return new ArrayList<>();
                            return getListOfStringsMatchingLastWord(args, structuresList);
                        }

                    case "fromExistingReward":
                        try {
                            player = getCommandSenderAsPlayer(sender);
                        } catch (PlayerNotFoundException ignored) {
                        }
                        if (player != null) {
                            String[] rewardsList = (new File(Config.rewardTemplatesPath)).list();
                            if (rewardsList == null) return new ArrayList<>();
                            return getListOfStringsMatchingLastWord(args, rewardsList);
                        }
                }
        }
        return Collections.emptyList();
    }

    /*
     * (non-Javadoc)
     *
     * @see net.minecraft.command.ICommand#execute(net.minecraft.command.
     * ICommandSender, java.lang.String[])
     */
    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {

        if (args.length < 1 || "help".equalsIgnoreCase(args[0])) {
            throw new WrongUsageException(getUsage(sender));
        }

        EntityPlayerMP player = null;
        if (sender instanceof EntityPlayerMP) {
            player = (EntityPlayerMP) sender;
        }


        if ("giveEmpty".equalsIgnoreCase(args[0])) {
            if (player != null) {
                ItemStack capsule = CapsuleItem.createEmptyCapsule(
                        0xFFFFFF,
                        0xFFFFFF,
                        (args.length >= 2 ? Integer.decode(args[1]) : 3),
                        (args.length >= 3 && Boolean.valueOf(args[2])),
                        null,
                        null
                );
                giveCapsule(capsule, player);
            }
        }

        // export give command exportHeldItem
        else if ("exportHeldItem".equalsIgnoreCase(args[0])) {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            if (player != null) {
                ItemStack heldItem = player.getHeldItemMainhand();
                if (!heldItem.isEmpty()) {

                    String command = "/give @p " + heldItem.getItem().getRegistryName().toString() + " 1 " + heldItem.getItemDamage();
                    if (heldItem.hasTagCompound()) {
                        //noinspection ConstantConditions
                        command += " " + heldItem.getTagCompound().toString();
                    }
                    TextComponentString msg = new TextComponentString(command);
                    msg.getStyle()
                            .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Copy/Paste from client log (click to open)")));
                    msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_FILE, "logs/latest.log"));

                    player.sendMessage(msg);

                }
            }
        } else if ("exportSeenBlock".equalsIgnoreCase(args[0])) {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            if (player != null) {
                if (!server.isDedicatedServer()) {
                    RayTraceResult rtc = Helpers.clientRayTracePreview(player, Minecraft.getMinecraft().getRenderPartialTicks());

                    if (rtc != null && rtc.typeOfHit == RayTraceResult.Type.BLOCK) {

                        BlockPos position = rtc.getBlockPos();
                        IBlockState state = player.getServerWorld().getBlockState(position);
                        TileEntity tileentity = player.getServerWorld().getTileEntity(position);

                        String command = "/give @p " + state.getBlock().getRegistryName().toString() + " 1 " + state.getBlock().getMetaFromState(state);
                        if (tileentity != null) {
                            command += " {BlockEntityTag:" + tileentity.serializeNBT().toString() + "}";
                        }
                        TextComponentString msg = new TextComponentString(command);
                        msg.getStyle()
                                .setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new TextComponentString("Copy/Paste from client log (click to open)")));
                        msg.getStyle().setClickEvent(new ClickEvent(Action.OPEN_FILE, "logs/latest.log"));

                        player.sendMessage(msg);

                    }
                } else {
                    player.sendMessage(new TextComponentString("This command only works on an integrated server, not on an dedicated one"));
                }
            }
        }

        // set author
        else if ("setAuthor".equalsIgnoreCase(args[0])) {
            if (args.length != 1 && args.length != 2) {
                throw new WrongUsageException(getUsage(sender));
            }

            if (player != null) {
                ItemStack heldItem = player.getHeldItemMainhand();
                if (!heldItem.isEmpty() && heldItem.getItem() instanceof CapsuleItem && heldItem.hasTagCompound()) {

                    if (args.length == 2) {
                        // set a new author
                        String author = args[1];
                        //noinspection ConstantConditions
                        heldItem.getTagCompound().setString("author", args[1]);
                        Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, player.getServerWorld());
                        CapsuleTemplate template = templatepair.getRight();
                        CapsuleTemplateManager templatemanager = templatepair.getLeft();
                        if (template != null && templatemanager != null) {
                            template.setAuthor(author);
                            templatemanager.writeTemplate(server, new ResourceLocation(CapsuleItem.getStructureName(heldItem)));
                        }

                    } else {
                        // called with one parameter = remove author information
                        //noinspection ConstantConditions
                        heldItem.getTagCompound().removeTag("author");
                        Pair<CapsuleTemplateManager, CapsuleTemplate> templatepair = StructureSaver.getTemplate(heldItem, player.getServerWorld());
                        CapsuleTemplate template = templatepair.getRight();
                        CapsuleTemplateManager templatemanager = templatepair.getLeft();
                        if (template != null && templatemanager != null) {
                            template.setAuthor("?");
                            templatemanager.writeTemplate(server, new ResourceLocation(CapsuleItem.getStructureName(heldItem)));
                        }
                    }

                }
            }

        }

        // set color
        else if ("setBaseColor".equalsIgnoreCase(args[0])) {
            if (args.length != 2) {
                throw new WrongUsageException(getUsage(sender));
            }

            int color = 0;
            try {
                color = Integer.decode(args[1]);
            } catch (NumberFormatException e) {
                throw new WrongUsageException("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456");
            }

            if (player != null) {
                ItemStack heldItem = player.getHeldItemMainhand();
                if (heldItem.getItem() instanceof CapsuleItem) {
                    CapsuleItem.setBaseColor(heldItem, color);
                }
            }
        } else if ("setMaterialColor".equalsIgnoreCase(args[0])) {
            if (args.length != 2) {
                throw new WrongUsageException(getUsage(sender));
            }

            int color = 0;
            try {
                color = Integer.decode(args[1]);
            } catch (NumberFormatException e) {
                throw new WrongUsageException("Color parameter must be a valid integer. ie. 0xCC3D2E or 123456");
            }

            if (player != null) {
                ItemStack heldItem = player.getHeldItemMainhand();
                if (heldItem.getItem() instanceof CapsuleItem) {
                    CapsuleItem.setMaterialColor(heldItem, color);
                }
            }
        }
        // set the held item as reward (or not)
        else if ("fromHeldCapsule".equalsIgnoreCase(args[0])) {
            if (args.length != 1 && args.length != 2) {
                throw new WrongUsageException(getUsage(sender));
            }

            if (player != null) {
                ItemStack heldItem = player.getHeldItemMainhand();
                if (heldItem.getItem() instanceof CapsuleItem && heldItem.hasTagCompound()) {

                    String outputName;
                    if (args.length == 1) {
                        //noinspection ConstantConditions
                        outputName = heldItem.getTagCompound().getString("label");
                    } else {
                        outputName = args[1];
                    }
                    if (Strings.isNullOrEmpty(outputName)) {
                        throw new WrongUsageException(
                                "/capsule fromHeldCapsule [outputName]. Please label the held capsule or provide an output name to be used for output template.");
                    }

                    // get source template data
                    Pair<CapsuleTemplateManager, CapsuleTemplate> sourcetemplatepair = StructureSaver.getTemplate(heldItem, player.getServerWorld());
                    NBTTagCompound data = new NBTTagCompound();
                    sourcetemplatepair.getRight().writeToNBT(data);

                    // create a destination template
                    ResourceLocation destinationLocation = new ResourceLocation(Config.rewardTemplatesPath + "/" + outputName);
                    CapsuleTemplateManager destManager = StructureSaver.getRewardManager(server);
                    CapsuleTemplate destTemplate = destManager.getTemplate(server, destinationLocation);
                    // write template from source data
                    destTemplate.read(data);
                    destManager.writeTemplate(server, destinationLocation);

                    ItemStack capsule = CapsuleItem.createRewardCapsule(
                            destinationLocation.toString(),
                            CapsuleItem.getBaseColor(heldItem),
                            CapsuleItem.getMaterialColor(heldItem),
                            CapsuleItem.getSize(heldItem),
                            outputName,
                            CapsuleItem.getAuthor(heldItem));
                    giveCapsule(capsule, player);

                }
            }
        } else if ("fromStructure".equalsIgnoreCase(args[0])) {

            if (args.length == 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            StringBuilder structureNameB = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                structureNameB.append(args[i]);
                if (i < args.length - 1) structureNameB.append(" ");
            }

            String structureName = structureNameB.toString().replaceAll(".nbt", "");

            if (player != null && structureName != null && player.getEntityWorld() instanceof WorldServer) {
                // template
                TemplateManager templatemanager = player.getServerWorld().getStructureTemplateManager();
                Template template = templatemanager.get(server, new ResourceLocation(structureName));
                if (template != null) {
                    int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                    if (size % 2 == 1)
                        size++;

                    // get source template data
                    NBTTagCompound data = new NBTTagCompound();
                    template.writeToNBT(data);

                    // create a destination template
                    ResourceLocation destinationLocation = new ResourceLocation(Config.rewardTemplatesPath + "/" + structureName);
                    CapsuleTemplateManager destManager = StructureSaver.getRewardManager(server);
                    CapsuleTemplate destTemplate = destManager.getTemplate(server, destinationLocation);
                    // write template from source data
                    destTemplate.read(data);
                    destManager.writeTemplate(server, destinationLocation);

                    ItemStack capsule = CapsuleItem.createRewardCapsule(
                            destinationLocation.toString(),
                            CapsuleLootEntry.getRandomColor(),
                            CapsuleLootEntry.getRandomColor(),
                            size,
                            structureName,
                            template.getAuthor());
                    giveCapsule(capsule, player);

                } else {
                    throw new CommandException("Structure \"%s\" not found ", structureName);
                }
            }

        } else if ("fromExistingReward".equalsIgnoreCase(args[0])) {

            if (args.length == 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            StringBuilder structureNameB = new StringBuilder();
            for (int i = 1; i < args.length; i++) {
                structureNameB.append(args[i]);
                if (i < args.length - 1) structureNameB.append(" ");
            }

            String structureName = structureNameB.toString().replaceAll(".nbt", "");

            if (player != null && structureName != null && player.getEntityWorld() instanceof WorldServer) {

                String stucturePath = Config.rewardTemplatesPath + "/" + structureName;

                CapsuleTemplate template = StructureSaver.getTemplateForReward(server, stucturePath).getRight();
                if (template != null) {
                    int size = Math.max(template.getSize().getX(), Math.max(template.getSize().getY(), template.getSize().getZ()));
                    if (size % 2 == 1)
                        size++;

                    ItemStack capsule = CapsuleItem.createRewardCapsule(
                            stucturePath,
                            CapsuleLootEntry.getRandomColor(),
                            CapsuleLootEntry.getRandomColor(),
                            size,
                            structureName,
                            template.getAuthor());
                    giveCapsule(capsule, player);

                } else {
                    throw new CommandException("Reward Capsule \"%s\" not found ", structureName);
                }
            }

        }

        // give a random loot capsule
        else if ("giveRandomLoot".equalsIgnoreCase(args[0])) {
            if (args.length != 1 && args.length != 2) {
                throw new WrongUsageException(getUsage(sender));
            }

            if (args.length == 2) {
                player = CommandBase.getPlayer(server, sender, args[1]);
            }
            if (player != null) {
                LootContext.Builder lootcontext$builder = new LootContext.Builder(player.getServerWorld());
                List<ItemStack> loots = new ArrayList<>();
                CapsuleLootTableHook.capsulePool.generateLoot(loots, new Random(), lootcontext$builder.build());
                if (loots.size() <= 0) {
                    player.sendMessage(new TextComponentString("No loot this time !"));
                } else {
                    for (ItemStack loot : loots) {
                        giveCapsule(loot, player);
                    }
                }
            }
        } else if ("reloadLootList".equalsIgnoreCase(args[0])) {
            if (args.length != 1) {
                throw new WrongUsageException(getUsage(sender));
            }
            StructureSaver.loadLootList(server);
        } else {
            throw new WrongUsageException(getUsage(sender));
        }
    }

    private void giveCapsule(ItemStack capsule, EntityPlayer player) {
        EntityItem entity = new EntityItem(player.getEntityWorld(), player.getPosition().getX(), player.getPosition().getY(), player.getPosition().getZ(), capsule);
        entity.setNoPickupDelay();
        entity.onCollideWithPlayer(player);
    }

}
