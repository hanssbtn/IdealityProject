package com.ideality.idealityproject

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.GLES11Ext
import android.opengl.GLES30

object OpenGL {

    fun getEGLErrorCodeName(errorCode: Int): String {
        return when (errorCode) {
            /** Function succeeded. */
            0x3000 -> "EGL_SUCCESS"

            /** EGL is not or could not be initialized, for the specified display. */
            0x3001 -> "EGL_NOT_INITIALIZED"

            /**
             * EGL cannot access a requested resource (for example, a context
             * is bound in another thread).
             **/
            0x3002 -> "EGL_BAD_ACCESS"

            /** EGL failed to allocate resources for the requested operation. */
            0x3003 -> "EGL_BAD_ALLOC"

            /**
             * An unrecognized attribute or attribute value was passed in an
             * attribute list.
             */
            0x3004 -> "EGL_BAD_ATTRIBUTE"

            /** An EGLConfig argument does not name a valid EGLConfig. */
            0x3005 -> "EGL_BAD_CONFIG"

            /** An EGLContext argument does not name a valid EGLContext. */
            0x3006 -> "EGL_BAD_CONTEXT"

            /**
             * The current surface of the calling thread is a window, pbuffer,
             * or pixmap that is no longer valid.
             */
            0x3007 -> "EGL_BAD_CURRENT_SURFACE"

            /** An EGLDisplay argument does not name a valid EGLDisplay. */
            0x3008 -> "EGL_BAD_DISPLAY"

            /**
             * Arguments are inconsistent; for example, an otherwise valid
             * context requires buffers (e.g. depth or stencil) not allocated by
             * an otherwise valid surface.
             */
            0x3009 -> "EGL_BAD_MATCH"

            /**
             * An EGLNativePixmapType argument does not refer to a valid
             * native pixmap.
             */
            0x300A -> "EGL_BAD_NATIVE_PIXMAP"

            /**
             * An EGLNativeWindowType argument does not refer to a valid
             * native window.
             */
            0x300B -> "EGL_BAD_NATIVE_WINDOW"

            /** One or more argument values are invalid. */
            0x300C -> "EGL_BAD_PARAMETER"

            /**
             * An EGLSurface argument does not name a valid surface (window,
             * pbuffer, or pixmap) configured for rendering.
             */
            0x300D -> "EGL_BAD_SURFACE"

            /**
             * A power management event has occurred. The application must
             * destroy all contexts and reinitialise client API state and objects to
             * continue rendering
             */
            0x300E -> "EGL_CONTEXT_LOST"
            else -> "UNKNOWN_ERROR"
        }
    }

    private const val EGL_OPENGL_ES3_BIT = 0x40

    fun createEGLContext(): EGLContext {
        return createEGLContext(EGL14.EGL_NO_CONTEXT)!!
    }

    fun createEGLContext(shareContext: EGLContext?): EGLContext? {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        EGL14.eglInitialize(display, null, 0, null, 0)
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfig = intArrayOf(0)
        val attribs = intArrayOf(EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES3_BIT, EGL14.EGL_NONE)
        EGL14.eglChooseConfig(display, attribs, 0, configs, 0, 1, numConfig, 0)
        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        val context = EGL14.eglCreateContext(
            display,
            configs[0], shareContext, contextAttribs, 0
        )
        val surfaceAttribs = intArrayOf(
            EGL14.EGL_WIDTH, 1,
            EGL14.EGL_HEIGHT, 1,
            EGL14.EGL_NONE
        )
        val surface = EGL14.eglCreatePbufferSurface(
            display,
            configs[0], surfaceAttribs, 0
        )
        check(
            EGL14.eglMakeCurrent(
                display,
                surface,
                surface,
                context
            )
        ) { "Error making GL context. ${getEGLErrorCodeName(EGL14.eglGetError())}" }
        return context
    }

    fun createExternalTextureID(): Int {
        val textures = IntArray(1)
        GLES30.glGenTextures(1, textures, 0)
        val result = textures[0]
        val textureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES
        GLES30.glBindTexture(textureTarget, result)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(textureTarget, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        return result
    }

    fun destroyEGLContext(context: EGLContext?) {
        val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        check(EGL14.eglDestroyContext(display, context)) { "Error destroying GL context. ${getEGLErrorCodeName(EGL14.eglGetError())}" }
    }
}

fun EGLContext.destroy() = OpenGL.destroyEGLContext(this)