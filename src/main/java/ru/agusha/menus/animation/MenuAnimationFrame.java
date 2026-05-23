package ru.agusha.menus.animation;

import java.util.List;
import ru.agusha.menus.menu.SoundEffect;

public record MenuAnimationFrame(int tick, List<MenuAnimationPlacement> placements, SoundEffect sound) {
}
