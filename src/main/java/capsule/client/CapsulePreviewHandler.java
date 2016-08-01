package capsule.client;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import capsule.Config;
import capsule.Helpers;
import capsule.blocks.CaptureTESR;
import capsule.blocks.TileEntityCapture;
import capsule.items.CapsuleItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.VertexBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.world.World;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent.Phase;
import net.minecraftforge.fml.common.gameevent.TickEvent.PlayerTickEvent;

public class CapsulePreviewHandler {
	public CapsulePreviewHandler() {
	}
	
	public static int ERROR_COLOR = 0xFC1216;
	
	public static Map<String,List<BlockPos>> currentPreview = new HashMap<String,List<BlockPos>>();
	
	/**
	 * set captureBlock data (clientside only ) when capsule is in hand.
	 * @param event
	 */
	@SubscribeEvent
	public void onLivingUpdateEvent(PlayerTickEvent event) {

		// do something to player every update tick:
		if (event.player instanceof EntityPlayerSP && event.phase.equals(Phase.START)) {
			EntityPlayerSP player = (EntityPlayerSP) event.player;			
			tryPreviewCapture(player, player.getHeldItemMainhand());
		}
	}

	private boolean tryPreviewCapture(EntityPlayerSP player, ItemStack heldItem) {
		// an item is in hand
		if (heldItem != null) {
			Item heldItemItem = heldItem.getItem();
			// it's an empty capsule : show capture zones
			if (heldItemItem instanceof CapsuleItem && (heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY || heldItem.getItemDamage() == CapsuleItem.STATE_EMPTY_ACTIVATED)) {
				CapsuleItem capsule = (CapsuleItem) heldItem.getItem();
				if (heldItem.getTagCompound().hasKey("size")) {
					setCaptureTESizeColor(heldItem.getTagCompound().getInteger("size"), capsule.getColorFromItemstack(heldItem, 0), player.worldObj);
					return true;
				}
			
			} else {
				setCaptureTESizeColor(0, 0, player.worldObj);
			}
		} else {
			setCaptureTESizeColor(0, 0, player.worldObj);
		}
		
		return false;
	}
	
	/**
	 * Render recall preview when deployed capsule in hand
	 * @param event
	 */
	@SubscribeEvent
	public void onWorldRenderLast(RenderWorldLastEvent event) {
		Minecraft mc = Minecraft.getMinecraft();
		
		if(mc.thePlayer != null) {
			tryPreviewRecall(mc.thePlayer.getHeldItemMainhand());
			tryPreviewDeploy(mc.thePlayer, event.getPartialTicks(), mc.thePlayer.getHeldItemMainhand());
		}
	}

	private void tryPreviewDeploy(EntityPlayerSP thePlayer, float partialTicks, ItemStack heldItemMainhand) {

		if (heldItemMainhand != null ) {
			if (heldItemMainhand.getItem() instanceof CapsuleItem
					&& (heldItemMainhand.getItemDamage() == CapsuleItem.STATE_ACTIVATED || heldItemMainhand.getItemDamage() == CapsuleItem.STATE_ONE_USE_ACTIVATED)) {
				
				RayTraceResult rtc = Helpers.rayTracePreview(thePlayer, partialTicks);
				if(rtc != null && rtc.typeOfHit == RayTraceResult.Type.BLOCK)
				{
					BlockPos anchorPos = rtc.getBlockPos();
	
					String structureName = heldItemMainhand.getTagCompound().getString("structureName");
					
					synchronized (CapsulePreviewHandler.currentPreview) {
						if (CapsulePreviewHandler.currentPreview.containsKey(structureName)) {
							
							int extendSize = (getSize(heldItemMainhand) - 1) / 2;
							List<BlockPos> blockspos = CapsulePreviewHandler.currentPreview.get(structureName);
							if(blockspos.isEmpty()){
								blockspos.add(new BlockPos(extendSize,0,extendSize));
							}

							GlStateManager.pushMatrix();
							
							GlStateManager.enableBlend();
							GlStateManager.disableLighting();
							GlStateManager.disableTexture2D();
							GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
					        GlStateManager.disableTexture2D();
					        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
	
							for (BlockPos blockpos : blockspos) {
								
								BlockPos destBlock = blockpos.add(anchorPos).add(-extendSize, 1, -extendSize);
								
								GlStateManager.pushMatrix();
								GlStateManager.translate(anchorPos.getX() + blockpos.getX() - extendSize - TileEntityRendererDispatcher.staticPlayerX, 
										anchorPos.getY() + blockpos.getY() + 1.01 - TileEntityRendererDispatcher.staticPlayerY, 
										anchorPos.getZ() + blockpos.getZ() - extendSize - TileEntityRendererDispatcher.staticPlayerZ);
								
								int color = 0xCCCCCC;
								if(!Config.overridableBlocks.contains(thePlayer.worldObj.getBlockState(destBlock).getBlock())){
									color = 0xaa0000;
								}
								
								drawDeployZone(color);
								
								GlStateManager.popMatrix();
							}
	
							GlStateManager.enableTexture2D();
							GlStateManager.disableBlend();
							GlStateManager.enableLighting();
							
							GlStateManager.popMatrix();
						}
					}
				}
			}
		}
	}
	
