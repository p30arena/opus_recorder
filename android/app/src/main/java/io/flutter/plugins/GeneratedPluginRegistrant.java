package io.flutter.plugins;

import io.flutter.plugin.common.PluginRegistry;
import com.xiaominfc.opus_recorder.OpusRecorderPlugin;

/**
 * Generated file. Do not edit.
 */
public final class GeneratedPluginRegistrant {
  public static void registerWith(PluginRegistry registry) {
    if (alreadyRegisteredWith(registry)) {
      return;
    }
    OpusRecorderPlugin.registerWith(registry.registrarFor("com.xiaominfc.opus_recorder.OpusRecorderPlugin"));
  }

  private static boolean alreadyRegisteredWith(PluginRegistry registry) {
    final String key = GeneratedPluginRegistrant.class.getCanonicalName();
    if (registry.hasPlugin(key)) {
      return true;
    }
    registry.registrarFor(key);
    return false;
  }
}
