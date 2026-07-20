package buildcraft.builders.client.screen;

import buildcraft.builders.FillerPattern;
import buildcraft.builders.menu.FillerPlannerMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import java.util.Locale;

public final class FillerPlannerScreen extends AbstractContainerScreen<FillerPlannerMenu> {
    private final Button[] patterns=new Button[FillerPattern.values().length];
    private Button invert,vertical,horizontal,hollow,sphereFace,sphereRotation,shapeAxis,shapeRotation,center;
    public FillerPlannerScreen(FillerPlannerMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,112);titleLabelX=8;titleLabelY=6;inventoryLabelY=-100;}
    @Override protected void init(){super.init();for(int i=0;i<patterns.length;i++)patterns[i]=button(FillerPattern.values()[i].name().substring(0,1),10+i,8+i%10*16,22+i/10*16,15);
        invert=button("Invert",38,8,58,40);vertical=button("U",30,50,58,20);horizontal=button("E",31,72,58,20);center=button("C",37,72,58,38);
        hollow=button("Solid",32,94,58,40);sphereFace=button("D",33,136,58,18);sphereRotation=button("R0",34,156,58,18);shapeAxis=button("Y",35,94,58,28);shapeRotation=button("R0",36,124,58,28);}
    private Button button(String text,int id,int x,int y,int width){return addRenderableWidget(Button.builder(Component.literal(text),b->{if(minecraft!=null&&minecraft.gameMode!=null)minecraft.gameMode.handleInventoryButtonClick(menu.containerId,id);}).bounds(leftPos+x,topPos+y,width,14).build());}
    @Override protected void containerTick(){super.containerTick();var d=menu.data();for(int i=0;i<patterns.length;i++)patterns[i].active=d.pattern().ordinal()!=i;invert.setMessage(Component.literal(d.inverted()?"Normal":"Invert"));
        boolean stepped=d.pattern()==FillerPattern.PYRAMID||d.pattern()==FillerPattern.STAIRS;vertical.visible=stepped;vertical.setMessage(Component.literal(d.verticalDirection()==net.minecraft.core.Direction.UP?"U":"D"));horizontal.visible=d.pattern()==FillerPattern.STAIRS;horizontal.setMessage(Component.literal(initial(d.horizontalDirection().getName())));center.visible=d.pattern()==FillerPattern.PYRAMID;center.setMessage(Component.literal(new String[]{"NW","N","NE","W","C","E","SW","S","SE"}[d.pyramidCenter()]));
        boolean sphere=d.pattern().isSphere(),shape=d.pattern().isShape2d();hollow.visible=sphere||shape;hollow.setMessage(Component.literal(d.hollow()?"Hollow":"Solid"));sphereFace.visible=d.pattern().openFaces()>0;sphereFace.setMessage(Component.literal(initial(d.sphereFacing().getName())));sphereRotation.visible=d.pattern().openFaces()>1;sphereRotation.setMessage(Component.literal("R"+d.sphereRotation()));shapeAxis.visible=shape;shapeAxis.setMessage(Component.literal(d.shapeAxis().getName().toUpperCase(Locale.ROOT)));shapeRotation.visible=shape;shapeRotation.setMessage(Component.literal("R"+d.shapeRotation()));}
    private static String initial(String value){return value.substring(0,1).toUpperCase(Locale.ROOT);}
    @Override public void extractBackground(GuiGraphicsExtractor graphics,int mouseX,int mouseY,float partialTick){graphics.fill(leftPos,topPos,leftPos+imageWidth,topPos+imageHeight,0xDD202830);}
}
