package com.example.playertp;

import net.minecraft.client.KeyMapping;
import org.lwjgl.glfw.GLFW;

public class KeyBindings {
    public static final KeyMapping OPEN_PLAYER_LIST = new KeyMapping(
            "key.playertp.open_player_list",
            GLFW.GLFW_KEY_EQUAL,
            "category.playertp"
    );

    public static final KeyMapping OPEN_PERSONAL_POINTS = new KeyMapping(
            "key.playertp.open_personal_points",
            GLFW.GLFW_KEY_P,
            "category.playertp"
    );

    public static final KeyMapping OPEN_PUBLIC_POINTS = new KeyMapping(
            "key.playertp.open_public_points",
            GLFW.GLFW_KEY_L,
            "category.playertp"
    );

    public static final KeyMapping QUICK_ADD_POINT = new KeyMapping(
            "key.playertp.quick_add_point",
            GLFW.GLFW_KEY_MINUS,
            "category.playertp"
    );

    public static final KeyMapping QUICK_TELEPORT = new KeyMapping(
            "key.playertp.quick_teleport",
            GLFW.GLFW_KEY_DOWN,
            "category.playertp"
    );
}