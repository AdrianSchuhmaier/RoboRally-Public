/* Copyright © 2012-present Lightweight Java Game Library All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer. Redistributions in binary
 * form must reproduce the above copyright notice, this list of conditions and
 * the following disclaimer in the documentation and/or other materials provided
 * with the distribution. Neither the name Lightweight Java Game Library nor the
 * names of its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission. THIS SOFTWARE
 * IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS
 * OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO
 * EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. */

package core.texture;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import static org.lwjgl.stb.STBImage.*;

/**
 * This Class can load textures into OpenGL with the matching buffers.
 * 
 * @author Adrian Schuhmaier
 */
public class TextureLoader {

    private static HashMap<String, Texture> textures = new HashMap<>();
    
    /**
     * clears all loaded textures
     */
    public static void clear(){
        textures.clear();
    }
    
    /**
     * Loads a texture from storage
     * 
     * @param path
     *            of the image file
     * @return textureID
     */
    public static Texture loadTexture(String path) {
        if(textures.keySet().contains(path)){
            return textures.get(path);
        }
        ByteBuffer imageBuffer = getTextureBuffer(path);

        IntBuffer w = BufferUtils.createIntBuffer(1);
        IntBuffer h = BufferUtils.createIntBuffer(1);
        IntBuffer comp = BufferUtils.createIntBuffer(1);

        if (!stbi_info_from_memory(imageBuffer, w, h, comp))
            throw new RuntimeException("Failed to read image information: " + stbi_failure_reason());

        System.out.println("Image source: " + path);
        System.out.println("Image dim: " + w.get(0) + ", " + h.get(0));
        System.out.println("Image components: " + comp.get(0));
        System.out.println("Image HDR: " + stbi_is_hdr_from_memory(imageBuffer));

        ByteBuffer image = stbi_load_from_memory(imageBuffer, w, h, comp, 0);
        if (image == null)
            throw new RuntimeException("Failed to load image: " + stbi_failure_reason());

        int texID = GL11.glGenTextures();
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texID);
        // System.out.println("Image texID: " + texID);

        if (comp.get(0) == 3) {
            if ((w.get(0) & 3) != 0)
                GL11.glPixelStorei(GL11.GL_UNPACK_ALIGNMENT, 2 - (w.get(0) & 1));
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGB, w.get(0), h.get(0), 0, GL11.GL_RGB,
                    GL11.GL_UNSIGNED_BYTE, image);
        } else {
            GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA, w.get(0), h.get(0), 0, GL11.GL_RGBA,
                    GL11.GL_UNSIGNED_BYTE, image);

            GL11.glEnable(GL11.GL_BLEND);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        }

        GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL11.GL_REPEAT);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, 0);

        stbi_image_free(image);
        Texture texture = new Texture(texID);
        textures.put(path, texture);
        return texture;
    }

    /**
     * Returns a ByteBuffer of the image data at the given path.
     * 
     * @param path
     *            of the file
     * @return buffer
     */
    public static ByteBuffer getTextureBuffer(String path) {

        ByteBuffer buffer;

        Path filePath = Paths.get(path);

        if (Files.isReadable(filePath)) {

            try (SeekableByteChannel byteChannel = Files.newByteChannel(filePath)) {

                buffer = BufferUtils.createByteBuffer((int) byteChannel.size() + 1);
                while (byteChannel.read(buffer) != -1)
                    ;

                buffer.flip();

                return buffer;

            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            System.err.println(path + " is not readable.");
            System.exit(-1);
        }

        return null;
    }
}