	private static AxisAlignedBB boundingBox1 = new AxisAlignedBB(0,0,0, 1, 1, 1);
	private static AxisAlignedBB extboundingBox1 = new AxisAlignedBB(0,0,0, 1, 1, 1);
	
	public static void drawDeployZone(int color) {
		Color c = new Color(color);
		int red = c.getRed();
		int green = c.getGreen();
		int blue = c.getBlue();
		int alpha = 150;

		AxisAlignedBB boundingBox = boundingBox1;
	
		if(color == 0xaa0000){
			GlStateManager.glLineWidth(5);
			boundingBox = extboundingBox1;
		}
		
		Tessellator tessellator = Tessellator.getInstance();
        VertexBuffer vertexbuffer = tessellator.getBuffer();
        vertexbuffer.begin(2, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        vertexbuffer.begin(2, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        vertexbuffer.begin(1, DefaultVertexFormats.POSITION_COLOR);
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.minZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.minY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        vertexbuffer.pos(boundingBox.minX, boundingBox.maxY, boundingBox.maxZ).color(red, green, blue, alpha).endVertex();
        tessellator.draw();
        
        GlStateManager.glLineWidth(1);

	}


	private void tryPreviewRecall(ItemStack heldItem) {
		// an item is in hand
		if (heldItem != null) {
			Item heldItemItem = heldItem.getItem();
			// it's an empty capsule : show capture zones
			if (heldItemItem instanceof CapsuleItem 
					&& heldItem.getItemDamage() == CapsuleItem.STATE_DEPLOYED 
					&& heldItem.getTagCompound().hasKey("spawnPosition")) {
				previewRecall(heldItem);
			}
		}
	}
	
	private int getSize(ItemStack capsule){
		int size = 1;
		if (capsule.getTagCompound().hasKey("size")) {
			size = capsule.getTagCompound().getInteger("size");
		}
		return size;
	}
	
	
	private void previewRecall(ItemStack capsule) {

		NBTTagCompound linkPos = capsule.getTagCompound().getCompoundTag("spawnPosition");
		
		int size = getSize(capsule);
		int extendSize = (size - 1) / 2;
		CapsuleItem capsuleItem = (CapsuleItem)capsule.getItem();
		int color = capsuleItem.getColorFromItemstack(capsule, 0);

		CaptureTESR.drawCaptureZone(
				linkPos.getInteger("x") + extendSize - TileEntityRendererDispatcher.staticPlayerX, 
				linkPos.getInteger("y") - 1 - TileEntityRendererDispatcher.staticPlayerY, 
				linkPos.getInteger("z") + extendSize - TileEntityRendererDispatcher.staticPlayerZ, size,
				extendSize, color);
	}

	private int lastSize = 0;
	private int lastColor = 0;
	private void setCaptureTESizeColor(int size, int color, World worldIn) {
		if(size == lastSize && color == lastColor) return;
		
		// change NBT of all existing TileEntityCapture in the world to make them display the preview zone
		// remember it's client side only
		for (Iterator<TileEntityCapture> iterator = TileEntityCapture.instances.iterator(); iterator.hasNext();) {
			TileEntityCapture te = (TileEntityCapture) iterator.next();
			if (te.getWorld() == worldIn) {
				TileEntityCapture tec = (TileEntityCapture) te;
				tec.getTileData().setInteger("size", size);
				tec.getTileData().setInteger("color", color);
				worldIn.markBlockRangeForRenderUpdate(te.getPos(), te.getPos());
			}
		}
		lastSize = size;
		lastColor = color;
	}
	
	
}
