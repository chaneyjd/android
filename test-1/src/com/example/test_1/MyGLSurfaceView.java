package com.example.test_1;

import android.content.Context;
import android.opengl.GLSurfaceView;

public class MyGLSurfaceView extends GLSurfaceView {

	public MyGLSurfaceView(Context context) {
		super(context);
		setEGLContextClientVersion(2);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		setRenderer(new MyRenderer());
	}

}
