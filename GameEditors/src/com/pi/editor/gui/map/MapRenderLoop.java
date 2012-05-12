package com.pi.editor.gui.map;

import java.io.File;
import java.io.IOException;

import com.pi.common.database.Sector;
import com.pi.common.database.io.DatabaseIO;
import com.pi.editor.Editor;
import com.pi.editor.gui.EditorPage;
import com.pi.graphics.device.IGraphics;
import com.pi.gui.PIContainer;

public class MapRenderLoop implements EditorPage {
    private final Editor editor;

    // GUIz
    private PIContainer root = new PIContainer();
    private MapViewerObject mapArea;
    private MapEditorObject mapEditor;
    // End GUIz

    public MapRenderLoop(Editor edit) {
	this.editor = edit;

	// Map Area
	mapArea = new MapViewerObject();
	mapArea.setSize(15, 15);

	//Map Editor
	mapEditor = new MapEditorObject(mapArea);
	mapEditor.setSize(500,500);
	
	// Root
	root.setLocation(0, 0);
	root.setSize(editor.getApplet().getWidth(), editor.getApplet()
		.getHeight());
	root.add(mapArea);
	root.add(mapEditor);
	
	root.compile();
    }

    @Override
    public void register() {
	editor.getApplet().addMouseListener(root);
	editor.getApplet().addMouseMotionListener(root);
	editor.getApplet().addMouseWheelListener(root);
	editor.getApplet().addKeyListener(root);
    }

    @Override
    public void unRegister() {
	editor.getApplet().removeMouseListener(root);
	editor.getApplet().removeMouseMotionListener(root);
	editor.getApplet().removeMouseWheelListener(root);
	editor.getApplet().removeKeyListener(root);
    }

    @Override
    public void render(IGraphics graphics) {
	root.render(graphics);
    }
}