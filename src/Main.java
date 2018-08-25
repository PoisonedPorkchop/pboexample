import com.sun.javafx.geom.Vec2d;
import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.*;
import java.util.Random;

import static org.lwjgl.glfw.Callbacks.*;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.*;
import static org.lwjgl.opengl.ARBVertexBufferObject.GL_READ_ONLY_ARB;
import static org.lwjgl.opengl.ARBVertexBufferObject.glUnmapBufferARB;
import static org.lwjgl.opengl.EXTPixelBufferObject.GL_PIXEL_PACK_BUFFER_EXT;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.GL_DYNAMIC_READ;
import static org.lwjgl.system.MemoryStack.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Main {

	// The window handle
	private long window;
	private int pboID; //variable to store pbo id

	public void run() {
		System.out.println("Hello LWJGL " + Version.getVersion() + "!");

		init();
		loop();

		// Free the window callbacks and destroy the window
		glfwFreeCallbacks(window);
		glfwDestroyWindow(window);

		// Terminate GLFW and free the error callback
		glfwTerminate();
		glfwSetErrorCallback(null).free();
	}

	private void init() {
		// Setup an error callback. The default implementation
		// will print the error message in System.err.
		GLFWErrorCallback.createPrint(System.err).set();

		// Initialize GLFW. Most GLFW functions will not work before doing this.
		if ( !glfwInit() )
			throw new IllegalStateException("Unable to initialize GLFW");

		// Configure GLFW
		glfwDefaultWindowHints(); // optional, the current window hints are already the default
		glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // the window will stay hidden after creation
		glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // the window will be resizable

		// Create the window
		window = glfwCreateWindow(300, 300, "Hello World!", NULL, NULL);
		if ( window == NULL )
			throw new RuntimeException("Failed to create the GLFW window");

		// Setup a key callback. It will be called every time a key is pressed, repeated or released.
		glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
			if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
				glfwSetWindowShouldClose(window, true); // We will detect this in the rendering loop
			else if(key == GLFW_KEY_SPACE && action == GLFW_RELEASE) {
				Random random = new Random();
				float r = random.nextFloat();
				float g = random.nextFloat();
				float b = random.nextFloat();
				glClearColor(r, g, b, 1f);
				System.out.println("Screen color change: (r-" + r * 255f + ", g-" + g * 255f + ",b-" + b * 255f + ")");
			}
			else if(key == GLFW_KEY_R && action == GLFW_RELEASE)
				glClearColor(1f, 0f, 0f, 1f);
			else if(key == GLFW_KEY_G && action == GLFW_RELEASE)
				glClearColor(0f, 1f, 0f, 1f);
			else if(key == GLFW_KEY_B && action == GLFW_RELEASE)
				glClearColor(0f, 0f, 1f, 1f);
		});

		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
			IntBuffer pWidth = stack.mallocInt(1); // int*
			IntBuffer pHeight = stack.mallocInt(1); // int*

			// Get the window size passed to glfwCreateWindow
			glfwGetWindowSize(window, pWidth, pHeight);

			// Get the resolution of the primary monitor
			GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

			// Center the window
			glfwSetWindowPos(
					window,
					(vidmode.width() - pWidth.get(0)) / 2,
					(vidmode.height() - pHeight.get(0)) / 2
			);
		} // the stack frame is popped automatically

		// Make the OpenGL context current
		glfwMakeContextCurrent(window);
		// Enable v-sync
		glfwSwapInterval(1);

		// This line is critical for LWJGL's interoperation with GLFW's
		// OpenGL context, or any context that is managed externally.
		// LWJGL detects the context that is current in the current thread,
		// creates the GLCapabilities instance and makes the OpenGL
		// bindings available for use.
		GL.createCapabilities();

		//THIS IS WHERE THE IMPORTANT CODE STARTS!!!

		pboID = glGenBuffersARB(); //Ask OpenGL for an id for our pixel buffer object
		glBindBufferARB(GL_PIXEL_PACK_BUFFER_EXT, pboID); //Bind the pbo so OpenGL knows we intend to use it
		glBufferDataARB(GL_PIXEL_PACK_BUFFER_EXT, 4, GL_DYNAMIC_READ); //Create the buffer for our pbo
		glBindBufferARB(GL_PIXEL_PACK_BUFFER_EXT, 0); //Unbind the pbo for good measure

		glfwSetMouseButtonCallback(window, new GLFWMouseButtonCallback() {
			@Override
			public void invoke(long window, int button, int action, int mods) {
				if(button == GLFW_MOUSE_BUTTON_1 && action == GLFW_RELEASE) {
					glBindBufferARB(GL_PIXEL_PACK_BUFFER_EXT, pboID);
					Vec2d mousePosition = getMousePosition();
					glReadPixels((int) mousePosition.x, (int) mousePosition.y, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, 0);
					ByteBuffer colorBuffer = glMapBufferARB(GL_PIXEL_PACK_BUFFER_EXT, GL_READ_ONLY_ARB);
					if(colorBuffer == null) {
						System.out.println("Colorbuffer null!");
						return;
					}
					System.out.print("color at x-" + mousePosition.x + ",y-" + mousePosition.y + ": ");
					for (int x = 0; x < colorBuffer.limit(); x++) {
						System.out.print(Byte.toUnsignedInt(colorBuffer.get(x)) + ",");
					}
					System.out.print("\n");
					glUnmapBufferARB(GL_PIXEL_PACK_BUFFER_EXT);
					glBindBufferARB(GL_PIXEL_PACK_BUFFER_EXT, 0);
				}
			}
		});

		//THIS IS WHERE THE IMPORTANT CODE ENDS!!!

		// Make the window visible
		glfwShowWindow(window);
	}

	private void loop() {

		// Set the clear color
		glClearColor(1.0f, 0.0f, 0.0f, 0.0f);

		// Run the rendering loop until the user has attempted to close
		// the window or has pressed the ESCAPE key.
		while ( !glfwWindowShouldClose(window) ) {
			glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // clear the framebuffer

			glfwSwapBuffers(window); // swap the color buffers

			// Poll for window events. The key callback above will only be
			// invoked during this call.
			glfwPollEvents();
		}
	}

	public static void main(String[] args) {
		new Main().run();
	}

	public Vec2d getMousePosition()
	{
		Vec2d mousePos = new Vec2d();
		// Get the thread stack and push a new frame
		try ( MemoryStack stack = stackPush() ) {
			DoubleBuffer mouseX = stack.mallocDouble(1);
			DoubleBuffer mouseY = stack.mallocDouble(1);

			// Get the current cursor position
			glfwGetCursorPos(window, mouseX, mouseY);

			mousePos.x = mouseX.get();
			mousePos.y = mouseY.get();

		} // the stack frame is popped automatically
		return mousePos;
	}

}