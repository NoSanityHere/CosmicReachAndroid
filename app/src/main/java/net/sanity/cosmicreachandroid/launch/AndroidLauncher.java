package net.sanity.cosmicreachandroid.launch;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration.GLEmulation;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import finalforeach.cosmicreach.BlockGame;
import finalforeach.cosmicreach.gamestates.GameState;
import finalforeach.cosmicreach.gamestates.PauseMenu;
import finalforeach.cosmicreach.lwjgl3.CrashScreen;
import finalforeach.cosmicreach.lwjgl3.StartupHelper;
import finalforeach.cosmicreach.rendering.shaders.GameShader;
import finalforeach.cosmicreach.settings.GraphicsSettings;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

public class AndroidLauncher {
    static long startTime = System.currentTimeMillis();

    public AndroidLauncher() {

    }

    public static void main(String[] args) {
        if (StartupHelper.startNewJvmIfRequired()) {
            final StringBuilder preStartErr = new StringBuilder();
            final PrintStream defaultErr = System.err;

            try {
                System.setErr(new PrintStream(new OutputStream() {
                    public void write(int b) throws IOException {
                        defaultErr.write(b);
                        if (!BlockGame.gameStarted) {
                            preStartErr.append(Character.toChars(b));
                        } else {
                            System.setErr(defaultErr);
                        }

                    }
                }));
                createApplication();
            } catch (Exception var4) {
                throw new RuntimeException("it didnt work", var4);
                //CrashScreen.showCrash(startTime, preStartErr, var4);
            }
        }
    }

    private static Lwjgl3Application createApplication() {
        Lwjgl3Application a = new Lwjgl3Application(new BlockGame(), getDefaultConfiguration());
        return a;
    }

    private static Lwjgl3ApplicationConfiguration getDefaultConfiguration() {
        Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();
        configuration.setTitle("Cosmic Reach");
        configuration.useVsync(GraphicsSettings.vSyncEnabled.getValue());
        configuration.setForegroundFPS(Lwjgl3ApplicationConfiguration.getDisplayMode().refreshRate);
        configuration.setWindowedMode(1024, 576);
        configuration.setWindowIcon("textures/logox128.png", "textures/logox64.png", "textures/logox48.png", "textures/logox32.png", "textures/logox16.png");
        configuration.setWindowListener(new Lwjgl3WindowListener() {
            public void focusLost() {
                BlockGame.isFocused = false;
                if (GameState.currentGameState == GameState.IN_GAME) {
                    GameState.switchToGameState(new PauseMenu(Gdx.input.isCursorCatched()));
                    Gdx.input.setCursorCatched(false);
                }

            }

            public void focusGained() {
                BlockGame.isFocused = true;
            }

            public void created(Lwjgl3Window window) {
            }

            public void iconified(boolean isIconified) {
            }

            public void maximized(boolean isMaximized) {
            }

            public boolean closeRequested() {
                return true;
            }

            public void filesDropped(String[] files) {
            }

            public void refreshRequested() {
            }
        });
        return configuration;
    }
}
