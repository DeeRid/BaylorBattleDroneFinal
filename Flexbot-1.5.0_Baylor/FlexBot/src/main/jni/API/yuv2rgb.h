#pragma once
void gl_initialize();
void gl_uninitialize();

//����yuv��buffer
void gl_set_framebuffer(const char* buffer, int buffersize, int width, int height); 
void gl_render_frame();



